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

package org.geysermc.floodgate.core.command.main;

import static org.geysermc.floodgate.core.platform.command.Placeholder.literal;

import it.unimi.dsi.fastutil.Pair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.geysermc.floodgate.core.command.util.Permission;
import org.geysermc.floodgate.core.connection.audience.UserAudience;
import org.geysermc.floodgate.core.http.api.GlobalApiClient;
import org.geysermc.floodgate.core.platform.command.FloodgateSubCommand;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.TranslatableMessage;
import org.geysermc.floodgate.core.util.Utils;
import org.incendo.cloud.context.CommandContext;

@Singleton
final class FirewallCheckSubcommand extends FloodgateSubCommand {
    @Inject GlobalApiClient globalApiClient;

    FirewallCheckSubcommand() {
        super(
                MainCommand.class,
                "firewall",
                "Check if your outgoing firewall allows Floodgate to work properly",
                Permission.COMMAND_MAIN_FIREWALL
        );
    }

    @Override
    public void execute(CommandContext<UserAudience> context) {
        UserAudience sender = context.sender();
        executeChecks(
                globalApiCheck(sender)
        ).whenComplete((response, $) ->
                sender.sendMessage(
                        Message.FIREWALL_RESULT,
                        literal("successful", response.left()),
                        literal("total", response.left() + response.right()))
        );
    }

    private BooleanSupplier globalApiCheck(UserAudience sender) {
        return executeFirewallText(sender, "global api", () -> globalApiClient.health());
    }

    private BooleanSupplier executeFirewallText(
            UserAudience sender, String name, Runnable runnable) {
        return () -> {
            sender.sendMessage(Message.CHECK_START, literal("target", name));
            try {
                runnable.run();
                sender.sendMessage(Message.CHECK_SUCCESS, literal("target", name));
                return true;
            } catch (Exception exception) {
                sender.sendMessage(Message.CHECK_FAILED, literal("target", name));
                sender.sendRaw(Utils.getStackTrace(exception), MessageType.ERROR);
                return false;
            }
        };
    }

    private CompletableFuture<Pair<Integer, Integer>> executeChecks(BooleanSupplier... checks) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicInteger okCount = new AtomicInteger();
            AtomicInteger failCount = new AtomicInteger();

            for (BooleanSupplier check : checks) {
                if (check.getAsBoolean()) {
                    okCount.getAndIncrement();
                    continue;
                }
                failCount.getAndIncrement();
            }

            return Pair.of(okCount.get(), failCount.get());
        });
    }

    public static final class Message {
        public static final TranslatableMessage FIREWALL_RESULT = new TranslatableMessage("floodgate.command.main.firewall.result", MessageType.INFO);
        public static final TranslatableMessage CHECK_START = new TranslatableMessage("floodgate.command.main.firewall.check.start", MessageType.INFO);
        public static final TranslatableMessage CHECK_SUCCESS = new TranslatableMessage("floodgate.command.main.firewall.check.success", MessageType.SUCCESS);
        public static final TranslatableMessage CHECK_FAILED = new TranslatableMessage("floodgate.command.main.firewall.check.failed", MessageType.ERROR);
    }
}
