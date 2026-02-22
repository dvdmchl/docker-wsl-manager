# Implementation Plan - Track: GH-39-volumes-containers

## Phase 1: Logic Implementation
- [x] Task: Update `VolumeViewItem` to store container names.
    - [x] Add `ObservableList<String> containerNames` or a similar property to the model.
- [x] Task: Implement container-to-volume mapping logic in `VolumeLogic`.
    - [x] Add a method to build a map: `Map<String, List<String>>` (Volume Name -> List of Container Names).
    - [x] Iterate through all containers and their mounts to populate this map.
- [x] Task: Write unit tests for the mapping logic.
- [x] Task: Conductor - User Manual Verification 'Logic Implementation' (Protocol in workflow.md)

## Phase 2: Controller & UI Integration
- [x] Task: Modify `MainController.refreshVolumes()` to fetch containers and apply the mapping.
    - [x] Fetch the list of all containers (`listContainersCmd().withShowAll(true)`).
    - [x] Update `VolumeViewItem` instances with the names of containers using them.
- [x] Task: Update `main.fxml` to add the "Containers" column.
- [x] Task: Implement custom cell factory for the "Containers" column.
    - [x] Use `FlowPane` or `HBox` to display comma-separated `Hyperlink` objects for each container name.
    - [x] Implement the navigation logic: switch to "Containers" tab and select the matching item.
- [x] Task: Conductor - User Manual Verification 'Controller & UI Integration' (Protocol in workflow.md)

## Phase 3: Verification and Polishing
- [x] Task: Ensure smooth navigation and UI responsiveness.
- [x] Task: Verify that the column sorts correctly (alphabetically by the first container name or count).
- [x] Task: Conductor - User Manual Verification 'Verification and Polishing' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions 67007db
