package dev.consti.commandbridge.velocity.helper.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.CommandSource;
import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.util.GeneralUtils;
import dev.consti.foundationlib.json.MessageBuilder;
import dev.consti.foundationlib.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadCommand {

    public static LiteralArgumentBuilder<CommandSource> build(GeneralUtils utils, Logger logger) {
        return LiteralArgumentBuilder.<CommandSource>literal("reload")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    if (!source.hasPermission("commandbridge.admin")) {
                        source.sendMessage(Component.text("You do not have permission to reload the plugin", NamedTextColor.RED));
                        return 0;
                    }

                    try {
                        // Logic from original reload command
                        Runtime.getInstance().getRegistrar().unregisterAllCommands();
                        logger.debug("All commands have been unregistered");

                        Runtime.getInstance().getConfig().reload();
                        logger.debug("Configuration files have been reloaded");

                        Runtime.getInstance().getScriptUtils().reload();
                        logger.debug("Scripts have been reloaded");

                        MessageBuilder builder = new MessageBuilder("system");
                        builder.addToBody("channel", "command");
                        builder.addToBody("command", "reload");
                        Runtime.getInstance().getServer().broadcastServerMessage(builder.build());

                        source.sendMessage(
                                Component.text("Waiting for clients to respond...")
                                        .color(NamedTextColor.YELLOW));
                        logger.debug("Waiting for clients to respond...");

                        // Use the utils method to start the failure check
                        utils.startFailureCheck(source);
                        return 1;
                    } catch (Exception e) {
                        logger.error("An error occurred during the reload process: {}", e.getMessage(), e);
                        source.sendMessage(
                                Component.text("ReloadCommand failed due to an internal error. Check logs for details")
                                        .color(NamedTextColor.RED));
                    }
                    return 1;

                });
    }
}

