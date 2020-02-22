package org.geysermc.floodgate.injector;

import com.velocitypowered.api.proxy.Player;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.Modifier;
import javassist.bytecode.Descriptor;
import lombok.Getter;
import org.geysermc.floodgate.FloodgateAPI;
import org.geysermc.floodgate.VelocityPlugin;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import static org.geysermc.floodgate.util.BedrockData.FLOODGATE_IDENTIFIER;

public class VelocityInjector {
    @Getter private static boolean injected = false;

    public static boolean inject(VelocityPlugin plugin) throws Exception {
        if (isInjected()) return true;
        // Tnx Velocity for making it difficult x)
        // But I made my way in >:)
        // Please note that doing this is not recommended

        // This method will edit the source code of MinecraftConnection to allow us to edit any outgoing packet.

        ClassPool classPool = ClassPool.getDefault();
        CtClass ctClass = classPool.get("com.velocitypowered.proxy.connection.MinecraftConnection");
        ctClass.defrost();

        // create dataConsumer field
        CtClass biConsumerClass = classPool.get("java.util.function.BiConsumer");
        CtField dataConsumer = new CtField(biConsumerClass, "dataConsumer", ctClass);
        dataConsumer.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        ctClass.addField(dataConsumer);

        // override write and delayedWrite in MinecraftConnection
        String voidObjectDescriptor = Descriptor.ofMethod(CtClass.voidType, new CtClass[] { classPool.get("java.lang.Object") });
        String codeToInsert =
                "if (msg != null && dataConsumer != null && msg instanceof com.velocitypowered.proxy.protocol.MinecraftPacket) {\n" +
                "   dataConsumer.accept(this, (com.velocitypowered.proxy.protocol.MinecraftPacket)msg);\n" +
                "}\n";

        ctClass.getMethod("write", voidObjectDescriptor).insertAt(1, codeToInsert);
        ctClass.getMethod("delayedWrite", voidObjectDescriptor).insertAt(1, codeToInsert);

        Class<?> clazz = ctClass.toClass();

        // The most important part is done,
        // So now we call initReflection to let us use getPrefixedClass
        plugin.initReflection();

        // handshake handle stuff
        Class<?> handshakePacket = ReflectionUtil.getPrefixedClass("protocol.packet.Handshake");
        Field serverAddress = ReflectionUtil.getField(handshakePacket, "serverAddress");

        Field sessionHandlerField = ReflectionUtil.getField(clazz, "sessionHandler");
        Class<?> loginHandler = ReflectionUtil.getPrefixedClass("connection.backend.LoginSessionHandler");

        Field serverConnField = ReflectionUtil.getField(loginHandler, "serverConn");

        Class<?> serverConnection = ReflectionUtil.getPrefixedClass("connection.backend.VelocityServerConnection");
        Method playerGetMethod = ReflectionUtil.getMethod(serverConnection, "getPlayer");
        assert playerGetMethod != null;

        // create and set our custom made Consumer

        BiConsumer<Object, Object> biConsumer = (minecraftConnection, packet) -> {
            // This consumer is only called when the server is sending data (outgoing).
            // So when we get a handshake packet it'll be Velocity trying to connect to a server.
            // And that is exactly what we need, to edit the outgoing handshake packet to include Floodgate data
            if (plugin.getConfig().isSendFloodgateData()) {
                try {
                    if (handshakePacket.isInstance(packet)) {
                        String address = ReflectionUtil.getCastedValue(packet, serverAddress, String.class);

                        Object sessionHandler = ReflectionUtil.getValue(minecraftConnection, sessionHandlerField);
                        if (loginHandler.isInstance(sessionHandler)) {
                            Object serverConn = ReflectionUtil.getValue(sessionHandler, serverConnField);
                            Player connectedPlayer = ReflectionUtil.invokeCasted(serverConn, playerGetMethod, Player.class);

                            String encryptedData = FloodgateAPI.getEncryptedData(connectedPlayer.getUniqueId());
                            if (encryptedData == null) return; // we know enough, this is not a Floodgate player

                            String[] splitted = address.split("\0");
                            String remaining = address.substring(splitted[0].length());
                            ReflectionUtil.setValue(packet, serverAddress, splitted[0] + '\0' + FLOODGATE_IDENTIFIER + '\0' + encryptedData + remaining);
                            // keep the same system as we have on Bungeecord. Our data is before the Bungee data
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Class<?> minecraftConnection = ReflectionUtil.getPrefixedClass("connection.MinecraftConnection");
        ReflectionUtil.setValue(null, ReflectionUtil.getField(minecraftConnection, "dataConsumer"), biConsumer);
        return true;
    }
}
