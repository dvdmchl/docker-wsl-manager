# Gemini CLI Project Guide

This document contains useful information, instructions, and context for the Gemini CLI agent to effectively work on the **Docker-WSL-Manager** project.

## Project Overview

**Docker-WSL-Manager** is a JavaFX-based GUI application designed to manage Docker instances, specifically optimized for Docker environments running within WSL 2 (Windows Subsystem for Linux). It provides a user-friendly interface to view and manage containers, images, volumes, networks, and logs.

## CRITICAL RULES

-   **NO GIT COMMITS**: **NEVER** use `git commit`. The user will always commit changes manually. You may stage changes (`git add`) if necessary to verify them, but never commit.
-   **NO GIT BRANCH/CHECKOUT**: **NEVER** create branches or checkout other branches unless explicitly instructed.
-   **QUALITY CHECKS**: Before signaling task completion, **ALWAYS** run `mvn clean verify` to ensure Checkstyle and SpotBugs pass.
-   **SONARQUBE**: **ALWAYS** run SonarQube analysis with the authentication token:
    ```bash
    mvn verify -Psonar sonar:sonar -Dsonar.token=$env:SONAR_TOKEN
    ```
    The build **will fail** if the Quality Gate is not passed. Any duplication, code smell, or bug must be fixed immediately.

### Tech Stack

-   **Language**: Java 22
-   **GUI Framework**: JavaFX 25.0.1 (using FXML for layout)
-   **Build System**: Maven
-   **Docker Integration**: `docker-java` library (v3.7.0) with Apache HttpClient 5 transport
-   **Logging**: SLF4J with Logback

## Repository
Hosted on GitHub.

## Project Structure

-   `src/main/java`: Source code.
    -   `org.dreamabout.sw.dockerwslmanager.Main`: Application entry point (JavaFX Application).
    -   `org.dreamabout.sw.dockerwslmanager.MainController`: Handles UI interactions and updates.
    -   `org.dreamabout.sw.dockerwslmanager.DockerConnectionManager`: Manages Docker client connections and WSL discovery.
-   `src/main/resources`: Assets and Configuration.
    -   `main.fxml`: Defines the UI layout (TabPane structure).
    -   `logback.xml`: Logging configuration.
-   `build-release.ps1`: PowerShell script to package the application for release.
-   `run.bat`: Batch script to run the application on Windows.
-   `pom.xml`: Maven configuration file.

## Development Instructions

### Prerequisites

-   JDK 22
-   Maven
-   Docker (running in WSL 2)

### Building the Application

To build the project and create the shaded (fat) JAR:

```bash
mvn clean package
```

### Running the Application

**Via Maven:**
```bash
mvn javafx:run
```

**Via Script (Windows):**
```cmd
run.bat
```

**Creating a Release:**
```powershell
./build-release.ps1
```

## Key Workflows & Commands

### Tool Execution Rules

- **Docker**: All `docker` or `docker compose` commands **MUST** be prefixed with `wsl` (e.g., `wsl docker ps`, `wsl docker info`). This is critical because the agent runs on Windows but interacts with the WSL Docker daemon.
- **curl**: `curl` commands should be executed **natively** (without `wsl`).

### UI Development (JavaFX)

-   **FXML**: UI layout is defined in `src/main/resources/main.fxml`. Use SceneBuilder principles when editing.
-   **Threading**: **DO NOT** block the JavaFX Application Thread. Use `javafx.concurrent.Task` or `Service` for long-running Docker operations to keep the UI responsive.

## Best Practices for Agent

-   **Code Quality**: Maintain clean, readable code. Follow standard Java naming conventions.
-   **Dependencies**: Do not add new libraries without explicit user permission.
-   **Git Operations**: Never use `git add`, `git stash`, or `git checkout`.
-   **Remote Repository**: Do not use tools that modify the remote GitHub repository.

### Testing Policy

-   **New Features**: When implementing logic that is decoupled from the UI, write **JUnit** tests in `src/test/java`.
-   **Do not write disabled tests**: Every test method must contain meaningful assertions or be removed.

### Architecture & Code Quality

-   **Static Analysis (Checkstyle & SpotBugs)**:
    -   **Checkstyle**: Enforces coding style and specific best practices.
    -   **SpotBugs**: Detects potential bugs in bytecode.
    -   **Policy**: These tools are configured to **fail the build** on any violation. Do not fight the tools; fix the code to comply with the rules.
    -   **Execution**:
        -   Checkstyle runs during `mvn validate`.
        -   SpotBugs runs during `mvn process-classes` (and `verify`).

-   **Error Handling**: Gracefully handle Docker connection failures (e.g., if WSL is stopped). Display user-friendly error messages in the UI rather than just logging stack traces.
-   **Resources**: Ensure Docker clients and other resources are closed properly (try-with-resources) where applicable.