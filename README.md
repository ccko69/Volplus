# Vol+

A lightweight Android volume-control utility with a floating control panel, a persistent notification, and a Quick Settings tile.

Vol+ provides quick access to the device's main audio streams without opening the full Settings app. The floating panel can be opened from the app launcher or the Quick Settings tile. Opening the panel starts the persistent notification controls.

## Screenshots

### Quick Settings tile

<!-- Screenshot 1: Replace this image with a square screenshot of the Vol+ Quick Settings tile. Recommended size: 512 × 512 px (1:1). -->
![Vol+ Quick Settings tile](docs/images/quick-settings-tile.jpg)

### Notification controls

<!-- Screenshot 2: Replace this image with a horizontal screenshot of the compact Vol+ notification. Recommended size: 1200 × 400 px (3:1). -->
![Vol+ notification controls](docs/images/notification-controls.jpg)

### Floating volume panel

<!-- Screenshot 3: Replace this image with a wide screenshot of the Vol+ floating activity, preferably showing expanded controls. Recommended size: 1600 × 900 px (16:9). -->
![Vol+ floating volume panel](docs/images/floating-panel.jpg)

To add your screenshots:

1. Capture the three screens on your Android device.
2. Create the folder `docs/images/` in the repository if it does not exist.
3. Upload the images using these exact filenames:
   - `quick-settings-tile.png`
   - `notification-controls.png`
   - `floating-panel.png`
4. Commit and push. GitHub will render the images automatically in this README.

You can use different filenames, but update the corresponding image paths above. Crop the images to the recommended proportions; keep the app UI readable and avoid including private notifications or personal information.

## Features

- Floating volume panel with a compact main slider and expandable controls for additional audio streams.
- Persistent notification with volume percentage, progress indicator, mute, volume-up, volume-down, and close actions.
- Quick Settings tile for opening the floating volume panel.
- Synchronization with system volume changes while the notification service is running.
- Optional restoration of the notification service after device restart, subject to Android background-execution rules.
- Dark interface with black surfaces and a deep-red accent.

## Requirements

- Android 11 (API 30) or later.
- Android Studio or another environment with a compatible Android SDK for local builds.
- JDK 17 for Gradle builds.

The project uses Kotlin, Jetpack Compose, AndroidX, and Gradle.

## Install

### Build locally

On macOS/Linux:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it with Android Debug Bridge:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After installation, grant notification permission if prompted and add the Vol+ tile from Android's Quick Settings tile editor.

## Build APKs with GitHub Actions

This repository includes a workflow at `.github/workflows/android.yml`.

1. Push the project to a GitHub repository.
2. Open the repository's **Actions** tab and enable Actions if GitHub asks.
3. Push a commit to `main` or `master`, open a pull request targeting either branch, or run **Android APK** manually using **Run workflow**.
4. Open the completed workflow run.
5. Under **Artifacts**, download `volplus-debug-apk`. The APK is ready to install for testing.

The workflow also builds a signed release APK when the signing secrets described below are configured. Without those secrets, the debug APK build still runs and is uploaded.

### Configure signed release builds (optional)

Do not commit your keystore or passwords to GitHub.

In your local terminal, encode your existing release keystore as Base64:

macOS:

```bash
base64 -i volplus-signing-key.jks | tr -d '\n' > keystore.base64
```

Linux:

```bash
base64 -w 0 volplus-signing-key.jks > keystore.base64
```

Open `keystore.base64` and copy its entire contents. In GitHub, go to **Settings → Secrets and variables → Actions → New repository secret**. Create these four repository secrets:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | Base64 contents of your existing `.jks` keystore |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Existing key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

The workflow decodes the keystore into the temporary GitHub Actions runner and uses it to sign the release APK. The release artifact is uploaded as `volplus-release-apk`.

**Important:** Keep a secure offline backup of the original keystore and its passwords. If this app has already been distributed, use the same signing key for future updates. Losing or changing the key can prevent users from installing updates over the existing app.

## Permissions and Android behavior

Depending on Android version and device configuration, Vol+ may request notification permission and use a foreground service for its persistent notification. The app can also register for boot-completed events to restore a previously enabled service.

Android controls foreground-service starts, background execution, notification presentation, and battery restrictions. No ordinary app can guarantee survival after the user force-stops it, and manufacturer-specific battery managers may impose additional restrictions.

## Project layout

```text
.
├── app/
│   ├── src/main/
│   │   ├── kotlin/ccko/volplus/
│   │   │   ├── MainActivity.kt
│   │   │   ├── VolumeColors.kt
│   │   │   ├── VolumeControlService.kt
│   │   │   ├── VolumePlusBootReceiver.kt
│   │   │   └── VolumeTileService.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/android.yml
├── docs/images/
├── build.gradle.kts
├── gradle/
└── settings.gradle.kts
```
