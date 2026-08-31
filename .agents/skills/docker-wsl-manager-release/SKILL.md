---
name: docker-wsl-manager-release
description: Prepare and release Docker WSL Manager versions with release notes, CHANGELOG.md, verified ZIP/MSI artifacts, checksums, annotated tags, and optional GitHub publication. Use only for this repository's release workflow.
---

# Docker WSL Manager release

Create a reproducible release from `main` while preserving unrelated user work. Treat `RELEASE_BUILD.md` as the authoritative project procedure and use `build-release.ps1` rather than recreating its Maven and packaging logic.

## Select the requested scope

Infer the mode from the request:

- **Prepare**: update versioned documentation, run verification, build local artifacts, and create the focused release commit. Do not push or tag.
- **Release**: perform Prepare, push the release commit to `origin/main`, create and push annotated tag `v<version>`, and leave the artifacts ready for later upload. This is the default for requests to "create a release" or "create/tag a new version".
- **Publish**: perform Release, create or update a draft GitHub Release, upload the verified assets, and publish it only when the user explicitly requests publication. Prefer a draft until every asset is attached.

Do not broaden Prepare into external GitHub mutations. Do not upload assets merely because Release mode pushes a tag.

## Establish the release baseline

1. Work from the repository root and read `AGENTS.md`, `RELEASE_BUILD.md`, `RELEASE_NOTES_TEMPLATE.md`, `pom.xml`, and `build-release.ps1`. Read relevant README release instructions if any of those files refer to them.
2. Inspect `git status --short --branch`, the latest version tag, `git log <previous-tag>..HEAD`, and the diff. Preserve unrelated or pre-existing changes; never stage them into the release commit.
3. Use GitHub MCP tools when available to inspect repository metadata, the latest published release, merged pull requests, and closed issues since the previous tag. Verify the current GitHub user first when the selected MCP workflow requires it. Use local Git for status, diff, staging, commit, and annotated tag creation. Use `gh` only when MCP is unavailable or cannot perform the required GitHub operation.
4. Confirm that the current branch is `main`, its upstream is `origin/main`, the intended release commit is present locally, and neither tag `v<version>` nor a GitHub Release for it already exists. Fetching metadata is allowed; do not rewrite local or remote history.
5. Keep unrelated working-tree changes intact. If they prevent proving what will be released, stop and identify the exact conflict instead of stashing, discarding, or committing them.

## Choose and apply the version

- Accept an explicitly supplied final SemVer version. Otherwise infer the next version from user-visible changes since the latest release: patch for compatible fixes, minor for backward-compatible features, and major for breaking behavior. State the inferred choice before editing; ask only when the available evidence makes the level materially ambiguous.
- Use the unprefixed version in `pom.xml` and filenames, and `v<version>` for the Git tag.
- Reject an already released version and versions inconsistent across `pom.xml`, release notes, artifact names, and the tag.
- Update only the project's top-level `<version>` in `pom.xml`; do not alter dependency or plugin versions as part of a release bump.

## Write release documentation

Derive all entries from commits, diffs, merged pull requests, and issues since the previous version. Rewrite implementation-oriented commit text into concise user-facing English. Include issue or pull-request references when verified. Never invent changes, upgrade steps, contributors, security claims, or compatibility guarantees.

1. Create `release-notes/<version>.md` from `RELEASE_NOTES_TEMPLATE.md` with the title `Docker WSL Manager <version>`.
2. Keep only useful sections: Highlights, Fixes, Upgrade notes, and Known limitations. Explicitly say when no migration or configuration changes are required; omit empty Highlights or Known limitations sections.
3. Create or update root `CHANGELOG.md` in Keep a Changelog style:
   - retain a top-level `Unreleased` section;
   - add one dated `<version>` section using `Added`, `Changed`, `Fixed`, `Removed`, `Security`, or `Deprecated` only when applicable;
   - preserve existing historical entries and comparison links;
   - if the file does not yet exist, seed only history supported by existing versioned release notes and tags rather than reconstructing speculative entries;
   - add or update comparison links for `Unreleased` and the new version using the repository URL.
