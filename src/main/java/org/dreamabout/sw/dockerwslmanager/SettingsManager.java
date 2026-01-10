package org.dreamabout.sw.dockerwslmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public final class SettingsManager {
    private static final Logger logger = LoggerFactory.getLogger(SettingsManager.class);
    private final Properties settings = new Properties();
    private static final String CONFIG_FILE_PATH = System.getProperty("user.home") + "/.docker-wsl-manager/settings.properties";

    public SettingsManager() {
        loadSettings();
    }

    private void loadSettings() {
        // Load default first
        try (InputStream input = getClass().getResourceAsStream("/settings.properties")) {
            if (input != null) {
                settings.load(input);
            }
        } catch (IOException e) {
            logger.error("Failed to load default settings", e);
        }

        // Load user overrides
        File userConfig = new File(CONFIG_FILE_PATH);
        if (userConfig.exists()) {
            try (InputStream input = new FileInputStream(userConfig)) {
                settings.load(input);
            } catch (IOException e) {
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

    public void saveSettings() throws IOException {
        File userConfig = new File(CONFIG_FILE_PATH);
        File parent = userConfig.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }
        
        try (FileWriter writer = new FileWriter(userConfig, StandardCharsets.UTF_8)) {
            settings.store(writer, "User Settings");
        }
    }
}
