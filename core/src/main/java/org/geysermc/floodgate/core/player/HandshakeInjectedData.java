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

package org.geysermc.floodgate.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.geysermc.api.connection.Connection;

public final class HandshakeInjectedData {
// TIM I PROMISE THIS IS JUST AN IDEA
//    public static Connection read() {
//        FloodgatePlayerImpl.FloodgatePlayerImplBuilder builder = FloodgatePlayerImpl.builder();
//
//    }
//
//    public static String write(Connection connection) {
//        ByteBuf buf = Unpooled.buffer();
//        writeString(connection.version(), buf);
//        writeString(connection.bedrockUsername(), buf);
//        writeString(connection.xuid(), buf);
//        writeVarInt(connection.platform().ordinal(), buf);
//        writeString(connection.languageCode(), buf);
//        writeVarInt(connection.uiProfile().ordinal(), buf);
//        writeVarInt(connection.inputMode().ordinal(), buf);
//        InetSocketAddress address = (InetSocketAddress) connection.socketAddress();
//        writeString(address.getHostString(), buf); // TODO
//        buf.writeShort(address.getPort());
//        buf.writeBoolean(connection.isLinked());
//        if (connection.isLinked()) {
//            writeString(connection.javaUsername(), buf);
//            UUID uuid = connection.javaUuid();
//            buf.writeLong(uuid.getMostSignificantBits());
//            buf.writeLong(uuid.getLeastSignificantBits());
//        }
//    }
//
//    private static int readVarInt(ByteBuf buf) {
//
//    }
//
//    private static void writeVarInt(int i, ByteBuf buf) {
//
//    }
//
//    private static String readString(ByteBuf buf) {
//        byte[] bytes = new byte[readVarInt(buf)];
//        buf.readBytes(bytes);
//        return new String(bytes, StandardCharsets.UTF_8);
//    }
//
//    private static void writeString(String s, ByteBuf buf) {
//        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
//        writeVarInt(bytes.length, buf);
//        buf.writeBytes(bytes);
//    }
}
