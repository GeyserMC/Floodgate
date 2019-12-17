package org.geysermc.floodgate;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.injector.BukkitInjector;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.util.logging.Level;

public class BukkitPlugin extends JavaPlugin {
    @Getter private static BukkitPlugin instance;
    @Getter private FloodgateConfig configuration;

    @Override
    public void onLoad() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        ReflectionUtil.setServerVersion(getServer().getClass().getPackage().getName().split("\\.")[3]);

        configuration = FloodgateConfig.load(getLogger(), getDataFolder().toPath().resolve("config.yml"));
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
    }

    @Override
    public void onDisable() {
        try {
            if (!BukkitInjector.removeInjection()) getLogger().severe("Failed to remove the injection!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to remove the injection!", e);
        }
    }
}
