package org.geysermc.floodgate.mod.logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.geysermc.floodgate.api.logger.FloodgateLogger;
import org.geysermc.floodgate.core.config.FloodgateConfig;
import org.geysermc.floodgate.core.util.LanguageManager;

import static org.geysermc.floodgate.core.util.MessageFormatter.format;

@Singleton
public final class Log4jFloodgateLogger implements FloodgateLogger {
    @Inject
    @Named("logger")
    private Logger logger;
    private LanguageManager languageManager;

    @Inject
    private void init(LanguageManager languageManager, FloodgateConfig config) {
        this.languageManager = languageManager;
        if (config.isDebug() && !logger.isDebugEnabled()) {
            Configurator.setLevel(logger.getName(), Level.DEBUG);
        }
    }

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
    public boolean isDebug() {
        return logger.isDebugEnabled();
    }
}
