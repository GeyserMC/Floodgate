package org.geysermc.floodgate.mod.util;

import com.mojang.authlib.GameProfile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.platform.command.CommandUtil;
import org.geysermc.floodgate.core.util.LanguageManager;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

@Singleton
public abstract class ModCommandUtil extends CommandUtil {

    private final MinecraftServer server;

    private final LanguageManager manager;

    private UserAudience.ConsoleAudience console;

    @Inject
    public ModCommandUtil(
            LanguageManager manager,
            MinecraftServer server,
            GeyserApiBase api
    ) {
        super(manager, api);
        this.server = server;
        this.manager = manager;
    }
    @Override
    public @NonNull UserAudience getUserAudience(@NonNull Object source) {
        if (!(source instanceof CommandSourceStack)) {
            throw new IllegalArgumentException("Source has to be a CommandSourceStack!");
        }
        CommandSourceStack sourceStack = (CommandSourceStack) source;

        if (!sourceStack.isPlayer()) {
            if (console != null) {
                return console;
            }
            return console = new UserAudience.ConsoleAudience(sourceStack, this);
        }

        ServerPlayer player = sourceStack.getPlayer();
        assert player != null;
        UUID uuid = player.getUUID();
        String username = player.getGameProfile().getName();
        String locale;
        locale = player.clientInformation().language();

        return new UserAudience.PlayerAudience(uuid, username, locale, sourceStack, this, true);
    }

    @Override
    protected String getUsernameFromSource(@NonNull Object source) {
        return ((CommandSourceStack) source).getTextName();
    }

    @Override
    protected UUID getUuidFromSource(@NonNull Object source) {
        return ((ServerPlayer) source).getUUID();
    }

    @Override
    protected Collection<?> getOnlinePlayers() {
        return server.getPlayerList().getPlayers();
    }

    @Override
    public Object getPlayerByUuid(@NonNull UUID uuid) {
        try {
            return server.getPlayerList().getPlayer(uuid);
        } catch (Exception e) {
            return uuid;
        }
    }

    @Override
    public Object getPlayerByUsername(@NonNull String username) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        return player != null ? player : username;
    }

    @Override
    public abstract boolean hasPermission(Object player, String permission);

    @Override
    public void sendMessage(Object target, String message) {
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.literal(message), false);
        } else {
            ((CommandSourceStack) target).sendSystemMessage(Component.literal(message));
        }
    }

    @Override
    public void kickPlayer(Object player, String message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.disconnect(Component.literal(message));
        }
    }

    @Override
    public boolean whitelistPlayer(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        if (server.getPlayerList().isWhiteListed(profile)) {
            return false;
        }
        server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(profile));
        return true;
    }

    @Override
    public boolean removePlayerFromWhitelist(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        if (!server.getPlayerList().isWhiteListed(profile)) {
            return false;
        }
        server.getPlayerList().getWhiteList().remove(profile);
        return true;
    }
}
