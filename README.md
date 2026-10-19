# JARVIS Assistant

Open-source modular Android voice assistant. The initial app is a **voice MVP**, not a trained clone of the reference voice.

## Phase 1 features

- Russian speech through Android TextToSpeech (Google Play Services not required, device Russian TTS voice required).
- Input text, speak/stop, speed and pitch controls. Settings are saved locally.
- Manual **Check updates** via GitHub Releases, SHA-256 verified downloads, Android install confirmation.
- GitHub Actions continuous builds and unit tests.

## Build

Kotlin 2.2.20, Jetpack Compose, AGP 8.13.2, Gradle 8.13, Java 17, compile SDK 36, min SDK 26, target SDK 35.

Use Android Studio or install Java 17 / Android SDK 36 / Gradle 8.13 locally: `gradle testDebugUnitTest assembleDebug`. CI installs prerequisites automatically.

## Install APK

Open **Actions > Android CI > latest successful run > Artifacts > jarvis-debug-apk** for a *test* APK. This is **not** a production updater channel: separate debug builds are not guaranteed to share signing keys.

For stable updates configure 4 signing secrets according to [SECURITY.md](SECURITY.md), then push a tag `v0.1.0`. The release workflow generates `jarvis-assistant.apk` in GitHub Releases. For future versions, increment `versionCode` and `versionName`, tag e.g. `v0.1.1`, and use **Check updates** inside the installed app. Users must approve every Android installation.

Audio samples and signing credentials must not be committed into this public repository.

See [ARCHITECTURE.md](ARCHITECTURE.md), [ROADMAP.md](ROADMAP.md), [TESTING.md](TESTING.md).

## 0.2.1 Huawei background preview
The UI offers **Enable background voice**, which starts a persistent foreground service with an ongoing notification and a 20-second delayed voice test for lock-screen verification. Grant notification access first; grant app notification display permission on Android 13+. Some Huawei phones require manual app-launch and battery-optimization exceptions. The OS may still terminate this service, and hourly Android tick events are not guaranteed during Doze. No automatic boot startup.
