package me.minseok.shopsystem.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.minseok.shopsystem.velocity.VelocityShopSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ShopManagerCommand implements SimpleCommand {

    private final VelocityShopSystem plugin;

    public ShopManagerCommand(VelocityShopSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelp(invocation);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> handleSetPrice(invocation, args);
            case "sync" -> handleSync(invocation);
            case "reload" -> handleReload(invocation);
            case "status" -> handleStatus(invocation);
            default -> sendHelp(invocation);
        }
    }

    private void handleSetPrice(Invocation invocation, String[] args) {
        if (args.length < 3) {
            invocation.source()
                    .sendMessage(Component.text("Usage: /shopmanager set <item> <buy> [sell]", NamedTextColor.RED));
            return;
        }

        String item = args[1].toUpperCase();
        double buyPrice;
        double sellPrice;

        try {
            buyPrice = Double.parseDouble(args[2]);
            sellPrice = args.length > 3 ? Double.parseDouble(args[3]) : buyPrice * 0.25;
        } catch (NumberFormatException e) {
            invocation.source().sendMessage(Component.text("Invalid price format", NamedTextColor.RED));
            return;
        }

        // Send update to all servers
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PRICE_UPDATE");
        out.writeUTF(item);
        out.writeDouble(buyPrice);
        out.writeDouble(sellPrice);

        int serverCount = 0;
        for (RegisteredServer server : plugin.getServer().getAllServers()) {
            if (!server.getPlayersConnected().isEmpty()) {
                server.sendPluginMessage(VelocityShopSystem.CHANNEL, out.toByteArray());
                serverCount++;
            }
        }

        invocation.source()
                .sendMessage(Component.text("Sent price update to " + serverCount + " servers:", NamedTextColor.GREEN));
        invocation.source().sendMessage(Component.text("Item: " + item, NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("Buy: " + buyPrice, NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("Sell: " + sellPrice, NamedTextColor.YELLOW));
    }

    private void handleSync(Invocation invocation) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("SYNC_REQUEST");

        for (RegisteredServer server : plugin.getServer().getAllServers()) {
            if (!server.getPlayersConnected().isEmpty()) {
                server.sendPluginMessage(VelocityShopSystem.CHANNEL, out.toByteArray());
            }
        }

        invocation.source().sendMessage(Component.text("Sent sync request to all servers", NamedTextColor.GREEN));
    }

    private void handleReload(Invocation invocation) {
        plugin.getConfigManager().sendConfigToAll();
        invocation.source()
                .sendMessage(Component.text("Reloaded configs and sent to all servers", NamedTextColor.GREEN));
    }

    private void handleStatus(Invocation invocation) {
        invocation.source().sendMessage(Component.text("=== Server Status ===", NamedTextColor.GOLD));

        for (RegisteredServer server : plugin.getServer().getAllServers()) {
            int playerCount = server.getPlayersConnected().size();
            String status = playerCount > 0 ? "Online" : "No Players";
            NamedTextColor color = playerCount > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY;

            invocation.source()
                    .sendMessage(Component.text("- " + server.getServerInfo().getName() + ": " + status, color));
        }
    }

    private void sendHelp(Invocation invocation) {
        invocation.source().sendMessage(Component.text("=== Velocity Shop Manager ===", NamedTextColor.GOLD));
        invocation.source().sendMessage(Component.text("/shopmanager set <item> <buy> [sell]", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/shopmanager sync", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/shopmanager reload", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("/shopmanager status", NamedTextColor.YELLOW));
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("shopmanager.admin");
    }
}
