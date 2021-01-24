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

package org.geysermc.floodgate.api;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.geysermc.floodgate.crypto.FloodgateCipher;
import org.geysermc.floodgate.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.util.BedrockData;

public final class ProxyFloodgateApi extends SimpleFloodgateApi {
    private final Map<UUID, String> encryptedData = new HashMap<>();
    private final FloodgateCipher cipher;

    public ProxyFloodgateApi(PluginMessageManager pluginMessageManager, FloodgateCipher cipher) {
        super(pluginMessageManager);
        this.cipher = cipher;
    }

    public String getEncryptedData(UUID uuid) {
        return encryptedData.get(uuid);
    }

    public void addEncryptedData(UUID uuid, String encryptedData) {
        this.encryptedData.put(uuid, encryptedData); // just override already existing data I guess
    }

    public void removeEncryptedData(UUID uuid) {
        encryptedData.remove(uuid);
    }

    public void updateEncryptedData(UUID uuid, BedrockData bedrockData) {
        try {
            byte[] encryptedData = cipher.encryptFromString(bedrockData.toString());
            addEncryptedData(uuid, new String(encryptedData, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("We failed to update the BedrockData, " +
                    "but we can't continue without the updated version!", exception);
        }
    }
}
