package org.dreamabout.sw.dockerwslmanager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Adds the configured Docker endpoint to Docker CLI arguments executed inside WSL. */
public final class WslDockerCommand {
    private WslDockerCommand() {
    }

    public static List<String> build(boolean tlsEnabled, String host, int port,
                                     String certificatePath, String... dockerArguments) {
        List<String> arguments = new ArrayList<>();
        arguments.add("docker");
        if (tlsEnabled) {
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("A Docker TLS host is required.");
            }
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Docker port must be between 1 and 65535.");
            }
            String wslCertificatePath = windowsPathToWsl(certificatePath);
            arguments.add("--host");
            arguments.add("tcp://" + host.trim() + ":" + port);
            arguments.add("--tlsverify");
            arguments.add("--tlscacert");
            arguments.add(wslCertificatePath + "/ca.pem");
            arguments.add("--tlscert");
            arguments.add(wslCertificatePath + "/cert.pem");
            arguments.add("--tlskey");
            arguments.add(wslCertificatePath + "/key.pem");
        }
        arguments.addAll(Arrays.asList(dockerArguments));
        return List.copyOf(arguments);
    }

    static String windowsPathToWsl(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("A Docker TLS certificate directory is required.");
        }
        String normalized = Path.of(path).toAbsolutePath().normalize().toString();
        if (normalized.length() < 3 || normalized.charAt(1) != ':'
                || (normalized.charAt(2) != '\\' && normalized.charAt(2) != '/')) {
            throw new IllegalArgumentException("The TLS certificate directory must be on a Windows drive.");
        }
        char drive = Character.toLowerCase(normalized.charAt(0));
        String remainder = normalized.substring(3).replace('\\', '/');
        return "/mnt/" + drive + "/" + remainder;
    }
}
