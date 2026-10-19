# Verification

CI runs `testDebugUnitTest` and `assembleDebug` and uploads a debug APK. The release job runs the same unit tests and signed `assembleRelease` (only after signing secrets are added).

**Device tests not yet performed:** install on Huawei Android; Russian speech availability; play, stop and saved rate/pitch; absence of Russian TTS; no-internet update check; unknown-app installation permission; actual signed update from v0.1.0 to v0.1.1; Bluetooth routing; foreground/background lifecycle.

Passing CI does not mean the real Android device was tested.
