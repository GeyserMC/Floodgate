package org.geysermc.floodgate;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.command.LinkAccountCommand;
import org.geysermc.floodgate.command.UnlinkAccountCommand;
import org.geysermc.floodgate.injector.BukkitInjector;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.util.logging.Level;

public class BukkitPlugin extends JavaPlugin implements Listener {
    @Getter private static BukkitPlugin instance;
    @Getter private FloodgateConfig configuration;
    @Getter private PlayerLink playerLink;

    @Override
    public void onLoad() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        ReflectionUtil.setPrefix("net.minecraft.server." + getServer().getClass().getPackage().getName().split("\\.")[3]);

        configuration = FloodgateConfig.load(getLogger(), getDataFolder().toPath().resolve("config.yml"));
        playerLink = PlayerLink.initialize(getLogger(), getDataFolder().toPath(), configuration);
    }

    @Override
    public void onEnable() {
        try {
            if (!BukkitInjector.inject()) getLogger().severe("Failed to inject the packet listener!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to inject the packet listener!", e);
        } finally {
            if (!BukkitInjector.isInjected()) {
                getServer().getPluginManager().disablePlugin(this);
            }
        }
        CommandUtil commandUtil = new CommandUtil(this);
        getCommand(CommandUtil.LINK_ACCOUNT_COMMAND).setExecutor(new LinkAccountCommand(playerLink, commandUtil));
        getCommand(CommandUtil.UNLINK_ACCOUNT_COMMAND).setExecutor(new UnlinkAccountCommand(playerLink, commandUtil));

        // Register the plugin as an event listener to we get join and leave events
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            if (!BukkitInjector.removeInjection()) getLogger().severe("Failed to remove the injection!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to remove the injection!", e);
        }
        playerLink.stop();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            FloodgateAPI.removePlayer(event.getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
//        System.out.println("Is bedrock? " + FloodgateAPI.isBedrockPlayer(event.getPlayer()));
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            FloodgateAPI.removePlayer(event.getPlayer().getUniqueId());
            return;
        }
        // if there was another player with the same uuid online,
        // he has been disconnected by now
        FloodgatePlayer player = FloodgateAPI.getPlayer(event.getPlayer());
        if (player != null) player.setLogin(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (FloodgateAPI.removePlayer(player.getUniqueId())) {
            System.out.println("Removed Bedrock player who was logged in as " + player.getName() + " " + event.getPlayer().getUniqueId());
        }
    }
}
