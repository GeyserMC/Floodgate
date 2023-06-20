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

package org.geysermc.floodgate.core.util;

import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.geysermc.event.Listener;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.event.lifecycle.PostEnableEvent;

@Listener
@Singleton
public final class PostEnableMessages {
    private final List<String> messages = new ArrayList<>();

    @Inject FloodgateConfig config;
    @Inject FloodgateLogger logger;
    @Inject
    @Named("commonScheduledPool")
    ScheduledExecutorService executorService;

    public void add(String[] message, Object... args) {
        StringBuilder builder = new StringBuilder();

        builder.append("\n**********************************\n");
        for (String part : message) {
            builder.append("* ").append(part).append('\n');
        }
        builder.append("**********************************");

        messages.add(MessageFormatter.format(builder.toString(), args));
    }

    @PostConstruct
    void registerPrefixMessages() {
        String prefix = config.rawUsernamePrefix();

        if (prefix.isEmpty()) {
            add(new String[]{
                    "You specified an empty prefix in your Floodgate config for Bedrock players!",
                    "Should a Java player join and a Bedrock player join with the same username, unwanted results and conflicts will happen!",
                    "We strongly recommend using . as the prefix, but other alternatives that will not conflict include: +, - and *"
            });
        } else if (!Utils.isUniquePrefix(prefix)) {
            add(new String[]{
                    "The prefix you entered in your Floodgate config ({}) could lead to username conflicts!",
                    "Should a Java player join with the username {}Notch, and a Bedrock player join as Notch (who will be given the name {}Notch), unwanted results will happen!",
                    "We strongly recommend using . as the prefix, but other alternatives that will not conflict include: +, - and *"
            }, prefix, prefix, prefix, prefix);
        }

        if (prefix.length() >= 16) {
            add(new String[]{
                    "The prefix you entered in your Floodgate config ({}) is longer than a Java username can be!",
                    "Because of this, we reset the prefix to the default Floodgate prefix (.)"
            }, prefix);
        } else if (prefix.length() > 2) {
            // we only have to warn them if we haven't replaced the prefix
            add(new String[]{
                    "The prefix you entered in your Floodgate config ({}) is long! ({} characters)",
                    "A prefix is there to prevent username conflicts. However, a long prefix makes the chance of username conflicts higher.",
                    "We strongly recommend using . as the prefix, but other alternatives that will not conflict include: +, - and *"
            }, prefix, prefix.length());
        }
    }

    @EventListener
    public void onPostEnable(PostEnableEvent ignored) {
        // normally proxies don't have a lot of plugins, so proxies don't need to sleep as long
        executorService.schedule(
                () -> messages.forEach(logger::warn),
                config.proxy() ? 2 : 5,
                TimeUnit.SECONDS
        );
    }
}
