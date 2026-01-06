package com.minekube.connect.util;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.minekube.connect.api.logger.ConnectLogger;

public class UpdateChecker {
    private static final String GITHUB_API_URL =
            "https://api.github.com/repos/minekube/connect-java/releases/latest";

    private final ConnectLogger logger;

    @Inject
    public UpdateChecker(ConnectLogger logger) {
        this.logger = logger;
    }

    public void checkForUpdates() {
        HttpUtils.asyncGet(GITHUB_API_URL).thenAccept(response -> {
            if (!response.isCodeOk()) {
                logger.debug("Failed to check for updates: HTTP " + response.getHttpCode());
                return;
            }

            JsonObject json = response.getResponse();
            if (json == null || !json.has("tag_name")) {
                logger.debug("Failed to parse update response");
                return;
            }

            String latestVersion = json.get("tag_name").getAsString();
            String currentVersion = Constants.VERSION;

            // Remove 'v' prefix if present
            if (latestVersion.startsWith("v")) {
                latestVersion = latestVersion.substring(1);
            }

            // Skip check for development versions
            if (currentVersion.contains("SNAPSHOT") || currentVersion.contains("${")) {
                logger.debug("Running development version, skipping update check");
                return;
            }

            if (isNewerVersion(latestVersion, currentVersion)) {
                logger.info("A new version of Connect is available: " + latestVersion
                        + " (current: " + currentVersion + ")");
                logger.info("Download: https://github.com/minekube/connect-java/releases/latest");
            } else {
                logger.debug("Connect is up to date (version " + currentVersion + ")");
            }
        }).exceptionally(throwable -> {
            logger.debug("Failed to check for updates: " + throwable.getMessage());
            return null;
        });
    }

    /**
     * Compares two semantic version strings.
     * @return true if latestVersion is newer than currentVersion
     */
    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        try {
            int[] latest = parseVersion(latestVersion);
            int[] current = parseVersion(currentVersion);

            for (int i = 0; i < Math.max(latest.length, current.length); i++) {
                int l = i < latest.length ? latest[i] : 0;
                int c = i < current.length ? current[i] : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
            return false;
        } catch (Exception e) {
            // If parsing fails, do string comparison
            return !latestVersion.equals(currentVersion);
        }
    }

    private int[] parseVersion(String version) {
        // Remove any suffix like -SNAPSHOT, -beta, etc.
        String cleanVersion = version.split("-")[0];
        String[] parts = cleanVersion.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }
}
