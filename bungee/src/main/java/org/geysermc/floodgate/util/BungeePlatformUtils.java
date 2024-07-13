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

package org.geysermc.floodgate.util;

import java.lang.reflect.Field;
import java.util.List;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.ProtocolConstants;
import org.geysermc.floodgate.core.util.ReflectionUtils;
import org.geysermc.floodgate.core.platform.util.PlatformUtils;

@SuppressWarnings("ConstantConditions")
public final class BungeePlatformUtils extends PlatformUtils {
    private static final String LATEST_SUPPORTED_VERSION;
    private final ProxyServer proxyServer = ProxyServer.getInstance();

    static {
        int protocolNumber = -1;
        String versionName = "";

        for (Field field : ProtocolConstants.class.getFields()) {
            if (!field.getName().startsWith("MINECRAFT_")) {
                continue;
            }

            int fieldValue = ReflectionUtils.castedStaticValue(field);
            if (fieldValue > protocolNumber) {
                protocolNumber = fieldValue;
                versionName = field.getName().substring(10).replace('_', '.');
            }
        }

        if (protocolNumber == -1) {
            List<String> versions = ProtocolConstants.SUPPORTED_VERSIONS;
            versionName = versions.get(versions.size() - 1);
        }
        LATEST_SUPPORTED_VERSION = versionName;
    }

    @Override
    public AuthType authType() {
        return proxyServer.getConfig().isOnlineMode() ? AuthType.ONLINE : AuthType.OFFLINE;
    }

    @Override
    public String minecraftVersion() {
        return LATEST_SUPPORTED_VERSION;
    }

    @Override
    public String serverImplementationName() {
        return proxyServer.getName();
    }
}
