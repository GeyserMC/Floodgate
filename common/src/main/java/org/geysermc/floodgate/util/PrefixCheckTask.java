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

package org.geysermc.floodgate.util;

import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;

public final class PrefixCheckTask {
    public static void checkAndExecuteDelayed(FloodgateConfig config, FloodgateLogger logger) {
        if (Utils.isUniquePrefix(config.getUsernamePrefix())) {
            return;
        }

        new Thread(() -> {
            // normally proxies don't have a lot of plugins, so proxies don't need to sleep as long
            try {
                Thread.sleep(config.isProxy() ? 1000 : 2000);
            } catch (InterruptedException ignored) {
            }

            if (config.getUsernamePrefix().isEmpty()) {
                logger.warn("\n" +
                        "**********************************\n" +
                        "* You specified an empty prefix in your Floodgate config for Bedrock players!\n" +
                        "* Should a Java player join and a Bedrock player join with the same username, unwanted results and conflicts will happen!\n" +
                        "* We strongly recommend using . as the prefix, but other alternatives that will not conflict include: *, - and +\n" +
                        "**********************************");
                return;
            }

            logger.warn(
                    "\n" +
                    "**********************************\n" +
                    "The prefix you entered in your Floodgate config ({}) could lead to username conflicts!\n" +
                    "Should a Java player join with the username {}Notch, and a Bedrock player join as Notch (who will be given the name {}Notch), unwanted results will happen!\n" +
                    "We strongly recommend using . as the prefix, but other alternatives that will not conflict include: *, - and +\n" +
                    "**********************************",
                    config.getUsernamePrefix(), config.getUsernamePrefix(),
                    config.getUsernamePrefix(), config.getUsernamePrefix());
        }).start();
    }
}
