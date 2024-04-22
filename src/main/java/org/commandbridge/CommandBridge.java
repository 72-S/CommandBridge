package org.commandbridge;

import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import dev.jorel.commandapi.CommandAPI;

public final class CommandBridge extends JavaPlugin {


    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true)); // Load with verbose output

        new CommandAPICommand("commandbridge")
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            // Logik zum Neuladen der Skripte
                            new LoadScripts(this).loadScripts();
                            sender.sendMessage("Scripts reloaded!");
                            return 1;
                        }))
                .executes((sender, args) -> {
                    sender.sendMessage("CommandBridge is running!");
                    return 1;
                })
                .register();
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        Logger logger = getLogger();
        logger.info("CommandBridge has been enabled!");
        saveDefaultConfig();
        copyExampleYml();
        new LoadScripts(this).loadScripts();
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
        Logger logger = getLogger();
        logger.info("CommandBridge has been disabled!");
    }


    private void copyExampleYml() {
        File scriptFolder = new File(getDataFolder(), "scripts");
        if (!scriptFolder.exists()) {
            scriptFolder.mkdirs();
        }
        File exampleFile = new File(scriptFolder, "example.yml");
        if (!exampleFile.exists()) {
            try (InputStream in = getResource("example.yml")) {
                if (in == null) {
                    getLogger().warning("Resource 'example.yml' not found.");
                    return;
                }
                Files.copy(in, exampleFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
