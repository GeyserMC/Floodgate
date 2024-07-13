/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.skin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.event.Listener;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.event.lifecycle.ShutdownEvent;

@Listener
@Singleton
public final class SkinUploadManager {
    private final Int2ObjectMap<SkinUploadSocket> connections =
            Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>());

    @Inject private FloodgateApi api;
    @Inject private SkinApplier applier;
    @Inject private FloodgateLogger logger;

    public void addConnectionIfNeeded(int id, String verifyCode) {
        connections.computeIfAbsent(id, (ignored) -> {
            SkinUploadSocket socket =
                    new SkinUploadSocket(id, verifyCode, this, api, applier, logger);
            socket.connect();
            return socket;
        });
    }

    public void removeConnection(int id, SkinUploadSocket socket) {
        connections.remove(id, socket);
    }

    public void closeAllSockets() {
        for (SkinUploadSocket socket : connections.values()) {
            socket.close();
        }
        connections.clear();
    }

    @Subscribe
    public void onShutdown(ShutdownEvent ignored) {
        closeAllSockets();
    }
}
