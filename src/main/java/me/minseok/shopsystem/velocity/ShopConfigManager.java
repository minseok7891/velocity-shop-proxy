package me.minseok.shopsystem.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ShopConfigManager {

    private final VelocityShopSystem plugin;
    private final File dataFolder;
    private final Logger logger;

    public ShopConfigManager(VelocityShopSystem plugin, File dataFolder) {
        this.plugin = plugin;
        this.dataFolder = dataFolder;
        this.logger = plugin.getLogger();

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void sendConfigToAll() {
        for (RegisteredServer server : plugin.getServer().getAllServers()) {
            sendConfigToServer(server);
        }
    }

    public void sendConfigToServer(RegisteredServer server) {
        try {
            Map<String, String> configs = loadAllConfigs();

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("SEND_CONFIG");
            out.writeInt(configs.size());

            for (Map.Entry<String, String> entry : configs.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue());
            }

            server.sendPluginMessage(VelocityShopSystem.CHANNEL, out.toByteArray());
            logger.info("Sent config to server: " + server.getServerInfo().getName());

        } catch (IOException e) {
            logger.error("Failed to load configs for sync", e);
        }
    }

    private Map<String, String> loadAllConfigs() throws IOException {
        Map<String, String> configs = new HashMap<>();

        // Load main config.yml
        File configFile = new File(dataFolder, "config.yml");
        if (configFile.exists()) {
            configs.put("config.yml", Files.readString(configFile.toPath()));
        }

        // Load shops directory
        File shopsDir = new File(dataFolder, "shops");
        if (shopsDir.exists() && shopsDir.isDirectory()) {
            File[] files = shopsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    configs.put("shops/" + file.getName(), Files.readString(file.toPath()));
                }
            }
        }

        return configs;
    }
}
