package org.dreamabout.sw.dockerwslmanager;

import org.junit.jupiter.api.Test;
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
}
