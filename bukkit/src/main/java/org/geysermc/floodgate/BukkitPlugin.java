package org.geysermc.floodgate;

import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.command.LinkAccountCommand;
import org.geysermc.floodgate.command.UnlinkAccountCommand;
import org.geysermc.floodgate.injector.BukkitInjector;
import org.geysermc.floodgate.util.CommandUtil;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.util.logging.Level;

public class BukkitPlugin extends JavaPlugin {
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
        CommandUtil commandUtil = new CommandUtil();
        getCommand(CommandUtil.LINK_ACCOUNT_COMMAND).setExecutor(new LinkAccountCommand(playerLink, commandUtil));
        getCommand(CommandUtil.UNLINK_ACCOUNT_COMMAND).setExecutor(new UnlinkAccountCommand(playerLink, commandUtil));
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FloodgatePlayer player = FloodgateAPI.getPlayer(event.getPlayer());
        if (player != null) {
            FloodgateAPI.players.remove(player.getCorrectUniqueId());
            System.out.println("Removed " + player.getUsername() + " " + event.getPlayer().getUniqueId());
        }
    }
}
