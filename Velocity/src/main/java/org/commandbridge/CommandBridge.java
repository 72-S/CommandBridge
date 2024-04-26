package org.commandbridge;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;


@Plugin(id = "commandbridge", name = "CommandBridgeVelocity", version = "1.0-SNAPSHOT", description = "A plugin to bridge commands between servers", authors = {"72S_"}, url = "https://github.com/72-S")
public class CommandBridge {

    private final ProxyServer server;
    private final VerboseLogger verboseLogger;
    private boolean verboseOutput;
    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("commandbridge", "main");

    @Inject
    public CommandBridge(ProxyServer server, Logger logger) {
        this.server = server;
        this.verboseLogger = new VerboseLogger(this, logger);
        loadConfig();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL);
        LoadScripts loadScripts = new LoadScripts(server,this);
        loadScripts.loadScripts();
        registerCommands(loadScripts);
        verboseLogger.info("CommandBridgeVelocity has been enabled!");
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

    private void loadConfig() {
        File dataFolder = new File("plugins/CommandBridgeVelocity");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) {
            try {
                Files.createFile(configFile.toPath());
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("verbose-output: false\n");
                }
                verboseLogger.info("Config file created with default settings.");
            } catch (IOException e) {
                verboseLogger.error("Failed to create the default config file", e);
            }
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml(new Constructor(Map.class));
            Map<String, Object> data = yaml.load(fis);
            this.verboseOutput = (boolean) data.getOrDefault("verboseOutput", false);
            verboseLogger.info("Config loaded. Verbose output is " + (this.verboseOutput ? "enabled" : "disabled"));
        } catch (IOException e) {
            verboseLogger.error("Failed to load config file", e);
        }
    }

    public boolean isVerboseOutputEnabled() {
        return verboseOutput;
    }

    public VerboseLogger getVerboseLogger() {
        return verboseLogger;
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(CHANNEL);
        verboseLogger.info("CommandBridgeVelocity has been disabled!");
    }




}
