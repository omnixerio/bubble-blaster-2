package dev.ultreon.gameprovider.bubbles;

import dev.ultreon.logging.Logger;
import dev.ultreon.logging.LoggerManager;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogHandler;
import net.fabricmc.loader.impl.util.log.LogLevel;

public class BB2LogHandler implements LogHandler {
    private static final Logger LOGGER = LoggerManager.getLogger("FabricLoader");

    @Override
    public void log(long time, LogLevel level, LogCategory category, String msg, Throwable exc, boolean fromReplay, boolean wasSuppressed) {
        LOGGER.log(time, BB2LogHandler.getLevel(level), dev.ultreon.logging.LogCategory.DEFAULT, msg, exc, fromReplay, wasSuppressed);
    }

    @Override
    public boolean shouldLog(LogLevel level, LogCategory category) {
        return LOGGER.isEnabled(BB2LogHandler.getLevel(level));
    }

    private static dev.ultreon.logging.LogLevel getLevel(LogLevel level) {
        return switch (level) {
            case INFO -> dev.ultreon.logging.LogLevel.INFO;
            case WARN -> dev.ultreon.logging.LogLevel.WARN;
            case DEBUG -> dev.ultreon.logging.LogLevel.DEBUG;
            case ERROR -> dev.ultreon.logging.LogLevel.ERROR;
            case TRACE -> dev.ultreon.logging.LogLevel.TRACE;
        };
    }

    @Override
    public void close() {

    }
}
