# Product Guide: Docker-WSL-Manager

## Initial Concept
Lightweight standalone JavaFX application for managing Docker running in WSL 2, connecting via TCP/IP.

## Vision
To provide a seamless, native-feeling desktop experience for developers using Docker within WSL 2, eliminating the need for complex command-line interactions for routine management tasks. It bridges the gap between the Windows desktop environment and the Linux-based Docker daemon running in WSL.

## Core Features
- **Container Management:**
  - View running and stopped containers.
  - Start, stop, restart, and remove containers.
  - Inspect container logs and details (supporting text selection, copy all, and standard shortcuts).
  - Safe interactive console access via 'docker exec'.
- **Image Management:**
  - List available images.
  - Pull new images from registries.
  - Remove unused images.
- **Volume & Network Management:**
  - CRUD operations for Docker volumes and networks.
  - Display volume disk usage (calculated manually to optimize performance).
- **Connectivity:**
  - Auto-discovery of WSL 2 Docker instances.
  - Manual connection via IP/Port.
  - Environment variable-based connection (`DOCKER_HOST`).

## Target Audience
- Developers using Windows with WSL 2 as their primary development environment.
- DevOps engineers who need a quick visual overview of their local Docker resources.
- Students and learners who prefer a GUI over CLI for understanding Docker concepts.

## User Experience
- **Native Look & Feel:** A clean, responsive JavaFX interface that integrates well with the Windows desktop.
- **Performance:** Lightweight and fast, avoiding the overhead of Electron-based alternatives.
- **Simplicity:** Focus on core management tasks without overwhelming the user with obscure configurations.
