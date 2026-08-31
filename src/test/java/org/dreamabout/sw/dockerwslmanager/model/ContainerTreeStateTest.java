package org.dreamabout.sw.dockerwslmanager.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContainerTreeStateTest {

    @Test
    void emptyStateExpandsCurrentGroupsWithoutSelectingAnything() {
        ContainerTreeState reconciled = ContainerTreeState.empty().reconcile(
                Map.of("project-a", Set.of("container-1")));

        assertNull(reconciled.selectedContainerId());
        assertNull(reconciled.selectedGroup());
        assertEquals(Map.of("project-a", true), reconciled.expandedGroups());
    }

    @Test
    void retainsExistingSelectionAndExpansionState() {
        ContainerTreeState saved = new ContainerTreeState(
                "container-1", "project-a", Map.of("project-a", false));

        ContainerTreeState reconciled = saved.reconcile(
                Map.of("project-a", Set.of("container-1", "container-2")));

        assertEquals("container-1", reconciled.selectedContainerId());
        assertEquals("project-a", reconciled.selectedGroup());
        assertEquals(Map.of("project-a", false), reconciled.expandedGroups());
    }

    @Test
    void fallsBackToFormerGroupWhenSelectedContainerWasRemoved() {
        ContainerTreeState saved = new ContainerTreeState(
                "container-1", "project-a", Map.of("project-a", false));

        ContainerTreeState reconciled = saved.reconcile(
                Map.of("project-a", Set.of("container-2")));

        assertNull(reconciled.selectedContainerId());
        assertEquals("project-a", reconciled.selectedGroup());
        assertEquals(Map.of("project-a", false), reconciled.expandedGroups());
    }

    @Test
    void dropsSelectionAndStaleExpansionWhenFormerGroupWasRemoved() {
        ContainerTreeState saved = new ContainerTreeState(
                "container-1", "project-a", Map.of("project-a", false));

        ContainerTreeState reconciled = saved.reconcile(
                Map.of("project-b", Set.of("container-2")));

        assertNull(reconciled.selectedContainerId());
        assertNull(reconciled.selectedGroup());
        assertEquals(Map.of("project-b", true), reconciled.expandedGroups());
    }

    @Test
    void newGroupsExpandWithoutChangingExistingGroupState() {
        ContainerTreeState saved = new ContainerTreeState(
                null, "project-a", Map.of("project-a", false, "removed", false));

        ContainerTreeState reconciled = saved.reconcile(Map.of(
                "project-a", Set.of("container-1", "container-2"),
                "project-b", Set.of("container-3")));

        assertEquals(Map.of("project-a", false, "project-b", true), reconciled.expandedGroups());
        assertEquals("project-a", reconciled.selectedGroup());
    }
}
