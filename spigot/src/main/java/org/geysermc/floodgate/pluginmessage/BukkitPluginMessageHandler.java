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

import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.geysermc.common.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.platform.pluginmessage.PluginMessageHandler;
import org.geysermc.floodgate.util.ReflectionUtils;

public class BukkitPluginMessageHandler extends PluginMessageHandler {
    private final JavaPlugin plugin;
    private final String formChannel;
    private final String skinChannel;

    public BukkitPluginMessageHandler(JavaPlugin plugin, String formChannel, String skinChannel) {
        this.plugin = plugin;
        this.formChannel = formChannel;
        this.skinChannel = skinChannel;

        Messenger messenger = plugin.getServer().getMessenger();

        // form
        messenger.registerIncomingPluginChannel(
                plugin, formChannel,
                (channel, player, message) -> callResponseConsumer(message));
        messenger.registerOutgoingPluginChannel(plugin, formChannel);

        // skin
        messenger.registerIncomingPluginChannel(
                plugin, skinChannel,
                (channel, player, message) -> {
                    String origin =
                            FloodgateApi.getInstance().getPlayer(player.getUniqueId()) != null
                                    ? "Geyser" : "player";
                    System.out.println("Got skin from " + origin + "!");
                }
        );
    }

    @Override
    public boolean sendForm(UUID playerId, Form form) {
        try {
            Player player = Bukkit.getPlayer(playerId);
            //todo improve
            Set<String> channels = ReflectionUtils.getCastedValue(player, "channels");
            if (channels != null) {
                channels.add("floodgate:form");
            }
            player.sendPluginMessage(plugin, formChannel, createFormData(form));
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }
}
