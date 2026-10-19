#!/usr/bin/env python3
"""Optional local GPU experiment; not required for the Android app."""
from __future__ import annotations
import argparse
from pathlib import Path

def main() -> None:
    p=argparse.ArgumentParser()
    p.add_argument("--reference", type=Path, required=True)
    p.add_argument("--output", type=Path, default=Path("voice-output/ru-demo.wav"))
    p.add_argument("--text", default="Сэр, вам поступило новое сообщение. Заряд батареи составляет 70 процентов.")
    p.add_argument("--device", choices=["cuda","cpu","mps"], default="cuda")
    a=p.parse_args()
    if not a.reference.is_file():
        p.error("Missing private reference WAV")
    import torchaudio as ta
    from chatterbox.mtl_tts import ChatterboxMultilingualTTS
    a.output.parent.mkdir(parents=True,exist_ok=True)
    model=ChatterboxMultilingualTTS.from_pretrained(device=a.device, t3_model="v3")
    audio=model.generate(a.text, language_id="ru", audio_prompt_path=str(a.reference))
    ta.save(str(a.output), audio, model.sr)
    print(f"Generated {a.output}; verify Russian pronunciation and speaker similarity manually.")

if __name__ == "__main__":
    main()
