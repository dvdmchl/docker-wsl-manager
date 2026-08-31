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
}
