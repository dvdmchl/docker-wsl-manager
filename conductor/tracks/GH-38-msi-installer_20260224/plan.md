# Implementation Plan: MSI Installer for Docker-WSL-Manager

## Phase 1: Maven Installer Configuration
- [x] Task: Configure custom JRE generation
    - [x] Update `pom.xml` to include a step (e.g., using `maven-dependency-plugin` or `jlink` directly) to bundle the required Java runtime.
- [x] Task: Integrate `jpackage` for MSI creation
    - [x] Add plugin configuration in `pom.xml` to execute `jpackage` during the package/install phase.
    - [x] Configure `jpackage` parameters to output an `.msi` format installer.
    - [x] Add the `--win-shortcut` parameter to ensure a Desktop shortcut is created.
    - [x] Add the `--win-menu` parameter to ensure a Start Menu shortcut is created.
    - [x] Ensure the installer uses the correct application icon.
    - [x] **Verified**: Building with JDK 25 and WiX 7 works after accepting WiX 7 EULA (`wix eula accept wix7`).
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)