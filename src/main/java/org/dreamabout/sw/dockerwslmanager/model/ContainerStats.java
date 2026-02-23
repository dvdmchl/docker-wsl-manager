package org.dreamabout.sw.dockerwslmanager.model;

/**
 * Model representing container resource consumption statistics.
 */
public class ContainerStats {
    private final String containerId;
    private final double cpuPercentage;
    private final long memoryUsage;
    private final long memoryLimit;
    private final long networkReadBytes;
    private final long networkWriteBytes;
    private final long diskReadBytes;
    private final long diskWriteBytes;

    @SuppressWarnings("java:S107")
    public ContainerStats(String containerId, double cpuPercentage, long memoryUsage, long memoryLimit,
                          long networkReadBytes, long networkWriteBytes, long diskReadBytes, long diskWriteBytes) {
        this.containerId = containerId;
        this.cpuPercentage = cpuPercentage;
        this.memoryUsage = memoryUsage;
        this.memoryLimit = memoryLimit;
        this.networkReadBytes = networkReadBytes;
        this.networkWriteBytes = networkWriteBytes;
        this.diskReadBytes = diskReadBytes;
        this.diskWriteBytes = diskWriteBytes;
    }

    public String getContainerId() {
        return containerId;
    }

    public double getCpuPercentage() {
        return cpuPercentage;
    }

    public long getMemoryUsage() {
        return memoryUsage;
    }

    public long getMemoryLimit() {
        return memoryLimit;
    }

    public long getNetworkReadBytes() {
        return networkReadBytes;
    }

    public long getNetworkWriteBytes() {
        return networkWriteBytes;
    }

    public long getDiskReadBytes() {
        return diskReadBytes;
    }

    public long getDiskWriteBytes() {
        return diskWriteBytes;
    }
}
