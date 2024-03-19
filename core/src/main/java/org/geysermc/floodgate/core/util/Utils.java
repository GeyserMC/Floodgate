/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.floodgate.core.util;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import org.geysermc.floodgate.core.crypto.RandomUtils;

public class Utils {
    private static final Pattern NON_UNIQUE_PREFIX = Pattern.compile("^\\w{0,16}$");
    private static final Random RANDOM = RandomUtils.secureRandom();
    private static final BigInteger MAX_LONG_VALUE = BigInteger.ONE.shiftLeft(64);

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

    /**
     * Reads a properties resource file
     * @param resourceFile the resource file to read
     * @return the properties file if the resource exists, otherwise null
     * @throws AssertionError if something went wrong while readin the resource file
     */
    public static Properties readProperties(String resourceFile) {
        Properties properties = new Properties();
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(resourceFile)) {
            if (is == null) {
                return null;
            }
            properties.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AssertionError("Failed to read properties file", e);
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

    public static UUID fromShortUniqueId(String uuid) {
        var bigInt = new BigInteger(uuid, 16);
        return new UUID(bigInt.shiftRight(64).longValue(), bigInt.xor(MAX_LONG_VALUE).longValue());
    }

    public static boolean isUniquePrefix(String prefix) {
        return !NON_UNIQUE_PREFIX.matcher(prefix).matches();
    }

    public static String generateCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(generateCodeChar());
        }
        return code.toString();
    }

    public static char generateCodeChar() {
        var codeChar = RANDOM.nextInt() % (10 + 26);
        if (codeChar < 10) {
            return (char) ('0' + codeChar);
        }
        return (char) ('A' + codeChar);
    }

    public static String getStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
