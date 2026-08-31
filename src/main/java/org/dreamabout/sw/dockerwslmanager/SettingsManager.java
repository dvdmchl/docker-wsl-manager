package org.dreamabout.sw.dockerwslmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dreamabout.sw.dockerwslmanager.model.ContainerTreeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SettingsManager {
    private static final Logger logger = LoggerFactory.getLogger(SettingsManager.class);
    private static final String CONTAINER_TREE_STATE_KEY = "containers.tree.state";
    private final Properties settings = new Properties();
    private final Path configFilePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SettingsManager() {
        this(Path.of(System.getProperty("user.home"), ".docker-wsl-manager", "settings.properties"));
    }

    SettingsManager(Path configFilePath) {
        this.configFilePath = configFilePath;
        loadSettings();
    }

    private void loadSettings() {
        // Load default first
        try (InputStream input = getClass().getResourceAsStream("/settings.properties")) {
            if (input != null) {
                settings.load(input);
            }
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to load default settings", e);
        }

        // Load user overrides
        File userConfig = configFilePath.toFile();
        if (userConfig.exists()) {
            try (InputStream input = new FileInputStream(userConfig)) {
                settings.load(input);
            } catch (IOException | RuntimeException e) {
                logger.error("Failed to load user settings", e);
            }
        }
    }

    public int getAutoRefreshInterval() {
        String val = settings.getProperty("auto.refresh.interval", "5");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public void setAutoRefreshInterval(int seconds) {
        settings.setProperty("auto.refresh.interval", String.valueOf(seconds));
    }

    public int getStatsRefreshInterval() {
        String val = settings.getProperty("stats.refresh.interval", "5");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public void setStatsRefreshInterval(int seconds) {
        settings.setProperty("stats.refresh.interval", String.valueOf(seconds));
    }

    public String getWslDistro() {
        return settings.getProperty("wsl.distro", "docker-desktop-data");
    }

    public void setWslDistro(String distro) {
        settings.setProperty("wsl.distro", distro);
    }

    public ContainerTreeState getContainerTreeState() {
        String json = settings.getProperty(CONTAINER_TREE_STATE_KEY);
        if (json == null || json.isBlank()) {
            return ContainerTreeState.empty();
        }

        try {
            return objectMapper.readValue(json, ContainerTreeState.class);
        } catch (JsonProcessingException | RuntimeException e) {
            logger.warn("Ignoring malformed persisted Containers tree state", e);
            settings.remove(CONTAINER_TREE_STATE_KEY);
            return ContainerTreeState.empty();
        }
    }

    public void setContainerTreeState(ContainerTreeState treeState) {
        try {
            settings.setProperty(CONTAINER_TREE_STATE_KEY, objectMapper.writeValueAsString(treeState));
        } catch (JsonProcessingException | RuntimeException e) {
            logger.warn("Failed to serialize Containers tree state", e);
            settings.remove(CONTAINER_TREE_STATE_KEY);
        }
    }

    public void saveSettings() throws IOException {
        File userConfig = configFilePath.toFile();
        File parent = userConfig.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
        
        try (FileWriter writer = new FileWriter(userConfig, StandardCharsets.UTF_8)) {
            settings.store(writer, "User Settings");
        }
    }
}
