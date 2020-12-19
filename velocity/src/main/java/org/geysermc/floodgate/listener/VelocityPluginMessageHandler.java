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

package org.geysermc.floodgate.listener;

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
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.geysermc.cumulus.Form;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.config.FloodgateConfigHolder;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.util.RawSkin;

public class VelocityPluginMessageHandler extends PluginMessageHandler {
    private ProxyServer proxy;
    private FloodgateLogger logger;
    private ChannelIdentifier formChannel;

    public VelocityPluginMessageHandler(FloodgateConfigHolder configHolder) {
        super(configHolder);
    }

    @Inject // called because this is a listener as well
    public void init(ProxyServer proxy, FloodgateLogger logger,
                     @Named("formChannel") String formChannelName,
                     @Named("skinChannel") String skinChannelName) {
        this.proxy = proxy;
        this.logger = logger;

        formChannel = MinecraftChannelIdentifier.from(formChannelName);

        proxy.getChannelRegistrar().register(formChannel);
        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from(skinChannelName));
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
                    logger.error("Invalid form response! Closing connection");
                    ((Player) source).disconnect(Component.text("Invalid form response!"));
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

        // proxies don't have to receive anything from the skins channel, they only have to send
    }

    @Override
    public boolean sendForm(UUID uuid, Form form) {
        return proxy.getPlayer(uuid)
                .map(value -> value.sendPluginMessage(formChannel, createFormData(form)))
                .orElse(false);
    }

    @Override
    public boolean sendSkinRequest(UUID player, RawSkin skin) {
        return false; //todo
    }
}
