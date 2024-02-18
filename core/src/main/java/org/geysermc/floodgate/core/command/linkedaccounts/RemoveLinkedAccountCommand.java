package org.geysermc.floodgate.core.command.linkedaccounts;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.command.CommonCommandMessage;
import org.geysermc.floodgate.core.command.LinkAccountCommand;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.audience.ProfileAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.http.ProfileFetcher;
import org.geysermc.floodgate.core.link.LocalPlayerLinking;
import org.geysermc.floodgate.core.platform.command.FloodgateSubCommand;
import org.geysermc.floodgate.core.util.Constants;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;

@Singleton
final class RemoveLinkedAccountCommand extends FloodgateSubCommand {
    @Inject Optional<LocalPlayerLinking> optionalLinking;
    @Inject FloodgateConfig config;
    @Inject ProfileFetcher fetcher;
    @Inject FloodgateLogger logger;

    RemoveLinkedAccountCommand() {
        super(LinkedAccountsCommand.class, "remove", "Manually remove a locally linked account", Permission.COMMAND_LINKED_MANAGE, "r");
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
            sender.sendMessage(CommonCommandMessage.LOCAL_LINKING_NOTICE, Constants.LINK_INFO_URL);
        }

        ProfileAudience playerInput = context.get("player");
        final boolean bedrock;

        var future = CompletableFuture.completedFuture(playerInput);
        if (playerInput.uuid() == null) {
            if (playerInput.username().startsWith(config.usernamePrefix())) {
                future = fetcher.fetchXuidFor(playerInput.username().substring(config.usernamePrefix().length()));
                bedrock = true;
            } else {
                bedrock = false;
                future = fetcher.fetchUniqueIdFor(playerInput.username());
            }
        } else {
            bedrock = playerInput.uuid().getMostSignificantBits() == 0;
        }

        String platform = bedrock ? "Bedrock" : "Java";

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Error while fetching player", throwable);
                return;
            }

            if (result == null) {
                sender.sendMessage("Could not find %s user with username %s"
                        .formatted(platform, playerInput.username()));
                return;
            }

            linking.unlink(result.uuid()).whenComplete(($, error) -> {
                if (error != null) {
                    sender.sendMessage(LinkAccountCommand.Message.LINK_REQUEST_ERROR);
                    logger.error("Exception while manually linking accounts", error);
                    return;
                }
                sender.sendMessage("You've successfully unlinked %s user %s"
                        .formatted(platform, playerInput.username()));
            });
        });
    }
}
