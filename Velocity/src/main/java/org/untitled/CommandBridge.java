package org.untitled;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(
        id = "CommandBridgeVelocity",
        name = "CommandBridgeVelocity",
        version = "1.0-SNAPSHOT"
)
public class CommandBridge {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("CommandBridgeVelocity has been enabled!");
    }
}
