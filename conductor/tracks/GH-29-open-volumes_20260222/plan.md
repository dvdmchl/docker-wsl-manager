# Implementation Plan - Track: GH-29-open-volumes

## Phase 1: Foundation & Path Logic
- [x] Task: Update `SettingsManager` to support the `wsl.distro` configuration.
    - [x] Add `getWslDistro()` and `setWslDistro(String)` methods to `SettingsManager`.
    - [x] Update the General Settings dialog in `MainController` to include an input for the WSL distribution name.
- [x] Task: Implement `VolumePathResolver` in `org.dreamabout.sw.dockerwslmanager.logic`.
    - [x] Add logic to construct Windows network paths for named volumes: `\\wsl.localhost\<distro>\var\lib\docker\volumes\<name>\_data`.
    - [x] Add logic to resolve bind mount paths, handling both Windows host paths and Linux paths.
- [x] Task: Write unit tests for `VolumePathResolver`.
- [x] Task: Conductor - User Manual Verification 'Foundation & Path Logic' (Protocol in workflow.md)

## Phase 2: Volumes Tab Integration
- [ ] Task: Update `main.fxml` to add the "Open Volume" button to the Volumes tab toolbar.
- [ ] Task: Implement `handleOpenVolumeAction` in `MainController`.
    - [ ] Logic to resolve the path for the selected volume and launch Windows Explorer.
    - [ ] Use `CompletableFuture` to ensure the UI remains responsive during path resolution.
- [ ] Task: Update `shortcuts.properties` and `ShortcutManager` to support the new action.
- [ ] Task: Conductor - User Manual Verification 'Volumes Tab Integration' (Protocol in workflow.md)

## Phase 3: Container Details Integration
- [ ] Task: Update `createDetailsTab` in `MainController` to include the "Open Volumes" button in the header or toolbar area.
- [ ] Task: Implement logic to extract all mounts from the `Container` object and open them in Explorer.
- [ ] Task: Conductor - User Manual Verification 'Container Details Integration' (Protocol in workflow.md)

## Phase 4: Auto-detection & Polishing
- [ ] Task: Enhance `DockerConnectionManager` or `VolumePathResolver` to attempt auto-detection of the active WSL distro if not explicitly configured.
- [ ] Task: Add error handling for cases where the network path is unreachable (e.g., WSL stopped).
- [ ] Task: Conductor - User Manual Verification 'Auto-detection & Polishing' (Protocol in workflow.md)
