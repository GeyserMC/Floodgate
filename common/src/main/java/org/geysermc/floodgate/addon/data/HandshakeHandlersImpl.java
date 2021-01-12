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

package org.geysermc.floodgate.addon.data;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import java.util.Random;
import org.geysermc.floodgate.api.handshake.HandshakeData;
import org.geysermc.floodgate.api.handshake.HandshakeHandler;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;

public class HandshakeHandlersImpl implements HandshakeHandlers {
    private final Random random = new Random();
    private final IntObjectMap<HandshakeHandler> handshakeHandlers = new IntObjectHashMap<>();

    @Override
    public int addHandshakeHandler(HandshakeHandler handshakeHandler) {
        if (handshakeHandler == null) {
            return -1;
        }

        int key;
        do {
            key = random.nextInt(Integer.MAX_VALUE);
        } while (handshakeHandlers.putIfAbsent(key, handshakeHandler) != null);

        return key;
    }

    @Override
    public void removeHandshakeHandler(int handshakeHandlerId) {
        // key is always positive
        if (handshakeHandlerId <= 0) {
            return;
        }

        handshakeHandlers.remove(handshakeHandlerId);
    }

    @Override
    public void removeHandshakeHandler(Class<? extends HandshakeHandler> handshakeHandler) {
        if (HandshakeHandler.class == handshakeHandler) {
            return;
        }

        handshakeHandlers.values().removeIf(handler -> handler.getClass() == handshakeHandler);
    }

    public void callHandshakeHandlers(HandshakeData handshakeData) {
        for (HandshakeHandler handshakeHandler : handshakeHandlers.values()) {
            handshakeHandler.handle(handshakeData);
        }
    }
}
