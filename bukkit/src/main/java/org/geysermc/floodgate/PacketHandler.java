package org.geysermc.floodgate;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.injector.BukkitInjector;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.EncryptionUtil;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.UUID;

import static org.geysermc.floodgate.util.BedrockData.FLOODGATE_IDENTIFIER;
import static org.geysermc.floodgate.util.ReflectionUtil.*;

@RequiredArgsConstructor
public class PacketHandler extends MessageToMessageDecoder<Object> {
    private static BukkitPlugin plugin = BukkitPlugin.getInstance();
    private static Class<?> networkManagerClass;
    private static Class<?> handshakePacketClass;
    private static Field hostField;

    private static Class<?> gameProfileClass;
    private static Constructor<?> gameProfileConstructor;
    private static Field gameProfileField;

    private static Class<?> loginStartPacketClass;
    private static Class<?> loginListenerClass;
    private static Method initUUIDMethod;

    private static Class<?> loginHandlerClass;
    private static Constructor<?> loginHandlerConstructor;
    private static Method fireEventsMethod;

    private static Field packetListenerField;
    private static Field protocolStateField;
    private static Object readyToAcceptState;

    /* per player stuff */

    private final ChannelFuture future;
    private Object networkManager;
    private FloodgatePlayer floodgatePlayer;
    private boolean bungee;

    @Override
    protected void decode(ChannelHandlerContext ctx, Object packet, List<Object> out) throws Exception {
        plugin.getLogger().finest("Injector step 6");
        boolean isHandhake = handshakePacketClass.isInstance(packet);
        boolean isLogin = loginStartPacketClass.isInstance(packet);
        try {
            if (isHandhake) {
                plugin.getLogger().finest("Injector step 7");
                networkManager = ctx.channel().pipeline().get("packet_handler");

                String[] host = getCastedValue(packet, hostField, String.class).split("\0");
                bungee = host.length == 6 || host.length == 7; // 4 = normal | 6, 7 = bungee

                if ((host.length == 4 || bungee) && host[1].equals(FLOODGATE_IDENTIFIER)) {
                    BedrockData bedrockData = EncryptionUtil.decryptBedrockData(
                            plugin.getConfiguration().getPrivateKey(),
                            host[2] + '\0' + host[3]
                    );

                    if (bedrockData.getDataLength() != BedrockData.EXPECTED_LENGTH) {
                        plugin.getLogger().info(String.format(
                                plugin.getConfiguration().getMessages().getInvalidArgumentsLength(),
                                BedrockData.EXPECTED_LENGTH, bedrockData.getDataLength()
                        ));
                        ctx.close(); //todo add option to see disconnect message?
                    }

                    FloodgatePlayer player = floodgatePlayer = new FloodgatePlayer(bedrockData);
                    FloodgateAPI.players.put(player.getJavaUniqueId(), player);

                    if (bungee) {
                        setValue(packet, hostField, host[0] + '\0' +
                                bedrockData.getIp() + '\0' + player.getJavaUniqueId() +
                                (host.length == 7 ? '\0' + host[6] : "")
                        );
                    } else {
                        // Use a spoofedUUID for initUUID (just like Bungeecord)
                        setValue(networkManager, "spoofedUUID", player.getJavaUniqueId());
                        // Use the player his IP for stuff instead of Geyser his IP
                        SocketAddress newAddress = InetSocketAddress.createUnresolved(
                                bedrockData.getIp(),
                                ((InetSocketAddress)ctx.channel().remoteAddress()).getPort()
                        );
                        setValue(networkManager, getFieldOfType(networkManagerClass, SocketAddress.class, false), newAddress);
                    }
                    plugin.getLogger().info("Added " + player.getJavaUsername() + " " + player.getJavaUniqueId());
                }
                out.add(packet);
            } else if (isLogin) {
                plugin.getLogger().finest("Injector step 8");
                if (!bungee) {
                    // we have to fake the offline player cycle
                    Object loginListener = packetListenerField.get(networkManager);

                    // Set the player his GameProfile
                    Object gameProfile = gameProfileConstructor.newInstance(floodgatePlayer.getJavaUniqueId(), floodgatePlayer.getJavaUsername());
                    setValue(loginListener, gameProfileField, gameProfile);

                    initUUIDMethod.invoke(loginListener); // LoginListener#initUUID
                    fireEventsMethod.invoke(loginHandlerConstructor.newInstance(loginListener)); // new LoginHandler().fireEvents();
                    setValue(loginListener, protocolStateField, readyToAcceptState); // LoginLister#protocolState = READY_TO_ACCEPT
                    // The tick of LoginListener will do the rest
                }
                // out.add(packet); don't let this packet through as we want to skip the login cycle
            }
        } finally {
            if (isHandhake && bungee || isLogin && !bungee) {
                // remove the injection of the client because we're finished
                BukkitInjector.removeInjectedClient(future, ctx.channel());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }

    static {
        networkManagerClass = getNMSClass("NetworkManager");
        loginStartPacketClass = getNMSClass("PacketLoginInStart");

        gameProfileClass = ReflectionUtil.getClass("com.mojang.authlib.GameProfile");
        assert gameProfileClass != null;
        try {
            gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        handshakePacketClass = getNMSClass("PacketHandshakingInSetProtocol");
        assert handshakePacketClass != null;
        hostField = getFieldOfType(handshakePacketClass, String.class, true);

        loginListenerClass = getNMSClass("LoginListener");
        assert loginListenerClass != null;
        gameProfileField = getFieldOfType(loginListenerClass, gameProfileClass, true);
        initUUIDMethod = getMethod(loginListenerClass, "initUUID");

        for (Field field : loginListenerClass.getDeclaredFields()) {
            if (field.getType().isEnum()) {
                protocolStateField = field;
            }
        }
        assert protocolStateField != null;

        Enum<?>[] protocolStates = (Enum<?>[]) protocolStateField.getType().getEnumConstants();
        for (Enum<?> protocolState : protocolStates) {
            if (protocolState.name().equals("READY_TO_ACCEPT")) {
                readyToAcceptState = protocolState;
            }
        }

        Class<?> packetListenerClass = getNMSClass("PacketListener");
        packetListenerField = getFieldOfType(networkManagerClass, packetListenerClass, true);

        loginHandlerClass = getNMSClass("LoginListener$LoginHandler");
        assert loginHandlerClass != null;
        try {
            loginHandlerConstructor = makeAccessible(loginHandlerClass.getDeclaredConstructor(loginListenerClass));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        fireEventsMethod = getMethod(loginHandlerClass, "fireEvents");
    }
}
