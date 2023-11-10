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

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.connection.audience.UserAudience.PlayerAudience;
import org.geysermc.floodgate.core.link.CommonPlayerLink;
import org.geysermc.floodgate.core.link.GlobalPlayerLinking;
import org.geysermc.floodgate.core.platform.command.FloodgateCommand;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;
import org.geysermc.floodgate.core.util.Constants;

@Singleton
public final class UnlinkAccountCommand implements FloodgateCommand {
    @Inject CommonPlayerLink link;

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        return commandManager.commandBuilder("unlinkaccount",
                ArgumentDescription.of("Unlink your Java account from your Bedrock account"))
                .senderType(PlayerAudience.class)
                .permission(Permission.COMMAND_UNLINK.get())
                .handler(this::execute)
                .build();
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        UserAudience sender = context.getSender();

        //todo make this less hacky
        if (link instanceof GlobalPlayerLinking) {
            if (((GlobalPlayerLinking) link).getDatabase() != null) {
                sender.sendMessage(CommonCommandMessage.LOCAL_LINKING_NOTICE,
                        Constants.LINK_INFO_URL);
            } else {
                sender.sendMessage(CommonCommandMessage.GLOBAL_LINKING_NOTICE,
                        Constants.LINK_INFO_URL);
                return;
            }
        }

        if (!link.isActive()) {
            sender.sendMessage(CommonCommandMessage.LINKING_DISABLED);
            return;
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

    @Getter
    public enum Message implements TranslatableMessage {
        NOT_LINKED("floodgate.command.unlink_account.not_linked"),
        UNLINK_SUCCESS("floodgate.command.unlink_account.unlink_success"),
        UNLINK_ERROR("floodgate.command.unlink_account.error " + CommonCommandMessage.CHECK_CONSOLE);

        private final String rawMessage;
        private final String[] translateParts;

        Message(String rawMessage) {
            this.rawMessage = rawMessage;
            this.translateParts = rawMessage.split(" ");
        }
    }
}
