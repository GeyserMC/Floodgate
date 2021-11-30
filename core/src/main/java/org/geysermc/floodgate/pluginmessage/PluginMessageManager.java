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

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PluginMessageManager {
    private final Map<Class<? extends PluginMessageChannel>, PluginMessageChannel> classInstanceMap = new HashMap<>();
    private final Map<String, PluginMessageChannel> identifierInstanceMap = new HashMap<>();

    @Inject
    public void addChannels(Set<PluginMessageChannel> channels) {
        if (!classInstanceMap.isEmpty()) {
            return;
        }

        for (PluginMessageChannel channel : channels) {
            classInstanceMap.put(channel.getClass(), channel);
            identifierInstanceMap.put(channel.getIdentifier(), channel);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginMessageChannel> T getChannel(Class<T> channelType) {
        return (T) classInstanceMap.get(channelType);
    }

    public PluginMessageChannel getChannel(String identifier) {
        return identifierInstanceMap.get(identifier);
    }
}
