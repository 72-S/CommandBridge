package dev.consti.bukkit.core;

import dev.consti.bukkit.Main;
import dev.consti.bukkit.utils.ScriptUtils;
import dev.consti.bukkit.websocket.Client;
import dev.consti.logging.Logger;
import dev.consti.utils.ConfigManager;
import dev.consti.utils.VersionChecker;

public class Runtime {
    private static Runtime instance;
    private Logger logger;
    private ConfigManager config;
    private ScriptUtils scriptUtils;
    private Client client;
    private Startup startup;
//    private CommandHelper helper;
//    private CommandRegistrar registrar;
//    private GeneralUtils generalUtils;

    private Runtime() {}

    public static synchronized Runtime getInstance() {
        if (instance == null) {
            instance = new Runtime();
            instance.getLogger().info("Runtime singleton instance initialized.");
        }
        return instance;
    }

    public synchronized Logger getLogger() {
        if (logger == null) {
            logger = new Logger("CommandBridge");
            logger.debug("Logger initialized.");
        }
        return logger;
    }

    public synchronized ConfigManager getConfig() {
        if (config == null) {
            config = new ConfigManager(getLogger(), "CommandBridge");
            getLogger().debug("ConfigManager initialized.");
        }
        return config;
    }

    public synchronized ScriptUtils getScriptUtils() {
        if (scriptUtils == null) {
            scriptUtils = new ScriptUtils(getLogger(), "CommandBridge");
            getLogger().debug("ScriptUtils initialized.");
        }
        return scriptUtils;
    }

    public synchronized Client getClient() {
        if (client == null) {
            client = new Client(getLogger(), getConfig().getSecret());
            getLogger().debug("Server initialized.");
        }
        return client;
    }

    public synchronized Startup getStartup() {
        if (startup == null) {
            startup = new Startup(getLogger());
            getLogger().debug("Startup initialized.");
        }
        return startup;
    }

//    public synchronized CommandHelper getHelper() {
//        if (helper == null) {
//            helper = new CommandHelper(getLogger(), Main.getInstance());
//            getLogger().debug("CommandHelper initialized.");
//        }
//        return helper;
//    }
//
//    public synchronized CommandRegistrar getRegistrar() {
//        if (registrar == null) {
//            registrar = new CommandRegistrar(getLogger());
//            getLogger().debug("CommandRegistrar initialized.");
//        }
//        return registrar;
//    }
//
//    public synchronized GeneralUtils getGeneralUtils() {
//        if (generalUtils == null) {
//            generalUtils = new GeneralUtils(getLogger());
//            getLogger().debug("GeneralUtils initialized.");
//        }
//        return generalUtils;
//    }
}