# Implementation Plan - Track: GH-32-volume-size

## Phase 1: Backend Implementation
- [ ] Task: Create `VolumeUsageService` to fetch volume sizes.
    - [ ] Create `VolumeUsageService` class in `org.dreamabout.sw.dockerwslmanager.service`.
    - [ ] Implement `fetchVolumeSizes()` method.
        - [ ] Strategy: Check if `docker-java` supports `systemDfCmd()`. If not, use `wsl docker system df -v --format "{{json .}}"` via shell execution.
    - [ ] Parse output to a `Map<String, Long>` (Volume Name -> Size in Bytes).
    - [ ] Write unit tests for parsing logic (using sample JSON output).
- [ ] Task: Conductor - User Manual Verification 'Backend Implementation' (Protocol in workflow.md)

## Phase 2: Data Model Updates
- [ ] Task: Update `VolumeViewItem` to support size.
    - [ ] Add `LongProperty sizeBytes` and `StringProperty sizeString`.
    - [ ] Implement formatting logic (or refactor existing `formatSize` from `MainController` to a utility).
    - [ ] Write unit tests for `VolumeViewItem` updates.
- [ ] Task: Conductor - User Manual Verification 'Data Model Updates' (Protocol in workflow.md)

## Phase 3: UI Implementation
- [ ] Task: Update `main.fxml` and `MainController`.
    - [ ] Modify `main.fxml`: Add "Size" `TreeTableColumn` to `volumesTable`.
    - [ ] Modify `MainController`:
        - [ ] Inject/Instantiate `VolumeUsageService`.
        - [ ] Configure `volumeSizeColumn` cell value factory and sorting.
        - [ ] Add "Calculate Sizes" Button to Volumes tab toolbar.
    - [ ] Implement "Calculate Sizes" action:
        - [ ] Run `VolumeUsageService.fetchVolumeSizes()` in a background task.
        - [ ] Update `VolumeViewItem` instances with the fetched sizes.
        - [ ] Handle errors gracefully.
- [ ] Task: Conductor - User Manual Verification 'UI Implementation' (Protocol in workflow.md)
