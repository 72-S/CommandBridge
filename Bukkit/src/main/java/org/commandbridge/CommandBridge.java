package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;
import org.commandbridge.Logger;

public final class CommandBridge extends JavaPlugin {
    private String serverId;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        serverId = "REPLACE THIS WITH YOUR SERVER NAME";
        getConfig().set("server-id", serverId);
        getConfig().set("verbose-output", false);
        saveConfig();
        Logger logger = new Logger(this);
        logger.loadConfig();
        logger.info("CommandBridge has been enabled!");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "commandbridge:main");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "commandbridge:main", new BukkitMessageListener(this));
    }

    @Override
    public void onDisable() {
        Logger logger = new Logger(this);
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        logger.info("CommandBridge has been disabled!");
    }



}
