package org.dreamabout.sw.dockerwslmanager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class DockerConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DockerConnectionManager.class);
    private DockerClient dockerClient;
    private String currentConnectionString;

    public DockerConnectionManager() {
        // Default constructor
    }

    /**
     * Connect using DOCKER_HOST environment variable.
     */
    public boolean connectFromEnvironment() {
        try {
            String dockerHost = System.getenv("DOCKER_HOST");
            if (dockerHost == null || dockerHost.isEmpty()) {
                logger.warn("DOCKER_HOST environment variable not set");
                return false;
            }
            
            logger.info("Connecting to Docker using DOCKER_HOST: {}", dockerHost);
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();
            
            return connectWithConfig(config, dockerHost);
        } catch (Exception e) {
            logger.error("Failed to connect using DOCKER_HOST", e);
            return false;
        }
    }

    /**
     * Connect using manual IP and port configuration.
     */
    public boolean connectManual(String host, int port) {
        try {
            String dockerHost = String.format("tcp://%s:%d", host, port);
            logger.info("Connecting to Docker at: {}", dockerHost);
            
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();
            
            return connectWithConfig(config, dockerHost);
        } catch (Exception e) {
            logger.error("Failed to connect manually to {}:{}", host, port, e);
            return false;
        }
    }

    /**
     * Auto-discover Docker in WSL using wsl command.
     */
    public boolean connectAutoDiscover() {
        try {
            logger.info("Auto-discovering Docker in WSL...");
            
            // Try to get WSL IP address
            String wslIp = getWslIpAddress();
            if (wslIp == null) {
                logger.error("Failed to discover WSL IP address");
                return false;
            }
            
            logger.info("Discovered WSL IP: {}", wslIp);
            
            // Try standard Docker port
            return connectManual(wslIp, 2375);
        } catch (Exception e) {
            logger.error("Failed to auto-discover Docker in WSL", e);
            return false;
        }
    }

    /**
     * Get WSL IP address using wsl command.
     */
    private String getWslIpAddress() {
        try {
            // Try to execute: wsl hostname -I
            ProcessBuilder pb = new ProcessBuilder("wsl", "hostname", "-I");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                
                int exitCode = process.waitFor();
                if (exitCode == 0 && line != null && !line.isEmpty()) {
                    // hostname -I may return multiple IPs, take the first one
                    String[] ips = line.trim().split("\\s+");
                    return ips[0];
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to get WSL IP address", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

    /**
     * Connect with a specific Docker client configuration.
     */
    private boolean connectWithConfig(DockerClientConfig config, String connectionString) {
        try {
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    // Use longer timeout for log streaming operations which may have long idle periods
                    .responseTimeout(Duration.ofMinutes(5))
                    .build();
            
            dockerClient = DockerClientBuilder.getInstance(config)
                    .withDockerHttpClient(httpClient)
                    .build();
            
            // Test connection by pinging
            dockerClient.pingCmd().exec();
            
            currentConnectionString = connectionString;
            logger.info("Successfully connected to Docker");
            return true;
        } catch (Exception e) {
            logger.error("Failed to establish Docker connection", e);
            dockerClient = null;
            currentConnectionString = null;
            return false;
        }
    }

    /**
     * Disconnect from Docker.
     */
    public void disconnect() {
        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (Exception e) {
                logger.error("Error closing Docker client", e);
            }
            dockerClient = null;
            currentConnectionString = null;
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
