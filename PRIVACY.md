# Privacy Policy

Last updated: September 8, 2026

Docker WSL Manager is an independent desktop application for managing a Docker
Engine that runs in Windows Subsystem for Linux (WSL). It does not require an
application account and does not include advertising, analytics, or telemetry.

## Data accessed and stored locally

The application reads Docker data needed to provide its user interface,
including container, image, network and volume metadata, container
configuration, logs, process lists, statistics, and volume paths. This data is
obtained from the Docker Engine selected by the user and is displayed locally.

Settings, keyboard shortcuts, and container-tree UI state are stored under
`%USERPROFILE%\.docker-wsl-manager`. Microsoft Store, MSI, and ZIP installations
use this same location so settings can be preserved during upgrades and when
switching installation formats. Uninstalling the application does not delete
this directory automatically.

The application can open WSL paths in Windows Explorer. Windows and WSL may
therefore access the selected files through `\\wsl.localhost\` integration.

## Network communication

- The GitHub/ZIP/MSI update channel sends an HTTPS request to GitHub's public
  Releases API to check the latest version. Microsoft Store builds do not make
  this update request; Store updates are managed by Microsoft Store.
- Docker API requests are sent to the endpoint configured in the application.
  Compatibility mode automatically discovers the WSL address and uses unencrypted
  TCP; the application displays a warning when this mode connects. Windows localhost
  and authenticated TLS modes are available for safer configurations.
- User-requested image pulls and other registry operations are performed by the
  selected Docker Engine. Docker and the selected registry may receive image
  names, credentials already configured in Docker, network addresses, and
  standard protocol metadata according to their own configuration and privacy
  terms.
- Opening a published container port, the privacy policy, source repository, or
  release page sends the address to the user's default browser and the remote
  service being opened.

Docker WSL Manager does not transmit settings, container metadata, logs, or
credentials to the publisher. Information can nevertheless leave the device
through the explicit operations listed above or through the user's Docker,
WSL, proxy, DNS, browser, and registry configuration.

## User actions and deletion

Container, image, network, and volume changes occur only after the corresponding
user action and confirmation where provided. Removing Docker WSL Manager does
not remove Docker, WSL distributions, containers, images, networks, or volumes.

To delete application settings after uninstalling, remove
`%USERPROFILE%\.docker-wsl-manager`. Back up that directory first if settings
should be retained.

## Third-party services and software

The application interacts with Docker Engine, WSL, GitHub, user-selected
container registries, and the user's default browser and file explorer. Their
respective terms and privacy policies apply. Bundled open-source component
licenses are listed in `THIRD_PARTY_NOTICES.txt` in release packages.

Docker, Microsoft, Windows, GitHub, and Java are trademarks of their respective
owners. Docker WSL Manager is an independent project and is not affiliated with
or endorsed by Docker Inc. or Microsoft Corporation.

## Contact

Questions and privacy requests can be submitted through the project's public
support tracker:
https://github.com/dvdmchl/docker-wsl-manager/issues

Stable public policy URL:
https://github.com/dvdmchl/docker-wsl-manager/blob/main/PRIVACY.md
