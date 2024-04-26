package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Logger {
    private boolean verboseOutput;
    private final JavaPlugin plugin;

    public Logger(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
    this.verboseOutput = plugin.getConfig().getBoolean("verbose-output", false);
}
    
    public void info(String message) {
        if (verboseOutput) {
            plugin.getLogger().info(message);
        }
    }
    
    public void warn(String message) {
        if (verboseOutput) {
            plugin.getLogger().warning(message);
        }
    }
    
    public void error(String message, Throwable e) {
        if (verboseOutput) {
            plugin.getLogger().severe(message + " : " + e.getMessage());
        }
    }
}
