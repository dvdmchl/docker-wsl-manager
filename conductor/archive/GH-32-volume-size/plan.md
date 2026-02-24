# Implementation Plan - Track: GH-32-volume-size

## Phase 1: Backend Implementation
- [x] Task: Create `VolumeUsageService` to fetch volume sizes.
    - [x] Create `VolumeUsageService` class in `org.dreamabout.sw.dockerwslmanager.service`.
    - [x] Implement `fetchVolumeSizes()` method.
        - [x] Strategy: Check if `docker-java` supports `systemDfCmd()`. If not, use `wsl docker system df -v --format "{{json .}}"` via shell execution.
    - [x] Parse output to a `Map<String, Long>` (Volume Name -> Size in Bytes).
    - [x] Write unit tests for parsing logic (using sample JSON output).
- [x] Task: Conductor - User Manual Verification 'Backend Implementation' (Protocol in workflow.md)

## Phase 2: Data Model Updates
- [x] Task: Update `VolumeViewItem` to support size.
    - [x] Add `LongProperty sizeBytes` and `StringProperty sizeString`.
    - [x] Implement formatting logic (or refactor existing `formatSize` from `MainController` to a utility).
    - [x] Write unit tests for `VolumeViewItem` updates.
- [x] Task: Conductor - User Manual Verification 'Data Model Updates' (Protocol in workflow.md)

## Phase 3: UI Implementation
- [x] Task: Update `main.fxml` and `MainController`.
    - [x] Modify `main.fxml`: Add "Size" `TreeTableColumn` to `volumesTable`.
    - [x] Modify `MainController`:
        - [x] Inject/Instantiate `VolumeUsageService`.
        - [x] Configure `volumeSizeColumn` cell value factory and sorting.
        - [x] Add "Calculate Sizes" Button to Volumes tab toolbar.
    - [x] Implement "Calculate Sizes" action:
        - [x] Run `VolumeUsageService.fetchVolumeSizes()` in a background task.
        - [x] Update `VolumeViewItem` instances with the fetched sizes.
        - [x] Handle errors gracefully.
- [x] Task: Conductor - User Manual Verification 'UI Implementation' (Protocol in workflow.md)
