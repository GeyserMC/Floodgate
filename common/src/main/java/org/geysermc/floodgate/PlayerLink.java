package org.geysermc.floodgate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.link.SQLitePlayerLink;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class PlayerLink {
    @Getter private static PlayerLink instance;
    @Getter private static boolean enabled;
    @Getter private static long verifyLinkTimeout;
    @Getter private static boolean allowLinking;

    @Getter private final ExecutorService executorService = Executors.newFixedThreadPool(11);
    @Getter private Logger logger;

    protected abstract void load(Path dataFolder);

    public abstract CompletableFuture<LinkedPlayer> getLinkedPlayer(UUID bedrockId);
    public abstract CompletableFuture<Boolean> isLinkedPlayer(UUID bedrockId);

    public abstract CompletableFuture<Void> linkPlayer(UUID bedrockId, UUID uuid, String username);
    public abstract CompletableFuture<Void> unlinkPlayer(UUID uuid);

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

    /**
     * Shutdown the thread pool and invalidates the PlayerLink instance
     */
    public void stop() {
        instance = null;
        executorService.shutdown();
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
        SQLITE(SQLitePlayerLink::new);

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

    public static <U> CompletableFuture<U> failedFuture(Throwable exception) {
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }
}
