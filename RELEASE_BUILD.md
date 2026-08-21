# Release process

This document is the authoritative release procedure for Docker WSL Manager.

## Prerequisites

- Git working tree is clean except for the intended release changes.
- Maven is available and uses a JDK capable of building Java 21 source.
- For an MSI: JDK 25 and WiX Toolset v7+ are installed; run `wix eula accept wix7` once.
- The release version in `pom.xml` is final and follows semantic versioning.

## 1. Prepare the release commit

1. Update `<version>` in `pom.xml`.
2. Copy `RELEASE_NOTES_TEMPLATE.md` to a version-specific Markdown file and complete it with user-visible changes, fixes, upgrade notes, and known limitations.
3. Commit and push those changes to `main`.
4. Run the test suite:

   ```powershell
   mvn test '-Dnet.bytebuddy.experimental=true'
   ```

## 2. Build artifacts

Build the standalone JAR and ZIP. The script reads the version from `pom.xml` and writes only to ignored `release-output/`:

```powershell
.\build-release.ps1 -ReleaseNotesPath ".\path\to\release-notes.md"
```

The ZIP contains:

- `docker-wsl-manager.jar` — standalone JavaFX application;
- `run.bat` — launcher for Java 21+;
- `README.md`, `LICENSE`, and `RELEASE_NOTES.md`.

For a Windows MSI, start PowerShell with JDK 25 and WiX 7 on `PATH`, then add `-BuildMsi`:

```powershell
$env:JAVA_HOME = "C:\dev\Java\jdk-25.0.1-full"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH;C:\Program Files\WiX Toolset v7.0\bin"
.\build-release.ps1 -ReleaseNotesPath ".\path\to\release-notes.md" -BuildMsi
```

## 3. Verify

- Run `docker-wsl-manager.jar` using Java 21+.
- Test the packaged `run.bat`.
- If built, install and launch the MSI on a clean Windows environment.
- Check that the ZIP and MSI contain the intended version and release notes.

## 4. Publish

After verification, create and push the matching annotated tag:

```powershell
git tag -a v<version> -m "Release v<version>"
git push origin v<version>
```

Create a GitHub Release from that tag and use the version-specific release notes as its body. Upload the ZIP and, when available, the MSI. The application checks GitHub's `releases/latest` endpoint, so publish the release rather than only creating a tag.
