package org.dreamabout.sw.dockerwslmanager.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import org.dreamabout.sw.dockerwslmanager.model.ContainerStats;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.Closeable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerStatsServiceTest {

    @Test
    void testFetchStatsReturnsCloseable() throws Exception {
        DockerClient dockerClient = mock(DockerClient.class);
        StatsCmd statsCmd = mock(StatsCmd.class);
        when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);
        // We need to mock the exec call which takes a ResultCallback
        ResultCallback<?> callback = mock(ResultCallback.class);
        when(statsCmd.exec(any())).thenReturn((ResultCallback) callback);

        ContainerStatsService service = new ContainerStatsService(dockerClient);
        Closeable closeable = service.fetchStats("container-id", stats -> {});
        
        assertNotNull(closeable);
    }
}
