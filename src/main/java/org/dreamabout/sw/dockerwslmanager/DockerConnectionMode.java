package org.dreamabout.sw.dockerwslmanager;

/** Supported transports between the Windows application and Docker Engine in WSL. */
public enum DockerConnectionMode {
    WSL_IP("WSL address (compatibility, unencrypted)"),
    LOOPBACK("Windows localhost"),
    TLS("Authenticated TLS");

    private final String displayName;

    DockerConnectionMode(String displayName) {
        this.displayName = displayName;
    }

    public boolean usesTls() {
        return this == TLS;
    }

    public boolean isInsecureCompatibilityMode() {
        return this == WSL_IP;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
