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

package org.geysermc.floodgate.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.avaje.inject.BeanScopeBuilder;
import java.nio.file.Path;
import org.geysermc.floodgate.core.FloodgatePlatform;
import org.geysermc.floodgate.core.util.ReflectionUtils;
import org.slf4j.Logger;

public class VelocityPlatform extends FloodgatePlatform {
    @Inject
    private @DataDirectory Path dataDirectory;
    @Inject
    private ProxyServer proxyServer;
    @Inject
    private Logger logger;

    public VelocityPlatform() {
        ReflectionUtils.setPrefix("com.velocitypowered.proxy");
    }

    @Override
    protected void onBuildBeanScope(BeanScopeBuilder builder) {
        builder.bean(ProxyServer.class, proxyServer)
                .bean("dataDirectory", Path.class, dataDirectory)
                .bean(Logger.class, logger)
                .modules(new VelocityModule());
    }

    @Override
    protected boolean isProxy() {
        return true;
    }
}
