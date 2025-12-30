/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import com.google.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.command.util.Permission;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.link.GlobalPlayerLinking;
import org.geysermc.floodgate.platform.command.FloodgateCommand;
import org.geysermc.floodgate.platform.command.TranslatableMessage;
import org.geysermc.floodgate.player.UserAudience;
import org.geysermc.floodgate.player.UserAudience.PlayerAudience;
import org.geysermc.floodgate.util.Constants;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;

@NoArgsConstructor
public final class UnlinkAccountCommand implements FloodgateCommand {
    @Inject private FloodgateApi api;

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

        PlayerLink link = api.getPlayerLink();

        //todo make this less hacky
        if (link instanceof GlobalPlayerLinking) {
            if (((GlobalPlayerLinking) link).getDatabaseImpl() != null) {
                sender.sendMessage(CommonCommandMessage.LOCAL_LINKING_NOTICE,
                        Constants.LINK_INFO_URL);
            } else {
                sender.sendMessage(CommonCommandMessage.GLOBAL_LINKING_NOTICE,
                        Constants.LINK_INFO_URL);
                return;
            }
        }

        if (!link.isEnabledAndAllowed()) {
            sender.sendMessage(CommonCommandMessage.LINKING_DISABLED);
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
        FloodgateConfig.PlayerLinkConfig linkConfig = config.getPlayerLink();
        return linkConfig.isEnabled() &&
                (linkConfig.isEnableOwnLinking() || linkConfig.isEnableGlobalLinking());
    }

    @Getter
    public enum Message implements TranslatableMessage {
        NOT_LINKED("floodgate.command.unlink_account.not_linked"),
        UNLINK_SUCCESS("floodgate.command.unlink_account.unlink_success"),
        UNLINK_ERROR("floodgate.command.unlink_account.error " + CHECK_CONSOLE);

        private final String rawMessage;
        private final String[] translateParts;

        Message(String rawMessage) {
            this.rawMessage = rawMessage;
            this.translateParts = rawMessage.split(" ");
        }
    }
}
