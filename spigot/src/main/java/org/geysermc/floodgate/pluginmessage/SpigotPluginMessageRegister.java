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

package org.geysermc.floodgate.pluginmessage;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.skin.ServerSkinHandler;
import org.geysermc.floodgate.util.Base64Utils;
import org.geysermc.floodgate.util.RawSkin;

@RequiredArgsConstructor
public class SpigotPluginMessageRegister {
    private final JavaPlugin plugin;
    private final FloodgateApi api;
    private final String formChannel;
    private final String skinChannel;
    private final SpigotPluginMessageHandler pluginMessageHandler;
    private final ServerSkinHandler skinHandler;
    private final FloodgateLogger logger;

    public void register() {
        Messenger messenger = plugin.getServer().getMessenger();

        // form
        messenger.registerIncomingPluginChannel(
                plugin, formChannel,
                (channel, player, message) ->
                        pluginMessageHandler.callResponseConsumer(message));

        messenger.registerOutgoingPluginChannel(plugin, formChannel);

        // skin
        messenger.registerIncomingPluginChannel(
                plugin, skinChannel,
                (channel, player, message) -> {
                    //todo make a Proxy and a Server class for this?
                    FloodgatePlayer floodgatePlayer = api.getPlayer(player.getUniqueId());
                    if (floodgatePlayer == null) {
                        logKick(player, "Non-Floodgate player sent a Skin plugin message.");
                        return;
                    }

                    // non-proxy servers can only handle requests (from proxies)

                    if (!floodgatePlayer.isFromProxy()) {
                        logKick(player, "Cannot receive Skin request from Player.");
                        return;
                    }

                    // 1 byte for isRequest and 9 for RawSkin itself
                    if (message.length < Base64Utils.getEncodedLength(9 + 1)) {
                        logKick(player, "Skin request data has to be at least 10 byte long.");
                        return;
                    }

                    boolean request = message[0] == 1;

                    if (!request) {
                        logKick(player, "Proxy sent a response instead of a request?");
                        return;
                    }

                    RawSkin rawSkin = null;
                    try {
                        rawSkin = RawSkin.decode(message, 1);
                    } catch (Exception exception) {
                        logger.error("Failed to decode RawSkin", exception);
                    }
                    // we let it continue since SkinHandler sends the plugin message for us
                    skinHandler.handleSkinUploadFor(floodgatePlayer, rawSkin);
                }
        );
        
        messenger.registerOutgoingPluginChannel(plugin, skinChannel);
    }

    private void logKick(Player player, String reason) {
        logger.error(reason + " Closing connection for " + player.getName());
        player.kickPlayer(reason);
    }
}
