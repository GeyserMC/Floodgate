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

        if(identifier.equals("device")){
            return getPlayerDeviceString(player);
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
            FloodgatePlayer floodgatePlayer = FloodgateAPI.getPlayer(player.getUniqueId());
            switch (floodgatePlayer.getDeviceOS()) {
                case ANDROID:
                    return plugin.getConfiguration().getPlaceholders().getAndroid().replace("&", "§");
                case IOS:
                    return plugin.getConfiguration().getPlaceholders().getIOS().replace("&", "§");
                case OSX:
                    return plugin.getConfiguration().getPlaceholders().getOSX().replace("&", "§");
                case FIREOS:
                    return plugin.getConfiguration().getPlaceholders().getFireos().replace("&", "§");
                case GEARVR:
                    return plugin.getConfiguration().getPlaceholders().getGearVR().replace("&", "§");
                case HOLOLENS:
                    return plugin.getConfiguration().getPlaceholders().getHololens().replace("&", "§");
                case WIN10:
                    return plugin.getConfiguration().getPlaceholders().getWin10().replace("&", "§");
                case WIN32:
                    return plugin.getConfiguration().getPlaceholders().getWin32().replace("&", "§");
                case DEDICATED:
                    return plugin.getConfiguration().getPlaceholders().getDedicated().replace("&", "§");
                case ORBIS:
                    return plugin.getConfiguration().getPlaceholders().getOrbis().replace("&", "§");
                case NX:
                    return plugin.getConfiguration().getPlaceholders().getNX().replace("&", "§");
                case SWITCH:
                    return plugin.getConfiguration().getPlaceholders().getNintendoSwitch().replace("&", "§");
                case XBOX_ONE:
                    return plugin.getConfiguration().getPlaceholders().getXboxOne().replace("&", "§");
                default:
                    return plugin.getConfiguration().getPlaceholders().getUnknown().replace("&", "§");
            }
        } else {
            return plugin.getConfiguration().getPlaceholders().getJava().replace("&", "§");
        }
    }

}
