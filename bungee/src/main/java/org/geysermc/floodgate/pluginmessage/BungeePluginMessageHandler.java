/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.geysermc.cumulus.Form;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.skin.SkinApplier;
import org.geysermc.floodgate.skin.SkinUploader.UploadResult;
import org.geysermc.floodgate.util.RawSkin;

public final class BungeePluginMessageHandler extends PluginMessageHandler implements Listener {
    private ProxyServer proxy;
    private FloodgateLogger logger;
    private String formChannel;
    private String skinChannel;
    private FloodgateApi api;
    private SkinApplier skinApplier;

    public BungeePluginMessageHandler(FloodgateConfigHolder configHolder) {
        super(configHolder);
    }

    @Inject // called because this is a listener as well
    public void init(Plugin plugin, FloodgateLogger logger,
                     @Named("formChannel") String formChannel,
                     @Named("skinChannel") String skinChannel,
                     FloodgateApi api, SkinApplier skinApplier) {
        this.proxy = plugin.getProxy();
        this.logger = logger;
        this.formChannel = formChannel;
        this.skinChannel = skinChannel;
        this.api = api;
        this.skinApplier = skinApplier;

        proxy.registerChannel(formChannel);
        proxy.registerChannel(skinChannel);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        Connection source = event.getSender();

        if (event.getTag().equals(formChannel)) {
            if (source instanceof Server) {
                // send it to the client
                event.setCancelled(false);
                return;
            }

            if (source instanceof ProxiedPlayer) {
                byte[] data = event.getData();
                if (data.length < 2) {
                    logKick(source, "Invalid form response!");
                    return;
                }

                short formId = getFormId(data);

                // if the bit is not set, it's for the connected server
                if ((formId & 0x8000) == 0) {
                    event.setCancelled(false);
                    return;
                }

                event.setCancelled(true);

                if (!callResponseConsumer(data)) {
                    logger.error("Couldn't find stored form with id {} for player {}",
                            formId, ((ProxiedPlayer) source).getName());
                }
            }
            return;
        }

        if (event.getTag().equals(skinChannel)) {
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

            if (source instanceof Server) {
                if (request) {
                    logKick(source, "Got Skin request from Server?");
                    return;
                }

                UUID playerUniqueId = ((ProxiedPlayer) event.getReceiver()).getUniqueId();
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

                JsonObject response;
                try {
                    Reader reader = new InputStreamReader(
                            new ByteArrayInputStream(event.getData()));
                    response = GSON.fromJson(reader, JsonObject.class);
                } catch (Throwable throwable) {
                    logger.error("Failed to read Skin response", throwable);
                    return;
                }

                skinApplier.applySkin(floodgatePlayer, UploadResult.success(response));
                return;
            }

            // Players (Geyser) can't send requests nor responses
            if (source instanceof ProxiedPlayer) {
                logKick(source, "Got Skin " + (request ? "request" : "response") + " from Player?");
            }
        }
    }

    private void logKick(Connection source, String reason) {
        logger.error(reason + " Closing connection");
        source.disconnect(new TextComponent(reason));
    }

    @Override
    public boolean sendForm(UUID uuid, Form form) {
        ProxiedPlayer player = proxy.getPlayer(uuid);
        if (player != null) {
            player.sendData(formChannel, createFormData(form));
            return true;
        }
        return false;
    }

    @Override
    public boolean sendSkinRequest(UUID uuid, RawSkin skin) {
        ProxiedPlayer player = proxy.getPlayer(uuid);
        if (player != null) {
            player.sendData(skinChannel, createSkinRequestData(skin.encode()));
            return true;
        }
        return false;
    }
}
