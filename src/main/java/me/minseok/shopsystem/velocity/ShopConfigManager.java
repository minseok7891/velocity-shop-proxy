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
import java.util.concurrent.TimeUnit;

public class ShopConfigManager {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

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
        sendConfigToServerWithRetry(server, 0);
    }

    /**
     * 설정을 서버로 전송하고 실패 시 재시도합니다
     * @param server 대상 서버
     * @param attempt 현재 시도 횟수
     */
    private void sendConfigToServerWithRetry(RegisteredServer server, int attempt) {
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
            logger.info("Config sent successfully to: " + server.getServerInfo().getName());

        } catch (IOException e) {
            if (attempt < MAX_RETRIES) {
                logger.warn("Failed to send config to " + server.getServerInfo().getName() + 
                           " (attempt " + (attempt + 1) + "/" + MAX_RETRIES + "). Retrying in " + 
                           RETRY_DELAY_MS + "ms...");
                
                // 재시도 스케줄
                plugin.getServer().getScheduler().buildTask(plugin, () -> 
                    sendConfigToServerWithRetry(server, attempt + 1))
                    .delay(RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                    .schedule();
            } else {
                logger.error("Failed to send config to " + server.getServerInfo().getName() + 
                           " after " + MAX_RETRIES + " attempts. Error: " + e.getMessage(), e);
            }
        }
    }

    private Map<String, String> loadAllConfigs() throws IOException {
        Map<String, String> configs = new HashMap<>();

        // Load main config.yml
        File configFile = new File(dataFolder, "config.yml");
        if (configFile.exists()) {
            try {
                configs.put("config.yml", Files.readString(configFile.toPath()));
                logger.info("Loaded config.yml");
            } catch (IOException e) {
                logger.error("Failed to read config.yml", e);
                throw e;
            }
        } else {
            logger.warn("config.yml not found");
        }

        // Load shops directory
        File shopsDir = new File(dataFolder, "shops");
        if (shopsDir.exists() && shopsDir.isDirectory()) {
            File[] files = shopsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        configs.put("shops/" + file.getName(), Files.readString(file.toPath()));
                    } catch (IOException e) {
                        logger.error("Failed to read shop file: " + file.getName(), e);
                    }
                }
                logger.info("Loaded " + files.length + " shop configuration(s)");
            }
        } else {
            logger.warn("shops directory not found");
        }

        return configs;
    }
}
