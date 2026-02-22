package org.dreamabout.sw.dockerwslmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VolumeUsageService {
    private static final Logger logger = LoggerFactory.getLogger(VolumeUsageService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches volume sizes using 'wsl docker system df -v --format "{{json .}}"'.
     * Returns a map of volume name to size in bytes.
     */
    public Map<String, Long> fetchVolumeSizes() {
        Map<String, Long> volumeSizes = new HashMap<>();
        try {
            logger.info("Executing 'wsl docker system df' to fetch volume sizes...");
            ProcessBuilder pb = new ProcessBuilder("wsl", "docker", "system", "df", "-v", "--format", "{{json .}}");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    String output = sb.toString().trim();
                    if (output.isEmpty()) {
                        logger.warn("'docker system df' returned empty output");
                        return volumeSizes;
                    }
                    
                    JsonNode root = objectMapper.readTree(output);
                    JsonNode volumes = root.get("Volumes");
                    if (volumes != null && volumes.isArray()) {
                        for (JsonNode vol : volumes) {
                            String name = vol.get("Name").asText();
                            String sizeStr = vol.get("Size").asText();
                            long sizeBytes = parseDockerSize(sizeStr);
                            volumeSizes.put(name, sizeBytes);
                        }
                        logger.info("Successfully fetched sizes for {} volumes", volumes.size());
                    } else {
                        logger.warn("No 'Volumes' node found in docker system df output");
                    }
                } else {
                    logger.error("'docker system df' failed with exit code: {}", exitCode);
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to fetch volume sizes", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return volumeSizes;
    }

    /**
     * Parses Docker size strings like "1.11GB", "66.59MB", "903.1kB", "9B" to bytes.
     */
    public static long parseDockerSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty() || sizeStr.equals("N/A")) {
            return 0L;
        }

        try {
            String unit = "";
            String valueStr = "";

            // Find where the unit starts
            int i = 0;
            while (i < sizeStr.length() && (Character.isDigit(sizeStr.charAt(i)) || sizeStr.charAt(i) == '.')) {
                i++;
            }

            valueStr = sizeStr.substring(0, i).trim();
            unit = sizeStr.substring(i).trim().toUpperCase(java.util.Locale.ROOT);

            if (valueStr.isEmpty()) {
                return 0L;
            }

            double value = Double.parseDouble(valueStr);
            return switch (unit) {
                case "KB", "K" -> (long) (value * 1024);
                case "MB", "M" -> (long) (value * 1024 * 1024);
                case "GB", "G" -> (long) (value * 1024 * 1024 * 1024);
                case "TB", "T" -> (long) (value * 1024 * 1024 * 1024 * 1024);
                default -> (long) value; // Assuming B or no unit
            };
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            logger.warn("Failed to parse size string: {}", sizeStr, e);
            return 0L;
        }
    }
}
