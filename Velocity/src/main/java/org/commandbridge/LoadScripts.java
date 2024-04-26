package org.commandbridge;

import com.google.inject.Inject;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
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

        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (!(source instanceof Player)) {
                        logger.warn("This command can only be used by a player.");
                        return 0;
                    }
                    Player player = (Player) source;

                    if (!player.hasPermission("commandbridge.command." + commandName)) {
                        player.sendMessage(Component.text("You do not have permission to use this command.", net.kyori.adventure.text.format.NamedTextColor.RED));
                        return 0;
                    }

                    for (Map<String, Object> cmdData : commandList) {
                        String cmd = parsePlaceholders((String) cmdData.get("command"), player);
                        int delay = (int) cmdData.getOrDefault("delay", 0);
                        String targetServerId = (String) cmdData.get("target-server-id");
                        String targetExecutor = (String) cmdData.getOrDefault("target-executor", "console");
                        boolean waitForOnline = (boolean) cmdData.getOrDefault("wait-until-player-is-online", false);

                        if (delay > 0) {
                            server.getScheduler().buildTask(plugin, () -> executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player))
                                    .delay(delay, TimeUnit.SECONDS)
                                    .schedule();
                        } else {
                            executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player);
                        }
                    }
                    return 1;
                })
                .build();

        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        server.getCommandManager().register(brigadierCommand);
        registeredCommands.add(commandName);
        logger.info("Command '{}' registered successfully.", commandName);
    }

    private String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getUsername());
    }

    private void executeCommand(String command, String targetServerId, String targetExecutor, boolean waitForOnline, Player playerMessage) {
        server.getServer(targetServerId).ifPresent(serverConnection -> {
            if (waitForOnline) {
                // Überprüft, ob der Spieler online ist und führt den Befehl aus oder plant eine erneute Überprüfung
                serverConnection.getPlayersConnected().stream()
                        .filter(player -> player.getUsername().equals(command.split(" ")[1])) // Annahme: Der Spielername ist das erste Argument nach dem Befehl.
                        .findFirst()
                        .ifPresentOrElse(player -> {
                            sendCommandToBukkit(command, targetServerId, targetExecutor);
                            logger.info("Executing command on server {}: {}", targetServerId, command);
                        }, () -> {
                            // Spieler ist nicht online, warte und versuche es erneut.
                            server.getScheduler().buildTask(plugin, () -> executeCommand(command, targetServerId, targetExecutor, true, playerMessage))
                                    .delay(1, TimeUnit.SECONDS)
                                    .schedule();
                            logger.info("Waiting for player to be online on server {}: {}", targetServerId, command);
                        });
            } else {
                // Überprüft, ob der Spieler online ist und führt den Befehl aus oder gibt eine Meldung aus, dass der Spieler nicht online ist
                serverConnection.getPlayersConnected().stream()
                        .filter(player -> player.getUsername().equals(command.split(" ")[1]))
                        .findFirst()
                        .ifPresentOrElse(player -> {
                            sendCommandToBukkit(command, targetServerId, targetExecutor);
                            logger.info("Executing command: '{}'", command);
                        }, () -> {
                            logger.warn("Player is not online on server {}: {}", targetServerId, command);
                            playerMessage.sendMessage(Component.text("You must be on the server " + targetServerId + " to use this command.", net.kyori.adventure.text.format.NamedTextColor.RED));
                        });
            }
        });
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

