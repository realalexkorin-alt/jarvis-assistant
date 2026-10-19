# Architecture

- Core: preferences and application state.
- Voice: independent `VoiceEngine` interface and initial Android `TextToSpeech` adapter.
- Updates: GitHub Releases `latest` discovery, version comparison, SHA-256 asset digest verification, package ID check, Android Package Installer.
- UI: minimal Compose screen.
- NotificationEngine: Phase 2, **not yet implemented**.
- IntegrationEngine: Phase 3, **not yet implemented**.
- AI Agent: Phase 5, **not yet implemented**.

Device TTS support varies; lack of Russian speech data must be handled gracefully. The initial app does not listen to notifications or microphones. No source/audio is copied from the separate Taxi Assistant. Future providers will be optional, replaceable, privacy-first adapters.

## v0.2.1 background lifecycle
The Android system binds the notification listener; user separately starts a persistent foreground service (`specialUse`) for local voice and battery/time events. This service owns a dedicated TTS engine, queue, short bounded PARTIAL_WAKE_LOCK during each utterance, and registered system broadcasts. It shows an ongoing notification. It does not bypass device vendor battery controls, guarantee exact timing during Doze, or auto-start from BOOT_COMPLETED. The foreground service type requires an explanation/review if later published through Google Play.
