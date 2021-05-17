package org.geysermc.floodgate.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.impl.accessor.ConnectionAccess;
import net.kyori.adventure.platform.fabric.impl.server.FriendlyByteBufBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.platform.command.CommandMessage;
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
        // Marked as internal??? Should probably find a better way to get this.
        Locale locale;
        Channel channel = ((ConnectionAccess) player.networkHandler.getConnection()).getChannel();
        if (channel != null) {
            locale = channel.attr(FriendlyByteBufBridge.CHANNEL_LOCALE).get();
        } else {
            locale = null;
        }
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
        GameProfile profile = SERVER.getUserCache().findByName(username);
        if (profile != null) {
            uuid = profile.getId();
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
    public void sendMessage(Object player, String locale, CommandMessage message, Object... args) {
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
    public void kickPlayer(Object player, String locale, CommandMessage message, Object... args) {
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

    protected ServerPlayerEntity getPlayer(Object instance) {
        try {
            ServerCommandSource source = (ServerCommandSource) instance;
            return source.getPlayer();
        } catch (ClassCastException | CommandSyntaxException exception) {
            logger.error("Failed to cast {} as a player", instance.getClass().getName());
            throw new RuntimeException();
        }
    }

    public Text translateAndTransform(String locale, CommandMessage message, Object... args) {
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
