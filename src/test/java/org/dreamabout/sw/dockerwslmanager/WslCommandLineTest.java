package org.dreamabout.sw.dockerwslmanager;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WslCommandLineTest {

    @Test
    void targetsSelectedDistributionWithoutShellQuoting() {
        assertEquals(
                List.of("wsl.exe", "--distribution", "Ubuntu-24.04", "--exec",
                        "docker", "volume", "inspect", "name with spaces"),
                WslCommandLine.build("Ubuntu-24.04", "docker", "volume", "inspect", "name with spaces"));
    }

    @Test
    void autoDetectUsesDefaultDistribution() {
        assertEquals(
                List.of("wsl.exe", "--exec", "docker", "info"),
                WslCommandLine.build("auto-detect", "docker", "info"));
    }

    @Test
    void rejectsShellMetacharactersInDistributionName() {
        assertThrows(IllegalArgumentException.class,
                () -> WslCommandLine.build("Ubuntu & whoami", "docker", "info"));
    }
}
