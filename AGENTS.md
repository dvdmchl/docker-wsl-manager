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
