package org.geysermc.floodgate;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.injector.BukkitInjector;
import org.geysermc.floodgate.link.SQLiteImpl;
import org.geysermc.floodgate.util.ReflectionUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

public class BukkitPlugin extends JavaPlugin {
    @Getter private static BukkitPlugin instance;
    @Getter private FloodgateConfig configuration;
    @Getter private PlayerLink playerLink;

    static final Map<String, LinkRequest> activeLinkRequests = new HashMap<>(); // Maps Java usernames to LinkRequest objects

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
    }

    @Override
    public void onDisable() {
        try {
            if (!BukkitInjector.removeInjection()) getLogger().severe("Failed to remove the injection!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to remove the injection!", e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FloodgatePlayer player = FloodgateAPI.getPlayer(event.getPlayer());
        if (player != null) {
            FloodgateAPI.players.remove(player.getCorrectUniqueId());
            System.out.println("Removed " + player.getUsername() + " " + event.getPlayer().getUniqueId());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Please head over to your Minecraft Account and link from there.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("linkaccount")) {
            UUID uuid = ((Player) sender).getUniqueId();
            String username = sender.getName();

            if (PlayerLink.isEnabledAndAllowed()) {
                if (playerLink.isLinkedPlayer(uuid)) {
                    sendFormat(sender, "&cYour account is already linked!");
                    sendFormat(sender, "&cIf you want to link to a different account, run &6/unlinkaccount&c and try it again");
                    return true;
                }
                // when the player is a Java player
                if (!AbstractFloodgateAPI.isBedrockPlayer(uuid)) {
                    if (args.length != 1) {
                        sendFormat(sender, "&cUsage: /linkaccount <gamertag>");
                        return true;
                    }
                    String code = String.format("%04d", new Random().nextInt(10000));
                    String bedrockUsername = args[0];
                    activeLinkRequests.put(username, new LinkRequest(username, uuid, code, bedrockUsername));
                    sendFormat(sender, "&aLog in as " + bedrockUsername + " on Bedrock and run &6/linkaccount " + username + " " + code);
                    return true;
                }
                // when the player is a Bedrock player
                if (args.length != 2) {
                    sendFormat(sender, "&cStart the process from Java! Usage: /linkaccount <gamertag>");
                    return true;
                }
                String javaUsername = args[0];
                String code = args[1];
                LinkRequest request = activeLinkRequests.getOrDefault(javaUsername, null);
                if (request != null && request.checkGamerTag(AbstractFloodgateAPI.getPlayer(uuid))) {
                    if (request.linkCode.equals(code)) {
                        activeLinkRequests.remove(javaUsername); // Delete the request, whether it has expired or is successful
                        if (request.isExpired()) {
                            sendFormat(sender, "&cThe code you entered is expired! Run &6/linkaccount&c again on your Java account");
                            return true;
                        }
                        if (playerLink.linkPlayer(uuid, request.javaUniqueId, request.javaUsername)) {
                            sendFormat(sender, "&aYou are successfully linked to " + request.javaUsername + "!");
                            return true;
                        }
                        sendFormat(sender, "&cAn error occurred while linking. Please check the console");
                        return true;
                    }
                    sendFormat(sender, "&cInvalid code! Please check your code or run the &6/linkaccount&c command again on your Java account");
                    return true;
                }
                sendFormat(sender, "&cThis player has not requested an account link! Please log in on Java and request one with &6/linkaccount");
                return true;
            }
            sendFormat(sender, "&cLinking is not enabled on this server");
            return true;
        }

        if (command.getName().equalsIgnoreCase("unlinkaccount")) {
            if (SQLiteImpl.isEnabledAndAllowed()) {
                sendFormat(sender, playerLink.unlinkPlayer(((Player) sender).getUniqueId()) ?
                        "&aUnlink successful!" :
                        "&cAn error occurred while unlinking player! Please check the console"
                );
            } else {
                sendFormat(sender, "&cLinking is not enabled on this server");
            }
        }
        return true;
    }

    private String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void sendFormat(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }
}
