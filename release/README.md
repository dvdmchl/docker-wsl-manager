# docker-wsl-manager

Lightweight standalone JavaFX application for managing Docker running in WSL 2, connecting via TCP/IP.

![Application Screenshot](doc/img/screen1.png)

## Features

- **Multiple Connection Methods**:
  - Connect using `DOCKER_HOST` environment variable
  - Manual connection with IP address and port
  - Auto-discovery of Docker in WSL using `wsl` command

- **Container Management**:
  - List all containers (running and stopped)
  - Start, stop, restart containers
  - Remove containers
  - View container logs
  - Attach to container console (basic support)

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

- Java 17 or higher
- Maven 3.6 or higher
- Docker running in WSL 2 (for Windows users)
- Docker daemon exposed on TCP port (typically 2375)

## Building

Build the project using Maven:

```bash
mvn clean package
```

This will create a shaded JAR file in the `target/` directory.

## Running

### Using Maven

```bash
mvn javafx:run
```

### Using the JAR file

```bash
java -jar target/docker-wsl-manager-1.0.0.jar
```

Note: If you have JavaFX modules installed separately, you may need to specify the module path.

## Setting up Docker in WSL

To expose Docker daemon on TCP port in WSL:

1. Edit Docker daemon configuration:
   ```bash
   sudo nano /etc/docker/daemon.json
   ```

2. Add the following content:
   ```json
   {
     "hosts": ["unix:///var/run/docker.sock", "tcp://0.0.0.0:2375"]
   }
   ```

3. Restart Docker:
   ```bash
   sudo service docker restart
   ```

4. Get your WSL IP address:
   ```bash
   hostname -I
   ```

5. Use this IP address to connect from the application.

**Security Note**: Exposing Docker daemon on TCP without TLS is insecure. Only use this in trusted networks or for local development.

## Usage

1. Launch the application
2. Connect to Docker using one of three methods:
   - **Auto-Discover WSL**: Automatically finds and connects to Docker in WSL
   - **Connect Manual**: Enter IP address (e.g., `172.x.x.x`) and port (e.g., `2375`)
   - **Connect from ENV**: Uses the `DOCKER_HOST` environment variable
3. Once connected, navigate through tabs to manage:
   - Containers
   - Images
   - Volumes
   - Networks
   - Logs

## Technologies Used

- **Java 17**: Programming language
- **JavaFX 21.0.1**: UI framework
- **docker-java 3.3.4**: Docker API client library
- **Maven**: Build and dependency management
- **SLF4J + Logback**: Logging

## Project Structure

```
docker-wsl-manager/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dvdmchl/dockerwslmanager/
│   │   │       ├── Main.java                     # Application entry point
│   │   │       ├── MainController.java           # Main UI controller
│   │   │       └── DockerConnectionManager.java  # Docker connection handler
│   │   └── resources/
│   │       └── main.fxml                         # JavaFX layout
│   └── test/
│       └── java/
├── pom.xml                                       # Maven configuration
└── README.md                                     # This file
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### AI Agent Support
This project is configured to work with Gemini AI coding agent.
