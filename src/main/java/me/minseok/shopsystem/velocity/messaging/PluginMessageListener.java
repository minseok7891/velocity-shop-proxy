package me.minseok.shopsystem.velocity.messaging;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import me.minseok.shopsystem.velocity.VelocityShopSystem;

public class PluginMessageListener {

    private final VelocityShopSystem plugin;

    public PluginMessageListener(VelocityShopSystem plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        plugin.getLogger().info("DEBUG: Received message on channel: " + event.getIdentifier().getId());

        if (!event.getIdentifier().getId().equals("shopsystem:sync")) {
            return;
        }

        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        // Handle incoming messages from backend servers
        if (event.getSource() instanceof ServerConnection) {
            ServerConnection connection = (ServerConnection) event.getSource();
            @SuppressWarnings("null")
            com.google.common.io.ByteArrayDataInput in = com.google.common.io.ByteStreams.newDataInput(event.getData());
            String subChannel = in.readUTF();

            if (subChannel.equals("REQUEST_CONFIG")) {
                plugin.getLogger().info("Received config request from " + connection.getServerInfo().getName());
                plugin.getConfigManager().sendConfigToServer(connection.getServer());
            } else if (subChannel.equals("PRICE_UPDATE")) {
                String item = in.readUTF();
                double buyPrice = in.readDouble();
                double sellPrice = in.readDouble();
                String sourceServer = "unknown";
                try {
                    sourceServer = in.readUTF();
                } catch (Exception e) {
                    // Legacy or missing source
                }

                plugin.getLogger().info(
                        "Velocity received PRICE_UPDATE for " + item + " from " + connection.getServerInfo().getName()
                                + " (Source: " + sourceServer + ")");

                // Forward to all other servers
                com.google.common.io.ByteArrayDataOutput out = com.google.common.io.ByteStreams.newDataOutput();
                out.writeUTF("PRICE_UPDATE");
                out.writeUTF(item);
                out.writeDouble(buyPrice);
                out.writeDouble(sellPrice);
                out.writeUTF(sourceServer);
                byte[] data = out.toByteArray();

                for (com.velocitypowered.api.proxy.server.RegisteredServer server : plugin.getServer()
                        .getAllServers()) {
                    if (!server.getServerInfo().getName().equals(connection.getServerInfo().getName())) {
                        if (!server.getPlayersConnected().isEmpty()) {
                            server.sendPluginMessage(VelocityShopSystem.CHANNEL, data);
                            plugin.getLogger()
                                    .info("Velocity forwarding PRICE_UPDATE to " + server.getServerInfo().getName());
                        } else {
                            plugin.getLogger().info("Skipping " + server.getServerInfo().getName() + " (no players)");
                        }
                    }
                }
            } else if (subChannel.equals("REQUEST_GLOBAL_RELOAD")) {
                plugin.getLogger().info("Received global reload request from " + connection.getServerInfo().getName());
                plugin.getConfigManager().sendConfigToAll();
            }
        }
    }
}
