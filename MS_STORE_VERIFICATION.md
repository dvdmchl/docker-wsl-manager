# Microsoft Store verification record

Use this file as the retained concise record for the exact candidate package.
Replace `NOT RUN` only after executing the check. Generated logs and packages
remain outside Git.

## Candidate

- Date: 2026-09-13
- Commit: uncommitted issue #49 worktree based on `6405652`
- Package filename: `DockerWSLManager-1.3.0.0-x64.msix`
- SHA-256: `1c4114a1afce4426faeddbe2d137819ba15e5cf1ab37b9eba2037b055a65079a`
- Windows edition/build: Windows x64 build 26220.9223
- WSL version/network mode: WSL 2.9.8.0; default NAT networking
- Distribution/version: Ubuntu 22.04.5 LTS
- Docker Engine/CLI version: 29.7.2 / 29.7.2
- JDK, Maven, Windows SDK, MakeAppx and WACK versions: Maven runtime JDK 26.0.1;
  packaging JDK 25.0.2; Maven 3.9.16; MakeAppx 10.0.22621.3233; WACK NOT RUN
- Account type / package identity: publisher input required

## Results

| Check | Result | Evidence or limitation |
| --- | --- | --- |
| Clean Windows x64 install; no separate Java | NOT RUN | Disposable environment required |
| Standard-user launch | NOT RUN | Disposable standard-user environment required |
| Unit tests | PASS | 49 tests, 0 failures or errors |
| Package manifest and development identity | PASS | Repository validator unpacked the package; x64 and only `runFullTrust` |
| Generated application-image launch | PASS | Connected through discovered WSL address `172.19.62.177:2375`; verification process then stopped |
| Sideload signature | NOT RUN | Development certificate required; Store replaces signature |
| WACK | NOT RUN | Run against final package |
| Malware scan | NOT RUN | Record scanner/version/result |
| WSL absent / no distribution | NOT RUN | UI must remain responsive and explain prerequisites |
| Docker stopped and recovery | NOT RUN | Restart Docker and reconnect without restarting app |
| Network offline / update check | NOT RUN | Store build must not query GitHub |
| WSL Docker Engine/CLI prerequisite | PASS | `wsl.exe -- docker version`; packaged app connected through explicit compatibility mode |
| Multiple distributions | NOT RUN | Commands and Explorer paths must use selected distribution |
| Containers, console, logs, stats, images, volumes, Explorer/browser | NOT RUN | Use disposable fixtures only |
| Upgrade from previous release | NOT RUN | Verify settings and container-tree state |
| MSI-to-MSIX migration/coexistence | NOT RUN | Uninstall MSI first; retain settings |
| Uninstall/reinstall | NOT RUN | Docker/WSL data must remain untouched |
| Bundled license/notices/runtime legal files | PASS | Validator found the files and successfully started bundled `java.exe -version` |

## Disposable reviewer scenario

```powershell
wsl.exe --distribution <DistroName> --exec docker run -d `
  --name dwm-certification-nginx -p 127.0.0.1:18080:80 nginx:alpine
wsl.exe --distribution <DistroName> --exec docker volume create dwm-certification-volume
```

Use the application to refresh, inspect logs and statistics, open the published
port, attach a shell, inspect/remove the sample volume, and stop/restart/remove
the sample container. Remove only these disposable fixtures after testing. No
registry credentials or private container data are needed.

## Unresolved checks

The local Docker Engine did not expose Windows `127.0.0.1:2375`; compatibility
mode was therefore used for the packaged application test. Project instructions
forbid changing Docker or WSL configuration as part of the application change.
A publisher identity, signing certificate, standard-user disposable environment,
final screenshots, malware scanner, WACK, and Partner Center account were unavailable.
Written procedures are not passed test evidence.
