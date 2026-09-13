# Microsoft Store publishing guide

This guide covers the reproducible Windows x64 MSIX route for Docker WSL
Manager. It was checked against the official sources linked below on September
8, 2026. Microsoft Store certification and publisher identity verification are
external maintainer actions; this repository does not claim that either has
been completed.

## 1. Supported route and prerequisites

The selected route is a full-trust MSIX containing the jpackage application
image and bundled Java runtime. The package targets x64 Windows Desktop,
requires Windows 10 build 19041 or newer, and declares only the restricted
`runFullTrust` capability. Full trust is required for JavaFX, `wsl.exe`, Docker
CLI, Explorer, browser, and user-home settings integration. It does not request
administrator elevation.

Runtime prerequisites, which must appear at the beginning of the Store listing:

1. Windows x64 with WSL 2.
2. A registered Linux distribution containing Docker Engine and Docker CLI.
3. Docker running in that distribution.
4. A Docker TCP endpoint reachable from Windows. Compatibility mode supports an
   automatically discovered WSL address, while localhost or authenticated TLS is
   recommended. Never expose unauthenticated port 2375 beyond a trusted machine.

Build prerequisites:

- clean checkout on Windows x64;
- Maven 3.6.3 or newer;
- full JDK 25 with `jlink.exe` and `jpackage.exe`;
- Windows 10/11 SDK x64 tools, including `MakeAppx.exe`, `SignTool.exe`, Windows
  App Certification Kit (WACK), and the current supported SDK;
- Partner Center identity values for a Store upload;
- a locally trusted development certificate only for sideload testing.

## 2. Docker connection security

Connection modes, the compatibility warning, localhost forwarding, and TLS
migration are documented separately in
[DOCKER_WSL_SECURITY.md](DOCKER_WSL_SECURITY.md). Keep that guide with the
application documentation; it is not a Store-only procedure.

## 3. Build an MSIX from a clean checkout

Run tests first, then build an unsigned Store-upload package with identity values
copied exactly (including case and punctuation) from Partner Center:

```powershell
git status --short
mvn test '-Dnet.bytebuddy.experimental=true'

.\scripts\Build-MsixPackage.ps1 `
  -IdentityName '<Partner Center Package/Identity/Name>' `
  -Publisher '<Partner Center Package/Identity/Publisher>' `
  -PublisherDisplayName '<verified public publisher name>'
```

The script creates the shaded JAR and third-party notices, builds the bundled
runtime and jpackage image, adds the Store update channel, renders required
package icons, creates the package with `MakeAppx`, unpacks and inspects the
result, and writes its SHA-256 hash under `target\msix`. Upload the generated
`.msix`; do not upload the work directory or checksum file as the package.

For local sideload testing, install a non-production test certificate in the
current user's certificate store whose subject exactly matches `-Publisher`,
then pass its SHA-1 thumbprint:

```powershell
.\scripts\Build-MsixPackage.ps1 `
  -SigningThumbprint '<development certificate thumbprint>'
```

The repository contains no certificate, private key, password, legal publisher,
or reserved identity. Do not commit any of those values. Store submissions need
not use a CA-trusted developer signature because Microsoft replaces the package
signature after certification. Sideloaded packages must be signed and trusted.

## 4. Identity and version mapping

The script maps Maven `major.minor.patch` to MSIX
`major.minor.patch.0`. Every update must have a numerically greater four-part
package version. Override `-PackageVersion` only when Partner Center versioning
requires it; all components must be integers from 0 through 65535.

Publisher-owned inputs still required:

- Individual or Company developer-account choice and completed verification;
- reserved product name;
- exact package Identity Name and Publisher strings;
- verified public publisher display name, support contact, and target markets;
- pricing and availability decisions;
- age-rating questionnaire answers;
- final privacy/support URLs and any legally required terms.

Choose Individual only when publishing under the verified person's name. Choose
Company only for a registered legal entity. Partner Center does not support
converting an Individual account to Company. Current onboarding documentation
states that both account types have no registration fee; reverify this before
registration.

## 5. Package validation

Run the repository validator and WACK against the final bytes:

```powershell
.\scripts\Verify-MsixPackage.ps1 `
  -MsixPath '.\target\msix\DockerWSLManager-<four-part-version>-x64.msix'

Get-FileHash `
  '.\target\msix\DockerWSLManager-<four-part-version>-x64.msix' `
  -Algorithm SHA256
