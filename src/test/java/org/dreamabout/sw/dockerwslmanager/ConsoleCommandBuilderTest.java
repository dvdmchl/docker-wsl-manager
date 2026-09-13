package org.dreamabout.sw.dockerwslmanager;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsoleCommandBuilderTest {

    @Test
    void launchesDockerCliInsideSelectedWslDistribution() {
        assertEquals(List.of(
                        "cmd.exe", "/c", "start", "", "wsl.exe", "--distribution", "Ubuntu",
                        "--exec", "docker", "exec", "-it", "0123456789abcdef", "sh"),
                ConsoleCommandBuilder.build("Ubuntu", "0123456789abcdef"));
    }

    @Test
    void rejectsContainerIdThatCouldBeInterpretedByCmd() {
        assertThrows(IllegalArgumentException.class,
                () -> ConsoleCommandBuilder.build("Ubuntu", "container & whoami"));
    }
}
