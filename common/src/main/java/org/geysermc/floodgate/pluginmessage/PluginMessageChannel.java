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

package org.geysermc.floodgate.pluginmessage;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface PluginMessageChannel {

    String getIdentifier();

    Result handleProxyCall(
            byte[] data,
            UUID targetUuid,
            String targetUsername,
            Identity targetIdentity,
            UUID sourceUuid,
            String sourceUsername,
            Identity sourceIdentity);

    Result handleServerCall(byte[] data, UUID targetUuid, String targetUsername);

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Result {
        private static final Result FORWARD = new Result(true, null);
        private static final Result HANDLED = new Result(false, null);

        private final boolean allowed;
        private final String reason;

        public static Result forward() {
            return FORWARD;
        }

        public static Result handled() {
            return HANDLED;
        }

        public static Result kick(String reason) {
            return new Result(false, reason);
        }
    }

    enum Identity {
        UNKNOWN,
        SERVER,
        PLAYER
    }
}
