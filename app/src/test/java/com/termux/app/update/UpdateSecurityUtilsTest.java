package com.termux.app.update;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Unit tests for UpdateSecurityUtils.
 */
public class UpdateSecurityUtilsTest {

    @Test
    public void testIsSecureUrl() {
        assertTrue(UpdateSecurityUtils.isSecureUrl("https://example.com"));
        assertTrue(UpdateSecurityUtils.isSecureUrl("HTTPS://example.com"));
        assertFalse(UpdateSecurityUtils.isSecureUrl("http://example.com"));
        assertFalse(UpdateSecurityUtils.isSecureUrl("ftp://example.com"));
        assertFalse(UpdateSecurityUtils.isSecureUrl("file:///path/to/file"));
    }

    @Test
    public void testSanitizeFilename() {
        assertEquals("test.apk", UpdateSecurityUtils.sanitizeFilename("test.apk"));
        assertEquals("test.apk", UpdateSecurityUtils.sanitizeFilename("../test.apk"));
        assertEquals("test.apk", UpdateSecurityUtils.sanitizeFilename("../../test.apk"));
        assertEquals("test.apk", UpdateSecurityUtils.sanitizeFilename("/path/to/test.apk"));
        assertEquals("test.apk", UpdateSecurityUtils.sanitizeFilename("path\\to\\test.apk"));
        assertEquals("test.apk", UpdateSecurityUtils.sanitizeFilename("  test.apk  "));
    }

    @Test
    public void testCalculateSHA256() throws IOException {
        // Create a temporary file with known content
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("Hello, World!".getBytes());
        }

        String sha256 = UpdateSecurityUtils.calculateSHA256(tempFile);
        assertNotNull(sha256);
        
        // SHA-256 of "Hello, World!" is known
        assertEquals("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f", 
                sha256.toLowerCase());
    }

    @Test
    public void testVerifySHA256() throws IOException {
        // Create a temporary file with known content
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("Hello, World!".getBytes());
        }

        String correctChecksum = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";
        String incorrectChecksum = "0000000000000000000000000000000000000000000000000000000000000000";

        assertTrue(UpdateSecurityUtils.verifySHA256(tempFile, correctChecksum));
        assertFalse(UpdateSecurityUtils.verifySHA256(tempFile, incorrectChecksum));
    }

    @Test
    public void testVerifySHA256WithNonExistentFile() {
        File nonExistent = new File("/tmp/nonexistent-file-12345.txt");
        assertFalse(UpdateSecurityUtils.verifySHA256(nonExistent, "anyChecksum"));
    }
}
