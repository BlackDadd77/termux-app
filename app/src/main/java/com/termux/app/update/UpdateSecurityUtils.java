package com.termux.app.update;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for security operations related to updates.
 */
public class UpdateSecurityUtils {

    private static final String LOG_TAG = "UpdateSecurityUtils";

    /**
     * Calculate SHA-256 checksum of a file.
     *
     * @param file File to calculate checksum for
     * @return Hexadecimal string representation of the checksum, or null on error
     */
    @Nullable
    public static String calculateSHA256(@NonNull File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            Logger.logError(LOG_TAG, "Failed to calculate SHA-256 checksum: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify SHA-256 checksum of a file.
     *
     * @param file             File to verify
     * @param expectedChecksum Expected checksum in hexadecimal format
     * @return true if checksum matches, false otherwise
     */
    public static boolean verifySHA256(@NonNull File file, @NonNull String expectedChecksum) {
        String actualChecksum = calculateSHA256(file);
        if (actualChecksum == null) {
            return false;
        }
        return actualChecksum.equalsIgnoreCase(expectedChecksum);
    }

    /**
     * Convert byte array to hexadecimal string.
     *
     * @param bytes Byte array to convert
     * @return Hexadecimal string representation
     */
    @NonNull
    private static String bytesToHex(@NonNull byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Validate if a URL is secure (uses HTTPS).
     *
     * @param url URL to validate
     * @return true if URL uses HTTPS, false otherwise
     */
    public static boolean isSecureUrl(@NonNull String url) {
        return url.toLowerCase().startsWith("https://");
    }

    /**
     * Sanitize a filename to prevent directory traversal attacks.
     *
     * @param filename Filename to sanitize
     * @return Sanitized filename
     */
    @NonNull
    public static String sanitizeFilename(@NonNull String filename) {
        // Remove any path separators and parent directory references
        return filename.replaceAll("[/\\\\]", "")
                .replaceAll("\\.\\.", "")
                .trim();
    }
}
