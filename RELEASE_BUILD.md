# Docker WSL Manager - Release Build Guide

## Building Standalone JAR

### Requirements
- Java Development Kit (JDK) 22 or higher
- Apache Maven 3.6 or higher

### Build Instructions

1. **Standard build (development):**
   ```bash
   mvn clean package
   ```

2. **Release build (optimized for distribution):**
   ```bash
   mvn clean package -P release
   ```
   
   The release profile includes:
   - Additional metadata in MANIFEST
   - Exclusion of Maven metadata files
   - Build information (JDK version, Maven version, build date)
   - Optimized for distribution

3. **Output files:**
   The build process creates two JAR files in the `target/` directory:
   
   - `docker-wsl-manager-1.0.0.jar` - Regular JAR (small, requires dependencies)
   - `docker-wsl-manager-1.0.0-standalone.jar` - **Standalone JAR (use this for distribution)**

### Distribution Package

The standalone JAR contains:
- All application code
- All dependencies (JavaFX, Docker Java client, logging libraries, etc.)
- Proper MANIFEST with main class configuration
- Service provider configurations
- Multi-release JAR support

### Running the Application

**Windows:**
```bash
java -jar docker-wsl-manager-1.0.0-standalone.jar
```

Or use the provided batch file:
```bash
run.bat
```

**Linux/Mac:**
```bash
java -jar docker-wsl-manager-1.0.0-standalone.jar
```

### Creating a Release

1. Build the standalone JAR:
   ```bash
   mvn clean package
   ```

2. Copy the standalone JAR from `target/`:
   ```bash
   cp target/docker-wsl-manager-1.0.0-standalone.jar release/
   ```

3. Include in the release package:
   - `docker-wsl-manager-1.0.0-standalone.jar`
   - `run.bat` (for Windows users)
   - `README.md` (user documentation)
   - `LICENSE` (if applicable)

### System Requirements

**Runtime Requirements:**
- Java Runtime Environment (JRE) 22 or higher
- Windows 10/11 with WSL 2 (for Docker WSL integration)
- Docker Desktop or Docker in WSL 2

**Recommended:**
- At least 512 MB RAM for the application
- Docker daemon running and accessible

### Troubleshooting

**"Could not find or load main class"**
- Ensure you're using Java 22 or higher: `java -version`
- Make sure you're running the `-standalone.jar` file

**"JavaFX runtime components are missing"**
- This should not happen with the standalone JAR as JavaFX is bundled
- If it occurs, ensure you're using the correct JAR file

**Docker connection issues**
- Verify Docker daemon is running
- Check Docker host configuration in the application
- Ensure proper permissions for Docker socket access

### Version Information

- **Current Version:** 1.0.0
- **Java Version:** 22
- **JavaFX Version:** 25.0.1
- **Docker Java Client:** 3.7.0

### Build Configuration

The standalone JAR is created using Maven Shade Plugin with:
- All dependencies bundled (uber JAR)
- Service provider configurations merged
- Signature files excluded to prevent security exceptions
- Multi-release JAR support enabled
- Manifest with proper main class and metadata

For more information, see `pom.xml` configuration.

