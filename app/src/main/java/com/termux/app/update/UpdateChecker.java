package com.termux.app.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class responsible for checking for application updates.
 */
public class UpdateChecker {

    private static final String LOG_TAG = "UpdateChecker";
    private final ExecutorService executor;
    private final Context context;
    private final Handler mainHandler;

    public UpdateChecker(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Shutdown the executor service.
     * Should be called when the UpdateChecker is no longer needed.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Check for updates asynchronously.
     *
     * @param callback Callback to receive the result
     */
    public void checkForUpdates(@NonNull UpdateCheckCallback callback) {
        executor.execute(() -> {
            UpdateCheckResult result = checkForUpdatesSync();
            mainHandler.post(() -> callback.onUpdateCheckComplete(result));
        });
    }

    /**
     * Check for updates synchronously.
     *
     * @return UpdateCheckResult containing the result of the check
     */
    @NonNull
    private UpdateCheckResult checkForUpdatesSync() {
        try {
            // Get current version
            int currentVersionCode = getCurrentVersionCode();
            if (currentVersionCode == -1) {
                return UpdateCheckResult.error("Failed to get current version");
            }

            // Fetch update information
            UpdateInfo updateInfo = fetchUpdateInfo();
            if (updateInfo == null) {
                return UpdateCheckResult.error("Failed to fetch update information");
            }

            // Check if update is available
            if (updateInfo.getVersionCode() > currentVersionCode) {
                Logger.logInfo(LOG_TAG, "Update available: " + updateInfo.getVersion() + 
                        " (current: " + currentVersionCode + ")");
                return UpdateCheckResult.updateAvailable(updateInfo);
            } else {
                Logger.logInfo(LOG_TAG, "No update available (current version: " + currentVersionCode + ")");
                return UpdateCheckResult.noUpdateAvailable();
            }
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error checking for updates: " + e.getMessage());
            return UpdateCheckResult.error("Error checking for updates: " + e.getMessage());
        }
    }

    /**
     * Fetch update information from the update server.
     *
     * @return UpdateInfo or null on error
     */
    @Nullable
    private UpdateInfo fetchUpdateInfo() {
        HttpURLConnection connection = null;
        try {
            String updateUrl = UpdateConfig.getUpdateCheckUrl();
            
            // Validate URL is secure
            if (!UpdateSecurityUtils.isSecureUrl(updateUrl)) {
                Logger.logError(LOG_TAG, "Update URL is not secure: " + updateUrl);
                return null;
            }

            URL url = new URL(updateUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(UpdateConfig.CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(UpdateConfig.READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Logger.logError(LOG_TAG, "Update check failed with response code: " + responseCode);
                return null;
            }

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Parse JSON response
            JSONObject json = new JSONObject(response.toString());
            return parseGitHubRelease(json);

        } catch (IOException | JSONException e) {
            Logger.logError(LOG_TAG, "Failed to fetch update info: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Parse GitHub release JSON to UpdateInfo.
     *
     * @param json GitHub release JSON
     * @return UpdateInfo or null on error
     */
    @Nullable
    private UpdateInfo parseGitHubRelease(@NonNull JSONObject json) {
        try {
            String tagName = json.getString("tag_name");
            String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            
            // Extract version code from version string (e.g., "0.118.0" -> 118)
            int versionCode = extractVersionCode(version);
            
            String body = json.optString("body", "");
            String publishedAt = json.optString("published_at", "");
            
            // Get first APK asset
            if (json.has("assets") && json.getJSONArray("assets").length() > 0) {
                JSONObject asset = json.getJSONArray("assets").getJSONObject(0);
                String downloadUrl = asset.getString("browser_download_url");
                long fileSize = asset.optLong("size", 0);
                
                // Look for a checksum file in assets (e.g., .sha256 file)
                String checksum = findChecksumInAssets(json.getJSONArray("assets"), asset.getString("name"));
                
                // Checksum is required for security - without it, we cannot verify file integrity
                if (checksum == null || checksum.isEmpty()) {
                    Logger.logWarn(LOG_TAG, "No checksum found for update, skipping for security");
                    return null;
                }
                
                return new UpdateInfo(
                        version,
                        versionCode,
                        downloadUrl,
                        body,
                        fileSize,
                        checksum,
                        null,
                        System.currentTimeMillis()
                );
            }
            
            return null;
        } catch (JSONException e) {
            Logger.logError(LOG_TAG, "Failed to parse update info: " + e.getMessage());
            return null;
        }
    }

    /**
     * Find checksum in GitHub release assets.
     *
     * @param assets GitHub release assets array
     * @param apkName Name of the APK file
     * @return Checksum string or null if not found
     */
    @Nullable
    private String findChecksumInAssets(@NonNull org.json.JSONArray assets, @NonNull String apkName) {
        try {
            // Look for a .sha256 file with matching name
            String checksumFileName = apkName + ".sha256";
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (checksumFileName.equals(asset.getString("name"))) {
                    // In a full implementation, we would fetch this file and parse it
                    // For now, return null to indicate checksum file exists but needs to be fetched
                    Logger.logInfo(LOG_TAG, "Found checksum file: " + checksumFileName);
                    // TODO: Fetch and parse checksum file
                    return null;
                }
            }
        } catch (JSONException e) {
            Logger.logError(LOG_TAG, "Error searching for checksum: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract version code from version string.
     *
     * @param version Version string (e.g., "0.118.0")
     * @return Version code or -1 on error
     */
    private int extractVersionCode(@NonNull String version) {
        try {
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            Logger.logError(LOG_TAG, "Failed to extract version code from: " + version);
        }
        return -1;
    }

    /**
     * Get current app version code.
     *
     * @return Version code or -1 on error
     */
    private int getCurrentVersionCode() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.logError(LOG_TAG, "Failed to get current version: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Callback interface for update check results.
     */
    public interface UpdateCheckCallback {
        void onUpdateCheckComplete(@NonNull UpdateCheckResult result);
    }

    /**
     * Result of an update check.
     */
    public static class UpdateCheckResult {
        private final boolean success;
        private final boolean updateAvailable;
        private final UpdateInfo updateInfo;
        private final String errorMessage;

        private UpdateCheckResult(boolean success, boolean updateAvailable, 
                                  UpdateInfo updateInfo, String errorMessage) {
            this.success = success;
            this.updateAvailable = updateAvailable;
            this.updateInfo = updateInfo;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isUpdateAvailable() {
            return updateAvailable;
        }

        @Nullable
        public UpdateInfo getUpdateInfo() {
            return updateInfo;
        }

        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }

        @NonNull
        static UpdateCheckResult updateAvailable(@NonNull UpdateInfo updateInfo) {
            return new UpdateCheckResult(true, true, updateInfo, null);
        }

        @NonNull
        static UpdateCheckResult noUpdateAvailable() {
            return new UpdateCheckResult(true, false, null, null);
        }

        @NonNull
        static UpdateCheckResult error(@NonNull String errorMessage) {
            return new UpdateCheckResult(false, false, null, errorMessage);
        }
    }
}
