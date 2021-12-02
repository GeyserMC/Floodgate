package org.geysermc.floodgate.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.platform.command.TranslatableMessage;
import org.geysermc.floodgate.platform.command.CommandUtil;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudienceArgument;

import java.util.*;

@RequiredArgsConstructor
public final class FabricCommandUtil implements CommandUtil {
    // Static because commands *need* to be initialized before the server is available
    // Otherwise it would be a class variable
    private static MinecraftServer SERVER;
    // This one also requires the server so it's bundled in
    private static FabricServerAudiences ADVENTURE;

    @Getter private final FloodgateApi api;
    @Getter private final FloodgateLogger logger;
    @Getter private final LanguageManager manager;

    @Override
    public @NonNull UserAudience getAudience(@NonNull Object source) {
        if (!(source instanceof CommandSourceStack commandSource)) {
            throw new RuntimeException();
        }

        if (commandSource.getEntity() instanceof ServerPlayer) {
            return getAudience0((ServerPlayer) commandSource.getEntity());
        }

        return new FabricUserAudience(null, manager.getDefaultLocale(), commandSource, this);
    }

    @Override
    public UserAudience getAudienceByUuid(@NonNull UUID uuid) {
        ServerPlayer player = SERVER.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return getAudience0(player);
        }
        return getOfflineAudienceByUuid(uuid);
    }

    @Override
    public UserAudience getAudienceByUsername(@NonNull String username) {
        ServerPlayer player = SERVER.getPlayerList().getPlayerByName(username);
        if (player != null) {
            return getAudience0(player);
        }
        return getOfflineAudienceByUsername(username);
    }

    private FabricUserAudience getAudience0(ServerPlayer player) {
        // Apparently can be null even if Javadocs say otherwise
        Locale locale = PlayerLocales.locale(player);
        return new FabricUserAudience.NamedFabricUserAudience(
                player.getName().getString(),
                player.getUUID(), locale != null ?
                locale.getLanguage().toLowerCase(Locale.ROOT) + "_" + locale.getCountry().toUpperCase(Locale.ROOT) :
                manager.getDefaultLocale(), player.createCommandSourceStack(), this, true);
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUuid(@NonNull UUID uuid) {
        return new FabricUserAudience(uuid, null, null, this);
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUsername(@NonNull String username) {
        UUID uuid = null;
        Optional<GameProfile> profile = SERVER.getProfileCache().get(username);
        if (profile.isPresent()) {
            uuid = profile.get().getId();
        }
        return new FabricUserAudience.NamedFabricUserAudience(username, uuid, username, null, this, false);
    }

    @Override
    public @NonNull Collection<String> getOnlineUsernames(UserAudienceArgument.@NonNull PlayerType limitTo) {
        List<ServerPlayer> players = SERVER.getPlayerList().getPlayers();

        Collection<String> usernames = new ArrayList<>();
        switch (limitTo) {
            case ALL_PLAYERS:
                for (ServerPlayer player : players) {
                    usernames.add(player.getName().getString());
                }
                break;
            case ONLY_JAVA:
                for (ServerPlayer player : players) {
                    if (!api.isFloodgatePlayer(player.getUUID())) {
                        usernames.add(player.getName().getString());
                    }
                }
                break;
            case ONLY_BEDROCK:
                for (ServerPlayer player : players) {
                    if (api.isFloodgatePlayer(player.getUUID())) {
                        usernames.add(player.getName().getString());
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unknown PlayerType");
        }
        return usernames;
    }

    @Override
    public boolean hasPermission(Object player, String permission) {
        return Permissions.check((Entity) player, permission);
    }

    @Override
    public Collection<Object> getOnlinePlayersWithPermission(String permission) {
        List<Object> players = new ArrayList<>();
        for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
            if (hasPermission(player, permission)) {
                players.add(player);
            }
        }
        return players;
    }

    @Override
    public void sendMessage(Object player, String locale, TranslatableMessage message, Object... args) {
        CommandSourceStack commandSource = (CommandSourceStack) player;
        if (commandSource.getEntity() instanceof ServerPlayer) {
            SERVER.execute(() -> ((ServerPlayer) commandSource.getEntity())
                    .displayClientMessage(translateAndTransform(locale, message, args), false));
        } else {
            // Console?
            logger.info(message.translateMessage(manager, locale, args));
        }
    }

    @Override
    public void sendMessage(Object target, String message) {
        CommandSourceStack commandSource = (CommandSourceStack) target;
        if (commandSource.getEntity() instanceof ServerPlayer) {
            SERVER.execute(() -> ((ServerPlayer) commandSource.getEntity())
                    .displayClientMessage(new TextComponent(message), false));
        } else {
            // Console?
            logger.info(message);
        }
    }

    @Override
    public void kickPlayer(Object player, String locale, TranslatableMessage message, Object... args) {
        getPlayer(player).connection.disconnect(translateAndTransform(locale, message, args));
    }

    @Override
    public boolean whitelistPlayer(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        SERVER.getPlayerList().getWhiteList().add(new UserWhiteListEntry(profile));
        return true;
    }

    @Override
    public boolean removePlayerFromWhitelist(UUID uuid, String username) {
        GameProfile profile = new GameProfile(uuid, username);
        SERVER.getPlayerList().getWhiteList().remove(profile);
        return true;
    }

    private ServerPlayer getPlayer(Object instance) {
        try {
            CommandSourceStack source = (CommandSourceStack) instance;
            return source.getPlayerOrException();
        } catch (ClassCastException | CommandSyntaxException exception) {
            logger.error("Failed to cast {} as a player", instance.getClass().getName());
            throw new RuntimeException();
        }
    }

    public Component translateAndTransform(String locale, TranslatableMessage message, Object... args) {
        return new TextComponent(message.translateMessage(manager, locale, args));
    }

    public FabricServerAudiences getAdventure() {
        return ADVENTURE;
    }

    public static void setLaterVariables(MinecraftServer server, FabricServerAudiences adventure) {
        SERVER = server;
        ADVENTURE = adventure;
    }
}
