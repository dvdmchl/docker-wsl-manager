package org.dreamabout.sw.dockerwslmanager.logic;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerMount;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VolumeLogic {

    public static final String UNGROUPED_LABEL = "Ungrouped";

    public Map<String, List<InspectVolumeResponse>> groupVolumes(List<InspectVolumeResponse> volumes) {
        if (volumes == null) {
            return Collections.emptyMap();
        }

        Map<String, List<InspectVolumeResponse>> grouped = new TreeMap<>();
        List<InspectVolumeResponse> ungrouped = new ArrayList<>();

        for (InspectVolumeResponse vol : volumes) {
            String project = null;
            if (vol.getLabels() != null) {
                project = vol.getLabels().get("com.docker.compose.project");
            }

            if (project != null && !project.isEmpty()) {
                grouped.computeIfAbsent(project, k -> new ArrayList<>()).add(vol);
            } else {
                ungrouped.add(vol);
            }
        }
        if (!ungrouped.isEmpty()) {
            grouped.put(UNGROUPED_LABEL, ungrouped);
        }
        return grouped;
    }

    public Set<String> extractVolumeNames(List<InspectVolumeResponse> volumes) {
        if (volumes == null) {
            return Collections.emptySet();
        }
        return volumes.stream()
                .map(InspectVolumeResponse::getName)
                .collect(Collectors.toSet());
    }

    public Map<String, List<String>> mapVolumesToContainers(List<Container> containers) {
        if (containers == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> volumeToContainers = new HashMap<>();

        for (Container container : containers) {
            String containerName = extractPrimaryName(container);
            if (container.getMounts() != null) {
                for (ContainerMount mount : container.getMounts()) {
                    String volumeName = mount.getName();
                    if (volumeName != null && !volumeName.isEmpty()) {
                        volumeToContainers.computeIfAbsent(volumeName, k -> new ArrayList<>()).add(containerName);
                    }
                }
            }
        }

        return volumeToContainers;
    }

    public Set<String> getRunningContainerVolumeNames(List<Container> containers) {
        if (containers == null) {
            return Collections.emptySet();
        }

        Set<String> volumeNames = new HashSet<>();
        for (Container container : containers) {
            if ("running".equalsIgnoreCase(container.getState()) && container.getMounts() != null) {
                for (ContainerMount mount : container.getMounts()) {
                    if (mount.getName() != null && !mount.getName().isEmpty()) {
                        volumeNames.add(mount.getName());
                    }
                }
            }
        }
        return volumeNames;
    }

    private String extractPrimaryName(Container container) {
        if (container.getNames() != null && container.getNames().length > 0) {
            String name = container.getNames()[0];
            return name.startsWith("/") ? name.substring(1) : name;
        }
        return container.getId().substring(0, 12);
    }
}
