package com.termux.app.update;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class responsible for downloading update files.
 */
public class UpdateDownloader {

    private static final String LOG_TAG = "UpdateDownloader";
    private final ExecutorService executor;
    private final Context context;
    private final Handler mainHandler;

    public UpdateDownloader(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Shutdown the executor service.
     * Should be called when the UpdateDownloader is no longer needed.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Download an update file asynchronously.
     *
     * @param updateInfo Update information
     * @param callback   Callback to receive download progress and result
     */
    public void downloadUpdate(@NonNull UpdateInfo updateInfo, @NonNull DownloadCallback callback) {
        executor.execute(() -> {
            File downloadedFile = downloadUpdateSync(updateInfo, callback);
            mainHandler.post(() -> {
                if (downloadedFile != null) {
                    callback.onDownloadComplete(downloadedFile);
                } else {
                    callback.onDownloadError("Failed to download update");
                }
            });
        });
    }

    /**
     * Download an update file synchronously.
     *
     * @param updateInfo Update information
     * @param callback   Callback for progress updates
     * @return Downloaded file or null on error
     */
    private File downloadUpdateSync(@NonNull UpdateInfo updateInfo, @NonNull DownloadCallback callback) {
        HttpURLConnection connection = null;
        FileOutputStream fos = null;
        
        try {
            String downloadUrl = updateInfo.getDownloadUrl();
            
            // Validate URL is secure
            if (!UpdateSecurityUtils.isSecureUrl(downloadUrl)) {
                Logger.logError(LOG_TAG, "Download URL is not secure: " + downloadUrl);
                return null;
            }

            // Create download directory
            File downloadDir = new File(context.getFilesDir(), UpdateConfig.UPDATE_DOWNLOAD_DIR);
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                Logger.logError(LOG_TAG, "Failed to create download directory");
                return null;
            }

            // Create temporary file
            String filename = UpdateSecurityUtils.sanitizeFilename("termux-update-" + 
                    updateInfo.getVersion() + ".apk");
            File tempFile = new File(downloadDir, filename + ".tmp");
            File finalFile = new File(downloadDir, filename);

            // Delete existing files
            if (tempFile.exists() && !tempFile.delete()) {
                Logger.logError(LOG_TAG, "Failed to delete existing temp file");
            }
            if (finalFile.exists() && !finalFile.delete()) {
                Logger.logError(LOG_TAG, "Failed to delete existing file");
            }

            // Download file
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(UpdateConfig.CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(UpdateConfig.READ_TIMEOUT_MS);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Logger.logError(LOG_TAG, "Download failed with response code: " + responseCode);
                return null;
            }

            long fileSize = connection.getContentLengthLong();
            
            // Validate file size
            if (fileSize > UpdateConfig.MAX_UPDATE_FILE_SIZE) {
                Logger.logError(LOG_TAG, "Update file too large: " + fileSize);
                return null;
            }

            // Download with progress updates
            fos = new FileOutputStream(tempFile);
            InputStream is = connection.getInputStream();
            
            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;
            int lastProgress = 0;

            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (fileSize > 0) {
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    if (progress > lastProgress) {
                        lastProgress = progress;
                        final int progressToReport = progress;
                        mainHandler.post(() -> callback.onDownloadProgress(progressToReport));
                    }
                }
            }

            fos.close();
            fos = null;

            // Verify checksum - this is MANDATORY for security
            String checksum = updateInfo.getSha256Checksum();
            if (checksum == null || checksum.isEmpty()) {
                Logger.logError(LOG_TAG, "No checksum provided - cannot verify download integrity");
                tempFile.delete();
                return null;
            }

            Logger.logInfo(LOG_TAG, "Verifying checksum...");
            if (!UpdateSecurityUtils.verifySHA256(tempFile, checksum)) {
                Logger.logError(LOG_TAG, "Checksum verification failed");
                tempFile.delete();
                return null;
            }
            Logger.logInfo(LOG_TAG, "Checksum verified successfully");

            // Rename temp file to final file
            if (!tempFile.renameTo(finalFile)) {
                Logger.logError(LOG_TAG, "Failed to rename temp file");
                tempFile.delete();
                return null;
            }

            Logger.logInfo(LOG_TAG, "Update downloaded successfully: " + finalFile.getAbsolutePath());
            return finalFile;

        } catch (IOException e) {
            Logger.logError(LOG_TAG, "Download error: " + e.getMessage());
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Logger.logError(LOG_TAG, "Error closing file stream: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Callback interface for download operations.
     */
    public interface DownloadCallback {
        /**
         * Called when download progress changes.
         *
         * @param progress Progress percentage (0-100)
         */
        void onDownloadProgress(int progress);

        /**
         * Called when download completes successfully.
         *
         * @param file Downloaded file
         */
        void onDownloadComplete(@NonNull File file);

        /**
         * Called when download fails.
         *
         * @param error Error message
         */
        void onDownloadError(@NonNull String error);
    }
}
