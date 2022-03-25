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

package com.minekube.connect.command.main;

import static com.minekube.connect.util.Constants.COLOR_CHAR;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.types.tuples.Pair;
import com.google.gson.JsonElement;
import com.minekube.connect.player.UserAudience;
import com.minekube.connect.util.Constants;
import com.minekube.connect.util.HttpUtils;
import com.minekube.connect.util.HttpUtils.HttpResponse;
import com.minekube.connect.util.Utils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

final class FirewallCheckSubcommand {
    private FirewallCheckSubcommand() {
    }

    static void executeFirewall(CommandContext<UserAudience> context) {
        UserAudience sender = context.getSender();
        executeChecks(
                globalApiCheck(sender)
        ).whenComplete((response, $) ->
                sender.sendMessage(String.format(
                        COLOR_CHAR + "eThe checks have finished. %s/%s were successful",
                        response.getFirst(), response.getFirst() + response.getSecond()
                ))
        );
    }

    private static BooleanSupplier globalApiCheck(UserAudience sender) {
        return executeFirewallText(
                sender, "global api",
                () -> {
                    HttpResponse<JsonElement> response =
                            HttpUtils.get(Constants.HEALTH_URL, JsonElement.class);

                    if (!response.isCodeOk()) {
                        throw new IllegalStateException(String.format(
                                "Didn't receive an 'ok' http code. Got: %s, response: %s",
                                response.getHttpCode(), response.getResponse()
                        ));
                    }
                });
    }

    private static BooleanSupplier executeFirewallText(
            UserAudience sender, String name, Runnable runnable) {
        return () -> {
            sender.sendMessage(COLOR_CHAR + "eTesting " + name + "...");
            try {
                runnable.run();
                sender.sendMessage(COLOR_CHAR + "aWas able to connect to " + name + "!");
                return true;
            } catch (Exception e) {
                sender.sendMessage(COLOR_CHAR + "cFailed to connect:");
                sender.sendMessage(Utils.getStackTrace(e));
                return false;
            }
        };
    }

    private static CompletableFuture<Pair<Integer, Integer>> executeChecks(
            BooleanSupplier... checks) {

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
}
