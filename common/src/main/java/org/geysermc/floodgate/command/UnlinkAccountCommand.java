/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.command;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.util.CommandUtil;

import java.util.UUID;

@NoArgsConstructor
public final class UnlinkAccountCommand implements Command {
    @Inject private FloodgateApi api;
    @Inject private CommandUtil commandUtil;

    @Override
    public void execute(Object player, UUID uuid, String username, String... args) {
        PlayerLink link = api.getPlayerLink();
        if (!link.isEnabledAndAllowed()) {
            sendMessage(player, Message.LINKING_NOT_ENABLED);
            return;
        }

        link.isLinkedPlayer(uuid).whenComplete((linked, throwable) -> {
            if (throwable != null) {
                sendMessage(player, CommonCommandMessage.IS_LINKED_ERROR);
                return;
            }

            if (!linked) {
                sendMessage(player, Message.NOT_LINKED);
                return;
            }

            link.unlinkPlayer(uuid).whenComplete((aVoid, throwable1) ->
                    sendMessage(player, throwable1 == null ?
                            Message.UNLINK_SUCCESS :
                            Message.UNLINK_ERROR
                    )
            );
        });
    }

    @Override
    public String getName() {
        return "unlinkaccount";
    }

    @Override
    public String getPermission() {
        return "floodgate.unlinkaccount";
    }

    @Override
    public boolean isRequirePlayer() {
        return true;
    }

    private void sendMessage(Object player, CommandMessage message, Object... args) {
        commandUtil.sendMessage(player, message, args);
    }

    public enum Message implements CommandMessage {
        NOT_LINKED("floodgate.command.unlink_account.not_linked"),
        UNLINK_SUCCESS("floodgate.command.unlink_account.unlink_success"),
        // TODO also used to have CHECK_CONSOLE
        UNLINK_ERROR("floodgate.command.unlink_account.error"),
        LINKING_NOT_ENABLED("floodgate.command.unlink_account.disabled");

        @Getter private final String message;

        Message(String message) {
            this.message = message;
        }
    }
}
