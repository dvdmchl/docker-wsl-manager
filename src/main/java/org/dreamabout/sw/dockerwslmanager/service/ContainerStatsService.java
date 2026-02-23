package org.dreamabout.sw.dockerwslmanager.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.BlkioStatEntry;
import com.github.dockerjava.api.model.CpuStatsConfig;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.StatisticNetworksConfig;
import com.github.dockerjava.api.model.Statistics;
import org.dreamabout.sw.dockerwslmanager.model.ContainerStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Service to fetch container statistics asynchronously.
 */
public class ContainerStatsService {
    private static final Logger logger = LoggerFactory.getLogger(ContainerStatsService.class);
    private final DockerClient dockerClient;

    public ContainerStatsService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    /**
     * Starts an asynchronous stream of statistics for the given container.
     *
     * @param containerId     The ID of the container.
     * @param onStatsReceived Callback to be invoked when new stats are available.
     * @param onComplete      Callback to be invoked when the stream completes.
     * @param onError         Callback to be invoked when an error occurs.
     * @return A Closeable that can be used to stop the stream.
     */
    public Closeable fetchStats(String containerId, Consumer<ContainerStats> onStatsReceived, 
                                Runnable onComplete, Consumer<Throwable> onError) {
        logger.info("Starting stats stream for container: {}", containerId);
        return dockerClient.statsCmd(containerId).exec(new ResultCallback<Statistics>() {
            @Override
            public void onStart(Closeable closeable) {
                logger.debug("Stats stream started for {}", containerId);
            }

            @Override
            public void onNext(Statistics statistics) {
                if (statistics != null) {
                    try {
                        ContainerStats parsedStats = parseStatistics(containerId, statistics);
                        onStatsReceived.accept(parsedStats);
                    } catch (Exception e) {
                        logger.error("Error parsing stats for container: {}", containerId, e);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error in stats stream for container: {}", containerId, throwable);
                if (onError != null) {
                    onError.accept(throwable);
                }
            }

            @Override
            public void onComplete() {
                logger.info("Stats stream completed for container: {}", containerId);
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void close() throws IOException {
                logger.debug("Closing stats stream for {}", containerId);
            }
        });
    }

    /**
     * Starts an asynchronous stream of statistics for the given container with default handlers.
     */
    public Closeable fetchStats(String containerId, Consumer<ContainerStats> onStatsReceived) {
        return fetchStats(containerId, onStatsReceived, null, null);
    }

    /**
     * Parses Docker Statistics into our internal ContainerStats model.
     */
    public ContainerStats parseStatistics(String containerId, Statistics stats) {
        long memUsage = 0L;
        long memLimit = 0L;
        MemoryStatsConfig mem = stats.getMemoryStats();
        if (mem != null) {
            Long usage = mem.getUsage();
            if (usage != null) {
                memUsage = usage;
            }
            Long limit = mem.getLimit();
            if (limit != null) {
                memLimit = limit;
            }
        }

        long netRead = 0L;
        long netWrite = 0L;
        Map<String, StatisticNetworksConfig> networks = stats.getNetworks();
        if (networks != null) {
            for (StatisticNetworksConfig net : networks.values()) {
                if (net != null) {
                    Long rx = net.getRxBytes();
                    if (rx != null) {
                        netRead += rx;
                    }
                    Long tx = net.getTxBytes();
                    if (tx != null) {
                        netWrite += tx;
                    }
                }
            }
        }

        long diskRead = 0L;
        long diskWrite = 0L;
        if (stats.getBlkioStats() != null) {
            List<BlkioStatEntry> ioBytes = stats.getBlkioStats().getIoServiceBytesRecursive();
            if (ioBytes != null) {
                for (BlkioStatEntry bio : ioBytes) {
                    if (bio != null && bio.getOp() != null) {
                        if ("Read".equalsIgnoreCase(bio.getOp())) {
                            Long val = bio.getValue();
                            if (val != null) {
                                diskRead += val;
                            }
                        } else if ("Write".equalsIgnoreCase(bio.getOp())) {
                            Long val = bio.getValue();
                            if (val != null) {
                                diskWrite += val;
                            }
                        }
                    }
                }
            }
        }

        double cpuPercent = calculateCpuPercent(stats);
        return new ContainerStats(containerId, cpuPercent, memUsage, memLimit, netRead, netWrite, diskRead, diskWrite);
    }

    private double calculateCpuPercent(Statistics stats) {
        CpuStatsConfig cpuStats = stats.getCpuStats();
        CpuStatsConfig precpuStats = stats.getPreCpuStats();

        if (cpuStats == null || precpuStats == null) {
            return 0.0;
        }

        com.github.dockerjava.api.model.CpuUsageConfig cpuUsage = cpuStats.getCpuUsage();
        com.github.dockerjava.api.model.CpuUsageConfig preCpuUsage = precpuStats.getCpuUsage();

        if (cpuUsage == null || preCpuUsage == null) {
            return 0.0;
        }

        Long totalUsageLong = cpuUsage.getTotalUsage();
        if (totalUsageLong == null) {
            return 0.0;
        }
        Long preTotalUsageLong = preCpuUsage.getTotalUsage();
        if (preTotalUsageLong == null) {
            return 0.0;
        }

        Long systemUsageLong = cpuStats.getSystemCpuUsage();
        if (systemUsageLong == null) {
            return 0.0;
        }
        Long preSystemUsageLong = precpuStats.getSystemCpuUsage();
        if (preSystemUsageLong == null) {
            return 0.0;
        }

        long cpuDelta = totalUsageLong.longValue() - preTotalUsageLong.longValue();
        long systemDelta = systemUsageLong.longValue() - preSystemUsageLong.longValue();

        if (systemDelta > 0 && cpuDelta > 0) {
            int numCpus = 1;
            if (cpuStats.getOnlineCpus() != null) {
                numCpus = cpuStats.getOnlineCpus().intValue();
            } else if (cpuUsage.getPercpuUsage() != null) {
                numCpus = cpuUsage.getPercpuUsage().size();
            }
            return (double) cpuDelta / systemDelta * numCpus * 100.0;
        }

        return 0.0;
    }
}
