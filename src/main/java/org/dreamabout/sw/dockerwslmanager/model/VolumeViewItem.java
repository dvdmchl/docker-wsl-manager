package org.dreamabout.sw.dockerwslmanager.model;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.dreamabout.sw.dockerwslmanager.logic.FormatUtils;

public class VolumeViewItem {
    private final String name;
    private final InspectVolumeResponse volume;
    private final boolean isGroup;
    private final boolean unused;
    private final LongProperty sizeBytes = new SimpleLongProperty(0);
    private final StringProperty sizeString = new SimpleStringProperty("-");
    private final ObservableList<String> containerNames = FXCollections.observableArrayList();
    private final BooleanProperty inUseByRunningContainer = new SimpleBooleanProperty(false);

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

    public long getSizeBytes() {
        return sizeBytes.get();
    }

    public LongProperty sizeBytesProperty() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes.set(sizeBytes);
        this.sizeString.set(FormatUtils.formatSize(sizeBytes));
    }

    public String getSizeString() {
        return sizeString.get();
    }

    public StringProperty sizeStringProperty() {
        return sizeString;
    }

    public ObservableList<String> getContainerNames() {
        return containerNames;
    }

    public boolean isInUseByRunningContainer() {
        return inUseByRunningContainer.get();
    }

    public BooleanProperty inUseByRunningContainerProperty() {
        return inUseByRunningContainer;
    }

    public void setInUseByRunningContainer(boolean inUseByRunningContainer) {
        this.inUseByRunningContainer.set(inUseByRunningContainer);
    }
}
