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
    private static final String SECURITY_WARNING_DISMISSED_KEY =
            "docker.security.warning.dismissed";
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
                Properties userSettings = new Properties();
                userSettings.load(input);
                settings.putAll(userSettings);
                if (!userSettings.containsKey("docker.connection.mode")
                        && userSettings.containsKey("docker.tls.enabled")) {
                    settings.setProperty("docker.connection.mode",
                            Boolean.parseBoolean(userSettings.getProperty("docker.tls.enabled"))
                                    ? DockerConnectionMode.TLS.name() : DockerConnectionMode.WSL_IP.name());
                }
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

    public int getDockerPort() {
        String value = settings.getProperty("docker.port", "2375");
        try {
            int port = Integer.parseInt(value);
            return port > 0 && port <= 65535 ? port : 2375;
        } catch (NumberFormatException e) {
            return 2375;
        }
    }

    public void setDockerPort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Docker port must be between 1 and 65535.");
        }
        settings.setProperty("docker.port", String.valueOf(port));
    }

    public DockerConnectionMode getDockerConnectionMode() {
        String configuredMode = settings.getProperty("docker.connection.mode");
        if (configuredMode == null || configuredMode.isBlank()) {
            return Boolean.parseBoolean(settings.getProperty("docker.tls.enabled", "false"))
                    ? DockerConnectionMode.TLS : DockerConnectionMode.WSL_IP;
        }
        try {
            return DockerConnectionMode.valueOf(configuredMode.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DockerConnectionMode.WSL_IP;
        }
    }

    public void setDockerConnectionMode(DockerConnectionMode mode) {
        settings.setProperty("docker.connection.mode",
                java.util.Objects.requireNonNull(mode, "mode").name());
        settings.remove("docker.tls.enabled");
    }

    public String getDockerTlsHost() {
        return settings.getProperty("docker.tls.host", "").trim();
    }

    public void setDockerTlsHost(String host) {
        settings.setProperty("docker.tls.host", host == null ? "" : host.trim());
    }

    public String getDockerCertPath() {
        return settings.getProperty("docker.tls.cert.path", "").trim();
    }

    public void setDockerCertPath(String path) {
        settings.setProperty("docker.tls.cert.path", path == null ? "" : path.trim());
    }

    public boolean isDockerSecurityWarningDismissed() {
        return Boolean.parseBoolean(settings.getProperty(SECURITY_WARNING_DISMISSED_KEY, "false"));
    }

    public void setDockerSecurityWarningDismissed(boolean dismissed) {
        settings.setProperty(SECURITY_WARNING_DISMISSED_KEY, String.valueOf(dismissed));
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
