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

package org.geysermc.floodgate.module;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.file.Path;
import org.geysermc.floodgate.api.SimpleFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.skin.ServerSkinHandler;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinHandler;

public final class ServerCommonModule extends CommonModule {
    public ServerCommonModule(Path dataDirectory) {
        super(dataDirectory);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(SkinHandler.class).to(ServerSkinHandler.class);
    }

    @Provides
    @Singleton
    @Named("configClass")
    public Class<? extends FloodgateConfig> floodgateConfigClass() {
        return FloodgateConfig.class;
    }

    @Provides
    @Singleton
    public SimpleFloodgateApi floodgateApi(PluginMessageHandler pluginMessageHandler) {
        return new SimpleFloodgateApi(pluginMessageHandler);
    }

    @Provides
    @Singleton
    public ServerSkinHandler skinHandler(SkinApplier skinApplier, FloodgateLogger logger,
                                         PluginMessageHandler pluginMessageHandler) {
        return new ServerSkinHandler(skinApplier, logger, pluginMessageHandler);
    }
}
