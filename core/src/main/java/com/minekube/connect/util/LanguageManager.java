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

package com.minekube.connect.util;

import com.google.common.base.Joiner;
import com.minekube.connect.api.logger.ConnectLogger;
import com.minekube.connect.config.ConfigHolder;
import com.minekube.connect.config.ConnectConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Manages translations for strings
 */
@RequiredArgsConstructor
public final class LanguageManager {
    private final Map<String, Properties> localeMappings = new HashMap<>();
    private final ConfigHolder configHolder;
    private final ConnectLogger logger;

    /**
     * The locale used in console and as a fallback
     */
    @Getter private String defaultLocale;

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

    public boolean isLoaded() {
        return logger != null && defaultLocale != null;
    }

    /**
     * Tries to load the log's locale file once a string has been requested
     */
    private void init() {
        if (!loadLocale("en_US")) {// Fallback
            logger.error("Failed to load the fallback language. This will likely cause errors!");
        }

        ConnectConfig config = configHolder.get();
        if (config == null) {
            // :thonk:
            return;
        }

        defaultLocale = formatLocale(config.getDefaultLocale());

        if (isValidLanguage(defaultLocale)) {
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
     * Loads a locale from resources; if the file doesn't exist it just logs a warning
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

        InputStream localeStream = LanguageManager.class.getClassLoader().getResourceAsStream(
                "languages/texts/" + formatLocale + ".properties");

        // load the locale
        if (localeStream != null) {
            Properties localeProp = new Properties();

            try (Reader reader = new InputStreamReader(localeStream, StandardCharsets.UTF_8)) {
                localeProp.load(reader);
            } catch (Exception e) {
                throw new AssertionError("Failed to load locale", e);
            }

            // insert the locale into the mappings
            localeMappings.put(formatLocale, localeProp);
            return true;
        }

        logger.warn("Missing locale file: " + formatLocale);
        return false;
    }

    /**
     * Get a formatted language string with the default locale
     *
     * @param key    language string to translate
     * @param values values to put into the string
     * @return translated string or "key arg1, arg2 (etc.)" if it was not found in the given locale
     */
    public String getLogString(String key, Object... values) {
        return getString(key, defaultLocale, values);
    }

    /**
     * Get a formatted language string with the given locale
     *
     * @param key    language string to translate
     * @param locale locale to translate to
     * @param values values to put into the string
     * @return translated string or "key arg1, arg2 (etc.)" if it was not found in the given locale
     */
    public String getString(String key, String locale, Object... values) {
        if (!isLoaded()) {
            init();
            // we can skip everything if the LanguageManager can't be loaded yet
            if (!isLoaded()) {
                return formatNotFound(key, values);
            }
        }

        Properties properties = localeMappings.get(locale);
        String formatString = null;

        if (properties != null) {
            formatString = properties.getProperty(key);
        }

        // try and get the key from the default locale
        if (formatString == null) {
            properties = localeMappings.get(defaultLocale);
            if (properties != null) {
                formatString = properties.getProperty(key);
            }
        }

        // key wasn't found
        if (formatString == null) {
            return formatNotFound(key, values);
        }

        //todo don't use color codes in the strings
        return MessageFormat.format(formatString.replace("'", "''").replace("&", "\u00a7"), values);
    }

    /**
     * Ensures that the given locale is supported
     *
     * @param locale the locale to validate
     * @return true if the given locale is supported
     */
    private boolean isValidLanguage(String locale) {
        if (locale == null) {
            return false;
        }

        URL languageFile = LanguageManager.class
                .getResource("/languages/texts/" + locale + ".properties");

        if (languageFile == null) {
            logger.warn(locale + " is not a supported language.");
            return false;
        }
        return true;
    }

    private String formatNotFound(String key, Object... args) {
        return key + " " + Joiner.on(", ").join(args);
    }
}
