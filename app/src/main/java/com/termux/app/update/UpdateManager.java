package com.termux.app.update;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.termux.shared.logger.Logger;

import java.io.File;

/**
 * Main manager class for coordinating the update system.
 */
public class UpdateManager {

    private static final String LOG_TAG = "UpdateManager";
    private static final String PREFS_NAME = "termux_update_prefs";

    private final Context context;
    private final UpdateChecker updateChecker;
    private final UpdateDownloader updateDownloader;
    private final SharedPreferences prefs;

    public UpdateManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.updateChecker = new UpdateChecker(context);
        this.updateDownloader = new UpdateDownloader(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if enough time has passed since the last update check.
     *
     * @return true if update check should be performed
     */
    public boolean shouldCheckForUpdates() {
        long lastCheck = prefs.getLong(UpdateConfig.PREF_LAST_UPDATE_CHECK, 0);
        long now = System.currentTimeMillis();
        return (now - lastCheck) >= UpdateConfig.MIN_UPDATE_CHECK_INTERVAL_MS;
    }

    /**
     * Check for updates if automatic updates are enabled and enough time has passed.
     *
     * @param callback Callback to receive the result
     */
    public void checkForUpdatesIfNeeded(@NonNull UpdateChecker.UpdateCheckCallback callback) {
        boolean autoCheck = prefs.getBoolean(UpdateConfig.PREF_AUTO_UPDATE_CHECK, true);
        
        if (!autoCheck) {
            Logger.logInfo(LOG_TAG, "Automatic update checks are disabled");
            return;
        }

        if (!shouldCheckForUpdates()) {
            Logger.logInfo(LOG_TAG, "Update check skipped (too soon since last check)");
            return;
        }

        checkForUpdates(callback);
    }

    /**
     * Check for updates manually.
     *
     * @param callback Callback to receive the result
     */
    public void checkForUpdates(@NonNull UpdateChecker.UpdateCheckCallback callback) {
        updateLastCheckTime();
        updateChecker.checkForUpdates(callback);
    }

    /**
     * Download an update.
     *
     * @param updateInfo Update information
     * @param callback   Callback for download progress and result
     */
    public void downloadUpdate(@NonNull UpdateInfo updateInfo, 
                               @NonNull UpdateDownloader.DownloadCallback callback) {
        updateDownloader.downloadUpdate(updateInfo, callback);
    }

    /**
     * Install a downloaded update APK.
     *
     * @param apkFile APK file to install
     * @return true if installation intent was launched successfully
     */
    public boolean installUpdate(@NonNull File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android N and above
                apkUri = FileProvider.getUriForFile(context, 
                        context.getPackageName() + ".provider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            context.startActivity(intent);

            Logger.logInfo(LOG_TAG, "Launched update installation for: " + apkFile.getName());
            return true;
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Failed to install update: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if an update has been dismissed by the user.
     *
     * @param versionCode Version code to check
     * @return true if the update has been dismissed
     */
    public boolean isUpdateDismissed(int versionCode) {
        int dismissedVersion = prefs.getInt(UpdateConfig.PREF_DISMISSED_UPDATE_VERSION, -1);
        return dismissedVersion == versionCode;
    }

    /**
     * Mark an update as dismissed by the user.
     *
     * @param versionCode Version code to dismiss
     */
    public void dismissUpdate(int versionCode) {
        prefs.edit()
                .putInt(UpdateConfig.PREF_DISMISSED_UPDATE_VERSION, versionCode)
                .apply();
        Logger.logInfo(LOG_TAG, "Dismissed update version: " + versionCode);
    }

    /**
     * Clear dismissed update status.
     */
    public void clearDismissedUpdate() {
        prefs.edit()
                .remove(UpdateConfig.PREF_DISMISSED_UPDATE_VERSION)
                .apply();
    }

    /**
     * Enable or disable automatic update checks.
     *
     * @param enabled true to enable, false to disable
     */
    public void setAutoUpdateCheckEnabled(boolean enabled) {
        prefs.edit()
                .putBoolean(UpdateConfig.PREF_AUTO_UPDATE_CHECK, enabled)
                .apply();
        Logger.logInfo(LOG_TAG, "Auto update check " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Check if automatic update checks are enabled.
     *
     * @return true if enabled
     */
    public boolean isAutoUpdateCheckEnabled() {
        return prefs.getBoolean(UpdateConfig.PREF_AUTO_UPDATE_CHECK, true);
    }

    /**
     * Set custom update check URL.
     *
     * @param url Update check URL
     */
    public void setUpdateCheckUrl(@Nullable String url) {
        if (url == null || url.isEmpty()) {
            prefs.edit().remove(UpdateConfig.PREF_UPDATE_CHECK_URL).apply();
        } else {
            prefs.edit().putString(UpdateConfig.PREF_UPDATE_CHECK_URL, url).apply();
        }
    }

    /**
     * Get the configured update check URL.
     *
     * @return Update check URL
     */
    @NonNull
    public String getUpdateCheckUrl() {
        return prefs.getString(UpdateConfig.PREF_UPDATE_CHECK_URL, 
                UpdateConfig.DEFAULT_UPDATE_CHECK_URL);
    }

    /**
     * Update the last check timestamp.
     */
    private void updateLastCheckTime() {
        prefs.edit()
                .putLong(UpdateConfig.PREF_LAST_UPDATE_CHECK, System.currentTimeMillis())
                .apply();
    }

    /**
     * Clean up old update files.
     */
    public void cleanupOldUpdates() {
        File downloadDir = new File(context.getFilesDir(), UpdateConfig.UPDATE_DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            return;
        }

        File[] files = downloadDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.delete()) {
                    Logger.logInfo(LOG_TAG, "Deleted old update file: " + file.getName());
                }
            }
        }
    }

    /**
     * Shutdown and release resources.
     * Should be called when the UpdateManager is no longer needed.
     */
    public void shutdown() {
        updateChecker.shutdown();
        updateDownloader.shutdown();
    }
}
