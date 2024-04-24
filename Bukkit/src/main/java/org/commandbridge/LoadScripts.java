package org.commandbridge;


import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class LoadScripts {
    private final JavaPlugin plugin;
    private final File scriptsFolder;

    public LoadScripts(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scriptsFolder = new File(plugin.getDataFolder(), "scripts");

        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs(); // Erstellt den Ordner, wenn er nicht existiert
        }
    }

    public void loadScripts() {


        File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            loadScript(config);
        }
    }

    private void loadScript(YamlConfiguration config) {
        if (!config.getBoolean("enabled", false)) return; // Überspringt, wenn das Script nicht aktiviert ist

        String commandName = config.getString("name", "");
        List<String> commandsExecuted = config.getStringList("commands");

        if (commandName.isEmpty() || commandsExecuted.isEmpty()) {
            plugin.getLogger().warning("Command name or commands list is empty in config.");
            return;
        }

        // Registriere den Command mit CommandAPI
        new CommandAPICommand(commandName)
                .executes((sender, args) -> {
                    for (String cmd : commandsExecuted) {
                        // Führe jeden Befehl aus, der im Skript angegeben ist
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }
                    return 1;  // Rückgabe 1 für Erfolg
                })
                .register();
    }


}
