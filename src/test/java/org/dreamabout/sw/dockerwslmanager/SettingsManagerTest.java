package org.dreamabout.sw.dockerwslmanager;

import org.dreamabout.sw.dockerwslmanager.model.ContainerTreeState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsManagerTest {

    @Test
    void testGetStatsRefreshIntervalDefault() {
        SettingsManager settingsManager = new SettingsManager();
        assertEquals(5, settingsManager.getStatsRefreshInterval());
    }

    @Test
    void testSetStatsRefreshInterval() {
        SettingsManager settingsManager = new SettingsManager();
        settingsManager.setStatsRefreshInterval(10);
        assertEquals(10, settingsManager.getStatsRefreshInterval());
    }

    @Test
    void savesAndLoadsContainerTreeState() throws Exception {
        Path configFile = Files.createTempDirectory("docker-wsl-manager-settings")
                .resolve("settings.properties");
        SettingsManager writer = new SettingsManager(configFile);
        ContainerTreeState expected = new ContainerTreeState(
                "container-1", "project-a", Map.of("project-a", false, "project-b", true));

        writer.setContainerTreeState(expected);
        writer.saveSettings();

        SettingsManager reader = new SettingsManager(configFile);
        assertEquals(expected, reader.getContainerTreeState());
    }

    @Test
    void malformedContainerTreeStateFallsBackToEmpty() throws Exception {
        Path configFile = Files.createTempFile("docker-wsl-manager-settings", ".properties");
        Files.writeString(configFile, "containers.tree.state=not-json\n");

        SettingsManager settingsManager = new SettingsManager(configFile);

        assertEquals(ContainerTreeState.empty(), settingsManager.getContainerTreeState());
    }

    @Test
    void connectionDefaultsPreserveWslCompatibility() throws Exception {
        Path configFile = Files.createTempDirectory("docker-wsl-manager-connection")
                .resolve("settings.properties");

        SettingsManager settingsManager = new SettingsManager(configFile);

        assertEquals(2375, settingsManager.getDockerPort());
        assertEquals(DockerConnectionMode.WSL_IP, settingsManager.getDockerConnectionMode());
        assertEquals("", settingsManager.getDockerTlsHost());
        assertEquals("", settingsManager.getDockerCertPath());
    }

    @Test
    void migratesLegacyTlsSetting() throws Exception {
        Path configFile = Files.createTempFile("docker-wsl-manager-tls", ".properties");
        Files.writeString(configFile, "docker.tls.enabled=true\n");

        SettingsManager settingsManager = new SettingsManager(configFile);

        assertEquals(DockerConnectionMode.TLS, settingsManager.getDockerConnectionMode());
    }

    @Test
    void savesExplicitConnectionMode() throws Exception {
        Path configFile = Files.createTempDirectory("docker-wsl-manager-mode")
                .resolve("settings.properties");
        SettingsManager writer = new SettingsManager(configFile);

        writer.setDockerConnectionMode(DockerConnectionMode.LOOPBACK);
        writer.saveSettings();

        SettingsManager reader = new SettingsManager(configFile);
        assertEquals(DockerConnectionMode.LOOPBACK, reader.getDockerConnectionMode());
    }

    @Test
    void persistsCompatibilityWarningPreference() throws Exception {
        Path configFile = Files.createTempDirectory("docker-wsl-manager-warning")
                .resolve("settings.properties");
        SettingsManager writer = new SettingsManager(configFile);

        writer.setDockerSecurityWarningDismissed(true);
        writer.saveSettings();

        SettingsManager reader = new SettingsManager(configFile);
        org.junit.jupiter.api.Assertions.assertTrue(reader.isDockerSecurityWarningDismissed());
    }
}
