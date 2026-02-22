package org.dreamabout.sw.dockerwslmanager.model;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class VolumeViewItemTest {

    @Test
    public void testGroupConstructor() {
        VolumeViewItem item = new VolumeViewItem("Group1");
        assertTrue(item.isGroup());
        assertEquals("Group1", item.getName());
        assertNull(item.getVolume());
        assertFalse(item.isUnused());
    }

    @Test
    public void testItemConstructor() {
        InspectVolumeResponse mockVolume = mock(InspectVolumeResponse.class);
        VolumeViewItem item = new VolumeViewItem(mockVolume, "Vol1", false);
        assertFalse(item.isGroup());
        assertEquals("Vol1", item.getName());
        assertEquals(mockVolume, item.getVolume());
        assertFalse(item.isUnused());
    }

    @Test
    public void testItemConstructorWithUnused() {
        InspectVolumeResponse mockVolume = mock(InspectVolumeResponse.class);
        VolumeViewItem item = new VolumeViewItem(mockVolume, "Vol1", true);
        assertFalse(item.isGroup());
        assertEquals("Vol1", item.getName());
        assertEquals(mockVolume, item.getVolume());
        assertTrue(item.isUnused());
    }

    @Test
    public void testSizeProperties() {
        VolumeViewItem item = new VolumeViewItem("Group1");
        assertEquals(0L, item.getSizeBytes());
        assertEquals("-", item.getSizeString());

        item.setSizeBytes(1024L);
        assertEquals(1024L, item.getSizeBytes());
        assertEquals("1.00 KB", item.getSizeString());
    }
}
