package org.dreamabout.sw.dockerwslmanager.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigLogic {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLogic.class);
    private final ObjectMapper objectMapper;

    public ConfigLogic() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public InspectContainerResponse inspectContainer(DockerClient client, String containerId) {
        if (client == null || containerId == null || containerId.isEmpty()) {
            return null;
        }
        return client.inspectContainerCmd(containerId).exec();
    }

    public String formatAsPrettyJson(Object object) {
        if (object == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Failed to format object as JSON", e);
            return "Error formatting configuration: " + e.getMessage();
        }
    }
}
