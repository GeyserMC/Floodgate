package org.geysermc.floodgate;

import lombok.Getter;
import org.geysermc.floodgate.util.BedrockData;
import org.geysermc.floodgate.util.DeviceOS;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Getter
public class FloodgatePlayer {
    /**
     * Bedrock version of the client
     */
    private String version;
    /**
     * Bedrock username (full version)
     */
    private String username;
    /**
     * Bedrock username with the given identifier<br>
     * This won't be null if it is an {@link LinkedPlayer LinkedPlayer}, but it isn't used
     */
    private String javaUsername;
    /**
     * The Unique Identifier used at the server to identify the bedrock client.<br>
     * Note that this field is only used when the player is not an {@link LinkedPlayer LinkedPlayer}
     */
    private UUID javaUniqueId;
    /**
     * The Xbox Unique Identifier
     */
    private String xuid;
    /**
     * The operation system of the bedrock client
     */
    private DeviceOS deviceOS;
    /**
     * The language code of the bedrock client
     */
    private String languageCode;
    /**
     * The InputMode of the bedrock client
     */
    private int inputMode;
    /**
     * The LinkedPlayer object if the player is linked to Java account.
     */
    private LinkedPlayer linkedPlayer;

    FloodgatePlayer(BedrockData data, String prefix, boolean replaceSpaces) {
        xuid = data.getXuid();
        version = data.getVersion();
        username = data.getUsername();
        javaUsername = prefix + data.getUsername().substring(0, Math.min(data.getUsername().length(), 16 - prefix.length()));
        if (replaceSpaces) {
            javaUsername = javaUsername.replaceAll(" ", "_");
        }
        deviceOS = DeviceOS.getById(data.getDeviceId());
        languageCode = data.getLanguageCode();
        inputMode = data.getInputMode();
        javaUniqueId = AbstractFloodgateAPI.createJavaPlayerId(Long.parseLong(data.getXuid()));
        // every implementation (Bukkit, Bungee and Velocity) all run this async,
        // so we can block this thread
        if (PlayerLink.isEnabledAndAllowed()) {
            linkedPlayer = fetchLinkedPlayer();
        }
    }

    public UUID getCorrectUniqueId() {
        return linkedPlayer != null ? linkedPlayer.javaUniqueId : javaUniqueId;
    }

    public String getCorrectUsername() {
        return linkedPlayer != null ? linkedPlayer.javaUsername : javaUsername;
    }

    /**
     * This will return the LinkedPlayer object if the player is linked.<br>
     * Please note that the LinkedPlayer will be loaded (sync) when used for the first time.<br>
     * This method also checks if linking is enabled
     * @return LinkedPlayer or null if the player isn't linked or linking isn't enabled
     * @see #fetchLinkedPlayerAsync() for the async alternative
     */
    public LinkedPlayer fetchLinkedPlayer() {
        if (!PlayerLink.isEnabledAndAllowed()) return null;
        try {
            return PlayerLink.getInstance().getLinkedPlayer(javaUniqueId).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * async alternative to {@link #fetchLinkedPlayer()}
     * @see #fetchLinkedPlayer() for the sync versionnon
     */
    public CompletableFuture<LinkedPlayer> fetchLinkedPlayerAsync() {
        return PlayerLink.isEnabledAndAllowed() ?
                PlayerLink.getInstance().getLinkedPlayer(javaUniqueId) :
                CompletableFuture.completedFuture(null);
    }
}
