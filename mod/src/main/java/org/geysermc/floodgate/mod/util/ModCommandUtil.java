package org.geysermc.floodgate.mod.util;

import com.mojang.authlib.GameProfile;
import io.micronaut.context.BeanProvider;
import jakarta.inject.Inject;
import lombok.Setter;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.util.LanguageManager;
import org.incendo.cloud.CommandManager;

import java.util.Collection;
import java.util.UUID;

public final class ModCommandUtil extends CommandUtil {
    private UserAudience console;
    @Setter
    private CommandManager<UserAudience> commandManager;

    BeanProvider<MinecraftServer> server;
    BeanProvider<MinecraftServerAudiences> audience;

    @Inject
    public ModCommandUtil(
            LanguageManager manager,
            GeyserApiBase api,
            BeanProvider<MinecraftServer> server,
            BeanProvider<MinecraftServerAudiences> audience) {
        super(manager, api);
        this.server = server;
        this.audience = audience;
    }

    @Override
    public @NonNull UserAudience getUserAudience(final @NonNull Object sourceObj) {
        if (!(sourceObj instanceof CommandSourceStack stack)) {
            throw new IllegalArgumentException("Source has to be a CommandSourceStack");
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
        ServerPlayer player = server.get().getPlayerList().getPlayer(uuid);
        return player != null ? player : uuid;
    }

    @Override
    public Object getPlayerByUsername(@NonNull String username) {
        ServerPlayer player = server.get().getPlayerList().getPlayerByName(username);
        return player != null ? player : username;
    }

    @Override
    protected Collection<?> getOnlinePlayers() {
        return server.get().getPlayerList().getPlayers();
    }

    @Override
    public boolean hasPermission(Object source, String permission) {
        return commandManager.hasPermission(getUserAudience(source), permission);
    }

    @Override
    public void sendMessage(Object target, net.kyori.adventure.text.Component message) {
        CommandSourceStack commandSource = (CommandSourceStack) target;
        if (commandSource.getEntity() instanceof ServerPlayer) {
            server.get().execute(() -> ((ServerPlayer) commandSource.getEntity())
                    .displayClientMessage(audience.get().asNative(message), false));
        }
    }

    @Override
    public void kickPlayer(Object o, net.kyori.adventure.text.Component message) {
        if (o instanceof ServerPlayer player) {
            player.connection.disconnect(audience.get().asNative(message));
        }
    }

    @Override
    public boolean whitelistPlayer(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        server.get().getPlayerList().getWhiteList().add(new UserWhiteListEntry(profile));
        return true;
    }

    @Override
    public boolean removePlayerFromWhitelist(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        server.get().getPlayerList().getWhiteList().remove(profile);
        return true;
    }
}
