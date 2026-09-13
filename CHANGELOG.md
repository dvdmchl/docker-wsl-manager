# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.4.0] - 2026-09-13

### Added

- Add WSL address compatibility, Windows localhost, and authenticated TLS Docker connection modes with secure onboarding guidance ([#49](https://github.com/dvdmchl/docker-wsl-manager/issues/49)).
- Add repeatable MSIX packaging and validation scripts, Store listing materials, privacy documentation, and release verification records ([#49](https://github.com/dvdmchl/docker-wsl-manager/issues/49)).

### Changed

- Keep Docker CLI operations inside the selected WSL distribution and improve error handling for unavailable WSL/Docker prerequisites ([#49](https://github.com/dvdmchl/docker-wsl-manager/issues/49)).
- Add a compatibility warning preference so users can dismiss the warning after reviewing the security guidance.

## [1.3.0] - 2026-08-31

### Added

- Persist and restore the selected container and container-group expansion state across application restarts, with safe reconciliation when Docker resources change ([#47](https://github.com/dvdmchl/docker-wsl-manager/issues/47)).

### Fixed

- Keep the interface responsive while Docker data refreshes or reconnects after Windows resumes or Docker is temporarily unavailable ([#46](https://github.com/dvdmchl/docker-wsl-manager/issues/46)).
- Stop active refresh, log, and statistics work cleanly during application shutdown.

## [1.2.1] - 2026-08-21

### Fixed

- Preserve the selected container or group and group expansion state during auto-refresh.
- Avoid moving keyboard focus to the Containers table when the selected item is unavailable after refresh.

[Unreleased]: https://github.com/dvdmchl/docker-wsl-manager/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/dvdmchl/docker-wsl-manager/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/dvdmchl/docker-wsl-manager/compare/v1.2.1...v1.3.0
[1.2.1]: https://github.com/dvdmchl/docker-wsl-manager/compare/v1.2.0...v1.2.1
