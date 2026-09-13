package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DockerConnectionManagerTest {

    @Test
    void disconnectIfCurrentDoesNotCloseNewerConnection() throws Exception {
        DockerConnectionManager manager = new DockerConnectionManager();
        DockerClient currentClient = mock(DockerClient.class);
        DockerClient staleClient = mock(DockerClient.class);
        setDockerClient(manager, currentClient);

        assertFalse(manager.disconnectIfCurrent(staleClient));
        assertTrue(manager.isConnected());
        verify(currentClient, never()).close();

        assertTrue(manager.disconnectIfCurrent(currentClient));
        assertFalse(manager.isConnected());
        verify(currentClient).close();
    }

    @Test
    void extractsTheFirstWslIpv4Address() throws Exception {
        assertEquals("172.19.62.177",
                DockerConnectionManager.extractFirstIpv4("172.19.62.177 172.17.0.1 fd00::1\n"));
    }

    @Test
    void rejectsMissingOrInvalidWslIpv4Address() {
        assertThrows(IOException.class,
                () -> DockerConnectionManager.extractFirstIpv4("fd00::1 not-an-address"));
        assertThrows(IOException.class,
                () -> DockerConnectionManager.extractFirstIpv4("999.19.62.177"));
    }

    private static void setDockerClient(DockerConnectionManager manager, DockerClient client) throws Exception {
        Field field = DockerConnectionManager.class.getDeclaredField("dockerClient");
        field.setAccessible(true);
        field.set(manager, client);
    }
}
