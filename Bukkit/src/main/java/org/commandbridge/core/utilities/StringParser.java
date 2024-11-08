package org.commandbridge.core.utilities;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

public class StringParser {
    public static String parsePlaceholders(String command, Player player, String args) {
        return command.replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%server%", player.getServer().getName())
                .replace("%world%", player.getWorld().getName())
                .replace("%args%", Objects.requireNonNull(appendArguments(args)));
    }
    public static String parseConsoleCommands(String command, ConsoleCommandSender consoleCommandSender, String args) {
        return command.replace("%server%", consoleCommandSender.getServer().getName())
                .replace("%args%", Objects.requireNonNull(appendArguments(args)));
    }

    public static String parseBlockCommands(String command, BlockCommandSender blockCommandSender, String args) {
        return command.replace("%server%", blockCommandSender.getServer().getName())
                .replace("%world%", blockCommandSender.getBlock().getWorld().getName())
                .replace("%player%", Objects.requireNonNull(blockCommandSender.getServer().getPlayer(blockCommandSender.getName())).getName())
                .replace("%uuid%", Objects.requireNonNull(blockCommandSender.getServer().getPlayer(blockCommandSender.getName())).getUniqueId().toString())
                .replace("%args%", appendArguments(args));
    }


    private static String appendArguments(String args) {
        if (args == null || args.isEmpty()) {
            return "";
        }
        return String.join(" ", args);
    }
}
