# Docker WSL Manager

Lightweight JavaFX desktop application for managing Docker Engine running in
WSL 2. The application discovers the WSL address and connects to the Docker
daemon over TCP/IP.

![Containers grouped by Docker Compose project](https://raw.githubusercontent.com/dvdmchl/docker-wsl-manager/main/doc/img/screen1.png)
![Container details with logs and resource statistics](https://raw.githubusercontent.com/dvdmchl/docker-wsl-manager/main/doc/img/screen2.png)

## Features

- **Docker connection**
  - Automatically discovers the WSL address using the `wsl` command
  - Connects to the Docker daemon on TCP port 2375
  - Reconnects without blocking the JavaFX interface when Docker or WSL is temporarily unavailable

- **Container management**
  - Lists running and stopped containers, grouped by Docker Compose project
  - Starts, stops, restarts, and removes individual containers or complete groups
  - Preserves the selected container and expanded groups during refreshes and across application restarts
  - Opens clickable port mappings in the default browser
  - Shows live logs with ANSI color support and provides basic console attachment
  - Shows live CPU, memory, network, and disk I/O statistics
  - Displays the container configuration, mounted volumes, and running processes

- **Image management**
  - Lists and groups Docker images
  - Pulls images from Docker Hub
  - Removes images

- **Volume management**
  - Lists volumes grouped by Docker Compose project, including the containers that use them
  - Calculates volume sizes through WSL
  - Opens volume paths in Windows Explorer
  - Removes individual volumes and prunes unused volumes

- **Network management**
  - Lists Docker networks
  - Removes networks

- **Settings and updates**
  - Configures keyboard shortcuts for major actions
  - Enables automatic container refresh and configures container and statistics refresh intervals
  - Checks GitHub Releases for a newer application version at startup and on demand

## Requirements

- **Operating system**: Windows with WSL 2
- **Docker**: Docker Engine running inside WSL 2
- **Docker configuration**: Docker daemon exposed on TCP port 2375
- **Standalone JAR runtime**: Java 21 or newer
- **Build**: JDK 21 or newer and Maven 3.6.3 or newer
- **MSI build**: JDK 25 and [WiX Toolset v7+](https://wixtoolset.org/)

The Windows MSI includes a Java runtime, so Java does not need to be installed
separately when using the installer.

## Building

### Standard build

```bash
mvn clean package
```

Creates the application JAR and
`target/docker-wsl-manager-<version>-standalone.jar`.

### Standalone release build

```bash
mvn clean package -P release
```

Creates `target/docker-wsl-manager-<version>-standalone.jar` with the release
manifest and merged service files.

### Windows MSI installer

MSI creation requires JDK 25 and WiX 7. Accept the WiX 7 EULA once with
`wix eula accept wix7`, then run the build from PowerShell:

```powershell
$env:JAVA_HOME = "C:\dev\Java\jdk-25.0.1-full"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH;C:\Program Files\WiX Toolset v7.0\bin"
mvn package -P msi -DskipTests
```

The build verifies the generated application image and an administrative MSI
extraction. Before publishing, install the MSI on a clean Windows machine or
VM, launch the Start Menu shortcut, and confirm that
`C:\Program Files\DockerWSLManager\runtime\bin\java.exe -version` succeeds.

## Running

Run the standalone JAR using Java 21 or newer:

```bash
java -jar target/docker-wsl-manager-<version>-standalone.jar
```

Release ZIP archives include the standalone application as
`docker-wsl-manager.jar` and a Windows `run.bat` launcher.

## Developer guide

### Preparing a new release

The complete checklist is in
[RELEASE_BUILD.md](https://github.com/dvdmchl/docker-wsl-manager/blob/main/RELEASE_BUILD.md).
In brief:

1. Update the version in `pom.xml`, add version-specific release notes, and update `CHANGELOG.md`.
2. Build the verified ZIP and optional MSI into a version-specific directory:

   ```powershell
   .\build-release.ps1 `
     -OutputDir ".\release-output\v<version>" `
     -ReleaseNotesPath ".\release-notes\<version>.md" `
     -BuildMsi
   ```

3. Generate and verify `SHA256SUMS.txt` for the ZIP and MSI.
4. Verify the artifacts, create and push the annotated `v<version>` tag, then publish a GitHub Release containing the ZIP, MSI, and checksums.

## Project structure

```text
docker-wsl-manager/
├── src/
│   ├── main/
│   │   ├── java/org/dreamabout/sw/dockerwslmanager/
│   │   │   ├── logic/                    # Configuration, formatting, and volume logic
│   │   │   ├── model/                    # JavaFX view models and persisted UI state
│   │   │   ├── service/                  # Container statistics and volume usage services
│   │   │   ├── Main.java                 # Application entry point
│   │   │   ├── MainController.java       # Main UI controller
│   │   │   └── DockerConnectionManager.java
│   │   └── resources/                    # FXML, settings, shortcuts, and application icons
│   └── test/                             # Unit tests
├── doc/img/                              # README screenshots
├── release-notes/                        # Version-specific release notes
├── scripts/                              # Packaging verification scripts
├── build-release.ps1                     # Reproducible release build
├── CHANGELOG.md
├── RELEASE_BUILD.md
├── pom.xml
└── README.md
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for
details.

## Contributing

Contributions are welcome. Please feel free to submit a pull request.

### AI agent support

Repository-specific instructions for Codex are available in
[AGENTS.md](https://github.com/dvdmchl/docker-wsl-manager/blob/main/AGENTS.md).
