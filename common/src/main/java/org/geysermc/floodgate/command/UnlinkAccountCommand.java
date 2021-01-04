/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.command;

import static org.geysermc.floodgate.command.CommonCommandMessage.CHECK_CONSOLE;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.Description;
import cloud.commandframework.context.CommandContext;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.FloodgateCommand;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudience.PlayerAudience;

@NoArgsConstructor
public final class UnlinkAccountCommand implements FloodgateCommand {
    @Inject private FloodgateApi api;

    @Override
    public Command<UserAudience> buildCommand(CommandManager<UserAudience> commandManager) {
        return commandManager.commandBuilder("unlinkaccount",
                Description.of("Unlink your Java account from your Bedrock account"))
                .senderType(PlayerAudience.class)
                .permission("floodgate.command.unlinkaccount")
                .handler(this::execute)
                .build();
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        UserAudience sender = context.getSender();

        PlayerLink link = api.getPlayerLink();
        if (!link.isEnabledAndAllowed()) {
            sender.sendMessage(Message.LINKING_NOT_ENABLED);
            return;
        }

        link.isLinkedPlayer(sender.uuid())
                .whenComplete((linked, error) -> {
                    if (error != null) {
                        sender.sendMessage(CommonCommandMessage.IS_LINKED_ERROR);
                        return;
                    }

                    if (!linked) {
                        sender.sendMessage(Message.NOT_LINKED);
                        return;
                    }

                    link.unlinkPlayer(sender.uuid())
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
        return !config.getPlayerLink().isUseGlobalLinking();
    }

    @Getter
    public enum Message implements CommandMessage {
        NOT_LINKED("floodgate.command.unlink_account.not_linked"),
        UNLINK_SUCCESS("floodgate.command.unlink_account.unlink_success"),
        UNLINK_ERROR("floodgate.command.unlink_account.error " + CHECK_CONSOLE),
        LINKING_NOT_ENABLED("floodgate.commands.linking_disabled");

        private final String rawMessage;
        private final String[] translateParts;

        Message(String rawMessage) {
            this.rawMessage = rawMessage;
            this.translateParts = rawMessage.split(" ");
        }
    }
}
