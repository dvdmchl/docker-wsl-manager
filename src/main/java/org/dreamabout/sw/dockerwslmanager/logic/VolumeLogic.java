package org.dreamabout.sw.dockerwslmanager.logic;

import com.github.dockerjava.api.command.InspectVolumeResponse;
import java.util.ArrayList;
import java.util.Collections;
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
}
