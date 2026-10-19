# JARVIS offline voice engine — implementation status

v0.2.2 is the **first architecture/build step**, not a neural-voice release.

Implemented:
- JARVIS-owned `LocalPcmSynthesisBackend` interface for future neural inference (PCM16 mono).
- `LocalPcmPlayer` using Android AudioTrack with cancellable worker; isolated from system TTS.
- Internal voice-model storage status, honest unavailability until a compatible inference runtime is connected.
- Offline Russian normalization for time and battery events, tested with JUnit.
- Existing RHVoice/system TTS continues handling real notifications and calls; Huawei background service is unchanged except safe text normalization.

Not implemented yet:
- Bundled ONNX Runtime / phonemizer / compatible neural model and verified model license.
- A trained or adapted version of the voice in the 16 private WAV recordings.
- Performance, power consumption and locked-screen quality verification for neural inference on Huawei.

Technical choice: begin with Piper-compatible small Russian VITS models via ONNX, avoiding server infrastructure. Candidate Android implementations: sherpa-onnx Android examples at https://github.com/k2-fsa/sherpa-onnx/tree/master/android/SherpaOnnxTtsEngine . A working Russian model example is https://k2-fsa.github.io/sherpa/onnx/tts/all/Russian/vits-piper-ru_RU-dmitri-medium.html . This is a research candidate, **not yet embedded** in JARVIS. Review each model's distribution terms before bundling.

Follow-up engineering gates:
1. Add actual ONNX model inference + Russian text-to-phoneme resources; validate model assets and metadata.
2. Test one offline sentence with no network permission, measure latency and memory on physical Huawei.
3. Route both foreground preview and notification service through local engine, keeping RHVoice fallback.
4. Evaluate timbre adaptation from legally usable reference audio without pushing private voice samples to public git.
