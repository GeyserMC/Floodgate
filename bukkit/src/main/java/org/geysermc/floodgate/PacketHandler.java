package org.geysermc.floodgate;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.geysermc.floodgate.HandshakeHandler.HandshakeResult;
import org.geysermc.floodgate.injector.BukkitInjector;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

import static org.geysermc.floodgate.util.ReflectionUtil.*;

@RequiredArgsConstructor
public class PacketHandler extends SimpleChannelInboundHandler<Object> {
    private static BukkitPlugin plugin = BukkitPlugin.getInstance();
    private static HandshakeHandler handshakeHandler;

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
    private FloodgatePlayer fPlayer;
    private boolean bungee;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object packet) throws Exception {
        boolean isHandhake = handshakePacketClass.isInstance(packet);
        boolean isLogin = loginStartPacketClass.isInstance(packet);

        try {
            if (isHandhake) {
                networkManager = ctx.channel().pipeline().get("packet_handler");

                HandshakeResult result = handshakeHandler.handle(getCastedValue(packet, hostField, String.class));
                switch (result.getResultType()) {
                    case SUCCESS:
                        break;
                    case INVALID_DATA_LENGTH:
                        plugin.getLogger().info(String.format(
                                plugin.getConfiguration().getMessages().getInvalidArgumentsLength(),
                                BedrockData.EXPECTED_LENGTH, result.getBedrockData().getDataLength()
                        ));
                        ctx.close();
                    default: // only continue when SUCCESS
                        return;
                }

                fPlayer = result.getFloodgatePlayer();
                BedrockData bedrockData = result.getBedrockData();
                String[] data = result.getHandshakeData();

                if (bungee = (data.length == 6 || data.length == 7)) {
                    setValue(packet, hostField, data[0] + '\0' +
                            bedrockData.getIp() + '\0' + fPlayer.getCorrectUniqueId() +
                            (data.length == 7 ? '\0' + data[6] : "")
                    );
                } else {
                    // Use a spoofedUUID for initUUID (just like Bungeecord)
                    setValue(networkManager, "spoofedUUID", fPlayer.getCorrectUniqueId());
                    // Use the player his IP for stuff instead of Geyser his IP
                    SocketAddress newAddress = new InetSocketAddress(
                            bedrockData.getIp(),
                            ((InetSocketAddress) ctx.channel().remoteAddress()).getPort()
                    );
                    setValue(networkManager, getFieldOfType(networkManagerClass, SocketAddress.class, false), newAddress);
                }
                plugin.getLogger().info("Added " + fPlayer.getCorrectUsername() + " " + fPlayer.getCorrectUniqueId());
            } else if (isLogin) {
                if (!bungee) {
                    // we have to fake the offline player cycle
                    Object loginListener = packetListenerField.get(networkManager);

                    // Set the player his GameProfile
                    Object gameProfile = gameProfileConstructor.newInstance(fPlayer.getCorrectUniqueId(), fPlayer.getCorrectUsername());
                    setValue(loginListener, gameProfileField, gameProfile);

                    initUUIDMethod.invoke(loginListener); // LoginListener#initUUID
                    fireEventsMethod.invoke(loginHandlerConstructor.newInstance(loginListener)); // new LoginHandler().fireEvents();
                    setValue(loginListener, protocolStateField, readyToAcceptState); // LoginLister#protocolState = READY_TO_ACCEPT
                    // The tick of LoginListener will do the rest
                }
            }
        } finally {
            // don't let the packet through if the packet is the login packet
            // because we want to skip the login cycle
            if (!isLogin) ctx.fireChannelRead(packet);

            if (isHandhake && bungee || isLogin && !bungee || fPlayer == null) {
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
        handshakeHandler = new HandshakeHandler(plugin.getConfiguration().getPrivateKey(), false, plugin.getConfiguration().getUsernamePrefix(), plugin.getConfiguration().isReplaceSpaces());

        networkManagerClass = getPrefixedClass("NetworkManager");
        loginStartPacketClass = getPrefixedClass("PacketLoginInStart");

        gameProfileClass = ReflectionUtil.getClass("com.mojang.authlib.GameProfile");
        assert gameProfileClass != null;
        try {
            gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        handshakePacketClass = getPrefixedClass("PacketHandshakingInSetProtocol");
        assert handshakePacketClass != null;
        hostField = getFieldOfType(handshakePacketClass, String.class, true);

        loginListenerClass = getPrefixedClass("LoginListener");
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

        Class<?> packetListenerClass = getPrefixedClass("PacketListener");
        packetListenerField = getFieldOfType(networkManagerClass, packetListenerClass, true);

        loginHandlerClass = getPrefixedClass("LoginListener$LoginHandler");
        assert loginHandlerClass != null;
        try {
            loginHandlerConstructor = makeAccessible(loginHandlerClass.getDeclaredConstructor(loginListenerClass));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        fireEventsMethod = getMethod(loginHandlerClass, "fireEvents");
    }
}
