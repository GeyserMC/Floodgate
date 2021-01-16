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

package org.geysermc.floodgate.listener;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.geysermc.cumulus.Form;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.api.player.PropertyKey;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinUploader.UploadResult;
import org.geysermc.floodgate.util.RawSkin;

public class VelocityPluginMessageHandler extends PluginMessageHandler {
    private ProxyServer proxy;
    private FloodgateApi api;
    private SkinApplier skinApplier;
    private ChannelIdentifier formChannel;
    private ChannelIdentifier skinChannel;
    private FloodgateLogger logger;

    public VelocityPluginMessageHandler(FloodgateConfigHolder configHolder) {
        super(configHolder);
    }

    @Inject // called because this is a listener as well
    public void init(ProxyServer proxy, FloodgateApi api, SkinApplier skinApplier,
                     @Named("formChannel") String formChannelName,
                     @Named("skinChannel") String skinChannelName,
                     FloodgateLogger logger) {
        this.proxy = proxy;
        this.api = api;
        this.skinApplier = skinApplier;
        this.logger = logger;

        formChannel = MinecraftChannelIdentifier.from(formChannelName);
        skinChannel = MinecraftChannelIdentifier.from(skinChannelName);

        proxy.getChannelRegistrar().register(formChannel);
        proxy.getChannelRegistrar().register(skinChannel);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        ChannelMessageSource source = event.getSource();

        if (event.getIdentifier().equals(formChannel)) {
            if (source instanceof ServerConnection) {
                // send it to the client
                event.setResult(ForwardResult.forward());
                return;
            }

            if (source instanceof Player) {
                byte[] data = event.getData();
                if (data.length < 2) {
                    logKick(source, "Invalid form response!");
                    return;
                }

                short formId = getFormId(data);

                // if the bit is not set, it's for the connected server
                if ((formId & 0x8000) == 0) {
                    event.setResult(ForwardResult.forward());
                    return;
                }

                event.setResult(ForwardResult.handled());

                if (!callResponseConsumer(data)) {
                    logger.error("Couldn't find stored form with id {} for player {}",
                            formId, ((Player) source).getUsername());
                }
            }
        }

        if (event.getIdentifier().equals(skinChannel)) {
            byte[] data = event.getData();

            if (data.length < 1) {
                logKick(source, "Got invalid Skin request/response.");
                return;
            }

            boolean request = data[0] == 1;

            if (!request && data.length < 2) {
                logKick(source, "Got invalid Skin response.");
                return;
            }

            if (source instanceof ServerConnection) {
                if (request) {
                    logKick(source, "Got Skin request from Server?");
                    return;
                }

                UUID playerUniqueId = ((Player) event.getTarget()).getUniqueId();
                FloodgatePlayer floodgatePlayer = api.getPlayer(playerUniqueId);

                if (floodgatePlayer == null) {
                    logKick(source, "Server issued Skin request for non-Floodgate player.");
                    return;
                }

                // 1 = failed, 0 = successful.

                // we'll try it again on the next server if it failed
                if (data[1] != 0) {
                    return;
                }

                // we only have to continue if the player doesn't already have a skin uploaded
                if (floodgatePlayer.hasProperty(PropertyKey.SKIN_UPLOADED)) {
                    return;
                }

                byte[] responseData = new byte[data.length - 2];
                System.arraycopy(data, 2, responseData, 0, responseData.length);

                JsonObject response;
                try {
                    Reader reader = new InputStreamReader(new ByteArrayInputStream(responseData));
                    response = GSON.fromJson(reader, JsonObject.class);
                } catch (JsonIOException | JsonSyntaxException throwable) {
                    logger.error("Failed to read Skin response", throwable);
                    return;
                }

                floodgatePlayer.addProperty(PropertyKey.SKIN_UPLOADED, response);
                skinApplier.applySkin(floodgatePlayer, UploadResult.success(response));
                return;
            }

            // Players (Geyser) can't send requests nor responses
            if (source instanceof Player) {
                logKick(source, "Got Skin " + (request ? "request" : "response") + " from Player?");
            }
        }
    }

    private void logKick(ChannelMessageSource source, String reason) {
        logger.error(reason + " Closing connection");
        ((Player) source).disconnect(Component.text(reason));
    }

    @Override
    public boolean sendForm(UUID uuid, Form form) {
        return proxy.getPlayer(uuid)
                .map(value -> value.sendPluginMessage(formChannel, createFormData(form)))
                .orElse(false);
    }

    @Override
    public boolean sendSkinRequest(UUID uuid, RawSkin skin) {
        return proxy.getPlayer(uuid)
                .flatMap(Player::getCurrentServer)
                .map(server -> server.sendPluginMessage(skinChannel,
                        createSkinRequestData(skin.encode())))
                .orElse(false);
    }
}
