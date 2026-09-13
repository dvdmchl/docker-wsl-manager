package org.dreamabout.sw.dockerwslmanager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateManagerTest {

    @Test
    void storeChannelDoesNotOfferGithubUpdates() {
        UpdateManager manager = new UpdateManager(UpdateManager.UpdateChannel.STORE);

        assertTrue(manager.isStoreManaged());
        assertTrue(manager.checkForUpdates().isEmpty());
        assertEquals(UpdateManager.UpdateChannel.STORE, manager.getUpdateChannel());
    }
}
