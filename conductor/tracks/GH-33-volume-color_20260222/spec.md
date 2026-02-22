# Specification: Highlight volumes of running containers

## Overview
Improve the visual identification of active data by displaying volumes used by running containers in green text color within the Volumes tab.

## Functional Requirements
1.  **Status Identification:**
    *   Identify all volumes currently attached to containers with a `running` state.
    *   This includes volumes mounted via `docker-compose` or standard `docker run`.
2.  **UI - Volumes Table:**
    *   Apply a green text color (`-fx-text-fill: green;`) to rows in the `volumesTable` where the volume is in use by at least one running container.
    *   **Color Precedence:** The green color for "in use by running container" takes precedence over the grey color used for "unused/dangling" volumes.
3.  **Dynamic Updates:**
    *   The volume colors must be updated whenever the Volumes list is refreshed.
    *   Since container states can change, the mapping between volumes and running containers should be re-evaluated during each refresh cycle.

## Non-Functional Requirements
*   **Performance:** The lookup for running containers should be optimized (e.g., using a Set of volume names) to avoid performance degradation during tab refreshes.
*   **Consistency:** The green color should be consistent with other status indicators in the application if any (e.g., container status colors).

## Out of Scope
*   Adding tooltips or status icons to the volume rows.
*   Direct navigation from the volume row to the specific running container (handled in GH-39).

## Acceptance Criteria
1.  Volumes used by running containers appear in green text.
2.  Volumes not used by running containers appear in standard color (or grey if unused).
3.  Starting or stopping a container and then refreshing the volumes tab correctly updates the colors.
