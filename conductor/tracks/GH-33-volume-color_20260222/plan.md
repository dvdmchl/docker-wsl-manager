# Implementation Plan - Track: GH-33-volume-color

## Phase 1: Logic & Model Updates
- [x] Task: Update `VolumeViewItem` model.
    - [x] Add `BooleanProperty inUseByRunningContainer`.
- [x] Task: Enhance `VolumeLogic` to identify volumes of running containers.
    - [x] Add a method to extract volume names from a list of containers filtered by state `running`.
- [x] Task: Write unit tests for the new identification logic in `VolumeLogicTest`.
- [x] Task: Conductor - User Manual Verification 'Logic & Model Updates' (Protocol in workflow.md)

## Phase 2: UI Integration
- [x] Task: Update `MainController.refreshVolumes()` to populate the "in use" status.
    - [x] Fetch the latest container states.
    - [x] Apply the `inUseByRunningContainer` property to each `VolumeViewItem`.
- [x] Task: Update `volumesTable` row factory.
    - [x] Modify the `updateItem` method in the row factory to apply `-fx-text-fill: green;` when the volume is in use by a running container.
    - [x] Ensure precedence logic: check `inUseByRunningContainer` before `unused`.
- [x] Task: Conductor - User Manual Verification 'UI Integration' (Protocol in workflow.md)

## Phase 3: Final Verification
- [x] Task: Verify the dynamic update behavior.
    - [x] Start a container and refresh volumes -> text turns green.
    - [x] Stop the container and refresh volumes -> text returns to normal/grey.
- [x] Task: Conductor - User Manual Verification 'Final Verification' (Protocol in workflow.md)
