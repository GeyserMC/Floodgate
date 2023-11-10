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

package org.geysermc.floodgate.core.api;

import io.micronaut.context.BeanProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.api.connection.Connection;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.connection.ConnectionManager;
import org.geysermc.floodgate.core.http.xbox.XboxClient;
import org.geysermc.floodgate.core.pluginmessage.PluginMessageManager;
import org.geysermc.floodgate.core.pluginmessage.channel.FormChannel;
import org.geysermc.floodgate.core.pluginmessage.channel.TransferChannel;

@Singleton
public class SimpleFloodgateApi implements GeyserApiBase {
    @Inject ConnectionManager connectionManager;
    @Inject BeanProvider<PluginMessageManager> pluginMessageManager;
    @Inject FloodgateConfig config;
    @Inject FloodgateLogger logger;
    @Inject XboxClient xboxClient;

    @Override
    public String usernamePrefix() {
        return config.usernamePrefix();
    }

    @Override
    public @NonNull List<? extends Connection> onlineConnections() {
        return new ArrayList<>(connectionManager.acceptedConnections());
    }

    @Override
    public int onlineConnectionsCount() {
        return connectionManager.acceptedConnectionsCount();
    }

    @Override
    public boolean isBedrockPlayer(@NonNull UUID uuid) {
        return connectionByUuid(uuid) != null;
    }

    @Override
    public @Nullable Connection connectionByUuid(@NonNull UUID uuid) {
        return connectionManager.connectionByUuid(uuid);
    }

    public @Nullable Connection connectionByPlatformIdentifier(@NonNull Object platformIdentifier) {
        return connectionManager.connectionByPlatformIdentifier(platformIdentifier);
    }

    @Override
    public @Nullable Connection connectionByXuid(@NonNull String xuid) {
        return connectionManager.connectionByXuid(xuid);
    }

    public boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }

    @Override
    public boolean sendForm(@NonNull UUID uuid, @NonNull Form form) {
        return pluginMessageManager.get().getChannel(FormChannel.class).sendForm(uuid, form);
    }

    @Override
    public boolean sendForm(@NonNull UUID uuid, FormBuilder<?, ?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    @Override
    public boolean transfer(@NonNull UUID uuid, @NonNull String address, int port) {
        return pluginMessageManager.get()
                .getChannel(TransferChannel.class)
                .sendTransfer(uuid, address, port);
    }

    /*
    @Override
    public CompletableFuture<Long> getXuidFor(String gamertag) {
        return xboxClient.xuidByGamertag(gamertag).thenApply(GetXuidResult::xuid);
    }

    @Override
    public CompletableFuture<String> getGamertagFor(long xuid) {
        return xboxClient.gamertagByXuid(xuid).thenApply(GetGamertagResult::gamertag);
    }

    @Override
    public final Unsafe unsafe() {
        String callerClass = Thread.currentThread().getStackTrace()[2].getClassName();
        logger.warn("A plugin is trying to access an unsafe part of the Floodgate api!" +
                " The use of this api can result in client crashes if used incorrectly." +
                " Caller: " + callerClass);
        return new UnsafeFloodgateApi(pluginMessageManager.get());
    }
    */

    public PlayerLink getPlayerLink() { // TODO
        return InstanceHolder.getPlayerLink();
    }
}
