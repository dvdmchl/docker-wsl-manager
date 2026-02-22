package org.dreamabout.sw.dockerwslmanager.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VolumePathResolver {
    private static final Logger logger = LoggerFactory.getLogger(VolumePathResolver.class);

    private String wslDistro;

    public VolumePathResolver(String wslDistro) {
        if (wslDistro == null || wslDistro.isEmpty() || wslDistro.equalsIgnoreCase("auto-detect")) {
            this.wslDistro = detectDefaultDistro();
        } else {
            this.wslDistro = wslDistro;
        }
    }

    private String detectDefaultDistro() {
        try {
            Process process = new ProcessBuilder("wsl", "--list", "--quiet").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_16LE))) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    // The first line of wsl -l -q is the default distro
                    String distro = line.trim();
                    logger.info("Auto-detected default WSL distro: {}", distro);
                    return distro;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to auto-detect WSL distro, falling back to docker-desktop-data", e);
        }
        return "docker-desktop-data";
    }

    /**
     * Resolves the Windows path for a named Docker volume.
     * Pattern: {@code \\\\wsl.localhost\\distro\\var\\lib\\docker\\volumes\\name\\_data}
     */
    public Optional<String> resolveNamedVolumePath(String volumeName) {
        if (volumeName == null || volumeName.isEmpty()) {
            return Optional.empty();
        }
        
        String path = "\\\\wsl.localhost\\" + wslDistro + "\\var\\lib\\docker\\volumes\\" + volumeName + "\\_data";
        
        logger.debug("Resolved named volume '{}' to path: {}", volumeName, path);
        return Optional.of(path);
    }

    /**
     * Resolves the Windows path for a bind mount.
     * If source is already a Windows path, returns it.
     * If it's a Linux path, attempts to bridge via \\wsl.localhost.
     */
    public Optional<String> resolveBindMountPath(String sourcePath) {
        if (sourcePath == null || sourcePath.isEmpty()) {
            return Optional.empty();
        }

        // Check if it's a Windows path (e.g., C:\..., \\server\...)
        if (sourcePath.matches("^[a-zA-Z]:\\\\.*") || sourcePath.startsWith("\\\\")) {
            return Optional.of(sourcePath);
        }

        // Handle WSL mount point /mnt/<drive>/...
        if (sourcePath.startsWith("/mnt/")) {
            String remainder = sourcePath.substring(5);
            if (remainder.length() >= 2 && remainder.charAt(1) == '/') {
                char drive = remainder.charAt(0);
                String windowsPath = drive + ":" + remainder.substring(1).replace('/', '\\');
                return Optional.of(windowsPath);
            }
        }

        // Otherwise assume it's a Linux internal path and bridge via network
        String path = "\\\\wsl.localhost\\" + wslDistro + sourcePath.replace('/', '\\');
        
        logger.debug("Resolved bind mount '{}' to path: {}", sourcePath, path);
        return Optional.of(path);
    }
    
    public boolean exists(String path) {
        if (path == null) {
            return false;
        }
        return new File(path).exists();
    }
}
