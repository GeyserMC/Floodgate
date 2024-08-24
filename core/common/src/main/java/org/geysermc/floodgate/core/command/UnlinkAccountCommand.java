/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.core.command;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience.PlayerAudience;
import org.geysermc.floodgate.core.link.CommonPlayerLink;
import org.geysermc.floodgate.core.platform.command.FloodgateCommand;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;
import org.geysermc.floodgate.core.util.Constants;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

@Singleton
public final class UnlinkAccountCommand implements FloodgateCommand {
    @Inject CommonPlayerLink link;

    @Override
    public Command<PlayerAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        return commandManager.commandBuilder("unlinkaccount",
                Description.of("Unlink your Java account from your Bedrock account"))
                .senderType(PlayerAudience.class)
                .permission(Permission.COMMAND_UNLINK.get())
                .handler(this::execute)
                .build();
    }

    public void execute(CommandContext<PlayerAudience> context) {
        UserAudience sender = context.sender();

        var linkState = link.state();
        if (!linkState.localLinkingActive()) {
            if (!linkState.globalLinkingEnabled()) {
                sender.sendMessage(CommonCommandMessage.LINKING_DISABLED);
            } else {
                sender.sendMessage(
                        CommonCommandMessage.GLOBAL_LINKING_NOTICE,
                        literal("url", Constants.LINK_INFO_URL));
            }
            return;
        } else if (linkState.globalLinkingEnabled()) {
            sender.sendMessage(CommonCommandMessage.LOCAL_LINKING_NOTICE, literal("url", Constants.LINK_INFO_URL));
        }

        link.isLinked(sender.uuid())
                .whenComplete((linked, error) -> {
                    if (error != null) {
                        sender.sendMessage(CommonCommandMessage.IS_LINKED_ERROR);
                        return;
                    }

                    if (!linked) {
                        sender.sendMessage(Message.NOT_LINKED);
                        return;
                    }

                    link.unlink(sender.uuid())
                            .whenComplete((unused, error1) -> {
                                if (error1 != null) {
                                    sender.sendMessage(Message.UNLINK_ERROR);
                                    return;
                                }

                                sender.sendMessage(Message.UNLINK_SUCCESS);
                            });
                });
    }

    @Override
    public boolean shouldRegister(FloodgateConfig config) {
        FloodgateConfig.PlayerLinkConfig linkConfig = config.playerLink();
        return linkConfig.enabled() &&
                (linkConfig.enableOwnLinking() || linkConfig.enableGlobalLinking());
    }

    public static final class Message {
        public static final TranslatableMessage NOT_LINKED = new TranslatableMessage("floodgate.command.unlink_account.not_linked", MessageType.ERROR);
        public static final TranslatableMessage UNLINK_SUCCESS = new TranslatableMessage("floodgate.command.unlink_account.unlink_success", MessageType.SUCCESS);
        public static final TranslatableMessage UNLINK_ERROR = new TranslatableMessage("floodgate.command.unlink_account.error " + CommonCommandMessage.CHECK_CONSOLE, MessageType.ERROR);
    }
}
