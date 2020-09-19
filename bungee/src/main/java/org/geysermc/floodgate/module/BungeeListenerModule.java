/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Floodgate
 *
 */

package org.geysermc.floodgate.module;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.netty.util.AttributeKey;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import org.geysermc.floodgate.HandshakeHandler;
import org.geysermc.floodgate.api.ProxyFloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.ProxyFloodgateConfig;
import org.geysermc.floodgate.listener.BungeeListener;
import org.geysermc.floodgate.register.ListenerRegister;
import org.geysermc.floodgate.util.LanguageManager;

import javax.inject.Named;

public final class BungeeListenerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<ListenerRegister<Listener>>() {}).asEagerSingleton();
    }

    @Singleton
    @ProvidesIntoSet
    public Listener bungeeListener(Plugin plugin, ProxyFloodgateConfig config,
                                   ProxyFloodgateApi api, HandshakeHandler handshakeHandler,
                                   @Named("playerAttribute") AttributeKey<FloodgatePlayer> playerAttribute,
                                   FloodgateLogger logger, LanguageManager languageManager) {
        return new BungeeListener(
                plugin, config, api, handshakeHandler, playerAttribute, logger, languageManager
        );
    }
}
