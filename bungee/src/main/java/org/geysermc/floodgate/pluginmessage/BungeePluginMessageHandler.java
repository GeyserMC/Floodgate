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

import static org.geysermc.floodgate.util.MessageFormatter.format;

import com.google.inject.Inject;
import com.google.inject.name.Named;
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
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.util.RawSkin;

public final class BungeePluginMessageHandler extends PluginMessageHandler implements Listener {
    private ProxyServer proxy;
    private FloodgateLogger logger;
    private String formChannel;

    public BungeePluginMessageHandler(FloodgateConfigHolder configHolder) {
        super(configHolder);
    }

    @Inject // called because this is a listener as well
    public void init(Plugin plugin, FloodgateLogger logger,
                     @Named("formChannel") String formChannel,
                     @Named("skinChannel") String skinChannel) {
        this.proxy = plugin.getProxy();
        this.logger = logger;
        this.formChannel = formChannel;

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
                    logger.error("Invalid form response! Closing connection");
                    source.disconnect(new TextComponent("Invalid form response!"));
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
                    logger.error(format(
                            "Couldn't find stored form with id {} for player {}",
                            formId, ((ProxiedPlayer) source).getName()));
                }
            }
        }
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
    public boolean sendSkinRequest(UUID player, RawSkin skin) {
        return false; //todo
    }

    @Override
    public void sendSkinResponse(UUID player, String response) {

    }
}
