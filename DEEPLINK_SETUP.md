# Deep Linking Setup Instructions

## Overview
Deep linking has been implemented for the iBurn Android app, allowing users to share and open specific content through URLs.

## Supported URL Formats

### Custom Scheme
- `iburn://art?uid={playaId}&title={title}`
- `iburn://camp?uid={playaId}&title={title}`
- `iburn://event?uid={playaId}&title={title}`
- `iburn://pin?lat={latitude}&lng={longitude}&title={title}`

### App Links (HTTPS)
- `https://iburnapp.com/art/?uid={playaId}&title={title}`
- `https://iburnapp.com/camp/?uid={playaId}&title={title}`
- `https://iburnapp.com/event/?uid={playaId}&title={title}`
- `https://iburnapp.com/pin?lat={latitude}&lng={longitude}&title={title}`

## Setup Steps

### 1. Get Your App's SHA256 Certificate Fingerprint

For debug builds:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA256
```

For release builds:
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-key-alias | grep SHA256
```

### 2. Update assetlinks.json

Replace `REPLACE_WITH_YOUR_APP_SIGNING_CERTIFICATE_SHA256_FINGERPRINT` in the `assetlinks.json` file with your actual SHA256 fingerprint.

The fingerprint should look like:
```
FA:C6:17:45:DC:09:03:78:6F:B9:ED:E6:2A:96:2B:39:9F:73:48:F0:BB:6F:89:9B:83:32:66:75:91:03:3B:9C
```

### 3. Deploy assetlinks.json to Website

The `assetlinks.json` file must be hosted at:
`https://iburnapp.com/.well-known/assetlinks.json`

Copy the file to the website repository:
```bash
cp assetlinks.json ../iburnapp.github.io/.well-known/
```

Make sure the file is served with:
- Content-Type: `application/json`
- HTTPS only
- No redirects

### 4. Testing Deep Links

#### Test with ADB

Test custom scheme:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "iburn://art?uid=a2Id0000000cbObEAI&title=Test%20Art"
```

Test app links:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "https://iburnapp.com/camp/?uid=a1XVI000001vN7N&title=Test%20Camp"
```

Test pin creation:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "iburn://pin?lat=40.7868&lng=-119.2068&title=Test%20Pin"
```

#### Verify App Links Status

Check if App Links are verified:
```bash
adb shell pm get-app-links com.gaiagps.iburn
```

Expected output should show:
```
com.gaiagps.iburn:
    ID: [some-id]
    Signatures: [your-signature]
    Domain verification state:
      iburnapp.com: verified
```

### 5. Share Functionality

Users can share items from the detail view using the share button in the action bar. This generates a deep link URL that includes metadata about the item.

## Troubleshooting

### App Links Not Auto-Verifying
1. Check that assetlinks.json is accessible at the correct URL
2. Verify the SHA256 fingerprint matches your signing certificate
3. Ensure `android:autoVerify="true"` is set in AndroidManifest.xml
4. Check network connectivity during app installation
5. Uninstall and reinstall the app after deploying assetlinks.json

### Deep Links Not Opening App
1. Verify intent filters match URL patterns
2. Check that MainActivity is exported (`android:exported="true"`)
3. Test with explicit package in intent
4. Check logs for any errors in DeepLinkHandler

### Database Migration Issues
If the app crashes after update:
1. Check migration SQL syntax in PlayaDatabase2.kt
2. Verify column types match entity fields
3. Consider clearing app data for testing

## Implementation Details

### New Classes
- `DeepLinkHandler.kt` - Handles URI parsing and routing
- `MapPin.kt` - Entity for custom map pins
- `MapPinDao.kt` - DAO for map pin operations
- `ShareUrlBuilder.kt` - Generates share URLs

### Modified Files
- `AndroidManifest.xml` - Added intent filters
- `MainActivity.java` - Added deep link handling
- `PlayaDatabase2.kt` - Added MapPin entity and migration
- `PlayaItemViewActivity.java` - Added share functionality
- `activity_playa_item.xml` - Added share menu item

### Database Changes
- Database version incremented from 1 to 2
- Added `map_pins` table for custom pins
- Migration handles upgrade from v1 to v2

## Next Steps

1. Deploy assetlinks.json to the website
2. Test all deep link scenarios
3. Consider adding QR code generation for sharing
4. Add analytics to track deep link usage
5. Implement map centering functionality in MapboxMapFragment