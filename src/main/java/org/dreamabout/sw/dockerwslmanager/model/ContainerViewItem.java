package org.dreamabout.sw.dockerwslmanager.model;

import com.github.dockerjava.api.model.Container;

public class ContainerViewItem {
    private final String name;
    private final Container container;
    private final boolean isGroup;

    // Constructor for group
    public ContainerViewItem(String name) {
        this.name = name;
        this.container = null;
        this.isGroup = true;
    }

    // Constructor for item
    public ContainerViewItem(Container container, String name) {
        this.name = name;
        this.container = container;
        this.isGroup = false;
    }

    public String getName() {
        return name;
    }

    public Container getContainer() {
        return container;
    }

    public boolean isGroup() {
        return isGroup;
    }
}
