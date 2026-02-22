package org.dreamabout.sw.dockerwslmanager.model;

import com.github.dockerjava.api.command.InspectVolumeResponse;

public class VolumeViewItem {
    private final String name;
    private final InspectVolumeResponse volume;
    private final boolean isGroup;
    private final boolean unused;

    // Constructor for group
    public VolumeViewItem(String name) {
        this.name = name;
        this.volume = null;
        this.isGroup = true;
        this.unused = false;
    }

    // Constructor for item
    public VolumeViewItem(InspectVolumeResponse volume, String name, boolean unused) {
        this.name = name;
        this.volume = volume;
        this.isGroup = false;
        this.unused = unused;
    }

    public String getName() {
        return name;
    }

    public InspectVolumeResponse getVolume() {
        return volume;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public boolean isUnused() {
        return unused;
    }
}
