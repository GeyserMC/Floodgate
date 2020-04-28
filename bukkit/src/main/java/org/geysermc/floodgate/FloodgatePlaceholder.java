package org.geysermc.floodgate;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class FloodgatePlaceholder extends PlaceholderExpansion {

    private BukkitPlugin plugin;

    public FloodgatePlaceholder(BukkitPlugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier(){
        return "floodgate";
    }

    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier){
        if(player == null){
            return "";
        }

        switch (identifier) {
            case "device":
                return getPlayerDeviceString(player);

            case "locale":
            case "locale_upper":
                if (FloodgateAPI.isBedrockPlayer(player.getUniqueId())) {
                    FloodgatePlayer floodgatePlayer = FloodgateAPI.getPlayer(player.getUniqueId());
                    boolean upper = identifier.endsWith("_upper");
                    return plugin.getConfiguration().getPlaceholders().getLocale().getFound().replace("%locale%", upper ? floodgatePlayer.getLanguageCode().toUpperCase() : floodgatePlayer.getLanguageCode().toLowerCase());
                } else {
                    return plugin.getConfiguration().getPlaceholders().getLocale().getNone();
                }

            case "version":
                if (FloodgateAPI.isBedrockPlayer(player.getUniqueId())) {
                    FloodgatePlayer floodgatePlayer = FloodgateAPI.getPlayer(player.getUniqueId());
                    return plugin.getConfiguration().getPlaceholders().getVersion().getFound().replace("%version%", floodgatePlayer.getVersion());
                } else {
                    return plugin.getConfiguration().getPlaceholders().getVersion().getNone();
                }

            case "username":
                if (FloodgateAPI.isBedrockPlayer(player.getUniqueId())) {
                    FloodgatePlayer floodgatePlayer = FloodgateAPI.getPlayer(player.getUniqueId());
                    return plugin.getConfiguration().getPlaceholders().getXboxUsername().getFound().replace("%username%", floodgatePlayer.getUsername());
                } else {
                    return plugin.getConfiguration().getPlaceholders().getXboxUsername().getNone();
                }

            case "xuid":
                if (FloodgateAPI.isBedrockPlayer(player.getUniqueId())) {
                    FloodgatePlayer floodgatePlayer = FloodgateAPI.getPlayer(player.getUniqueId());
                    return plugin.getConfiguration().getPlaceholders().getXboxXuid().getFound().replace("%xuid%", floodgatePlayer.getXuid());
                } else {
                    return plugin.getConfiguration().getPlaceholders().getXboxXuid().getNone();
                }
        }

        return null;
    }

    /**
     * Get the device string from config for the specified player
     *
     * @param player The player to get the device for
     * @return The formatted device string from config
     */
    private String getPlayerDeviceString(Player player) {
        if (FloodgateAPI.isBedrockPlayer(player.getUniqueId())) {
            if (plugin.getConfiguration().getPlaceholders().isSpecificDeviceDescriptors()) {
                FloodgatePlayer floodgatePlayer = FloodgateAPI.getPlayer(player.getUniqueId());
                switch (floodgatePlayer.getDeviceOS()) {
                    case ANDROID:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getAndroid().replace("&", "§");
                    case IOS:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getIOS().replace("&", "§");
                    case OSX:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getOSX().replace("&", "§");
                    case FIREOS:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getFireos().replace("&", "§");
                    case GEARVR:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getGearVR().replace("&", "§");
                    case HOLOLENS:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getHololens().replace("&", "§");
                    case WIN10:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getWin10().replace("&", "§");
                    case WIN32:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getWin32().replace("&", "§");
                    case DEDICATED:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getDedicated().replace("&", "§");
                    case ORBIS:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getOrbis().replace("&", "§");
                    case NX:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getNX().replace("&", "§");
                    case SWITCH:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getNintendoSwitch().replace("&", "§");
                    case XBOX_ONE:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getXboxOne().replace("&", "§");
                    default:
                        return plugin.getConfiguration().getPlaceholders().getDevice().getUnknown().replace("&", "§");
                }
            }else{
                return plugin.getConfiguration().getPlaceholders().getDevice().getGeneric().replace("&", "§");
            }
        } else {
            return plugin.getConfiguration().getPlaceholders().getDevice().getJava().replace("&", "§");
        }
    }

}
