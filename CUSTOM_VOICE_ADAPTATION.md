# JARVIS custom voice: adaptation gate

The deployed offline VITS/Dmitri model is a **generic Russian voice**, not yet the intended JARVIS speaker. The 16 private references are NOT public GitHub assets.

## Completed, reproducible
- Offline 16-clip PCM audit: 40.73 seconds total, 24 kHz mono normalization.
- Longer initial candidate references: #10 (8.30s), #09 (5.50s), #08 (3.60s), #12 (3.36s). Duration and signal only; listening and transcript review remain mandatory.
- Run `python3 tools/audit_voice_references.py PRIVATE_REFERENCES.zip --output voice-output/quality.csv` and mark each clip's speaker, transcript, cleanliness and reuse permission locally.

## Adaptation plan
1. Manually confirm that clean speech belongs to one speaker and that its use is authorized. Remove dialogue overlap, music and heavy processing. Do not claim all clips pass just because RMS/clipping checks pass.
2. Compare privately generated **new** Russian sentences through a reference-conditioned model (Chatterbox Multilingual V3 research candidate). This stage requires separate compatible GPU/runtime and is not accomplished by the fixed-speaker Piper model.
3. Evaluate phonetics, latency, speaker similarity and artifacting *before* exporting a deployable model.
4. Distill or select a mobile-compatible format and test it on Huawei CPU under lock-screen/background conditions.
5. Retain selectable fixed-speaker offline VITS and RHVoice fallbacks until custom on-device inference is truly validated.

**Current blocker**: no GPU voice-cloning inference executed on these references; there is no custom adapted voice model to ship yet. Never publish a 'custom JARVIS' APK that only changes text or pitch of the generic Russian model.
