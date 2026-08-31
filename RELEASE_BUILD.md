# Release process

This document is the authoritative release procedure for Docker WSL Manager.

## Prerequisites

- The Git working tree is clean except for the intended release changes.
- Maven 3.6.3 or newer is available and uses a JDK capable of building Java 21 source.
- For an MSI, JDK 25 and WiX Toolset v7+ are installed; run `wix eula accept wix7` once.
- The release version in `pom.xml` is final and follows semantic versioning.

## 1. Prepare the release commit

1. Update the top-level `<version>` in `pom.xml`.
2. Copy `RELEASE_NOTES_TEMPLATE.md` to `release-notes/<version>.md` and complete it with user-visible changes, fixes, upgrade notes, and known limitations.
3. Add the version to `CHANGELOG.md` and update its comparison links.
4. Commit and push those changes to `main`.
5. Run the test suite:

   ```powershell
   mvn test '-Dnet.bytebuddy.experimental=true'
   ```

## 2. Build artifacts

Use a version-scoped output directory so previously built releases are not
removed. The script reads the version from `pom.xml`, runs the tests, and
builds the standalone JAR and ZIP:

```powershell
.\build-release.ps1 `
  -OutputDir ".\release-output\v<version>" `
  -ReleaseNotesPath ".\release-notes\<version>.md"
```

The ZIP contains:

- `docker-wsl-manager.jar` — standalone JavaFX application;
- `run.bat` — launcher for Java 21+;
- `README.md`, `LICENSE`, and `RELEASE_NOTES.md`.

For a Windows MSI, start PowerShell with JDK 25 and WiX 7 on `PATH`, then add
`-BuildMsi`:

```powershell
$env:JAVA_HOME = "C:\dev\Java\jdk-25.0.1-full"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH;C:\Program Files\WiX Toolset v7.0\bin"
.\build-release.ps1 `
  -OutputDir ".\release-output\v<version>" `
  -ReleaseNotesPath ".\release-notes\<version>.md" `
  -BuildMsi
```

## 3. Verify

- Run the packaged `docker-wsl-manager.jar` using Java 21+.
- Test the packaged `run.bat`.
- Confirm that the ZIP and MSI names, embedded JAR manifest, and release notes use the intended version.
- Confirm that `scripts/Verify-MsiPackage.ps1` completed successfully during the MSI build.
- Install and launch the MSI on a clean Windows environment. Test the Start Menu shortcut and verify `C:\Program Files\DockerWSLManager\runtime\bin\java.exe -version`.

Generate `SHA256SUMS.txt` beside the uploadable artifacts:

```powershell
$releaseDir = ".\release-output\v<version>"
Get-ChildItem -LiteralPath $releaseDir -File |
  Where-Object { $_.Extension -in ".zip", ".msi" } |
  Get-FileHash -Algorithm SHA256 |
  ForEach-Object { "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf) } |
  Set-Content -LiteralPath (Join-Path $releaseDir "SHA256SUMS.txt") -Encoding ascii
```

Recompute the checksums after rebuilding any artifact, then independently
verify that every recorded hash matches its file.

## 4. Publish

After verification, create and push the matching annotated tag:

```powershell
git tag -a v<version> -m "Release v<version>"
git push origin v<version>
```

Create a GitHub Release from that tag and use the version-specific release
notes as its body. Upload the ZIP, MSI, and `SHA256SUMS.txt`. The application
checks GitHub's `releases/latest` endpoint, so publish the release rather than
only creating a tag.
