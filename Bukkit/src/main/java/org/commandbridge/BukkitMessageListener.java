package org.commandbridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class BukkitMessageListener implements PluginMessageListener {
    private final JavaPlugin plugin;

    public BukkitMessageListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!"commandbridge:main".equals(channel)) return;
        try (ByteArrayInputStream stream = new ByteArrayInputStream(message);
             DataInputStream in = new DataInputStream(stream)) {
            String subChannel = in.readUTF();
            if ("ExecuteCommand".equals(subChannel)) {
                String targetServerId = in.readUTF();  // Lese die Zielserver-ID
                String targetExecutor = in.readUTF();  // Lese den Ziel-Ausführenden
                String command = in.readUTF();         // Lese den Befehl

                // Prüfe, ob der Befehl für diesen Server bestimmt ist
                if (!targetServerId.equals(plugin.getConfig().getString("server-id"))) {
                    plugin.getLogger().info("Command not for this server, ignoring.");
                    return;
                }

                // Führe den Befehl aus, abhängig vom Ziel-Ausführenden
                if ("player".equals(targetExecutor) && player != null) {
                    plugin.getLogger().info("Executing command as player: " + command);
                    Bukkit.dispatchCommand(player, command);
                } else if ("console".equals(targetExecutor)){
                    plugin.getLogger().info("Executing command as console: " + command);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read plugin message: " + e.getMessage());
        }
    }

}