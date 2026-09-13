package org.dreamabout.sw.dockerwslmanager;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WslDockerCommandTest {

    @Test
    void loopbackModeUsesTheDistributionsUnixSocketForCliOperations() {
        assertEquals(List.of("docker", "info"),
                WslDockerCommand.build(false, "", 2375, "", "info"));
    }

    @Test
    void tlsModePassesMutualTlsFilesAsSeparateArguments() {
        assertEquals(List.of(
                        "docker", "--host", "tcp://docker.example:2376", "--tlsverify",
                        "--tlscacert", "/mnt/c/Users/Test User/certs/ca.pem",
                        "--tlscert", "/mnt/c/Users/Test User/certs/cert.pem",
                        "--tlskey", "/mnt/c/Users/Test User/certs/key.pem", "info"),
                WslDockerCommand.build(true, "docker.example", 2376,
                        "C:\\Users\\Test User\\certs", "info"));
    }
}
