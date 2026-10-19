# Verification

CI runs `testDebugUnitTest` and `assembleDebug` and uploads a debug APK. The release job runs the same unit tests and signed `assembleRelease` (only after signing secrets are added).

**Device tests not yet performed:** install on Huawei Android; Russian speech availability; play, stop and saved rate/pitch; absence of Russian TTS; no-internet update check; unknown-app installation permission; actual signed update from v0.1.0 to v0.1.1; Bluetooth routing; foreground/background lifecycle.

Passing CI does not mean the real Android device was tested.

## Huawei background preview v0.2.1
- Auto-test: Android CI unit tests and APK compilation only.
- Manual: grant Notification listener access, launch persistent voice service from UI, confirm ongoing system notification.
- Tap 20-second delayed voice test and lock screen immediately; confirm audio plays without opening the application.
- Send a selected-app message while locked and confirm reading according to privacy toggle.
- Recheck after 10 minutes screen off and after charge drops across a 10% threshold; confirm hourly announcement near the hour (may be missed/delayed by Doze).
- Huawei: Settings > Battery > App launch > manage manually, allow all startup/background switches, and exempt JARVIS from battery optimization.
- Confirm stop button releases ongoing notification, service and partial wake lock. No real-device tests have been performed by CI.
