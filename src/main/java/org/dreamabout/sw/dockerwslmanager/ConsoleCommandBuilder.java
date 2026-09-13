package org.dreamabout.sw.dockerwslmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Builds the Windows console command used for an interactive Docker exec session. */
public final class ConsoleCommandBuilder {
    private static final Pattern CONTAINER_ID = Pattern.compile("[A-Fa-f0-9]{12,64}");

    private ConsoleCommandBuilder() {
    }

    public static List<String> build(String distribution, String containerId) {
        return build(distribution, containerId, false, "", 2375, "");
    }

    public static List<String> build(String distribution, String containerId, boolean tlsEnabled,
                                     String host, int port, String certificatePath) {
        if (containerId == null || !CONTAINER_ID.matcher(containerId).matches()) {
            throw new IllegalArgumentException("Docker returned an invalid container ID.");
        }

        List<String> command = new ArrayList<>();
        command.add("cmd.exe");
        command.add("/c");
        command.add("start");
        command.add("");
        List<String> dockerCommand = WslDockerCommand.build(
                tlsEnabled, host, port, certificatePath, "exec", "-it", containerId, "sh");
        command.addAll(WslCommandLine.build(distribution, dockerCommand.toArray(String[]::new)));
        return List.copyOf(command);
    }
}