4. Update README or other tracked documentation only when the release actually changes installation, runtime, configuration, or user-facing usage.
5. Review the release notes and changelog together: they may share facts, but the release notes should read as the GitHub Release description while the changelog remains a durable compact history.

## Verify and build

1. Check prerequisites without changing machine configuration:
   - Maven must be available with a JDK capable of Java 21 source;
   - MSI creation requires JDK 25 and WiX Toolset v7+ on `PATH`, including any already-required WiX EULA acceptance.
2. Do not install Docker Desktop, a JDK, WiX, or alter system configuration. If JDK 25 or WiX is missing, still complete documentation and non-MSI checks when useful, then report the MSI as a concrete blocker. Do not tag or publish a release that promises an MSI until it exists and passes verification.
3. Run the documented build from PowerShell with a version-scoped output directory so older ignored artifacts are not deleted:

   ```powershell
   .\build-release.ps1 `
     -OutputDir ".\release-output\v<version>" `
     -ReleaseNotesPath ".\release-notes\<version>.md" `
     -BuildMsi
   ```

   The script runs `mvn test '-Dnet.bytebuddy.experimental=true'`, builds the standalone package, and invokes the existing MSI verification. Do not bypass failed tests with `-DskipTests`; it is permitted only inside the script's later packaging phases after the test phase succeeds.
4. Verify the outputs rather than relying only on exit status:
   - the ZIP and MSI exist under `release-output/v<version>/` and have non-zero sizes;
   - names and embedded release notes use the intended version;
   - the ZIP contains the standalone JAR, `run.bat`, `README.md`, `LICENSE`, and `RELEASE_NOTES.md`;
   - the packaged JAR manifest reports the intended implementation version;
   - `scripts/Verify-MsiPackage.ps1`/the Maven profile completed successfully;
   - perform the clean-Windows MSI installation and Start Menu launch required by `RELEASE_BUILD.md` when that environment is available. If it is not available, report it as a remaining manual publication gate.
5. Generate `SHA256SUMS.txt` beside the uploadable assets with SHA-256 hashes and relative filenames for the ZIP and MSI. Recompute it after any artifact is rebuilt. Verify the recorded hashes once before tagging or publishing.

## Create the release commit and tag

1. Review `git diff --check`, the focused diff, artifact checks, and version consistency. Ignored build output must remain untracked.
2. Stage only `pom.xml`, `CHANGELOG.md`, `release-notes/<version>.md`, and any other intentionally changed release documentation. Never use a blanket staging command when unrelated files exist.
3. Commit on `main` with `chore(release): Prepare v<version>`. If this release implements a GitHub issue, obey the repository rule and prefix the commit with `#<issue> - `.
4. Ensure tests and artifacts were produced from the exact committed content and that no intended tracked release change remains uncommitted.
5. In Release or Publish mode, push the commit to `origin/main`, then create an annotated tag on that exact commit:

   ```powershell
   git tag -a v<version> -m "Release v<version>"
   git push origin v<version>
   ```

6. Verify the remote tag resolves to the release commit. Never force, move, replace, or reuse a release tag. If pushing the commit or tag fails, stop and report the state precisely.

## Publish only when requested

- Use the version-specific release notes as the GitHub Release body and `Docker WSL Manager <version>` as the title.
- Prefer creating a draft, attaching the ZIP, MSI, and `SHA256SUMS.txt`, verifying their names/sizes/hashes, and then publishing. This order remains safe if immutable releases are enabled.
- Mark a release as prerelease only when the version or user request says so. Do not mark an ordinary stable release as latest until it is published successfully.
- The application reads GitHub's `releases/latest`; a pushed tag alone does not make the update visible. State clearly whether the result is only tagged, is a draft, or is published.
- After publication, verify the release URL, tag, body, asset list, and latest-release status. Do not delete or replace a published asset to hide a mismatch; prepare a corrected version instead.

## Handoff

Report the version and commit SHA, documentation changed, test/build results, local artifact paths with sizes and SHA-256 hashes, local and remote tag state, GitHub Release state, and every remaining manual gate. Link tracked files with local file links. Never expose credentials or include production access details in commands or logs.
