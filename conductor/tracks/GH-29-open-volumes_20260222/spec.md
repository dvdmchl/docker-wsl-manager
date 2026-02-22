# Specification: Allow to open volume and browse files

## Overview
Add functionality to open Docker volumes and bind mounts directly in Windows Explorer. This feature bridges the Windows desktop environment with the WSL 2 backend where Docker data resides, facilitating easy file browsing and management.

## Functional Requirements
1.  **UI - Volumes Tab:**
    *   Add an "Open Volume" button to the Volumes tab toolbar.
    *   The button should be enabled only when a single volume is selected.
    *   Clicking the button opens the `_data` folder of the selected volume in Windows Explorer.
2.  **UI - Container Details Tab:**
    *   Add an "Open Volumes" button to the Container Details view.
    *   Clicking the button opens a separate Windows Explorer window for every volume and bind mount configured for the selected container.
3.  **Path Resolution Logic:**
    *   **Named Volumes:** Construct the Windows path using the pattern: `\wsl.localhost\<distro>\var\lib\docker\volumes\<volume_name>\_data`.
    *   **Bind Mounts:** 
        *   If the source path is a Windows path (e.g., `C:\data`), open it directly.
        *   If it's a Linux path, attempt to resolve it via the WSL network path (e.g., `\wsl.localhost\<distro>\<linux_path>`).
4.  **Configuration:**
    *   Add a `wsl.distro` setting to `settings.properties` to allow users to specify the distribution name (defaulting to `docker-desktop-data` or the current default WSL distro).
    *   Provide a way to edit this setting in the "General Settings" dialog.
5.  **Error Handling:**
    *   Check for path existence before opening.
    *   Display a clear error message if the WSL distribution is not found or the volume path is inaccessible.
    *   Specifically handle permission errors for `/var/lib/docker` by providing the `chmod` commands needed to fix access in WSL.

## Non-Functional Requirements
*   **Responsiveness:** Path resolution and Explorer spawning should happen on a background thread to prevent UI freezing.
*   **Security:** Ensure that only valid file paths are passed to the system shell.

## Out of Scope
*   An internal file browser within the application.
*   Support for remote Docker instances not running in WSL 2.

## Acceptance Criteria
1.  Users can open a named volume's storage directory from the Volumes tab.
2.  Users can open all mounts of a container from its Details tab.
3.  The application correctly handles both named volumes and host bind mounts.
4.  The WSL distribution name used for path resolution is configurable.
