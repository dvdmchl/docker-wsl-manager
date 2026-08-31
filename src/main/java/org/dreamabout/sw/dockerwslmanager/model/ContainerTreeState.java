package org.dreamabout.sw.dockerwslmanager.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Persisted UI state for the Containers tree. */
public record ContainerTreeState(String selectedContainerId, String selectedGroup,
                                 Map<String, Boolean> expandedGroups) {

    public ContainerTreeState {
        Map<String, Boolean> sanitizedGroups = new TreeMap<>();
        if (expandedGroups != null) {
            expandedGroups.forEach((group, expanded) -> {
                if (group != null && !group.isBlank() && expanded != null) {
                    sanitizedGroups.put(group, expanded);
                }
            });
        }
        expandedGroups = Collections.unmodifiableMap(sanitizedGroups);
    }

    public static ContainerTreeState empty() {
        return new ContainerTreeState(null, null, Map.of());
    }

    /** Reconciles saved preferences with containers that currently exist. */
    public ContainerTreeState reconcile(Map<String, Set<String>> currentGroups) {
        Map<String, Boolean> reconciledGroups = new TreeMap<>();
        currentGroups.keySet().forEach(group ->
                reconciledGroups.put(group, expandedGroups.getOrDefault(group, true)));

        if (selectedContainerId != null) {
            String currentContainerGroup = currentGroups.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(selectedContainerId))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            if (currentContainerGroup != null) {
                return new ContainerTreeState(selectedContainerId, currentContainerGroup, reconciledGroups);
            }
        }

        String reconciledSelectionGroup = selectedGroup != null && currentGroups.containsKey(selectedGroup)
                ? selectedGroup : null;
        return new ContainerTreeState(null, reconciledSelectionGroup, reconciledGroups);
    }
}
