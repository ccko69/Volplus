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
