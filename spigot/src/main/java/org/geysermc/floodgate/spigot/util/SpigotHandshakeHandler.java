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

package org.geysermc.floodgate.spigot.util;

import jakarta.inject.Inject;
import java.util.UUID;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.handshake.HandshakeHandler;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.util.BedrockData;

public class SpigotHandshakeHandler implements HandshakeHandler {
    @Inject FloodgateLogger logger;

    @Override
    public void handle(HandshakeData data) {
        // we never have to do anything when BedrockData is null.
        // BedrockData is null when something went wrong (e.g. invalid key / exception)
        if (data.getBedrockData() == null) {
            return;
        }

        BedrockData bedrockData = data.getBedrockData();
        UUID correctUuid = data.getCorrectUniqueId();

        // replace the ip and uuid with the Bedrock client IP and an uuid based of the xuid
        String[] split = data.getHostname().split("\0");
        if (split.length >= 3) {
            if (logger.isDebug()) {
                logger.info("Replacing hostname arg1 '{}' with '{}' and arg2 '{}' with '{}'",
                        split[1], bedrockData.getIp(), split[2], correctUuid.toString()
                );
            }
            split[1] = bedrockData.getIp();
            split[2] = correctUuid.toString();
        }
        data.setHostname(String.join("\0", split));
    }
}
