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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.PreProcess;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.logger.FloodgateLogger;
import org.geysermc.floodgate.core.platform.command.MessageType;
import org.geysermc.floodgate.core.platform.command.Placeholder;

/**
 * Manages translations for strings in Floodgate
 */
@Singleton
public final class LanguageManager {
    private final Map<String, Properties> localeMappings = new HashMap<>();

    @Inject FloodgateLogger logger;

    /**
     * The locale used in console and as a fallback
     */
    @Getter String defaultLocale;

    /**
     * Cleans up and formats a locale string
     *
     * @param locale the locale to format
     * @return the formatted locale
     */
    private static String formatLocale(String locale) {
        try {
            String[] parts = locale.toLowerCase(Locale.ROOT).split("_");
            return parts[0] + "_" + parts[1].toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return locale;
        }
    }

    /**
     * Tries to load the log's locale file once a string has been requested
     */
    @Inject
    public void init(FloodgateConfig config) {
        if (!loadLocale("en_US")) {
            logger.error("Failed to load the fallback language. This will likely cause errors!");
        }
        defaultLocale = formatLocale(config.defaultLocale());

        if (!"system".equals(defaultLocale) && isValidLanguage(defaultLocale)) {
            if (loadLocale(defaultLocale)) {
                return;
            }
            logger.warn("Language provided in the config wasn't found. Will use system locale.");
        }

        String systemLocale = formatLocale(Utils.getLocale(Locale.getDefault()));

        if (isValidLanguage(systemLocale)) {
            if (loadLocale(systemLocale)) {
                defaultLocale = systemLocale;
                return;
            }
            logger.warn("Language file for system locale wasn't found. Falling back to en_US");
        }

        defaultLocale = "en_US";
    }

    /**
     * Loads a Floodgate locale from resources; if the file doesn't exist it just logs a warning
     *
     * @param locale locale to load
     * @return true if the locale has been found
     */
    public boolean loadLocale(String locale) {
        String formatLocale = formatLocale(locale);

        // just return if the locale has been loaded already
        if (localeMappings.containsKey(formatLocale)) {
            return true;
        }

        Properties properties =
                Utils.readProperties("languages/texts/" + formatLocale + ".properties");

        if (properties != null) {
            localeMappings.put(formatLocale, properties);
            return true;
        }

        logger.warn("Missing locale file: " + formatLocale);
        return false;
    }

    /**
     * Get a formatted language string with the given locale for Floodgate
     *
     * @param key    language string to translate
     * @param locale locale to translate to
     * @param type determines the scale of the message (and its arguments)
     * @param placeholders values to put into the string
     * @return translated string or "key arg1, arg2 (etc.)" if it was not found in the given locale
     */
    public Component getString(String key, @Nullable String locale, MessageType type, Placeholder... placeholders) {
        var translated = rawTranslation(key, locale);
        if (translated == null) {
            translated = formatNotFound(key, placeholders);
        }
        return MiniMessageUtils.formatMessage(translated, type, placeholders);
    }

    public String rawTranslation(String key, @Nullable String locale) {
        Properties properties = localeMappings.get(locale);
        String translated = null;

        if (properties != null) {
            translated = properties.getProperty(key);
        }

        // try and get the key from the default locale
        if (translated == null) {
            properties = localeMappings.get(defaultLocale);
            translated = properties.getProperty(key);
        }

        return translated;
    }

    /**
     * Ensures that the given locale is supported by Floodgate
     *
     * @param locale the locale to validate
     * @return true if the given locale is supported by Floodgate
     */
    private boolean isValidLanguage(String locale) {
        if (locale == null) {
            return false;
        }

        URL languageFile = LanguageManager.class
                .getResource("/languages/texts/" + locale + ".properties");

        if (languageFile == null) {
            logger.warn(locale + " is not a supported Floodgate language.");
            return false;
        }
        return true;
    }

    private String formatNotFound(String key, Placeholder... placeholders) {
        return key + ' ' + Arrays.stream(placeholders)
                .map((resolver) -> {
                    if (resolver.tag() instanceof PreProcess preProcess) {
                        return resolver.key() + ": " + preProcess.value();
                    } else if (resolver.tag() instanceof Inserting inserting) {
                        return resolver.key() + ": " + MiniMessage.miniMessage().serialize(inserting.value());
                    }
                    return resolver.key() + " (unknown tag)";
                })
                .collect(Collectors.joining(", "));
    }
}
