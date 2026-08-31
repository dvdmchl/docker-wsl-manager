package org.dreamabout.sw.dockerwslmanager.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.StatsCmd;
import org.dreamabout.sw.dockerwslmanager.model.ContainerStats;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContainerStatsServiceTest {

    @Test
    void testFetchStatsReturnsCloseable() throws Exception {
        DockerClient dockerClient = mock(DockerClient.class);
        StatsCmd statsCmd = mock(StatsCmd.class);
        when(dockerClient.statsCmd(anyString())).thenReturn(statsCmd);
        AtomicReference<ResultCallback<?>> callback = new AtomicReference<>();
        when(statsCmd.exec(any())).thenAnswer(invocation -> {
            ResultCallback<?> suppliedCallback = invocation.getArgument(0);
            callback.set(suppliedCallback);
            return suppliedCallback;
        });

        ContainerStatsService service = new ContainerStatsService(dockerClient);
        Closeable closeable = service.fetchStats("container-id", stats -> {});

        assertNotNull(closeable);

        Closeable transportStream = mock(Closeable.class);
        callback.get().onStart(transportStream);
        closeable.close();

        verify(transportStream).close();
    }
}
