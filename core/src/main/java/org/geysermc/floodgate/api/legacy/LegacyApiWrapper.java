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

package org.geysermc.floodgate.api.legacy;

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
import org.geysermc.floodgate.player.FloodgateConnection;
import org.geysermc.floodgate.util.WebEndpoints;

public final class LegacyApiWrapper implements FloodgateApi {
    private final GeyserApiBase apiBase;
    private final WebEndpoints webEndpoints;

    public LegacyApiWrapper(GeyserApiBase apiBase, WebEndpoints webEndpoints) {
        this.apiBase = apiBase;
        this.webEndpoints = webEndpoints;
    }

    @Override
    public String getPlayerPrefix() {
        return apiBase.usernamePrefix();
    }

    @Override
    public Collection<FloodgatePlayer> getPlayers() {
        return apiBase.onlineConnections()
                .stream()
                .map(connection -> new LegacyPlayerWrapper((FloodgateConnection) connection))
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
        if (connection != null) {
            return new LegacyPlayerWrapper(connection);
        }
        return null;
    }

    @Override
    public UUID createJavaPlayerId(long xuid) {
        return new UUID(0, xuid);
    }

    @Override
    public boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
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
    public boolean sendForm(UUID uuid, org.geysermc.cumulus.Form<?> form) {
        return sendForm(uuid, form.newForm());
    }

    @Override
    public boolean sendForm(UUID uuid, org.geysermc.cumulus.util.FormBuilder<?, ?> formBuilder) {
        return sendForm(uuid, formBuilder.build());
    }

    @Override
    public boolean transferPlayer(UUID uuid, String address, int port) {
        return apiBase.transfer(uuid, address, port);
    }

    @Override
    public CompletableFuture<Long> getXuidFor(String gamertag) {
        return webEndpoints.getXuidFor(gamertag);
    }

    @Override
    public CompletableFuture<String> getGamertagFor(long xuid) {
        return webEndpoints.getGamertagFor(xuid);
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }
}
