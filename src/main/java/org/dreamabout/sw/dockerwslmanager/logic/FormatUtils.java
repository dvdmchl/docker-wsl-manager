package org.dreamabout.sw.dockerwslmanager.logic;

import java.util.Locale;

public final class FormatUtils {
    private FormatUtils() {}

    /**
     * Formats a size in bytes into a human-readable string.
     */
    public static String formatSize(Long size) {
        if (size == null) {
            return "0 B";
        }

        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format(Locale.US, "%.2f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format(Locale.US, "%.2f MB", size / (1024.0 * 1024));
        }
        return String.format(Locale.US, "%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}
