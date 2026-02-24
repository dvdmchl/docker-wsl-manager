# Specification: MSI Installer for Docker-WSL-Manager

## Overview
This track implements the generation of an MSI installer for Docker-WSL-Manager as requested in issue #38. The goal is to provide a standard, easily distributable, and installable package for the x64 Windows platform.

## Functional Requirements
- Produce a `.msi` Windows Installer for the application.
- The installer must be self-contained (bundled with the required Java Runtime Environment).
- The installer must create a Start Menu shortcut for the application.
- The installer must create a Desktop shortcut for the application.
- The installer generation process must be integrated directly into the `pom.xml` using a Maven plugin (e.g., `javafx-maven-plugin` or `exec-maven-plugin` using `jpackage`).

## Non-Functional Requirements
- The installer build process must be reproducible and not require manual steps outside of executing the Maven build command.
- The final artifact must be reasonably sized (e.g., by creating a custom minimal JRE via `jlink` if possible).

## Acceptance Criteria
- Executing the Maven build command successfully generates an `.msi` file in the target directory.
- The generated `.msi` file successfully installs the application on a Windows x64 machine.
- The installation creates functional Start Menu and Desktop shortcuts.
- The installed application launches successfully without requiring a pre-installed system JRE.

## Out of Scope
- Cross-platform installers (e.g., macOS DMG, Linux deb/rpm).
- `.exe` installer (focus is explicitly on MSI).
- Code signing (unless already integrated, not a primary goal of simply generating the package).
- Built-in auto-update mechanisms.