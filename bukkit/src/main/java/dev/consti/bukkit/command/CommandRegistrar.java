package dev.consti.bukkit.command;

import dev.consti.bukkit.Main;
import dev.consti.bukkit.core.Runtime;
import dev.consti.logging.Logger;
import dev.consti.utils.ScriptManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class CommandRegistrar {
    private final Logger logger;
    private final Main plugin;
    private final CommandHelper helper;

    public CommandRegistrar(Logger logger, Main plugin) {
        this.logger = logger;
        this.plugin = plugin;
        this.helper = Runtime.getInstance().getHelper();
        logger.debug("CommandRegistrar initialized with helper: {}", helper.getClass().getSimpleName());
    }

    public void registerCommand(ScriptManager.ScriptConfig script) {
        String commandName = script.getName();
        logger.debug("Starting registration for command: {}", commandName);

        try {
            // Create the base command without arguments
            CommandAPICommand command = new CommandAPICommand(commandName)
                    .executes((sender, args) -> {
                        logger.debug("Executing base command: {}", commandName);
                        return helper.executeScriptCommands(sender, script, new String[0]);
                    })
                    // Add optional arguments
                    .withArguments(new GreedyStringArgument("args"))
                    .executes((sender, args) -> {
                        String argsString = (String) args.get("args");
                        logger.debug("Command {} called with arguments: {}", commandName, argsString);
                        String[] splitArgs = argsString != null ? argsString.split(" ") : new String[0];
                        return helper.executeScriptCommands(sender, script, splitArgs);
                    });

            // Register the command
            command.register();

            logger.info("Command {} registered successfully.", commandName);
        } catch (Exception e) {
            logger.error("Failed to register command {}. Error: {}", commandName, e.getMessage(), e);
        }
    }
}
