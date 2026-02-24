# docker-wsl-manager

Lightweight standalone JavaFX application for managing Docker running in WSL 2, connecting via TCP/IP.

![Application Screenshot](doc/img/screen1.png)
![Application Screenshot](doc/img/screen2.png)

## Features

- **Connection Methods**:
  - Auto-discovery of Docker in WSL using `wsl` command

- **Container Management**:
  - List all containers (running and stopped)
  - Start, stop, restart containers
  - Remove containers
  - **Open Details**: View container logs in real-time (with ANSI color support) and control the container from a dedicated tab
  - Attach to container console (basic support)
  - Clickable port links for running containers

- **Settings & Customization**:
  - **Configurable Shortcuts**: Customize keyboard shortcuts for all major actions via the Settings menu
  - **Auto-Refresh**: Toggle and configure the interval for automatic container list refreshing
  - **Auto-Update**: Automatically checks for new releases on startup

- **Image Management**:
  - List all Docker images
  - Pull new images from Docker Hub
  - Remove images

- **Volume Management**:
  - List all volumes
  - Remove volumes

- **Network Management**:
  - List all networks
  - Remove networks

## Requirements

- **Runtime**: Java 22 or higher
- **Build**: Maven 3.6 or higher
- **MSI Build**: JDK 25 and [WiX Toolset v7+](https://wixtoolset.org/)
- **Environment**: Docker running in WSL 2 (for Windows users)
- **Configuration**: Docker daemon exposed on TCP port (typically 2375)

## Building

### Standard Build
```bash
mvn clean package
```
Creates a shaded JAR in `target/`.

### Standalone Release (Optimized)
```bash
mvn clean package -P release
```
Creates `docker-wsl-manager-[version]-standalone.jar` with optimized manifest and merged service files.

### Windows MSI Installer
```bash
mvn clean package -P msi -DskipTests
```
Requires JDK 25 and WiX 7. One-time WiX 7 EULA acceptance is required: `wix eula accept wix7`.

## Running

### Using the Standalone JAR
```bash
java -jar target/docker-wsl-manager-1.2.0-standalone.jar
```

## Developer Guide

### Preparing a New Release
1. **Update Version**: Increment version in `pom.xml`.
2. **Run Release Script**:
   ```powershell
   .\build-release.ps1 -Version "1.2.0"
   ```
   This script automates cleaning, building the release profile, and packaging the artifacts into a `release/` folder and a ZIP archive.
3. **Generate MSI**: Run the MSI profile (requires JDK 25).
4. **Test**: Verify the standalone JAR and MSI installer on a clean environment.

## Project Structure

```
docker-wsl-manager/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/dreamabout/sw/dockerwslmanager/
│   │   │       ├── Main.java                     # Application entry point
│   │   │       ├── MainController.java           # Main UI controller
│   │   │       └── DockerConnectionManager.java  # Docker connection handler
│   │   └── resources/
│   │       ├── main.fxml                         # JavaFX layout
│   │       └── shortcuts.properties              # Keyboard shortcuts
│   └── test/
├── conductor/                                    # Project documentation & tracks (Conductor)
├── release/                                      # Created by build-release.ps1
├── pom.xml                                       # Maven configuration
└── README.md                                     # This file
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### AI Agent Support
This project is configured to work with Gemini AI coding agent.
