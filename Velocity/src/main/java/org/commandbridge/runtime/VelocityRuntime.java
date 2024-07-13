package org.commandbridge.runtime;

import com.velocitypowered.api.proxy.ProxyServer;
import org.commandbridge.CommandBridge;
import org.commandbridge.utilities.VerboseLogger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class VelocityRuntime {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final File scriptsFolder = new File("plugins/CommandBridgeVelocity/scripts");

    public VelocityRuntime(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.verboseLogger = plugin.getVerboseLogger();
    }

    public void loadScripts() {
        verboseLogger.info("Loading scripts...");
        unloadScripts();

        if (!scriptsFolder.exists() || !scriptsFolder.isDirectory()) {
            verboseLogger.warn("Scripts folder does not exist or is not a directory: " + scriptsFolder.getPath());
            return;
        }

        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            verboseLogger.warn("No scripts found in folder: " + scriptsFolder.getPath());
            return;
        }

        for (File file : files) {
            verboseLogger.info("Loading script file: " + file.getName());
            try (InputStream input = new FileInputStream(file)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);

                if (Boolean.TRUE.equals(data.get("enabled"))) {
                    plugin.getCommandRegistrar().registerCommand(data);
                    verboseLogger.forceInfo("Command registered successfully from script: " + file.getName());
                } else {
                    verboseLogger.info("Script disabled, skipping file: " + file.getName());
                }
            } catch (IOException e) {
                verboseLogger.error("IOException occurred while accessing script file: " + file.getName(), e);
            } catch (YAMLException e) {
                verboseLogger.error("YAMLException occurred while parsing script file: " + file.getName(), e);
            } catch (ClassCastException e) {
                verboseLogger.error("ClassCastException occurred - invalid format in script file: " + file.getName(), e);
            } catch (Exception e) {
                verboseLogger.error("Unexpected error occurred while loading script file: " + file.getName(), e);
            }
        }

        verboseLogger.info("Scripts loading process completed.");
    }

    public void unloadScripts() {
        verboseLogger.info("Unloading all registered scripts...");

        ArrayList<String> registeredCommands = new ArrayList<>(plugin.getRegisteredCommands());
        for (String command : registeredCommands) {
            server.getCommandManager().unregister(command);
            verboseLogger.info("Command unregistered successfully: " + command);
        }

        plugin.clearRegisteredCommands();
        verboseLogger.info("All commands have been unregistered and cleared.");
    }
}
