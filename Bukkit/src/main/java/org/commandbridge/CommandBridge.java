package org.commandbridge;

import org.bukkit.plugin.java.JavaPlugin;

public final class CommandBridge extends JavaPlugin {


    @Override
    public void onEnable() {
        saveDefaultConfig();
        String serverId = "REPLACE THIS WITH YOUR SERVER NAME";
        getConfig().set("server-id", serverId);
        getConfig().set("verbose-output", false);
        saveConfig();
        VerboseLogger logger = new VerboseLogger(this);
        logger.loadConfig();
        logger.info("CommandBridge has been enabled!");
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "commandbridge:main");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "commandbridge:main", new MessageListener(this));
    }

    @Override
    public void onDisable() {
        VerboseLogger logger = new VerboseLogger(this);
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        logger.info("CommandBridge has been disabled!");
    }



}
