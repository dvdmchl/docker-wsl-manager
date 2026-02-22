# Specification: Show containers in volumes tab

## Overview
Add a new column "Containers" to the Volumes view. This column will display a comma-separated list of all containers (running or stopped) that are using or have configured each specific volume. Clicking a container name will navigate the user to the "Containers" tab and select the corresponding container.

## Functional Requirements
1.  **UI - Volumes Table:**
    -   Add a new column titled "Containers" to the `volumesTable`.
    -   Display container names as a comma-separated list.
    -   Container names should be interactive (e.g., using `Hyperlink` or a custom cell factory).
2.  **Navigation Logic:**
    -   When a container name is clicked:
        -   Switch the main `TabPane` selection to the "Containers" tab.
        -   Find and select the corresponding container in the `containersTable`.
3.  **Data Integration:**
    -   During the volume refresh process, fetch all containers (including stopped ones).
    -   Map containers to volumes based on their mount configurations.
    -   This mapping should happen automatically whenever the Volumes tab is refreshed.
4.  **Scope of Inclusion:**
    -   Include all containers that have the volume listed in their "Mounts" property, regardless of current execution state.

## Non-Functional Requirements
-   **Performance:** The mapping logic must be efficient. It should fetch the container list once per refresh and build a lookup map to avoid multiple Docker API calls per volume row.
-   **Thread Safety:** Background data fetching must not block the UI thread.

## Out of Scope
-   Calculating container disk usage (only names are displayed).
-   Directly starting/stopping containers from the Volumes tab.

## Acceptance Criteria
1.  A "Containers" column appears in the Volumes tab.
2.  It accurately lists the names of containers using each volume.
3.  Clicking a container name successfully navigates to the Containers tab and highlights that container.
4.  The list updates correctly when the "Refresh" button is clicked.
