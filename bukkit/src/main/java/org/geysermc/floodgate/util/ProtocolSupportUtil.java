package org.geysermc.floodgate.util;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.geysermc.floodgate.FloodgateAPI;
import protocolsupport.api.events.PlayerLoginStartEvent;

public class ProtocolSupportUtil implements Listener {

    public static boolean isProtocolSupport = false;

    public static void checkForProtocolSupport(Plugin floodgate) {
        if (Bukkit.getServer().getPluginManager().getPlugin("ProtocolSupport") != null) {
            isProtocolSupport = true;
            Bukkit.getServer().getPluginManager().registerEvents(new ProtocolSupportUtil(), floodgate);
        }
    }

    /**
     * Force ProtocolSupport to handle Bedrock users in offline mode, otherwise they kick us
     * @param event ProtocolSupport's player login start event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginStart(final PlayerLoginStartEvent event) {
        if (event.getConnection().getProfile().getUUID() == null) return; // A normal player that doesn't have a UUID set
        if (FloodgateAPI.isBedrockPlayer(event.getConnection().getProfile().getUUID())) {
            event.setOnlineMode(false); // Otherwise ProtocolSupport attempts to connect with online mode
        }
    }

}