```

For a signed sideload build add `-RequireTrustedSignature`. Then install the
package as a standard user and execute the matrix in
[`MS_STORE_VERIFICATION.md`](MS_STORE_VERIFICATION.md). Run WACK on the exact
uploadable package and retain its report outside Git. Rebuilds change package
bytes, so repeat validation, malware scanning, and hashing afterward.

## 6. Migration and coexistence

ZIP, MSI, and MSIX builds use `%USERPROFILE%\.docker-wsl-manager`, so application
settings survive in-place upgrades and migration between formats. The package
does not move or delete Docker or WSL data.

Before installing the Store MSIX, close and uninstall an existing MSI to avoid
two Start Menu entries and two independently updated installations. The MSI
uninstaller must leave user settings in place. A ZIP copy can coexist, but users
should remove old shortcuts and launch only the Store entry. MSIX uninstall also
leaves the shared settings directory; deletion is an explicit user choice
documented in `PRIVACY.md`.

The MSIX launcher sets the update channel to `store`. It does not query GitHub
Releases or direct the user to a parallel MSI. ZIP/MSI builds retain the GitHub
release notification flow.

## 7. Listing materials

Prepared English copy, a 300×300 app tile, and reviewer notes are in
`store-listing`. The first paragraph discloses WSL and Docker prerequisites.
Final screenshots still must be captured from the candidate build using the
disposable fixtures, because the existing repository screenshots show the old
network connection behavior. Desktop screenshots must be PNG files at least
1366×768 and must contain no credentials, private names, addresses, or other
personal data.

Suggested Partner Center values:

- category: Developer tools;
- language: English (United States);
- architecture: x64 only;
- license: Apache License 2.0;
- privacy URL:
  `https://github.com/dvdmchl/docker-wsl-manager/blob/main/PRIVACY.md`;
- support URL: `https://github.com/dvdmchl/docker-wsl-manager/issues`.

Do not claim Docker Inc. or Microsoft endorsement. Complete the age rating from
actual functionality rather than copying a proposed value.

## 8. Partner Center submission

1. Register at the Microsoft Store developer onboarding site and complete the
   chosen Individual or Company identity verification.
2. In **Apps and games**, create the product and reserve its name.
3. Copy the package identity values into the build command and rebuild.
4. Create a submission. Select markets, visibility, pricing, category, and
   language according to the maintainer's decisions.
5. Upload the final `.msix` and resolve all package-analysis errors.
6. Complete properties, age ratings, privacy/support URLs, license information,
   listing copy, icons, screenshots, and accessibility declarations.
7. Add the copy-ready notes from `store-listing/certification-notes.md`. Declare
   WSL/Docker dependencies and justify `runFullTrust`.
8. Run the pre-submission checklist below, then submit. Microsoft alone decides
   certification.

Pre-submission checklist:

- clean build and tests passed;
- final identity/version match Partner Center exactly;
- `MakeAppx` pack/unpack verification, WACK, signature status, malware scan, and SHA-256 retained;
- standard-user install/launch and every applicable verification-matrix row recorded;
- WSL-absent, stopped-Docker, offline, recovery, upgrade, and uninstall tested;
- notices are present in the JAR and package application directory;
- privacy/support URLs load without authentication;
- screenshots and descriptions match the submitted version;
- no secrets, private data, development certificate, or placeholder publisher
  values are in the upload.

If certification fails, preserve the report, reproduce the failure against the
same hash, fix only the identified problem, increment the package version when
required, and rerun every affected validation. Do not describe an unexecuted
test as passed.

## 9. Subsequent releases

Prepare the normal version and release notes, then rebuild with the same Partner
Center identity and a greater package version. Repeat tests, package validation,
WACK, standard-user upgrade and settings-preservation checks. Submit the new
package to the existing product. Do not change the Identity Name or Publisher.

Continue publishing ZIP/MSI artifacts through the existing GitHub release flow.
They remain the non-Store channel and must not be presented as an update for an
installed Store package.

## Official sources checked September 8, 2026

- [MSIX package requirements](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/app-package-requirements)
- [Manual MSIX package generation](https://learn.microsoft.com/en-us/windows/msix/desktop/desktop-to-uwp-manual-conversion)
- [App capability declarations](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/app-capability-declarations)
- [Microsoft Store policies](https://learn.microsoft.com/en-us/windows/apps/publish/store-policies)
- [Partner Center developer account](https://learn.microsoft.com/en-us/windows/apps/publish/partner-center/open-a-developer-account)
- [MSIX name reservations](https://learn.microsoft.com/en-us/windows/apps/publish/partner-center/msix/manage-app-name-reservations)
- [MSIX screenshots and images](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/screenshots-and-images)
- [WSL networking](https://learn.microsoft.com/en-us/windows/wsl/networking)
- [Docker daemon remote access](https://docs.docker.com/engine/daemon/remote-access/)
- [Protect the Docker daemon socket](https://docs.docker.com/engine/security/protect-access/)
