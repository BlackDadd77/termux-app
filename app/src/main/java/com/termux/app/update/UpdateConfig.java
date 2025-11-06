package com.termux.app.update;

import androidx.annotation.NonNull;

/**
 * Configuration constants for the update system.
 */
public class UpdateConfig {

    /**
     * Default update check URL.
     * This should point to a JSON endpoint that returns update information.
     */
    public static final String DEFAULT_UPDATE_CHECK_URL = "https://api.github.com/repos/termux/termux-app/releases/latest";

    /**
     * Minimum time between update checks in milliseconds (24 hours).
     */
    public static final long MIN_UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L;

    /**
     * Connection timeout for network operations in milliseconds.
     */
    public static final int CONNECTION_TIMEOUT_MS = 30000;

    /**
     * Read timeout for network operations in milliseconds.
     */
    public static final int READ_TIMEOUT_MS = 30000;

    /**
     * Maximum allowed file size for updates (500 MB).
     */
    public static final long MAX_UPDATE_FILE_SIZE = 500 * 1024 * 1024L;

    /**
     * Directory name for storing downloaded updates.
     */
    public static final String UPDATE_DOWNLOAD_DIR = "updates";

    /**
     * Preference key for last update check timestamp.
     */
    public static final String PREF_LAST_UPDATE_CHECK = "last_update_check";

    /**
     * Preference key for automatic update checks.
     */
    public static final String PREF_AUTO_UPDATE_CHECK = "auto_update_check";

    /**
     * Preference key for update check URL.
     */
    public static final String PREF_UPDATE_CHECK_URL = "update_check_url";

    /**
     * Preference key for dismissed update version code.
     */
    public static final String PREF_DISMISSED_UPDATE_VERSION = "dismissed_update_version";

    /**
     * Get the update check URL from preferences or default.
     *
     * @return Update check URL
     */
    @NonNull
    public static String getUpdateCheckUrl() {
        // In a full implementation, this would read from SharedPreferences
        // For now, return the default
        return DEFAULT_UPDATE_CHECK_URL;
    }
}
