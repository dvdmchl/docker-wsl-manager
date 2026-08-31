package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static void setDockerClient(DockerConnectionManager manager, DockerClient client) throws Exception {
        Field field = DockerConnectionManager.class.getDeclaredField("dockerClient");
        field.setAccessible(true);
        field.set(manager, client);
    }
}
