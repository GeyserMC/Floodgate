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

package com.minekube.connect.addon.debug;

import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.util.Constants;
import com.minekube.connect.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import java.nio.charset.StandardCharsets;

public class StateChangeDetector {
    private static volatile int pluginMessageToClientId = -1;
    private static volatile int pluginMessageToServerId = -1;

    private final Channel channel;
    private final ConnectLogger logger;
    private final String packetEncoderName;
    private final String packetDecoderName;

    private volatile boolean enableCompressionNext;
    private volatile State currentState = State.HANDSHAKE;

    public StateChangeDetector(
            Channel channel,
            String packetEncoderName,
            String packetDecoderName,
            ConnectLogger logger) {
        this.channel = channel;
        this.packetEncoderName = packetEncoderName;
        this.packetDecoderName = packetDecoderName;
        this.logger = logger;
    }

    /**
     * Checks (and acts) if the current packet is one of the packets that we need to switch states.
     *
     * @param packet     the packet to check
     * @param fromClient if the packet is clientbound or serverbound
     */
    public void checkPacket(ByteBuf packet, boolean fromClient) {
        int index = packet.readerIndex();

        if (enableCompressionNext) {
            // data length
            Utils.readVarInt(packet);

            fixCompressionPipes();
            enableCompressionNext = false;
        }

        int packetId = Utils.readVarInt(packet);

        if (fromClient) {
            if (currentState == State.HANDSHAKE && packetId == Constants.HANDSHAKE_PACKET_ID) {
                // have to read the content to determine the next state

                // protocol version
                Utils.readVarInt(packet);
                // server address
                int hostLength = Utils.readVarInt(packet);
                // read server address + port (short = 2 bytes)
                packet.readerIndex(packet.readerIndex() + hostLength + 2);
                // next state
                currentState = State.getById(Utils.readVarInt(packet));
            }
        } else {
            if (currentState == State.LOGIN) {
                if (packetId == Constants.LOGIN_SUCCESS_PACKET_ID) {
                    currentState = State.PLAY;
                }
                if (packetId == Constants.SET_COMPRESSION_PACKET_ID) {
                    enableCompressionNext = true;
                }
            }
        }

        // reset index
        packet.readerIndex(index);
    }

    private void fixCompressionPipes() {
        // The previous packet was the compression packet, meaning that starting with this
        // packet the data can already be compressed. The compression handler has been added
        // directly before the packet encoder and decoder, so we have to reclaim that place.
        // If we don't, we'll see the compressed data.

        ChannelPipeline pipeline = channel.pipeline();

        ChannelHandler outDebug = pipeline.remove(ChannelOutDebugHandler.class);
        ChannelHandler inDebug = pipeline.remove(ChannelInDebugHandler.class);

        pipeline.addBefore(packetEncoderName, "connect_debug_out", outDebug);
        pipeline.addBefore(packetDecoderName, "connect_debug_in", inDebug);
    }

    public boolean shouldPrintPacket(ByteBuf packet, boolean clientbound) {
        return Constants.PRINT_ALL_PACKETS ||
                currentState == State.HANDSHAKE || currentState == State.LOGIN ||
                currentState != State.STATUS && shouldPrintPlayPacket(packet, clientbound);
    }

    public boolean shouldPrintPlayPacket(ByteBuf packet, boolean clientbound) {
        int index = packet.readerIndex();

        int packetId = Utils.readVarInt(packet);

        // we're only interested in the plugin message packets

        // use cached packet ids
        if (clientbound && pluginMessageToClientId != -1) {
            return pluginMessageToClientId == packetId;
        }
        if (!clientbound && pluginMessageToServerId != -1) {
            return pluginMessageToServerId == packetId;
        }

        boolean shouldPrint = false;

        if (packet.isReadable()) {
            // format plugin message packet: channel - remaining data
            try {
                int channelLength = Utils.readVarInt(packet);

                if (channelLength >= 1 && channelLength <= 128 &&
                        packet.isReadable(channelLength)) {

                    byte[] channelBytes = new byte[channelLength];
                    packet.readBytes(channelBytes);

                    String channelName = new String(channelBytes, StandardCharsets.UTF_8);
                    if (channelName.contains(":")) {
                        // some other packets will still match,
                        // but since plugin message packets are send early on
                        // we can almost know for certain that this is a plugin message packet
                        printIdentified(clientbound, packetId);
                        if (clientbound) {
                            pluginMessageToClientId = packetId;
                        } else {
                            pluginMessageToServerId = packetId;
                        }
                        shouldPrint = true;
                    }
                }
            } catch (RuntimeException ignored) {
                // not the plugin message packet
            }
        }

        // reset index
        packet.readerIndex(index);

        return shouldPrint;
    }

    private void printIdentified(boolean clientbound, int packetId) {
        logger.info(
                "Identified plugin message packet ({}) as {} ({})",
                clientbound ? "clientbound" : "serverbound",
                packetId,
                Integer.toHexString(packetId)
        );
    }

    public State getCurrentState() {
        return currentState;
    }
}
