package dev.consti.commandbridge.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import dev.consti.commandbridge.velocity.core.Runtime;
import dev.consti.commandbridge.velocity.util.ProxyUtils;
import dev.consti.foundationlib.logging.Logger;
import dev.consti.foundationlib.utils.VersionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

@Plugin(id = "commandbridge", name = "CommandBridge", version = "2.0.0", authors = "72-S")
public class Main {
    private static Main instance;
    private final ProxyServer proxy;
    private final Logger logger;


    @Inject
    public Main(ProxyServer proxy) {
        this.proxy = proxy;
        this.logger = Runtime.getInstance().getLogger();
        instance = this;
        ProxyUtils.setProxyServer(proxy);
    }

    public static Main getInstance() {
        return instance;
    }

    public static String getVersion() {
        return Main.class.getAnnotation(Plugin.class).version();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing CommandBridge...");
        Runtime.getInstance().getStartup().start();
        logger.info("CommandBridge initialized successfully.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Stopping CommandBridge...");
        Runtime.getInstance().getStartup().stop();
        logger.info("CommandBridge stopped successfully.");
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            logger.warn("PostLoginEvent triggered with a null player object.");
            return;
        }

        if (player.hasPermission("commandbridge.admin")) {
            logger.debug("Player '{}' has admin permissions. Checking for updates...", player.getUsername());

            proxy.getScheduler().buildTask(this, () -> {
                String currentVersion = Main.getVersion();
                String latestVersion = VersionChecker.getLatestVersion();

                if (latestVersion == null) {
                    player.sendMessage(Component.text("Unable to check for updates.").color(NamedTextColor.RED));
                    logger.warn("Update check failed: Unable to retrieve the latest version.");
                    return;
                }

                if (VersionChecker.isNewerVersion(latestVersion, currentVersion)) {
                    player.sendMessage(Component.text("A new version of CommandBridge is available: " + latestVersion).color(NamedTextColor.RED));
                    player.sendMessage(Component.text("Please download the latest release: ")
                            .append(Component.text("here")
                                    .color(NamedTextColor.BLUE)
                                    .decorate(TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.openUrl(VersionChecker.getDownloadUrl()))));
                    logger.debug("Notified player '{}' about the new version: {}", player.getUsername(), latestVersion);
                } else {
                    logger.debug("Player '{}' is running the latest version: {}", player.getUsername(), currentVersion);
                }
            }).schedule();
        } else {
            logger.debug("Player {} does not have admin permissions. No update check performed.", player.getUsername());
        }
    }
}
