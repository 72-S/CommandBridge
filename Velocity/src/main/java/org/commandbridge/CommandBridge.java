package org.commandbridge;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;


@Plugin(id = "commandbridge", name = "CommandBridgeVelocity", version = "1.0-SNAPSHOT", description = "A plugin to bridge commands between servers", authors = {"72S_"}, url = "https://github.com/72-S")
public class CommandBridge {

    private final ProxyServer server;
    private final Logger logger;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("commandbridge", "main");

    @Inject
    public CommandBridge(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL);
        LoadScripts loadScripts = new LoadScripts(server, logger, this);
        loadScripts.loadScripts();
        registerCommands(loadScripts);
        logger.info("CommandBridgeVelocity has been enabled!");
    }

    public void registerCommands(LoadScripts loadScripts) {
        LiteralCommandNode<CommandSource> commandBridgeNode = LiteralArgumentBuilder.<CommandSource>literal("commandbridge")
                .executes(context -> {
                    if (context.getSource().hasPermission("commandbridge.admin")) {
                        context.getSource().sendMessage(Component.text("Use /commandbridge reload to reload scripts."));
                    } else {
                        context.getSource().sendMessage(Component.text("You do not have permission to use this command.", net.kyori.adventure.text.format.NamedTextColor.RED));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .executes(context -> {
                            if (context.getSource().hasPermission("commandbridge.admin")) {
                                return reloadScripts(context, loadScripts); // Execute reload and return result
                            } else {
                                context.getSource().sendMessage(Component.text("You do not have permission to reload scripts.", net.kyori.adventure.text.format.NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                        })
                        .build())
                .build();

        BrigadierCommand brigadierCommand = new BrigadierCommand(commandBridgeNode);
        server.getCommandManager().register("commandbridge", brigadierCommand, "cb");
    }

    private int reloadScripts(CommandContext<CommandSource> context, LoadScripts loadScripts) {
        loadScripts.loadScripts();
        context.getSource().sendMessage(Component.text("Scripts reloaded!", net.kyori.adventure.text.format.NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }


    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(CHANNEL);
        logger.info("CommandBridgeVelocity has been disabled!");
    }




}
