package org.dreamabout.sw.dockerwslmanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class UpdateManager {
    private static final Logger logger = LoggerFactory.getLogger(UpdateManager.class);
    private static final String REPO_OWNER = "dvdmchl";
    private static final String REPO_NAME = "docker-wsl-manager";
    
    // Fallback if manifest is missing
    private static final String DEV_VERSION = "1.1.0";

    public static class ReleaseInfo {
        private String tagName;
        private String htmlUrl;
        private String body;
        
        public String getTagName() {
            return tagName;
        }

        public String getHtmlUrl() {
            return htmlUrl;
        }

        public String getBody() {
            return body;
        }
    }

    public String getCurrentVersion() {
        String v = getClass().getPackage().getImplementationVersion();
        return v != null ? v : DEV_VERSION;
    }

    public Optional<ReleaseInfo> checkForUpdates() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest"))
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                
                String tagName = root.path("tag_name").asText();
                String cleanTag = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                String current = getCurrentVersion();
                
                if (isNewer(cleanTag, current)) {
                    ReleaseInfo info = new ReleaseInfo();
                    info.tagName = tagName;
                    info.htmlUrl = root.path("html_url").asText();
                    info.body = root.path("body").asText();
                    return Optional.of(info);
                }
            } else {
                logger.warn("GitHub API returned status: {}", response.statusCode());
            }
        } catch (java.io.IOException | InterruptedException e) {
            logger.error("Failed to check for updates", e);
        }
        return Optional.empty();
    }

    private boolean isNewer(String remote, String current) {
        if (remote == null || current == null) {
            return false;
        }
        if (remote.equals(current)) {
            return false;
        }
        
        String[] remoteParts = remote.split("\\.");
        String[] currentParts = current.split("\\.");
        
        int length = Math.max(remoteParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int r = i < remoteParts.length ? parse(remoteParts[i]) : 0;
            int c = i < currentParts.length ? parse(currentParts[i]) : 0;
            if (r > c) {
                return true;
            }
            if (r < c) {
                return false;
            }
        }
        return false;
    }
    
    private int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
