package com.geysermc.floodgate.logger;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.util.LanguageManager;

import static org.geysermc.floodgate.util.MessageFormatter.format;

@RequiredArgsConstructor
public class Log4jFloodgateLogger implements FloodgateLogger {
    private final Logger logger;
    private final LanguageManager languageManager;

    @Override
    public void error(String message, Object... args) {
        logger.error(message, args);
    }

    @Override
    public void error(String message, Throwable throwable, Object... args) {
        logger.error(format(message, args), throwable);
    }

    @Override
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    @Override
    public void info(String message, Object... args) {
        logger.info(message, args);
    }

    @Override
    public void translatedInfo(String message, Object... args) {
        logger.info(languageManager.getLogString(message, args));
    }

    @Override
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    @Override
    public void trace(String message, Object... args) {
        logger.trace(message, args);
    }

    @Override
    public void enableDebug() {
        if (!logger.isDebugEnabled()) {
            Configurator.setLevel(logger.getName(), Level.DEBUG);
        }
    }

    @Override
    public void disableDebug() {
        if (logger.isDebugEnabled()) {
            Configurator.setLevel(logger.getName(), Level.INFO);
        }
    }

    @Override
    public boolean isDebug() {
        return logger.isDebugEnabled();
    }
}
