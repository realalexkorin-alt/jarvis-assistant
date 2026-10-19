# Architecture

- Core: preferences and application state.
- Voice: independent `VoiceEngine` interface and initial Android `TextToSpeech` adapter.
- Updates: GitHub Releases `latest` discovery, version comparison, SHA-256 asset digest verification, package ID check, Android Package Installer.
- UI: minimal Compose screen.
- NotificationEngine: Phase 2, **not yet implemented**.
- IntegrationEngine: Phase 3, **not yet implemented**.
- AI Agent: Phase 5, **not yet implemented**.

Device TTS support varies; lack of Russian speech data must be handled gracefully. The initial app does not listen to notifications or microphones. No source/audio is copied from the separate Taxi Assistant. Future providers will be optional, replaceable, privacy-first adapters.
