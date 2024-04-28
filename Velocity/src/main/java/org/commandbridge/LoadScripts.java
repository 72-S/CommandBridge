package org.commandbridge;

import com.google.inject.Inject;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadScripts {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final File scriptsFolder;
    private final Path dataDirectory = Path.of("plugins", "CommandBridgeVelocity");
    private final List<String> registeredCommands = new ArrayList<>();
    

    @Inject
    public LoadScripts(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.verboseLogger = plugin.getVerboseLogger();
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
                        verboseLogger.warn("Could not find example.yml in resources");
                        return;
                    }
                    Files.copy(in, exampleScript, StandardCopyOption.REPLACE_EXISTING);
                    verboseLogger.info("example.yml has been copied successfully.");
                }
            }
        } catch (IOException e) {
            verboseLogger.error("Failed to create scripts folder or copy example.yml", e);
        }
    }

    public void loadScripts() {
        unloadScripts();  // Deregister all commands before loading new ones
        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            verboseLogger.info("No scripts found.");
            return;
        }

        for (File file : files) {
            try (InputStream input = new FileInputStream(file)) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(input);
                if (Boolean.TRUE.equals(data.get("enabled"))) {
                    registerCommand(data);
                } else {
                    verboseLogger.info("Skipping disabled command in: " + file.getName());
                }
            } catch (Exception e) {
                verboseLogger.error("Failed to load or parse script file: " + file.getName(), e);
            }
        }
    }

    private void unloadScripts() {
        // Method to unload scripts
        for (String command : registeredCommands) {
            server.getCommandManager().unregister(command);
            verboseLogger.info("Command " + command + " unregistered successfully.");
        }
        registeredCommands.clear();
    }




    private void registerCommand(Map<String, Object> commandData) {
        String commandName = (String) commandData.get("name");
        List<Map<String, Object>> commandList = (List<Map<String, Object>>) commandData.get("commands");
        boolean disableExecutorIsPlayerCheck = (boolean) commandData.getOrDefault("disable-check-if-executor-is-player", false);
        if (disableExecutorIsPlayerCheck) {
            verboseLogger.warn("Executor is player check is disabled for command " + commandName);
        } else {
            verboseLogger.info("Executor is player check is enabled for command " + commandName);
        }

        if (commandName == null || commandList == null || commandList.isEmpty()) {
            verboseLogger.warn("Command name or command list is missing or empty in config.");
            return;
        }

        LiteralCommandNode<CommandSource> rootNode = BrigadierCommand.literalArgumentBuilder(commandName)
                .requires(source -> disableExecutorIsPlayerCheck || source instanceof Player)
                .executes(context -> {
                    CommandSource source = context.getSource();

                    // Prüfe Berechtigungen und ob der Befehl von einem Spieler ausgeführt werden muss, wenn nicht deaktiviert
                    if (!disableExecutorIsPlayerCheck && !(source instanceof Player player)) {
                        verboseLogger.warn("This command can only be used by a player.");
                        return 0;
                    }
                    Player player = (Player) source; // Casting sicher durchführen

                    if (!source.hasPermission("commandbridge.command." + commandName)) {
                        source.sendMessage(Component.text("You do not have permission to use this command.", net.kyori.adventure.text.format.NamedTextColor.RED));
                        return 0;
                    }

                    for (Map<String, Object> cmdData : commandList) {
                        String cmd = parsePlaceholders((String) cmdData.get("command"), player);
                        int delay = (int) cmdData.getOrDefault("delay", 0);
                        String targetServerId = (String) cmdData.get("target-server-id");
                        String targetExecutor = (String) cmdData.getOrDefault("target-executor", "console");
                        boolean waitForOnline = (boolean) cmdData.getOrDefault("wait-until-player-is-online", false);
                        boolean disablePlayerOnline = (boolean) cmdData.getOrDefault("disable-check-if-executor-is-on-server", false);

                        if (disablePlayerOnline) {
                            verboseLogger.warn("Player online check is disabled for command " + commandName);
                        } else {
                            verboseLogger.info("Player online check is enabled for command " + commandName);
                        }

                        // Ausführen des Befehls sofort oder verzögert
                        if (delay > 0) {
                            server.getScheduler().buildTask(plugin, () -> executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline))
                                    .delay(delay, TimeUnit.SECONDS)
                                    .schedule();
                        } else {
                            executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline);
                        }
                    }
                    return 1;
                })
                .build();

        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        server.getCommandManager().register(brigadierCommand);
        registeredCommands.add(commandName);
        verboseLogger.info("Command " + commandName + " registered successfully.");
    }


    private String parsePlaceholders(String command, Player player) {
        return command.replace("%player%", player.getUsername())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%server%", player.getCurrentServer().get().getServerInfo().getName());
    }


    //TODO: make exeption option that the player can execute the command if not online

    private void executeCommand(String command, String targetServerId, String targetExecutor, boolean waitForOnline, Player playerMessage, AtomicInteger timeElapsed, String playerUUID, boolean disable_player_online) {
        server.getServer(targetServerId).ifPresent(serverConnection -> {
            if (waitForOnline) {
                serverConnection.getPlayersConnected().stream()
                        .filter(player -> player.getUniqueId().toString().equals(playerUUID)) // Der Spielername ist das erste Argument im Befehl.
                        .findFirst()
                        .ifPresentOrElse(player -> {
                            sendCommandToBukkit(command, targetServerId, targetExecutor);
                            verboseLogger.info("Executing command on server " + targetServerId + ": " + command);
                        }, () -> {
                            if (timeElapsed.getAndIncrement() < 20) {
                                server.getScheduler().buildTask(plugin, () -> executeCommand(command, targetServerId, targetExecutor, true, playerMessage, timeElapsed, playerUUID, disable_player_online))
                                        .delay(1, TimeUnit.SECONDS)
                                        .schedule();
                                verboseLogger.info("Waiting for player to be online on server " + targetServerId + ": " + command);
                            } else {
                                playerMessage.sendMessage(Component.text("Timeout reached. Player not online within 20 seconds on server " + targetServerId, net.kyori.adventure.text.format.NamedTextColor.RED));
                                verboseLogger.warn("Timeout reached. Player not online on server " + targetServerId + ": " + command);
                            }
                        });
            } else {
                checkAndExecute(command, targetServerId, targetExecutor, playerMessage, playerUUID, disable_player_online);
            }
        });
    }

    private void checkAndExecute(String command, String targetServerId, String targetExecutor, Player playerMessage, String playerUUID, boolean disable_player_online) {
        server.getServer(targetServerId).ifPresent(serverConnection -> {
            verboseLogger.warn("Player UUID: " + playerUUID);


            if (disable_player_online) {
                sendCommandToBukkit(command, targetServerId, targetExecutor);
                verboseLogger.info("Executing command: " + command);

            } else {
                serverConnection.getPlayersConnected().stream()
                        .filter(player -> player.getUniqueId().toString().equals(playerUUID))
                        .findFirst()
                        .ifPresentOrElse(player -> {
                            sendCommandToBukkit(command, targetServerId, targetExecutor);
                            verboseLogger.info("Executing command: " + command);
                        }, () -> {
                            verboseLogger.warn("Player is not online on server " + targetServerId + ": " + command);
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
                        verboseLogger.info("Command sent to Bukkit server " + targetServerId + ": " + command);
                    });
        } catch (IOException e) {
            verboseLogger.error("Failed to send command to Bukkit", e);
        }
    }


}

