package org.geysermc.floodgate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.link.SQLiteImpl;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class PlayerLink {
    @Getter private static PlayerLink instance;
    @Getter private static boolean enabled;
    @Getter private static long verifyLinkTimeout;
    @Getter private static boolean allowLinking;

    @Getter private Logger logger;

    protected abstract void load(Path dataFolder);

    public abstract LinkedPlayer getLinkedPlayer(UUID bedrockId);
    public abstract boolean isLinkedPlayer(UUID bedrockId);

    public abstract boolean linkPlayer(UUID bedrockId, UUID uuid, String username);
    public abstract boolean unlinkPlayer(UUID uuid);

    public static PlayerLink initialize(Logger logger, Path dataFolder, FloodgateConfig config) {
        if (PlayerLink.instance == null) {
            FloodgateConfig.PlayerLinkConfig linkConfig = config.getPlayerLink();
            ImplementationType type = ImplementationType.getByName(linkConfig.getType());
            if (type == null) {
                logger.severe("Failed to find an implementation for type: "+linkConfig.getType());
                return null;
            }
            PlayerLink.instance = type.instanceSupplier.get();
            PlayerLink.enabled = linkConfig.isEnabled();
            PlayerLink.verifyLinkTimeout = linkConfig.getLinkCodeTimeout();
            PlayerLink.allowLinking = linkConfig.isAllowLinking();
            instance.logger = logger;
            instance.load(dataFolder);
            return instance;
        }
        return instance;
    }

    protected LinkedPlayer createLinkedPlayer(String javaUsername, UUID javaUniqueId, UUID bedrockId) {
        return new LinkedPlayer(javaUsername, javaUniqueId, bedrockId);
    }

    public static boolean isEnabledAndAllowed() {
        return enabled && allowLinking;
    }

    @AllArgsConstructor
    @Getter
    public enum ImplementationType {
        SQLITE(SQLiteImpl::new);

        private Supplier<? extends PlayerLink> instanceSupplier;

        public static final ImplementationType[] VALUES = values();

        public static ImplementationType getByName(String implementationName) {
            String uppercase = implementationName.toUpperCase();
            for (ImplementationType type : VALUES) {
                if (type.name().equals(uppercase)) {
                    return type;
                }
            }
            return null;
        }
    }
}
