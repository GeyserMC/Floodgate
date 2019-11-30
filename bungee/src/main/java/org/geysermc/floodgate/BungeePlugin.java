package org.geysermc.floodgate;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.EncryptionUtil;
import org.geysermc.floodgate.util.ReflectionUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BungeePlugin extends Plugin implements Listener {
    @Getter private static BungeePlugin instance;
    @Getter private FloodgateConfig config = null;

    @Override
    public void onLoad() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        config = FloodgateConfig.load(getLogger(), getDataFolder().toPath().resolve("config.yml"));
    }

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPreLogin(PreLoginEvent event) {
        // only need to check when server is in online mode :D
        if (ProxyServer.getInstance().getConfig().isOnlineMode()) {
            event.registerIntent(this);

            getProxy().getScheduler().runAsync(this, () -> {
                String[] data = ((InitialHandler) event.getConnection()).getExtraDataInHandshake().split("\0");
                System.out.println(data.length);

                if (data.length == 4 && data[1].equals("Geyser-Floodgate")) {
                    try {
                        BedrockData bedrockData = EncryptionUtil.decryptToInstance(
                                config.getPrivateKey(),
                                data[2] + '\0' + data[3]
                        );

                        if (bedrockData.getDataLength() != BedrockData.EXPECTED_LENGTH) {
                            event.setCancelReason(String.format(
                                    config.getMessages().getInvalidArgumentsLength(),
                                    BedrockData.EXPECTED_LENGTH, bedrockData.getDataLength()
                            ));
                            event.setCancelled(true);
                            return;
                        }

                        FloodgatePlayer player = new FloodgatePlayer(bedrockData);
                        FloodgateAPI.players.put(player.getJavaUniqueId(), player);

                        event.getConnection().setOnlineMode(false);
                        event.getConnection().setUniqueId(player.getJavaUniqueId());
                        ReflectionUtil.setValue(event.getConnection(), "name", player.getJavaUsername());

                        System.out.println("Added " + player.getUsername() + " " + player.getJavaUniqueId());
                    } catch (NullPointerException | NoSuchPaddingException | NoSuchAlgorithmException |
                            InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                        event.setCancelReason(config.getMessages().getInvalidKey());
                        event.setCancelled(true);
                        e.printStackTrace();
                    }
                }
                event.completeIntent(this);
            });
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        FloodgatePlayer player = BungeeFloodgateAPI.getPlayerByConnection(event.getPlayer().getPendingConnection());
        if (player != null) {
            FloodgateAPI.players.remove(player.getJavaUniqueId());
            System.out.println("Removed " + player.getUsername() + " " + event.getPlayer().getUniqueId());
        }
    }
}
