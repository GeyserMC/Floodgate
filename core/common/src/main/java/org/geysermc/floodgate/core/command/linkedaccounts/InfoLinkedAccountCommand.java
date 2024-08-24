package org.geysermc.floodgate.core.command.linkedaccounts;

import static org.geysermc.floodgate.core.platform.command.Placeholder.dynamic;
import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.core.command.CommonCommandMessage;
import org.geysermc.floodgate.core.command.linkedaccounts.LinkedAccountsCommand.LinkedAccountsCommonMessage;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.audience.ProfileAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.http.ProfileFetcher;
import org.geysermc.floodgate.core.link.LocalPlayerLinking;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.command.FloodgateSubCommand;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;
import org.geysermc.floodgate.core.util.Constants;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;

@Singleton
final class InfoLinkedAccountCommand extends FloodgateSubCommand {
    @Inject Optional<LocalPlayerLinking> optionalLinking;
    @Inject FloodgateConfig config;
    @Inject ProfileFetcher fetcher;
    @Inject FloodgateLogger logger;

    InfoLinkedAccountCommand() {
        super(LinkedAccountsCommand.class, "info", "Gets info about the link status of an user", "i");
    }

    @Override
    public Command.Builder<UserAudience> onBuild(Command.Builder<UserAudience> commandBuilder) {
        return super.onBuild(commandBuilder).argument(ProfileAudience.ofAnyIdentifierBoth("player"));
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        UserAudience sender = context.sender();

        if (optionalLinking.isEmpty()) {
            sender.sendMessage(CommonCommandMessage.LINKING_DISABLED);
            return;
        }

        var linking = optionalLinking.get();
        if (linking.state().globalLinkingEnabled()) {
            sender.sendMessage(CommonCommandMessage.LOCAL_LINKING_NOTICE, literal("url", Constants.LINK_INFO_URL));
        }

        ProfileAudience playerInput = context.get("player");
        String gamertagInput;
        final boolean bedrock;

        var future = CompletableFuture.completedFuture(playerInput);
        if (playerInput.uuid() == null) {
            if (playerInput.username().startsWith(config.usernamePrefix())) {
                gamertagInput = playerInput.username().substring(config.usernamePrefix().length());
                future = fetcher.fetchXuidFor(gamertagInput);
                bedrock = true;
            } else {
                gamertagInput = null;
                bedrock = false;
                future = fetcher.fetchUniqueIdFor(playerInput.username());
            }
        } else {
            gamertagInput = null;
            bedrock = playerInput.uuid().getMostSignificantBits() == 0;
        }

        String platform = bedrock ? "Bedrock" : "Java";

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Error while fetching player", throwable);
                return;
            }

            if (result == null) {
                sender.sendMessage(
                        LinkedAccountsCommonMessage.NOT_FOUND,
                        literal("platform", platform),
                        literal("target", playerInput.username()));
                return;
            }

            linking.fetchLink(result.uuid()).whenComplete((link, error) -> {
                if (error != null) {
                    sender.sendMessage(Message.INFO_ERROR);
                    logger.error("Exception while fetching link status", error);
                    return;
                }

                var usernameOrUniqueId = playerInput.username() != null ? playerInput.username() : playerInput.uuid();

                if (link == null) {
                    sender.sendMessage(
                            Message.INFO_NOT_LINKED,
                            literal("platform", platform),
                            literal("target", usernameOrUniqueId));
                    return;
                }

                CompletableFuture<ProfileAudience> gamertagFetch =
                        CompletableFuture.completedFuture(new ProfileAudience(null, gamertagInput));
                if (gamertagInput == null) {
                    gamertagFetch = fetcher.fetchGamertagFor(link.bedrockId());
                }

                gamertagFetch.whenComplete((gamertagResult, $) -> {
                    String gamertag = "unknown";
                    if (gamertagResult != null) {
                        gamertag = gamertagResult.username();
                    }

                    sender.sendMessage(
                        Message.INFO_LINKED,
                        literal("platform", platform),
                        literal("target", usernameOrUniqueId),
                        dynamic("link_info", LinkedAccountsCommonMessage.LINK_INFO, sender),
                        literal("bedrock_id", link.bedrockId()),
                        literal("bedrock_name", gamertag),
                        literal("java_name", link.javaUsername()),
                        literal("java_uuid", link.javaUniqueId()));
                });

            });
        });
    }

    public static final class Message {
        public static final TranslatableMessage INFO_ERROR = new TranslatableMessage("floodgate.command.linkedaccounts.info.error", MessageType.ERROR);
        public static final TranslatableMessage INFO_NOT_LINKED = new TranslatableMessage("floodgate.command.linkedaccounts.info.not_linked", MessageType.NORMAL);
        public static final TranslatableMessage INFO_LINKED = new TranslatableMessage("floodgate.command.linkedaccounts.info.linked", MessageType.NORMAL);
    }
}
