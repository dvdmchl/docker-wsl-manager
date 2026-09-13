package org.dreamabout.sw.dockerwslmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Builds argument-safe WSL commands that consistently target one distribution. */
public final class WslCommandLine {
    private static final Pattern DISTRO_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    private WslCommandLine() {
    }

    public static List<String> build(String distribution, String... linuxCommand) {
        List<String> command = new ArrayList<>();
        command.add("wsl.exe");

        String normalizedDistribution = normalizeDistribution(distribution);
        if (normalizedDistribution != null) {
            command.add("--distribution");
            command.add(normalizedDistribution);
        }

        command.add("--exec");
        command.addAll(Arrays.asList(linuxCommand));
        return List.copyOf(command);
    }

    public static ProcessBuilder processBuilder(String distribution, String... linuxCommand) {
        return new ProcessBuilder(build(distribution, linuxCommand));
    }

    public static String normalizeDistribution(String distribution) {
        if (distribution == null || distribution.isBlank()
                || "auto-detect".equalsIgnoreCase(distribution.trim())) {
            return null;
        }

        String normalized = distribution.trim();
        if (!DISTRO_NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "WSL distribution names may contain only letters, numbers, dots, underscores and hyphens.");
        }
        return normalized;
    }
}
