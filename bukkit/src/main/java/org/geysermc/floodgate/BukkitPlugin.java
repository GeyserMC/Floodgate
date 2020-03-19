package org.geysermc.floodgate;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.injector.BukkitInjector;
import org.geysermc.floodgate.util.ReflectionUtil;
import org.geysermc.floodgate.PlayerLink;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

import java.util.UUID;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

public class BukkitPlugin extends JavaPlugin {
    @Getter private static BukkitPlugin instance;
    @Getter private FloodgateConfig configuration;

    static final Map<String, LinkRequest> activeLinkRequests = new HashMap<>(); // Maps Java usernames to LinkRequest objects

    @Override
    public void onLoad() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        ReflectionUtil.setPrefix("net.minecraft.server." + getServer().getClass().getPackage().getName().split("\\.")[3]);

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

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {
        if (command.getName().equalsIgnoreCase("linkaccount")) {
          UUID uuid = ((Player) sender).getUniqueId();
          String username = sender.getName();

          if (PlayerLink.enabled && PlayerLink.allowLinking) {
            Random rand = new Random();
            if (PlayerLink.isLinkedPlayer(uuid)) {
              sender.sendMessage("§cYour account is already linked!§f");
              sender.sendMessage("§cIf you want to link to a different account, run §6/unlinkaccount§c and try again.§f");
            }
            if (AbstractFloodgateAPI.isBedrockPlayer(uuid)) {
              if (args.length != 2) {
                sender.sendMessage("§cStart the process from Java! Usage: /linkaccount <gamertag>§f"); // Print the command usage message for Java
              }
              if (activeLinkRequests.containsKey(args[0])) {
                LinkRequest request = activeLinkRequests.get(args[0]);
                System.out.println(request.linkCode + args[1]);
                System.out.println(request.linkCode == args[1]);
                if (request.linkCode.equals(args[1])) {
                  activeLinkRequests.remove(args[0]); // Delete the request, whether it has expired or is successful
                  if (request.isExpired()) {
                    sender.sendMessage("§cCode expired! Run §6/linkaccount§c again on Java.§f");
                  }
                  if (PlayerLink.linkPlayer(uuid, request.javaUniqueId, request.javaUsername)) {
                    sender.sendMessage("§aLink successful!§f");
                  } else {
                    sender.sendMessage("§cError while linking!§f");
                  }
                } else {
                  sender.sendMessage("§cInvalid code! Try running §6/linkaccount§c again on Java.§f");
                }

              } else {
                sender.sendMessage("§cThis player has not requested an account link! Please log in on Java and request one with §6/linkaccount§c.§f");
              }
            } else {
              if (args.length != 1) {
                sender.sendMessage("§cUsage: /linkaccount <gamertag>§f");
              }
              String code = String.format("%04d", rand.nextInt(10000));
              String bedrockUsername = args[0];
              String messageUsername;
              if (bedrockUsername.charAt(0) == '*') {
                messageUsername = args[0].substring(1, args[0].length());
              } else {
                bedrockUsername = "*" + bedrockUsername; // Add a * to match the Floodgate username if it doesn't have one
                messageUsername = args[0];
              }
              bedrockUsername = bedrockUsername.substring(0, Math.min(bedrockUsername.length(), 16)); // Cut the name down to match the Floodgate username
              activeLinkRequests.put(username, new LinkRequest(username, uuid, code, args[0]));
              sender.sendMessage("§aLog in as " + messageUsername + " on Bedrock and run §6/linkaccount " + username + " " + code + "§f");
            }
            sender.sendMessage("§cUnknown error§f");
          } else {
            sender.sendMessage("§cLinking is not enabled on this server§f");
          }
          return true;
        } else if (command.getName().equalsIgnoreCase("unlinkaccount")) {
          sender.sendMessage(PlayerLink.unlinkPlayer(((Player)sender).getUniqueId()));
          return true;
        }
        return false;
    }
}
