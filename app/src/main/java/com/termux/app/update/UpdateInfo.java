package com.termux.app.update;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Model class representing update information.
 */
public class UpdateInfo {

    private final String version;
    private final int versionCode;
    private final String downloadUrl;
    private final String releaseNotes;
    private final long fileSize;
    private final String sha256Checksum;
    private final String signatureUrl;
    private final long releaseDate;

    public UpdateInfo(@NonNull String version, int versionCode, @NonNull String downloadUrl,
                      String releaseNotes, long fileSize, @NonNull String sha256Checksum,
                      String signatureUrl, long releaseDate) {
        this.version = version;
        this.versionCode = versionCode;
        this.downloadUrl = downloadUrl;
        this.releaseNotes = releaseNotes;
        this.fileSize = fileSize;
        this.sha256Checksum = sha256Checksum;
        this.signatureUrl = signatureUrl;
        this.releaseDate = releaseDate;
    }

    @NonNull
    public String getVersion() {
        return version;
    }

    public int getVersionCode() {
        return versionCode;
    }

    @NonNull
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public long getFileSize() {
        return fileSize;
    }

    @NonNull
    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public String getSignatureUrl() {
        return signatureUrl;
    }

    public long getReleaseDate() {
        return releaseDate;
    }

    /**
     * Parse update information from JSON.
     *
     * @param json JSON object containing update information
     * @return UpdateInfo instance
     * @throws JSONException if JSON parsing fails
     */
    @NonNull
    public static UpdateInfo fromJson(@NonNull JSONObject json) throws JSONException {
        return new UpdateInfo(
                json.getString("version"),
                json.getInt("versionCode"),
                json.getString("downloadUrl"),
                json.optString("releaseNotes", ""),
                json.optLong("fileSize", 0),
                json.getString("sha256Checksum"),
                json.optString("signatureUrl", null),
                json.optLong("releaseDate", System.currentTimeMillis())
        );
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("versionCode", versionCode);
        json.put("downloadUrl", downloadUrl);
        json.put("releaseNotes", releaseNotes);
        json.put("fileSize", fileSize);
        json.put("sha256Checksum", sha256Checksum);
        if (signatureUrl != null) {
            json.put("signatureUrl", signatureUrl);
        }
        json.put("releaseDate", releaseDate);
        return json;
    }
}
