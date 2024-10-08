package org.commandbridge.core.utilities;

import org.commandbridge.CommandBridge;

import java.util.logging.Logger;

public class VerboseLogger {
    private boolean verboseOutput;
    private final CommandBridge plugin;
    private final Logger logger;

    public VerboseLogger(CommandBridge plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void loadConfig() {
    this.verboseOutput = plugin.getConfig().getBoolean("verbose-output", false);
}
    
    public void info(String message) {
        if (verboseOutput) {
            logger.info(message);
        }
    }

    public void forceInfo(String message) {
        logger.info(message);
    }

    public void warn(String message) {
            logger.warning(message);
    }
    
    public void error(String message, Throwable e) {
        if (verboseOutput) {
            logger.severe(message + " : " + e.getMessage());
        } else {
            logger.severe(message);
        }
    }
}
