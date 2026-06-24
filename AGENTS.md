# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android project. App code lives under `app/src/main/java/com/example`, organized by role: `data` for Room entities, DAO, database, and Gemini service code; `repository` for data access coordination; `viewmodel` for UI state; and `ui/screens` plus `ui/theme` for Jetpack Compose screens and styling. Android resources are in `app/src/main/res`. Unit, Robolectric, and screenshot tests are in `app/src/test/java`; device or emulator instrumentation tests are in `app/src/androidTest/java`. Gradle version aliases are centralized in `gradle/libs.versions.toml`.

## Build, Test, and Development Commands

This repository does not currently commit a Gradle wrapper, so use Android Studio's Gradle integration or an installed `gradle` command. If a wrapper is added later, prefer `./gradlew`.

- `gradle :app:assembleDebug` builds a debug APK.
- `gradle :app:testDebugUnitTest` runs local JVM tests, including Robolectric and Roborazzi tests.
- `gradle :app:connectedDebugAndroidTest` runs instrumentation tests on a connected device or emulator.
- `gradle :app:assembleRelease` builds the release variant using signing values from environment variables or the configured keystore path.

For local AI features, create `.env` from `.env.example` and set `GEMINI_API_KEY`.

## Coding Style & Naming Conventions

Use Kotlin and Jetpack Compose idioms already present in the app. Match nearby formatting: generally 4-space indentation in Kotlin app sources and 2-space indentation in Kotlin Gradle scripts. Use PascalCase for classes and composables, camelCase for functions and properties, and descriptive suffixes such as `Screen`, `ViewModel`, `Repository`, `Entity`, and `Dao`. Keep Compose state flow through `LauncherViewModel` rather than adding screen-local business logic.

## Testing Guidelines

Name tests `*Test.kt`; reserve `*ScreenshotTest.kt` for Roborazzi visual coverage. Add local tests in `app/src/test/java` for repository, database, ViewModel, and Compose logic that can run on the JVM. Use `app/src/androidTest/java` only when device APIs or instrumentation are required. Run `gradle :app:testDebugUnitTest` before opening a PR.

## Commit & Pull Request Guidelines

Recent history uses short, conventional-style messages such as `chore: initialize android project structure`; prefer `type: concise imperative summary` (`fix:`, `feat:`, `test:`, `chore:`). Pull requests should include a short description, test results, linked issues when relevant, and screenshots or recordings for UI changes.

## Security & Configuration Tips

Do not commit `.env`, API keys, `debug.keystore`, release keystores, or signing passwords. Keep dependency versions in `gradle/libs.versions.toml` and document any new runtime permissions in `app/src/main/AndroidManifest.xml`.
