# Docker WSL Manager security and connection setup

This guide explains how Docker WSL Manager reaches Docker Engine and how to
move from compatibility mode to a safer connection. It is separate from the
[Microsoft Store publishing guide](MS_STORE_PUBLISHING.md).

## Connection modes

Choose a mode in **Settings > General Settings**:

- **WSL address (compatibility, unencrypted)** automatically discovers the
  selected distribution's WSL address and connects to its TCP port. This keeps
  existing installations working, but port 2375 has no authentication or
  encryption. The application displays a warning after a successful connection.
  Select **Don't show this warning again** to dismiss it on future launches;
  the same guide remains available from the Help menu.
- **Windows localhost** connects only to `127.0.0.1` on Windows. Use it when
  Windows-to-WSL localhost forwarding reaches the Docker listener.
- **Authenticated TLS** verifies the Docker server and client certificates and
  is the preferred mode for a non-loopback endpoint.

The application does not silently fall back between modes. The Docker CLI
preflight always runs inside the selected WSL distribution, so the CLI and the
Java Docker client target the same Docker Engine.

## Quick secure setup

1. Open **Settings > General Settings**.
2. Try **Windows localhost** only after confirming that Windows can reach the
   selected port with `Test-NetConnection 127.0.0.1 -Port 2375`.
3. For a remote or non-loopback endpoint, choose **Authenticated TLS**, normally
   use port `2376`, and select a local directory containing `ca.pem`,
   `cert.pem`, and `key.pem`.
4. Keep the private key readable only by the Windows user running the app.
5. If localhost forwarding is unavailable and TLS is not ready, use compatibility
   mode temporarily on a trusted local machine and plan the TLS migration.

## Docker listener examples

For localhost mode, keep the Unix socket and bind the TCP listener only to WSL
loopback. The exact service configuration varies by distribution. A reviewed
systemd override can look like:

```ini
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd -H unix:///var/run/docker.sock -H tcp://127.0.0.1:2375
```

Reload and restart Docker inside WSL, then verify both sides:

```powershell
wsl.exe --distribution Ubuntu --exec docker info
Test-NetConnection -ComputerName 127.0.0.1 -Port 2375
```

If the Windows test fails, do not assume localhost forwarding works on every
WSL/network configuration. Use authenticated TLS or retain compatibility mode
until the listener is configured correctly.

Compatibility mode commonly encounters an existing daemon listener such as:

```text
tcp://0.0.0.0:2375
```

That is not a secure target: every client able to reach the WSL address can
control Docker without credentials. Do not expose it beyond a trusted local
machine, and do not use it as the final configuration for a shared or hostile
network.

## TLS migration checklist

- Configure Docker mutual TLS according to the Docker daemon documentation.
- Bind the listener to the intended host and port; do not leave an unauthenticated
  2375 listener enabled alongside TLS.
- Copy only the client `ca.pem`, `cert.pem`, and `key.pem` needed by the app.
- Select **Authenticated TLS** and enter the matching host, port, and certificate
  directory.
- Reconnect and confirm that the application reports a TLS connection.
- Remove or firewall any old unauthenticated listener after validation.

## Why TCP is used

The application is a Windows JavaFX process, while Docker's Unix socket lives
inside WSL. The Java Docker client therefore needs a Windows-reachable TCP
endpoint. TCP is not inherently unsafe; the security boundary depends on the
bind address and authentication. Loopback limits reachability, and mutual TLS
adds endpoint authentication and encryption. An unauthenticated listener on
`0.0.0.0:2375` provides neither.

## Official references

- [Docker daemon remote access](https://docs.docker.com/engine/daemon/remote-access/)
- [Protect the Docker daemon socket](https://docs.docker.com/engine/security/protect-access/)
- [WSL networking](https://learn.microsoft.com/en-us/windows/wsl/networking)
