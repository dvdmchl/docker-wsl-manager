package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DockerConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DockerConnectionManager.class);
    private volatile DockerClient dockerClient;
    private volatile String currentConnectionString;
    private volatile String lastConnectionError;

    public DockerConnectionManager() {
        // Default constructor
    }

    /**
     * Connects using the selected WSL distribution and the configured secure transport.
     * Loopback mode never discovers or connects to the distribution's network address.
     */
    public boolean connectConfigured(String distribution, int port, DockerConnectionMode mode,
                                     String tlsHost, String certificatePath) {
        disconnect();
        lastConnectionError = null;

        try {
            WslCommandLine.normalizeDistribution(distribution);
        } catch (IllegalArgumentException e) {
            return fail(e.getMessage(), e);
        }

        DockerConnectionMode selectedMode = mode == null ? DockerConnectionMode.WSL_IP : mode;
        String prerequisiteFailure = checkWslDockerPrerequisites(
                distribution, selectedMode.usesTls(), tlsHost, port, certificatePath);
        if (prerequisiteFailure != null) {
            return fail(prerequisiteFailure, null);
        }

        return switch (selectedMode) {
            case WSL_IP -> connectWslAddress(distribution, port);
            case LOOPBACK -> connectLoopback(port);
            case TLS -> connectTls(tlsHost, port, certificatePath);
        };
    }

    private boolean connectWslAddress(String distribution, int port) {
        if (!isValidPort(port)) {
            return fail("Docker port must be between 1 and 65535.", null);
        }
        try {
            String address = discoverWslIpv4Address(distribution);
            return connectTcp(address, port, " (unencrypted compatibility mode)");
        } catch (IOException e) {
            return fail("Could not discover the selected WSL distribution address: " + rootMessage(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fail("WSL address discovery was interrupted.", e);
        }
    }

    private boolean connectLoopback(int port) {
        if (!isValidPort(port)) {
            return fail("Docker port must be between 1 and 65535.", null);
        }
        return connectTcp("127.0.0.1", port, "");
    }

    private boolean connectTcp(String host, int port, String descriptionSuffix) {
        String dockerHost = String.format("tcp://%s:%d", host, port);
        try {
            logger.info("Connecting to Docker at: {}{}", dockerHost, descriptionSuffix);
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();
            return connectWithConfig(config, dockerHost + descriptionSuffix);
        } catch (RuntimeException e) {
            return fail("Could not configure the Docker TCP connection: " + rootMessage(e), e);
        }
    }

    private boolean connectTls(String host, int port, String certificatePath) {
        if (host == null || host.isBlank()) {
            return fail("A Docker TLS host is required when TLS mode is enabled.", null);
        }
        if (!isValidPort(port)) {
            return fail("Docker port must be between 1 and 65535.", null);
        }
        if (certificatePath == null || certificatePath.isBlank()) {
            return fail("A Docker TLS certificate directory is required when TLS mode is enabled.", null);
        }

        Path certDirectory = Path.of(certificatePath);
        if (!Files.isDirectory(certDirectory)
                || !Files.isRegularFile(certDirectory.resolve("ca.pem"))
                || !Files.isRegularFile(certDirectory.resolve("cert.pem"))
                || !Files.isRegularFile(certDirectory.resolve("key.pem"))) {
            return fail("The Docker TLS directory must contain ca.pem, cert.pem and key.pem.", null);
        }

        String dockerHost = String.format("tcp://%s:%d", host.trim(), port);
        try {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .withDockerTlsVerify(true)
                    .withDockerCertPath(certDirectory.toString())
                    .build();
            return connectWithConfig(config, dockerHost + " (TLS)");
        } catch (RuntimeException e) {
            return fail("Could not configure authenticated Docker TLS: " + rootMessage(e), e);
        }
    }

    /**
     * Auto-discover Docker in WSL using wsl command.
     */
    public boolean connectAutoDiscover() {
        return connectConfigured("auto-detect", 2375, DockerConnectionMode.WSL_IP, "", "");
    }

    private String discoverWslIpv4Address(String distribution)
            throws IOException, InterruptedException {
        Process process = WslCommandLine.processBuilder(distribution, "hostname", "-I")
                .redirectErrorStream(true)
                .start();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("WSL did not return its address within 10 seconds.");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IOException("hostname -I failed: " + output.trim());
            }
            return extractFirstIpv4(output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    static String extractFirstIpv4(String output) throws IOException {
        if (output != null) {
            for (String candidate : output.trim().split("\\s+")) {
                String[] octets = candidate.split("\\.", -1);
                if (octets.length != 4) {
                    continue;
                }
                boolean valid = true;
                for (String octet : octets) {
                    if (!octet.matches("\\d{1,3}")) {
                        valid = false;
                        break;
                    }
                    try {
                        int value = Integer.parseInt(octet);
                        valid &= value >= 0 && value <= 255;
                    } catch (NumberFormatException e) {
                        valid = false;
                    }
                }
                if (valid) {
                    return candidate;
                }
            }
        }
        throw new IOException("No IPv4 address was returned by WSL.");
    }

    private String checkWslDockerPrerequisites(String distribution, boolean tlsEnabled,
                                               String host, int port, String certificatePath) {
        Process process = null;
        try {
            java.util.List<String> dockerCommand = WslDockerCommand.build(
                    tlsEnabled, host, port, certificatePath,
                    "info", "--format", "{{.ServerVersion}}");
            ProcessBuilder builder = WslCommandLine.processBuilder(
                    distribution, dockerCommand.toArray(String[]::new));
            builder.redirectErrorStream(true);
            process = builder.start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "Docker did not respond inside the selected WSL distribution within 15 seconds.";
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                String suffix = output.isBlank() ? "" : " Details: " + output;
                return "Docker CLI is unavailable or the daemon is stopped in the selected WSL distribution."
                        + suffix;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (IOException e) {
            return "WSL could not be started. Install WSL 2 and a Linux distribution, then retry. Details: "
                    + rootMessage(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "The WSL prerequisite check was interrupted.";
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Connect with a specific Docker client configuration.
     */
    private synchronized boolean connectWithConfig(DockerClientConfig config, String connectionString) {
        DockerClient newClient = null;
        try {
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    // Use longer timeout for log streaming operations which may have long idle periods
                    .responseTimeout(Duration.ofMinutes(5))
                    .build();
            
            newClient = DockerClientImpl.getInstance(config, httpClient);
            
            // Test connection by pinging
            newClient.pingCmd().exec();

            DockerClient previousClient = dockerClient;
            dockerClient = newClient;
            if (previousClient != null) {
                closeClient(previousClient);
            }
            
            currentConnectionString = connectionString;
            lastConnectionError = null;
            logger.info("Successfully connected to Docker");
            return true;
        } catch (Exception e) {
            logger.error("Failed to establish Docker connection", e);
            if (newClient != null) {
                closeClient(newClient);
            }
            if (dockerClient != null) {
                closeClient(dockerClient);
            }
            dockerClient = null;
            currentConnectionString = null;
            if (lastConnectionError == null) {
                lastConnectionError = "Docker is running in WSL, but its configured endpoint is unreachable. "
                        + "For the recommended setup, expose Docker only on WSL loopback and enable "
                        + "WSL localhost forwarding. "
                        + "Details: " + rootMessage(e);
            }
            return false;
        }
    }

    private boolean fail(String message, Exception exception) {
        lastConnectionError = message;
        if (exception == null) {
            logger.warn(message);
        } else {
            logger.error(message, exception);
        }
        return false;
    }

    private static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    /**
     * Disconnect from Docker.
     */
    public synchronized void disconnect() {
        if (dockerClient != null) {
            closeClient(dockerClient);
            dockerClient = null;
            currentConnectionString = null;
        }
    }

    /**
     * Disconnect only if the supplied client is still the active connection.
     * This prevents a failed background request from closing a newer reconnect.
     */
    public synchronized boolean disconnectIfCurrent(DockerClient expectedClient) {
        if (expectedClient == null || dockerClient != expectedClient) {
            return false;
        }
        disconnect();
        return true;
    }

    private void closeClient(DockerClient client) {
        try {
            client.close();
        } catch (Exception e) {
            logger.error("Error closing Docker client", e);
        }
    }

    /**
     * Check if connected to Docker.
     */
    public boolean isConnected() {
        return dockerClient != null;
    }

    /**
     * Get the Docker client.
     */
    public DockerClient getDockerClient() {
        return dockerClient;
    }

    /**
     * Get current connection string.
     */
    public String getCurrentConnectionString() {
        return currentConnectionString;
    }

    public String getLastConnectionError() {
        return lastConnectionError;
    }

    /**
     * Test connection.
     */
    public boolean testConnection() {
        if (dockerClient == null) {
            return false;
        }
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            logger.error("Connection test failed", e);
            return false;
        }
    }
}
