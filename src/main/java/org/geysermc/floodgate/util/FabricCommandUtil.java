package org.geysermc.floodgate.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.kyori.adventure.platform.fabric.impl.accessor.ConnectionAccess;
import net.kyori.adventure.platform.fabric.impl.server.FriendlyByteBufBridge;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
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
        if (!(source instanceof ServerCommandSource)) {
            throw new RuntimeException();
        }

        ServerCommandSource commandSource = (ServerCommandSource) source;
        if (commandSource.getEntity() instanceof ServerPlayerEntity) {
            return getAudience0((ServerPlayerEntity) commandSource.getEntity());
        }

        return new FabricUserAudience(null, manager.getDefaultLocale(), commandSource, this);
    }

    @Override
    public UserAudience getAudienceByUuid(@NonNull UUID uuid) {
        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return getAudience0(player);
        }
        return getOfflineAudienceByUuid(uuid);
    }

    @Override
    public UserAudience getAudienceByUsername(@NonNull String username) {
        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(username);
        if (player != null) {
            return getAudience0(player);
        }
        return getOfflineAudienceByUsername(username);
    }

    private FabricUserAudience getAudience0(ServerPlayerEntity player) {
        // Apparently can be null even if Javadocs say otherwise
        Locale locale = PlayerLocales.locale(player);
        return new FabricUserAudience.NamedFabricUserAudience(
                player.getName().asString(),
                player.getUuid(), locale != null ?
                locale.getLanguage().toLowerCase(Locale.ROOT) + "_" + locale.getCountry().toUpperCase(Locale.ROOT) :
                manager.getDefaultLocale(), player.getCommandSource(), this, true);
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUuid(@NonNull UUID uuid) {
        return new FabricUserAudience(uuid, null, null, this);
    }

    @Override
    public @NonNull UserAudience getOfflineAudienceByUsername(@NonNull String username) {
        UUID uuid = null;
        Optional<GameProfile> profile = SERVER.getUserCache().findByName(username);
        if (profile.isPresent()) {
            uuid = profile.get().getId();
        }
        return new FabricUserAudience.NamedFabricUserAudience(username, uuid, username, null, this, false);
    }

    @Override
    public @NonNull Collection<String> getOnlineUsernames(UserAudienceArgument.@NonNull PlayerType limitTo) {
        List<ServerPlayerEntity> players = SERVER.getPlayerManager().getPlayerList();

        Collection<String> usernames = new ArrayList<>();
        switch (limitTo) {
            case ALL_PLAYERS:
                for (ServerPlayerEntity player : players) {
                    usernames.add(player.getName().asString());
                }
                break;
            case ONLY_JAVA:
                for (ServerPlayerEntity player : players) {
                    if (!api.isFloodgatePlayer(player.getUuid())) {
                        usernames.add(player.getName().asString());
                    }
                }
                break;
            case ONLY_BEDROCK:
                for (ServerPlayerEntity player : players) {
                    if (api.isFloodgatePlayer(player.getUuid())) {
                        usernames.add(player.getName().asString());
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
        for (ServerPlayerEntity player : SERVER.getPlayerManager().getPlayerList()) {
            if (hasPermission(player, permission)) {
                players.add(player);
            }
        }
        return players;
    }

    @Override
    public void sendMessage(Object player, String locale, TranslatableMessage message, Object... args) {
        ServerCommandSource commandSource = (ServerCommandSource) player;
        if (commandSource.getEntity() instanceof ServerPlayerEntity) {
            SERVER.execute(() -> ((ServerPlayerEntity) commandSource.getEntity())
                    .sendMessage(translateAndTransform(locale, message, args), false));
        } else {
            // Console?
            logger.info(message.translateMessage(manager, locale, args));
        }
    }

    @Override
    public void sendMessage(Object target, String message) {
        ServerCommandSource commandSource = (ServerCommandSource) target;
        if (commandSource.getEntity() instanceof ServerPlayerEntity) {
            SERVER.execute(() -> ((ServerPlayerEntity) commandSource.getEntity())
                    .sendMessage(new LiteralText(message), false));
        } else {
            // Console?
            logger.info(message);
        }
    }

    @Override
    public void kickPlayer(Object player, String locale, TranslatableMessage message, Object... args) {
        getPlayer(player).networkHandler.disconnect(translateAndTransform(locale, message, args));
    }

    @Override
    public boolean whitelistPlayer(String xuid, String username) {
        GameProfile profile = new GameProfile(Utils.getJavaUuid(xuid), username);
        SERVER.getPlayerManager().getWhitelist().add(new WhitelistEntry(profile));
        return true;
    }

    @Override
    public boolean removePlayerFromWhitelist(String xuid, String username) {
        GameProfile profile = new GameProfile(Utils.getJavaUuid(xuid), username);
        SERVER.getPlayerManager().getWhitelist().remove(profile);
        return true;
    }

    private ServerPlayerEntity getPlayer(Object instance) {
        try {
            ServerCommandSource source = (ServerCommandSource) instance;
            return source.getPlayer();
        } catch (ClassCastException | CommandSyntaxException exception) {
            logger.error("Failed to cast {} as a player", instance.getClass().getName());
            throw new RuntimeException();
        }
    }

    public Text translateAndTransform(String locale, TranslatableMessage message, Object... args) {
        return new LiteralText(message.translateMessage(manager, locale, args));
    }

    public FabricServerAudiences getAdventure() {
        return ADVENTURE;
    }

    public static void setLaterVariables(MinecraftServer server, FabricServerAudiences adventure) {
        SERVER = server;
        ADVENTURE = adventure;
    }
}
