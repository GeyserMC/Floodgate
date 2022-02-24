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

package org.geysermc.floodgate.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern NON_UNIQUE_PREFIX = Pattern.compile("^[a-zA-Z0-9_]{0,16}$");
    private static final Pattern DATABASE_NAME = Pattern.compile(Constants.DATABASE_NAME_FORMAT);

    /**
     * This method is used in Addons.<br> Most addons can be removed once the player associated to
     * the channel has been logged in, but they should also be removed once the inject is removed.
     * Because of how Netty works it will throw an exception and we don't want that. This method
     * removes those handlers safely.
     *
     * @param pipeline the pipeline
     * @param handler  the name of the handler to remove
     */
    public static void removeHandler(ChannelPipeline pipeline, String handler) {
        ChannelHandler channelHandler = pipeline.get(handler);
        if (channelHandler != null) {
            pipeline.remove(channelHandler);
        }
    }

    public static List<String> readAllLines(String resourcePath) throws IOException {
        InputStream stream = Utils.class.getClassLoader().getResourceAsStream(resourcePath);
        try (BufferedReader reader = newBufferedReader(stream, StandardCharsets.UTF_8)) {
            List<String> result = new ArrayList<>();
            for (; ; ) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
            return result;
        }
    }

    public static BufferedReader newBufferedReader(InputStream inputStream, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder();
        Reader reader = new InputStreamReader(inputStream, decoder);
        return new BufferedReader(reader);
    }

    public static Properties readProperties(String resourceFile) {
        Properties properties = new Properties();
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceFile)) {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static String getLocale(Locale locale) {
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    public static UUID getJavaUuid(long xuid) {
        return new UUID(0, xuid);
    }

    public static UUID getJavaUuid(String xuid) {
        return getJavaUuid(Long.parseLong(xuid));
    }

    public static boolean isUniquePrefix(String prefix) {
        return !NON_UNIQUE_PREFIX.matcher(prefix).matches();
    }

    public static boolean isValidDatabaseName(String databaseName) {
        return DATABASE_NAME.matcher(databaseName).matches();
    }

    public static int readVarInt(ByteBuf buffer) {
        int out = 0;
        int count = 0;
        byte current;
        do {
            current = buffer.readByte();
            out |= (current & 0x7F) << (count++ * 7);

            if (count > 5) {
                throw new RuntimeException("VarInt is bigger then allowed");
            }
        } while ((current & 0x80) != 0);
        return out;
    }

    public static String getStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Returns a new CompletableFuture that is already completed exceptionally with the given
     * exception.
     *
     * @param ex  the exception
     * @param <U> the type of the value
     * @return the exceptionally completed CompletableFuture
     */
    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }
}
