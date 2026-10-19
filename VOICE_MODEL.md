# JARVIS Voice Laboratory

**Status:** Reference-audio preparation verified locally. **Voice cloning/inference has not been executed or tested.** The working Android TTS and notifications are unchanged.

## Dataset (private)

- 16 user-supplied WAV clips, 40.73 seconds total.
- Input: stereo at 44.1 / 48 kHz, uneven level; a few samples reach near 0 dBFS peak.
- Prepared locally to 24 kHz / mono / signed 16-bit PCM, mild 65-Hz high-pass, per-clip gain cap to -2 dBFS.
- No pitch-shifting, voice enhancement, silence hallucination, manual transcriptions, denoising, or upload.
- Candidate continuous reference durations: #10 8.30 s, #09 5.50 s, #08 3.60 s, #12 3.36 s. **Length alone does not establish cleanliness.** Manually reject music, dialogue overlap or wrong speaker.
- A manifest with checksums and blank verification/transcript fields is generated outside git.

## Voice-model candidate

**Resemble Chatterbox Multilingual V3**, open source MIT, multilingual Russian support, voice-prompt conditioning. Docs: https://github.com/resemble-ai/chatterbox . Runtime is ~500M parameters; prefer a GPU-capable *private* inference environment to evaluate generation latency and quality. A model may require several seconds, and **there is no confirmed practical Huawei-native runtime**. The fallback remains device RHVoice.

A zero-shot prompt is not a trained personal model. Supervised fine-tuning requires substantially more **clean, single-speaker, transcribed** and authorized speech. Do not publish a copied recognizable entertainment voice for commercial use without rights.

## Reproducible local preparation

With Python 3 and FFmpeg:

```bash
python tools/prepare_voice_references.py path/to/private-originals voice-data/processed
```

The `voice-data/` path is ignored by git. The script formats audio and normalizes loudness only; it does not judge identity, remove background music or reconstruct dialogue.

## OPTIONAL Python / CUDA zero-shot evaluation

In a GPU Python 3.11 environment (model download and substantial free disk/RAM required):

```bash
pip install chatterbox-tts
python tools/try_chatterbox_v3.py --reference voice-data/processed/reference_10.wav
```

The test emits a new Russian sentence to `voice-output/ru-demo.wav`. Change `--reference` after listening for the cleanest speaking segment. **This inference command was prepared but not executed in this environment.** Never submit voice recordings to hosted services without explicit permission.

## Integration design (future, not yet deployed)

1. `VoiceEngine` continues to support on-device RHVoice for immediate/offline alerts.
2. A separate opt-in `RemoteTtsEngine` will request generated audio over authenticated HTTPS from a user-controlled inference service; no incoming notification bodies leave the phone without explicit approval.
3. Short nonsensitive fixed announcements (battery/time) may be cached locally. Private message contents are not cached.
4. If the remote model times out or is offline, local RHVoice speaks instead. Android foreground/background processes remain otherwise unchanged.
5. Compare Russian pronunciation, naturalness, timbre, generation time, audio routing and battery impact before choosing a model. No forced network dependency.
