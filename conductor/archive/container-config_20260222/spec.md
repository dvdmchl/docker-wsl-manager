# Specification: Show container configuration in details tab

## Overview
Add functionality to view the full configuration details of a selected container directly from its Details tab. This will expose the comprehensive `docker inspect` data in a readable, searchable format, aiding in debugging and environment verification.

## Functional Requirements
1.  **UI - Details Tab Button:**
    *   Add a new button labeled "⚙ _Config" to the footer of every Container Details tab.
    *   The button should have a keyboard shortcut (e.g., `Ctrl+Alt+C`).
2.  **Configuration View:**
    *   Clicking the "Config" button opens a separate view (either a new tab in the `mainTabPane` or a distinct panel within the Details tab).
    *   The view displays the full output of the `docker inspect <container_id>` command.
    *   **Format:** The configuration data must be rendered as **Pretty JSON** with proper indentation.
3.  **Interactions:**
    *   **Search/Filter:** Include a search bar at the top of the configuration view to allow users to filter lines or search for specific keys/values.
    *   **Copy to Clipboard:** Provide a "📋 Copy Config" button to copy the entire JSON to the system clipboard.
4.  **Data Integration:**
    *   Fetch the inspection data via `inspectContainerCmd(id)` using the existing `docker-java` client.
    *   The configuration should be fetched on demand (when the button is clicked) to avoid bloating the initial tab load.

## Non-Functional Requirements
*   **Responsiveness:** Large JSON outputs should be handled efficiently using a `TextArea` or a virtualized list to prevent UI lag.
*   **Consistency:** The styling of the configuration view (fonts, colors) should match the existing log viewer.

## Out of Scope
*   Editing the configuration (view-only).
*   Comparing configurations of two different containers.

## Acceptance Criteria
1.  A "Config" button is visible in the Container Details tab.
2.  Clicking the button displays the full, pretty-printed JSON metadata of the container.
3.  The text in the configuration view is searchable.
4.  The user can copy the configuration to the clipboard.
