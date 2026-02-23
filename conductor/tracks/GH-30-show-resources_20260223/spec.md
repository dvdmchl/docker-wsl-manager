# Specification: Show Resources Consumption (GH-30)

## Overview
This feature introduces real-time monitoring of hardware resource consumption (CPU, RAM, Network, and Disk I/O) for Docker containers. These metrics will be displayed in the container's Details tab, with an option to view a detailed process list in a separate window.

## Functional Requirements
1. **Resource Monitoring:**
   - Periodically fetch resource statistics for the selected container from the Docker daemon.
   - Collected metrics:
     - CPU Usage (%)
     - RAM Usage (Current vs Limit)
     - Network I/O (Read/Write)
     - Disk I/O (Read/Write)
2. **Details Tab Integration:**
   - Add a "Resource Consumption" section to the container Details tab.
   - Display the collected metrics in a clear, human-readable format.
3. **Configurable Update Rate:**
   - The refresh interval for statistics must be configurable in settings.
   - Default interval: 5 seconds.
4. **Detailed Process View:**
   - Provide a way to open a new window from the Details tab.
   - This window must display the output of `docker top` (process list) for the selected container.
   - The process list should be refreshable.

## Non-Functional Requirements
- **Performance:** Resource fetching must be asynchronous to prevent UI freezes.
- **Resource Efficiency:** Statistics should only be fetched for the currently selected/active container to minimize overhead.

## Acceptance Criteria
- [ ] Selecting a container displays its CPU, RAM, Network, and Disk usage in the Details tab.
- [ ] The metrics refresh automatically based on the configured interval (default 5s).
- [ ] Clicking a "Show Processes" (or similar) button opens a new window.
- [ ] The new window correctly displays the output of `docker top`.

## Out of Scope
- Historical resource usage charts (only real-time/latest data).
- Resource monitoring for images, volumes, or networks.
- Managing/killing processes from the detailed view.
