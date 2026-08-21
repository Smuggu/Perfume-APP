# ScentVault

A private, dark-themed Android app for tracking a fragrance collection: bottle photos, purchase dates, batch codes, notes, and searchable tags — plus one-tap backup/restore to a single `.zip` file.

## Features

- **Bottle photos** — capture with the camera or pick from the gallery; photos are downsampled, EXIF-rotated, and stored privately inside the app.
- **Purchase dates** — Material 3 date picker per bottle.
- **Batch codes** — free-text field for the batch/lot code printed on the bottle.
- **Notes** — free-text notes per bottle.
- **Searchable tags** — add any number of tags per bottle; filter the collection by tag and full-text search across name, brand, notes, batch code, and tags.
- **Backup/restore** — export the entire collection (database + photos) to a single `.zip` via Android's file picker (save to Drive, local storage, etc.), and restore it later on the same or a different device.
- **Dark UI** — a dedicated dark Material 3 theme throughout.

## Requirements

- Android Studio (Ladybug or newer recommended)
- A device or emulator running Android 8.0 (API 26) or newer — this targets Android 15 (API 35), which matches a Galaxy S24 Ultra on the latest OS updates.

## Building

This repo was authored outside Android Studio (in a sandboxed environment without access to the Android SDK), so it has **not** been built/compiled here. To build it:

```
git clone <this repo>
cd Perfume-APP
./gradlew assembleDebug
```

or simply open the project folder in Android Studio and let it sync — Android Studio will download the Android SDK components (compileSdk/targetSdk 35, build-tools) and Gradle dependencies automatically on first sync.

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`; install it on your S24 Ultra via `adb install` or by copying it to the device.

## Project layout

- `app/src/main/java/com/scentvault/app/data` — Room entities, DAOs, database, repository
- `app/src/main/java/com/scentvault/app/photo` — bottle photo storage (camera/gallery import, downsampling, EXIF rotation)
- `app/src/main/java/com/scentvault/app/backup` — zip export/import of the whole collection
- `app/src/main/java/com/scentvault/app/ui` — Jetpack Compose UI (list, detail/edit, settings) and the dark theme

## Privacy

Everything is stored locally on-device (Room database + app-private photo files). Nothing is uploaded anywhere. Backups are written only to a location you explicitly choose via Android's document picker.
