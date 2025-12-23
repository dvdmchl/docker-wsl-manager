package org.dreamabout.sw.dockerwslmanager.model;

import com.github.dockerjava.api.model.Image;

public class ImageViewItem {
    private final String name;
    private final Image image;
    private final boolean isGroup;

    // Constructor for group
    public ImageViewItem(String name) {
        this.name = name;
        this.image = null;
        this.isGroup = true;
    }

    // Constructor for item
    public ImageViewItem(Image image, String name) {
        this.name = name;
        this.image = image;
        this.isGroup = false;
    }

    public String getName() {
        return name;
    }

    public Image getImage() {
        return image;
    }

    public boolean isGroup() {
        return isGroup;
    }
}
