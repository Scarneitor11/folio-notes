# Folio Notes

Folio is a local-first notes app for Android, built with Kotlin and Jetpack Compose. It pairs a quiet, paper-like editor with a flexible home feed: markdown-lite formatting, preview mode, search and replace, checklists, templates, tags, pinning, archiving, trash, and JSON backup/restore.

## Requirements

- Android 6.0 (API 23) or newer
- JDK 17
- Android SDK 36

The app has no native libraries, so a single APK runs on ARM, ARM64, and x86 devices.

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease
```

Release builds require signing configuration at `~/.folio-signing/keystore.properties` (or the path in `FOLIO_KEYSTORE_PROPERTIES`). Signing files and `local.properties` are intentionally ignored by Git.

## Privacy

- Notes are stored only in the app's private SQLite database.
- The app requests only the vibration permission; it has no network permission and no analytics.
- Android backup is disabled, and backup/restore is user-controlled through exported JSON.

## Verification

The test suite covers editing operations, list continuation, templates, search/replace, persistence, backup codecs, responsive layout, navigation, and motion helpers. Lint runs with `abortOnError = true`, and release builds use R8 with resource shrinking.
