# Tech Stack: Docker-WSL-Manager

## Core
- **Language:** Java 22
- **Framework:** JavaFX 25.0.1 (using FXML for layout)
- **Docker Client:** docker-java 3.7.0 (with Apache HttpClient 5 transport)
- **JSON Processing:** Jackson Databind 2.18.2

## Build & Dependencies
- **Build System:** Maven
- **Dependency Management:** Maven (pom.xml)

## Testing
- **Unit Testing:** JUnit 5 (Jupiter)
- **Mocking:** Mockito

## Tooling
- **Static Analysis:** Checkstyle, SpotBugs, SonarQube
- **Coverage:** JaCoCo
- **Logging:** SLF4J + Logback

## Platform
- **OS:** Windows (running WSL 2)
- **Runtime:** Java 22+ JRE
- **Critical Constraints:**
  - **Docker:** All `docker` commands **MUST** be prefixed with `wsl` (e.g., `wsl docker ps`).
  - **curl:** `curl` commands should be executed **natively** (without `wsl`).
