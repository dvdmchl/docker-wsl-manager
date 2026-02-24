# Specification: Show space usage in volumes

## Overview
Add a new column "Size" to the Volumes view to display the disk usage of each volume. Since this operation can be resource-intensive, it will be triggered manually by the user rather than running automatically.

## Functional Requirements
1.  **UI - Volumes Table:**
    -   Add a new column titled "Size" to the `VolumeViewItem` table.
    -   Display the size in a human-readable format (e.g., "1.5 GB", "200 MB").
    -   Initially, the column should display "-" or be empty until data is loaded.

2.  **Interaction - Load Data:**
    -   Add a "Calculate Sizes" button to the Volumes view toolbar (or context menu).
    -   **Behavior:** When clicked, the application triggers the size calculation for volumes.
    -   **Loading State:** Show a progress indicator or disable the button while calculating.

3.  **Backend - Docker Integration:**
    -   Implement logic to fetch volume usage.
    -   *Strategy:* Use `docker system df -v` (via `wsl` prefix) to get usage stats for all volumes efficiently in one go, rather than querying each volume individually.
    -   Parse the output and update the `VolumeViewItem` models.

4.  **Sorting:**
    -   The "Size" column must be sortable.
    -   Sorting must be numerical based on bytes (e.g., 1 GB > 500 MB), not alphabetical.
    -   Unloaded values ("-") should be sorted as 0 or appear at the bottom.

## Non-Functional Requirements
-   **Performance:** The calculation must run on a background thread to prevent UI freezing.
-   **Responsiveness:** The UI should remain responsive during the calculation.
