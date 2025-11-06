package com.termux.app.update;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for UpdateInfo.
 */
public class UpdateInfoTest {

    @Test
    public void testUpdateInfoCreation() {
        UpdateInfo info = new UpdateInfo(
                "1.0.0",
                100,
                "https://example.com/app.apk",
                "Release notes",
                1024000L,
                "abc123def456",
                "https://example.com/app.apk.sig",
                System.currentTimeMillis()
        );

        assertEquals("1.0.0", info.getVersion());
        assertEquals(100, info.getVersionCode());
        assertEquals("https://example.com/app.apk", info.getDownloadUrl());
        assertEquals("Release notes", info.getReleaseNotes());
        assertEquals(1024000L, info.getFileSize());
        assertEquals("abc123def456", info.getSha256Checksum());
        assertEquals("https://example.com/app.apk.sig", info.getSignatureUrl());
    }

    @Test
    public void testFromJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", "1.0.0");
        json.put("versionCode", 100);
        json.put("downloadUrl", "https://example.com/app.apk");
        json.put("releaseNotes", "Release notes");
        json.put("fileSize", 1024000L);
        json.put("sha256Checksum", "abc123def456");
        json.put("signatureUrl", "https://example.com/app.apk.sig");
        json.put("releaseDate", 1234567890000L);

        UpdateInfo info = UpdateInfo.fromJson(json);

        assertEquals("1.0.0", info.getVersion());
        assertEquals(100, info.getVersionCode());
        assertEquals("https://example.com/app.apk", info.getDownloadUrl());
        assertEquals("Release notes", info.getReleaseNotes());
        assertEquals(1024000L, info.getFileSize());
        assertEquals("abc123def456", info.getSha256Checksum());
        assertEquals("https://example.com/app.apk.sig", info.getSignatureUrl());
        assertEquals(1234567890000L, info.getReleaseDate());
    }

    @Test
    public void testFromJsonWithOptionalFields() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", "1.0.0");
        json.put("versionCode", 100);
        json.put("downloadUrl", "https://example.com/app.apk");
        json.put("sha256Checksum", "abc123def456");

        UpdateInfo info = UpdateInfo.fromJson(json);

        assertEquals("1.0.0", info.getVersion());
        assertEquals(100, info.getVersionCode());
        assertEquals("https://example.com/app.apk", info.getDownloadUrl());
        assertEquals("", info.getReleaseNotes());
        assertEquals(0L, info.getFileSize());
        assertEquals("abc123def456", info.getSha256Checksum());
        assertNull(info.getSignatureUrl());
        assertTrue(info.getReleaseDate() > 0);
    }

    @Test
    public void testToJson() throws JSONException {
        UpdateInfo info = new UpdateInfo(
                "1.0.0",
                100,
                "https://example.com/app.apk",
                "Release notes",
                1024000L,
                "abc123def456",
                "https://example.com/app.apk.sig",
                1234567890000L
        );

        JSONObject json = info.toJson();

        assertEquals("1.0.0", json.getString("version"));
        assertEquals(100, json.getInt("versionCode"));
        assertEquals("https://example.com/app.apk", json.getString("downloadUrl"));
        assertEquals("Release notes", json.getString("releaseNotes"));
        assertEquals(1024000L, json.getLong("fileSize"));
        assertEquals("abc123def456", json.getString("sha256Checksum"));
        assertEquals("https://example.com/app.apk.sig", json.getString("signatureUrl"));
        assertEquals(1234567890000L, json.getLong("releaseDate"));
    }

    @Test
    public void testToJsonWithoutSignatureUrl() throws JSONException {
        UpdateInfo info = new UpdateInfo(
                "1.0.0",
                100,
                "https://example.com/app.apk",
                "Release notes",
                1024000L,
                "abc123def456",
                null,
                1234567890000L
        );

        JSONObject json = info.toJson();

        assertEquals("1.0.0", json.getString("version"));
        assertFalse(json.has("signatureUrl"));
    }

    @Test(expected = JSONException.class)
    public void testFromJsonWithMissingRequiredField() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("version", "1.0.0");
        // Missing versionCode, downloadUrl, and sha256Checksum

        UpdateInfo.fromJson(json);
    }
}
