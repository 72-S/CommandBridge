package org.commandbridge;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commandbridge.Utilities.parsePlaceholders;

public class CommandRegistrar {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private final CommandBridge plugin;
    private final CommandExecutor commandExecutor;

    public CommandRegistrar(ProxyServer server, CommandBridge plugin) {
        this.server = server;
        this.plugin = plugin;
        this.commandExecutor = new CommandExecutor(server, plugin);
        this.verboseLogger = plugin.getVerboseLogger();
    }



    public void registerCommand(Map<String, Object> commandData) {
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

                        if (delay > 0) {
                            server.getScheduler().buildTask(plugin, () -> commandExecutor.executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline))
                                    .delay(delay, TimeUnit.SECONDS)
                                    .schedule();
                        } else {
                            commandExecutor.executeCommand(cmd, targetServerId, targetExecutor, waitForOnline, player, new AtomicInteger(0), player.getUniqueId().toString(), disablePlayerOnline);
                        }
                    }
                    return 1;
                })
                .build();

        BrigadierCommand brigadierCommand = new BrigadierCommand(rootNode);
        server.getCommandManager().register(brigadierCommand);
        plugin.addRegisteredCommand(commandName);
        verboseLogger.info("Command " + commandName + " registered successfully.");
    }
}
