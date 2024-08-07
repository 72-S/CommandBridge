package org.commandbridge.utilities;

import org.commandbridge.CommandBridge;
import org.slf4j.Logger;

public class VerboseLogger {
    private final CommandBridge plugin;
    private final Logger logger;

    public VerboseLogger(CommandBridge plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void info(String message) {
        if (plugin.isVerboseOutputEnabled()) {
            logger.info(message);
        }
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void error(String message, Throwable e) {
        if (plugin.isVerboseOutputEnabled()) {
            logger.error(message, e);
        } else {
            logger.error(message);
        }
    }

    public void forceInfo(String message) {
        logger.info(message);
    }

}
