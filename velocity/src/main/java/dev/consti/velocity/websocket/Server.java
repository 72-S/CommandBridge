package dev.consti.velocity.websocket;

import com.velocitypowered.api.proxy.Player;
import dev.consti.json.MessageBuilder;
import dev.consti.json.MessageParser;
import dev.consti.logging.Logger;
import dev.consti.websocket.SimpleWebSocketServer;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends SimpleWebSocketServer {
    private final Logger logger;
    private final List<String> connectedClients;
    private final Map<String, WebSocket> clientConnections = new HashMap<>();

    public Server(Logger logger, String secret) {
        super(logger, secret);
        this.logger = logger;
        this.connectedClients = new ArrayList<>();
        logger.debug("WebSocket server initialized with secret.");
    }

    @Override
    protected void onMessage(WebSocket webSocket, String message) {
        MessageParser parser = new MessageParser(message);
        logger.debug("Received message: {}", message);
        try {
            String type = parser.getType();
            logger.info("Processing message of type: {}", type);
            switch (type) {
                case "command":
                    handleCommandRequest(webSocket, message);
                    break;
                case "system":
                    handleSystemRequest(webSocket, message);
                    break;
                default:
                    logger.warn("Unknown message type received: {}", type);
                    sendError(webSocket, "Unknown message type");
            }
        } catch (Exception e) {
            logger.error("Error while processing message: {}. Error: {}", message, e.getMessage(), e);
            sendError(webSocket, "Internal server error");
        }
    }

    @Override
    protected void onConnectionClose(WebSocket conn, int code, String reason) {
        logger.info("Client {} disconnected with reason: {}", conn.getRemoteSocketAddress(), reason);

        String disconnectedClientName = null;
        for (Map.Entry<String, WebSocket> entry : clientConnections.entrySet()) {
            if (entry.getValue().equals(conn)) {
                disconnectedClientName = entry.getKey();
                break;
            }
        }

        if (disconnectedClientName != null) {
            connectedClients.remove(disconnectedClientName);
            clientConnections.remove(disconnectedClientName);
            logger.info("Removed disconnected client: {}", disconnectedClientName);
        } else {
            logger.warn("Disconnected client not found in client connections map.");
        }
    }

    private void handleCommandRequest(WebSocket webSocket, String message) {
        logger.info("Handling command response: {}", message);
    }

    private void handleSystemRequest(WebSocket webSocket, String message) {
        logger.info("Handling system request.");
        MessageParser parser = new MessageParser(message);
        String channel = parser.getBodyValueAsString("channel");
        String name = parser.getBodyValueAsString("name");
        String status = parser.getStatus();


        if (channel.equals("name")) {
            if (name != null) {
                connectedClients.add(name);
                clientConnections.put(name, webSocket);
                logger.info("Added connected client: {}", name);
            } else {
                logger.warn("No name provided in system request.");
            }
        } else if (channel.equals("error")) {
            logger.warn("Error Message from client: {}: {}", webSocket.toString(), status);
        } else if (channel.equals("info")) {
            logger.info("Info from client: {}: {}", webSocket.toString(), status);
        }
    }

    public boolean isServerConnected(String serverName) {
        boolean exists = connectedClients.contains(serverName);
        logger.debug("Checking if server '{}' is connected: {}", serverName, exists);
        return exists;
    }

    public List<String> getConnectedClients() {
        return connectedClients;
    }

    private void sendError(WebSocket webSocket, String errorMessage) {
        MessageBuilder builder = new MessageBuilder("system");
        builder.addToBody("channel", "error");
        builder.withStatus(errorMessage);
        sendMessage(builder.build(), webSocket);
    }

    public void sendJSON(String command, String server, String[] arguments, Player executor) {
        WebSocket conn = clientConnections.get(server);
        if (conn == null) {
            logger.warn("Server '{}' is not connected, cannot send message.", server);
            return;
        }

        MessageBuilder builder = new MessageBuilder("command");
        builder.addToBody("command", command);
        builder.addToBody("server", server);
        builder.addToBody("arguments", arguments);

        if (executor != null) {
            builder.addToBody("name", executor.getUsername());
            builder.addToBody("uuid", executor.getUniqueId());
        }

        logger.info("Sending JSON command to client: {} | Payload: {}", server, builder.build().toString());
        sendMessage(builder.build(), conn);
    }


}
