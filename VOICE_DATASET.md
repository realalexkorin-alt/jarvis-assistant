# Reference voice recordings

The user has supplied 16 stereo WAV audio clips (~40.73 seconds) at 44.1 / 48 kHz. This is not enough to claim a production-quality trained voice. WAV files are excluded from the public repository.

v0.1.0 uses Android TTS. The modular voice interface allows introducing a separate local or cloud engine after checking licensing / permission to use reference voice, preparing aligned transcripts and evaluating synthesized Russian speech. The app does not upload sample recordings to any server.

## Voice lab update, 2026-10-19
The locally processed user clips are private and are not stored in GitHub; format is 24-kHz, mono, PCM16. Audio integrity, duration, loudness and codec were checked automatically. Content, speaker identity, transcripts and rights have **not** been established automatically. See [VOICE_MODEL.md](VOICE_MODEL.md).
