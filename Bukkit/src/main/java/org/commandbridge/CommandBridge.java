package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public final class CommandBridge extends JavaPlugin {
    private String serverId;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        serverId = "lobby";
        getConfig().set("server-id", serverId);
        saveConfig();
        Logger logger = getLogger();
        logger.info("CommandBridge has been enabled!");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "commandbridge:main");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "commandbridge:main", new BukkitMessageListener(this));
    }

    @Override
    public void onDisable() {
        Logger logger = getLogger();
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        logger.info("CommandBridge has been disabled!");
    }



}
