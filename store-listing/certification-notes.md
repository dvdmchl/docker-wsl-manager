# Copy-ready certification notes

Docker WSL Manager is an x64 full-trust JavaFX desktop application. It requires
WSL 2, a registered Linux distribution, and Docker Engine plus Docker CLI inside
that distribution. Java is bundled; no separate Java installation is required.

The `runFullTrust` restricted capability is required to launch `wsl.exe`, open
Windows Explorer and the default browser, read/write per-user settings, and run
the bundled JavaFX desktop process. The application does not require elevation,
install drivers, services, WSL, Docker, or system configuration.

The application first verifies Docker CLI in the configured distribution. It offers
three explicit transports: automatic WSL-address compatibility, Windows localhost,
and authenticated mutual TLS. Compatibility mode supports existing port 2375 setups
and displays an unencrypted-connection warning with secure setup guidance once per
launch. Localhost and TLS are the recommended modes. No transport is selected
silently as a fallback from another mode.

Create disposable fixtures with the commands in `MS_STORE_VERIFICATION.md`.
They require no credentials or private data. Exercise container refresh,
start/stop/restart, logs, statistics, console, images, volumes, Explorer, and
browser integration. Remove only the named fixtures after testing.

The Store package does not query GitHub for application updates; Microsoft Store
manages them. User-triggered Docker registry operations may use credentials
already configured in the selected Docker Engine. See the submitted privacy
policy for complete data and network behavior.

Publisher identity, exact package hash, test distribution, and any temporary
review instructions must be added by the maintainer for the submitted package.
