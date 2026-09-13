# Docker WSL Manager – Codex instructions

## Project overview

- JavaFX desktop application for managing Docker running in WSL 2.
- Production code: `src/main/java`; resources: `src/main/resources`; tests: `src/test/java`.
- Build system: Maven. The project targets Java 21 (`pom.xml`).

## Working conventions

- Keep changes focused on the requested behaviour; do not reformat unrelated code.
- Preserve user changes already present in the working tree.
- Do not commit build output (`target/`) or release artifacts.
- Update or add tests when behaviour can be covered without a Docker daemon.
- Use GitHub MCP tools for GitHub issues and pull requests when available. Use local Git only for local status, diff, staging, and commits.
- Work directly on the `main` branch. Do not create a feature branch or pull request unless the user explicitly requests one.

## Verification

- Run `mvn test` for ordinary changes.
- On JDK versions newer than Byte Buddy supports, use:

  ```powershell
  mvn test '-Dnet.bytebuddy.experimental=true'
  ```

- For MSI packaging, use the documented JDK 25 and WiX prerequisites in `README.md`; do not modify local Docker, WSL, or system configuration as part of application changes.

## JavaFX UI changes

- Preserve users' current UI context during refreshes whenever possible (selection, focus, expansion, scroll position).
- Keep FXML controller fields, `fx:id` values, and handler method names in sync with `src/main/resources/*.fxml`.

## Microsoft Store distribution

- The application is distributed through both the existing GitHub ZIP/MSI channel and the Microsoft Store MSIX channel. Treat Store compatibility as a continuing product requirement, not a one-off release task.
- The current Partner Center package identity is:
  - `IdentityName`: `Dreamabout.org.DockerWSLManager`
  - `Publisher`: `CN=804C413F-ADEF-408E-AA28-0CD506852A4C`
  - `PublisherDisplayName`: `Dreamabout.org`
- Never use the development MSIX identity (`DockerWSLManager.Development`) or a development certificate for a Store submission. Store builds must pass the exact Partner Center identity values explicitly to `scripts/Build-MsixPackage.ps1`; the Store signs the submitted package.
- Keep the MSIX package x64-only unless an ARM64 package is deliberately implemented and tested. Do not claim support for device families that are not packaged and verified.
- Preserve the MSIX manifest's only restricted capability, `runFullTrust`. It is required for the JavaFX desktop process, `wsl.exe`, Docker CLI, Explorer, browser, and user-requested local Docker operations. It must not be used to install services, require elevation, or modify Docker/WSL system configuration.
- Preserve the Store launcher update channel (`dockerwslmanager.updateChannel=store`). Store builds must not direct users to GitHub releases or perform GitHub update checks; ZIP/MSI builds remain on the GitHub update channel.
- Every release must increment the Maven version and the four-part MSIX version. Never reuse a Store package identity/version or move an existing release tag.
- Keep `PRIVACY.md`, the public privacy URL, Store listing text, screenshots, icons, certification notes, and `MS_STORE_VERIFICATION.md` consistent with the actual shipped behavior. Store descriptions must disclose WSL 2 and Docker prerequisites before feature marketing.
- For Store-facing changes, run the daemon-free tests, build and validate the exact MSIX bytes, verify manifest identity/capabilities/update channel, record SHA-256 hashes, and report unavailable WACK, clean-Windows, standard-user, malware, or Partner Center checks as unresolved rather than passed.
- Follow `MS_STORE_PUBLISHING.md` for packaging and Partner Center submission. Follow `RELEASE_BUILD.md` and the `docker-wsl-manager-release` workflow for GitHub ZIP/MSI releases; do not mix Store artifacts into the GitHub update path without an explicit release decision.
