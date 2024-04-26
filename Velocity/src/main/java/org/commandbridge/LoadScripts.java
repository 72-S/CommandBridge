package org.commandbridge;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoadScripts {

    private final ProxyServer server;
    private final Logger logger;
    private final CommandBridge plugin;
    private final File scriptsFolder;
    private final Path dataDirectory = Path.of("plugins", "CommandBridgeVelocity");
    private final List<String> registeredCommands = new ArrayList<>();

    @Inject
    public LoadScripts(ProxyServer server, Logger logger, CommandBridge plugin) {
        this.server = server;
        this.logger = logger;
        this.plugin = plugin;
        this.scriptsFolder = new File("plugins/CommandBridgeVelocity/scripts");
        copyExampleYml();
    }

    private void copyExampleYml() {
        Path scriptFolder = dataDirectory.resolve("scripts");
        try {
            Files.createDirectories(scriptFolder); // Stellt sicher, dass der Ordner existiert
            Path exampleScript = scriptFolder.resolve("example.yml");
            if (Files.notExists(exampleScript)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("example.yml")) {
                    if (in == null) {
                        logger.warn("Could not find example.yml in resources");
                        return;
                    }
                    Files.copy(in, exampleScript, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("example.yml has been copied successfully.");
                }
            }
        } catch (IOException e) {
            logger.error("Failed to create scripts folder or copy example.yml", e);
        }
    }

    public void loadScripts() {
        unloadScripts();  // Deregister all commands before loading new ones
        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            logger.info("No scripts found.");
            return;
        }

        for (File file : files) {
            try (InputStream input = new FileInputStream(file)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                if (Boolean.TRUE.equals(data.get("enabled"))) {
                    registerCommand(data);
                } else {
                    logger.info("Skipping disabled command in {}", file.getName());
                }
            } catch (Exception e) {
                logger.error("Failed to load or parse script file: {}", file.getName(), e);
            }
        }
    }

    private void unloadScripts() {
        // Method to unload scripts
        for (String command : registeredCommands) {
            server.getCommandManager().unregister(command);
            logger.info("Command '{}' unregistered successfully.", command);
        }
        registeredCommands.clear();
    }

    private void registerCommand(Map<String, Object> commandData) {
        String commandName = (String) commandData.get("name");
        List<Map<String, Object>> commandList = (List<Map<String, Object>>) commandData.get("commands");
        if (commandName == null || commandList == null || commandList.isEmpty()) {
            logger.warn("Command name or command list is missing or empty in config.");
            return;
        }
        registeredCommands.add(commandName);
        server.getCommandManager().register(commandName, new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                if (!(invocation.source() instanceof Player)) {
                    logger.warn("This command can only be used by a player.");
                    return;
                }
                Player player = (Player) invocation.source();

                for (Map<String, Object> cmdData : commandList) {
                    String cmd = parsePlaceholders((String) cmdData.get("command"), player);
                    int delay = (int) cmdData.getOrDefault("delay", 0);
                    String targetServerId = (String) cmdData.get("target-server-id");
                    String targetExecutor = (String) cmdData.getOrDefault("target-executor", "console");

                    if (delay > 0) {
                        server.getScheduler().buildTask(plugin, () -> executeCommand(cmd, targetServerId, targetExecutor))
                                .delay(delay, TimeUnit.SECONDS)
                                .schedule();
                    } else {
                        executeCommand(cmd, targetServerId, targetExecutor);
                    }
                }
            }
        }, commandName);

        logger.info("Command '{}' registered successfully.", commandName);
    }

    private String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getUsername());
    }

    private void executeCommand(String command, String targetServerId, String targetExecutor) {
        // Method to send the command to Bukkit servers
        sendCommandToBukkit(command, targetServerId, targetExecutor);
        logger.info("Executing command: '{}'", command);
    }

    private void sendCommandToBukkit(String command, String targetServerId, String targetExecutor) {
        // Implementation to send the command to the Bukkit server
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.writeUTF("ExecuteCommand");
            out.writeUTF(targetServerId);
            out.writeUTF(targetExecutor);
            out.writeUTF(command);
            server.getAllServers().stream()
                    .filter(serverConnection -> serverConnection.getServerInfo().getName().equals(targetServerId))
                    .forEach(serverConnection -> {
                        serverConnection.sendPluginMessage(MinecraftChannelIdentifier.create("commandbridge", "main"), byteOut.toByteArray());
                        logger.info("Command sent to Bukkit server {}: {}", targetServerId, command);
                    });
        } catch (IOException e) {
            logger.error("Failed to send command to Bukkit", e);
        }
    }


}

