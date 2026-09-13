# Microsoft Store listing — en-US

## Product name

Docker WSL Manager

## Short description

Manage Docker Engine running in WSL 2 from an independent Windows desktop app.

## Description

Requires Windows x64, WSL 2, a configured Linux distribution, and Docker Engine
and Docker CLI running inside that distribution. Existing installations can use
automatic WSL-address compatibility mode; Windows localhost and authenticated TLS
are available for safer configurations.

Docker WSL Manager provides a focused JavaFX desktop interface for everyday
container work without requiring Docker CLI on Windows. Browse containers by
Docker Compose project, start and stop workloads, view logs and live resource
statistics, attach a WSL-hosted console, and inspect container configuration and
processes.

Manage images, volumes, and networks from the same interface. Open published
ports in your browser and supported WSL volume paths in Explorer. The selected
WSL distribution is used consistently for Docker CLI and filesystem operations.

Settings and UI state are stored in your Windows user profile. This Store build
receives application updates through Microsoft Store.

Docker WSL Manager is independent open-source software licensed under Apache
License 2.0. It is not affiliated with or endorsed by Docker Inc. or Microsoft
Corporation.

## Feature bullets

- Manage containers grouped by Docker Compose project.
- View logs, configuration, processes, and live resource usage.
- Launch an interactive Docker console through the selected WSL distribution.
- Manage images, Docker volumes, and networks.
- Preserve settings, selection, and expanded groups across updates.
- Choose automatic WSL compatibility, Windows localhost, or authenticated TLS.
- Receive a clear warning and setup guidance when using unencrypted compatibility mode.

## Required screenshot order and captions

1. `screenshots/01-containers.png` — Containers grouped by Docker Compose project.
2. `screenshots/02-details.png` — Container logs, details, and resource statistics.

These files must be captured from the final candidate. The old README images are
not suitable because they display the retired WSL-address connection mode.
Verify that no credentials, private image names, internal addresses, or personal
data are visible.
