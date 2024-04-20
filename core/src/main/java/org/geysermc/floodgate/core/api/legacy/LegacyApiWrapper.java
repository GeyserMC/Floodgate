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

package org.geysermc.floodgate.core.api.legacy;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.util.FormBuilder;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.unsafe.Unsafe;
import org.geysermc.floodgate.core.connection.FloodgateConnection;
import org.geysermc.floodgate.core.http.xbox.GetGamertagResult;
import org.geysermc.floodgate.core.http.xbox.GetXuidResult;
import org.geysermc.floodgate.core.http.xbox.XboxClient;
import org.geysermc.floodgate.core.util.Utils;

@Singleton
public final class LegacyApiWrapper implements FloodgateApi {
    @Inject GeyserApiBase apiBase;
    @Inject XboxClient xboxClient;

    @Override
    public String getPlayerPrefix() {
        return apiBase.usernamePrefix();
    }

    @Override
    public Collection<FloodgatePlayer> getPlayers() {
        return apiBase.onlineConnections()
                .stream()
                .map(connection -> ((FloodgateConnection) connection).legacySelf())
                .collect(Collectors.toList());
    }

    @Override
    public int getPlayerCount() {
        return apiBase.onlineConnectionsCount();
    }

    @Override
    public boolean isFloodgatePlayer(UUID uuid) {
        return apiBase.isBedrockPlayer(uuid);
    }

    @Override
    public FloodgatePlayer getPlayer(UUID uuid) {
        FloodgateConnection connection = (FloodgateConnection) apiBase.connectionByUuid(uuid);
        if (connection == null) {
            return null;
        }
        return connection.legacySelf();
    }

    @Override
    public UUID createJavaPlayerId(long xuid) {
        return new UUID(0, xuid);
    }

    @Override
    public boolean isFloodgateId(UUID uuid) {
        return Utils.isFloodgateUniqueId(uuid);
    }

    @Override
    public boolean sendForm(UUID uuid, Form form) {
        return apiBase.sendForm(uuid, form);
    }

    @Override
    public boolean sendForm(UUID uuid, FormBuilder<?, ?, ?> formBuilder) {
        return apiBase.sendForm(uuid, formBuilder);
    }

    @Override
    public boolean transferPlayer(UUID uuid, String address, int port) {
        return apiBase.transfer(uuid, address, port);
    }

    @Override
    public CompletableFuture<Long> getXuidFor(String gamertag) {
        return xboxClient.xuidByGamertag(gamertag).thenApply(GetXuidResult::xuid);
    }

    @Override
    public CompletableFuture<String> getGamertagFor(long xuid) {
        return xboxClient.gamertagByXuid(xuid).thenApply(GetGamertagResult::gamertag);
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }
}
