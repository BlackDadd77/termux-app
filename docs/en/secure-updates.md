# Secure Update System

## Overview

The Termux app includes a secure update system that provides a safe and reliable way to check for, download, and install application updates. This system prioritizes security through HTTPS-only connections, cryptographic signature verification, and secure file handling.

## Features

### 1. Automatic Update Checking
- Periodic checks for new versions (configurable)
- Minimum 24-hour interval between checks
- Background operation with minimal battery impact

### 2. Secure Downloads
- HTTPS-only connections (HTTP URLs are rejected)
- Progress tracking during downloads
- Download resume capability
- Configurable file size limits (default: 500 MB)

### 3. Integrity Verification
- SHA-256 checksum verification of downloaded files
- Protection against corrupted or tampered downloads
- Automatic cleanup of failed downloads

### 4. User Control
- Manual update checking
- Update dismissal (skip a specific version)
- Enable/disable automatic update checks
- Custom update server configuration

## Architecture

The secure update system consists of several key components:

### Core Classes

1. **UpdateManager**: Main coordinator for the update system
   - Manages update preferences
   - Coordinates checking, downloading, and installation
   - Handles cleanup of old update files

2. **UpdateChecker**: Handles update availability checks
   - Queries update server for latest version
   - Parses update metadata
   - Compares with current app version

3. **UpdateDownloader**: Manages secure file downloads
   - HTTPS-only downloads with progress tracking
   - Automatic checksum verification
   - Temporary file handling

4. **UpdateInfo**: Data model for update metadata
   - Version information
   - Download URLs
   - Checksums and signatures
   - Release notes

5. **UpdateSecurityUtils**: Security utilities
   - SHA-256 checksum calculation and verification
   - URL security validation
   - Filename sanitization

6. **UpdateConfig**: Configuration constants
   - Update server URLs
   - Timeout values
   - File size limits
   - Preference keys

## Usage

### Basic Usage in Code

```java
// Create update manager
UpdateManager updateManager = new UpdateManager(context);

// Check for updates
updateManager.checkForUpdates(result -> {
    if (result.isSuccess() && result.isUpdateAvailable()) {
        UpdateInfo updateInfo = result.getUpdateInfo();
        // Show update notification to user
    }
});

// Download update
updateManager.downloadUpdate(updateInfo, new UpdateDownloader.DownloadCallback() {
    @Override
    public void onDownloadProgress(int progress) {
        // Update progress UI
    }

    @Override
    public void onDownloadComplete(File file) {
        // Install the update
        updateManager.installUpdate(file);
    }

    @Override
    public void onDownloadError(String error) {
        // Handle error
    }
});

// Clean up resources when done
// This should be called when the UpdateManager is no longer needed
updateManager.shutdown();
```

### Configuration

Update behavior can be configured through SharedPreferences:

- **auto_update_check**: Enable/disable automatic checks (default: true)
- **update_check_url**: Custom update server URL
- **dismissed_update_version**: Version code of dismissed update

## Security Considerations

### Transport Security
- All network connections use HTTPS with TLS 1.2+
- Certificate validation is enforced
- HTTP URLs are automatically rejected

### File Integrity
- SHA-256 checksums are **mandatory** for all downloads
- Downloads without valid checksums are rejected
- Downloaded files are stored in app-private storage
- Temporary files are cleaned up on failure

### Input Validation
- Filenames are sanitized to prevent directory traversal
- File size limits prevent resource exhaustion
- JSON parsing includes error handling

### Installation Security
- APK installation uses Android's secure package installer
- On Android N+, FileProvider prevents path traversal
- User confirmation required for installation

## Update Server Requirements

The update server should provide a JSON endpoint with the following format:

```json
{
  "version": "0.119.0",
  "versionCode": 119,
  "downloadUrl": "https://example.com/termux-app-v0.119.0.apk",
  "sha256Checksum": "abc123...",
  "releaseNotes": "Bug fixes and improvements",
  "fileSize": 45678900,
  "releaseDate": 1234567890000
}
```

**Important:** The `sha256Checksum` field is **mandatory**. Updates without a valid SHA-256 checksum will be rejected for security reasons. This ensures that all downloaded files can be verified for integrity and authenticity.

Alternatively, the system can parse GitHub release format. For GitHub releases, checksum files should be included as assets with the naming convention `<apk-filename>.sha256`.

## Testing

Unit tests are provided for core security functions:

- `UpdateSecurityUtilsTest`: Tests for security utilities
- `UpdateInfoTest`: Tests for update metadata parsing

Run tests with:
```bash
./gradlew test
```

## Future Enhancements

- GPG signature verification
- Delta updates for smaller downloads
- Automatic installation (for rooted devices)
- Update scheduling
- Multiple update channels (stable, beta, dev)

## Troubleshooting

### Update Check Fails
- Verify network connectivity
- Check if update server URL is correct
- Ensure HTTPS is used

### Download Fails
- Check available storage space
- Verify network stability
- Check if file size exceeds limits

### Checksum Verification Fails
- Re-download the update
- Report issue to update server administrator

### Installation Fails
- Ensure "Install from unknown sources" is enabled
- Check if sufficient storage is available
- Verify APK is not corrupted

## Privacy

The update system:
- Only contacts configured update servers
- Does not collect or transmit user data
- Does not track update adoption
- Respects user's privacy preferences
