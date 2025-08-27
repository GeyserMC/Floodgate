package org.geysermc.floodgate.mod.util;

import com.mojang.authlib.GameProfile;
import lombok.Setter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.player.UserAudience;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.geysermc.floodgate.mod.MinecraftServerHolder;
import org.incendo.cloud.CommandManager;

import java.util.Collection;
import java.util.UUID;

public final class ModCommandUtil extends CommandUtil {
    private final FloodgateLogger logger;
    private UserAudience console;
    @Setter
    private CommandManager<UserAudience> commandManager;

    public ModCommandUtil(LanguageManager manager, FloodgateApi api, FloodgateLogger logger) {
        super(manager, api);
        this.logger = logger;
    }

    @Override
    public @NonNull UserAudience getUserAudience(final @NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSourceStack stack)) {
            throw new IllegalArgumentException();
        }
        if (stack.getEntity() == null) {
            if (console != null) {
                return console;
            }
            return console = new UserAudience.ConsoleAudience(stack, this);
        }
        ServerPlayer player = stack.getPlayer();
        //Locale locale = PlayerLocales.locale(player);
        return new UserAudience.PlayerAudience(player.getUUID(), player.getGameProfile().getName(), "en_US",
                stack, this, true);
    }

    @Override
    protected String getUsernameFromSource(@NonNull Object source) {
        return ((ServerPlayer) source).getGameProfile().getName();
    }

    @Override
    protected UUID getUuidFromSource(@NonNull Object source) {
        return ((ServerPlayer) source).getUUID();
    }

    @Override
    public Object getPlayerByUuid(@NonNull UUID uuid) {
        ServerPlayer player = MinecraftServerHolder.get().getPlayerList().getPlayer(uuid);
        return player != null ? player : uuid;
    }

    @Override
    public Object getPlayerByUsername(@NonNull String username) {
        ServerPlayer player = MinecraftServerHolder.get().getPlayerList().getPlayerByName(username);
        return player != null ? player : username;
    }

    @Override
    protected Collection<?> getOnlinePlayers() {
        return MinecraftServerHolder.get().getPlayerList().getPlayers();
    }

    @Override
    public boolean hasPermission(Object source, String permission) {
        return commandManager.hasPermission(getUserAudience(source), permission);
    }

    @Override
    public void sendMessage(Object target, String message) {
        CommandSourceStack commandSource = (CommandSourceStack) target;
        if (commandSource.getEntity() instanceof ServerPlayer) {
            MinecraftServerHolder.get().execute(() -> ((ServerPlayer) commandSource.getEntity())
                    .displayClientMessage(Component.literal(message), false));
        } else {
            // Console?
            logger.info(message);
        }
    }

    @Override
    public void kickPlayer(Object o, String message) {
        if (o instanceof ServerPlayer player) {
            player.connection.disconnect(Component.literal(message));
        }
    }

    @Override
    public boolean whitelistPlayer(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        MinecraftServerHolder.get().getPlayerList().getWhiteList().add(new UserWhiteListEntry(profile));
        return true;
    }

    @Override
    public boolean removePlayerFromWhitelist(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        MinecraftServerHolder.get().getPlayerList().getWhiteList().remove(profile);
        return true;
    }
}
