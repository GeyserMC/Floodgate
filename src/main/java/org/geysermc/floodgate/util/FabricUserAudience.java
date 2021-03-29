package org.geysermc.floodgate.util;

import lombok.RequiredArgsConstructor;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.player.UserAudience;

import java.util.UUID;

@RequiredArgsConstructor
public class FabricUserAudience implements UserAudience, ForwardingAudience.Single {
    private final UUID uuid;
    private final String locale;
    private final ServerCommandSource source;
    private final FabricCommandUtil commandUtil;

    @Override
    public @NonNull Audience audience() {
        return commandUtil.getAdventure().audience(source);
    }

    @Override
    public @NonNull UUID uuid() {
        return uuid;
    }

    @Override
    public @NonNull String username() {
        if (source == null) {
            return "";
        }

        return source.getName();
    }

    @Override
    public @NonNull String locale() {
        return locale;
    }

    @Override
    public @NonNull ServerCommandSource source() {
        return source;
    }

    @Override
    public boolean hasPermission(@NonNull String permission) {
        return Permissions.check(source, permission);
    }

    @Override
    public void sendMessage(@NonNull Identity source, @NonNull Component message, @NonNull MessageType type) {
        commandUtil.getAdventure().audience(this.source).sendMessage(message);
    }

    @Override
    public void sendMessage(CommandMessage message, Object... args) {
        commandUtil.sendMessage(this.source, this.locale, message, args);
    }

    @Override
    public void disconnect(@NonNull Component reason) {
        if (source.getEntity() instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) source.getEntity()).networkHandler.disconnect(
                    commandUtil.getAdventure().toNative(reason)
            );
        }
    }

    @Override
    public void disconnect(CommandMessage message, Object... args) {
        if (source.getEntity() instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) source.getEntity()).networkHandler.disconnect(
                    commandUtil.translateAndTransform(this.locale, message, args)
            );
        }
    }

    /**
     * Used whenever a name has been explicitly defined for us. Most helpful in offline players.
     */
    public static final class NamedFabricUserAudience extends FabricUserAudience implements PlayerAudience {
        private final String name;
        private final boolean online;

        public NamedFabricUserAudience(
                String name,
                UUID uuid,
                String locale,
                ServerCommandSource source,
                FabricCommandUtil commandUtil,
                boolean online) {
            super(uuid, locale, source, commandUtil);
            this.name = name;
            this.online = online;
        }

        @Override
        public @NonNull String username() {
            return name;
        }

        @Override
        public boolean online() {
            return online;
        }
    }
}
