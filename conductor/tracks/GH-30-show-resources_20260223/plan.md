# Implementation Plan: Show Resources Consumption (GH-30)

## Phase 1: Core Statistics Fetching & Configuration [checkpoint: ebda668]
- [x] Task: Implement settings for resource update interval (default 5s) in `SettingsManager`. (9c0da9c)
- [x] Task: Create a service to fetch container stats asynchronously using `docker-java`. (458d974)
- [x] Task: Implement parsing logic for Docker stats (CPU, RAM, Net, Disk) into a dedicated model. (458d974)
- [x] Task: Conductor - User Manual Verification 'Phase 1: Core Statistics Fetching & Configuration' (Protocol in workflow.md)

## Phase 2: UI Integration in Details Tab
- [x] Task: Update `main.fxml` to include a layout for resource consumption in the container Details tab. (aa55d31)
- [x] Task: Implement `MainController` logic to periodically update the UI with new statistics when a container is selected. (aa55d31)
- [x] Task: Add a button/link in the Details tab to trigger the detailed process view. (aa55d31)
- [~] Task: Conductor - User Manual Verification 'Phase 2: UI Integration in Details Tab' (Protocol in workflow.md)

## Phase 3: Detailed Process View Window
- [~] Task: Create a new FXML and Controller for the "Process List" window.
- [ ] Task: Implement the backend logic to execute `docker top` and return the process data.
- [ ] Task: Wire the "Show Processes" button to open the new window and populate it with data.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Detailed Process View Window' (Protocol in workflow.md)

## Phase 4: Quality Assurance & Polish
- [ ] Task: Ensure resource fetching stops when the application is minimized or the container is no longer selected.
- [ ] Task: Verify error handling for cases where stats cannot be fetched (e.g., container stopped during fetching).
- [ ] Task: Final verification of all Quality Gates (Checkstyle, SpotBugs, Tests).
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Quality Assurance & Polish' (Protocol in workflow.md)
