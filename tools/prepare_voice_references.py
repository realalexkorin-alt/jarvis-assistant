#!/usr/bin/env python3
"""Private reference-audio normalization. Do not commit generated voice data."""
from __future__ import annotations
import argparse
import csv
import hashlib
import math
import subprocess
import wave
from array import array
from pathlib import Path

def normalize(path: Path, target_dbfs: float = -22.0, peak_limit: float = 0.88) -> tuple[float,float]:
    with wave.open(str(path), "rb") as wav:
        assert wav.getsampwidth() == 2 and wav.getnchannels() == 1
        sr = wav.getframerate()
        pcm = wav.readframes(wav.getnframes())
    samples = array("h")
    samples.frombytes(pcm)
    if not samples:
        return 0.,1.
    rms = math.sqrt(sum(v*v for v in samples) / len(samples)) / 32768
    peak = max(abs(v) for v in samples) / 32768
    gain = min(10 ** (target_dbfs/20) / rms, peak_limit/peak) if rms and peak else 1.
    clean = array("h", (max(-32768, min(32767, round(v * gain))) for v in samples))
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1); wav.setsampwidth(2); wav.setframerate(sr)
        wav.writeframes(clean.tobytes())
    return rms,gain

def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("source_dir", type=Path)
    p.add_argument("output_dir", type=Path)
    args = p.parse_args()
    if args.source_dir.resolve() == args.output_dir.resolve():
        p.error("source and output directories must be different")
    args.output_dir.mkdir(parents=True, exist_ok=True)
    originals = sorted(args.source_dir.glob("*.wav"))
    if not originals:
        p.error("No WAV clips found")
    records=[]
    for index,source in enumerate(originals,1):
        with wave.open(str(source), "rb") as wav:
            duration=wav.getnframes()/wav.getframerate()
            rate=wav.getframerate()
            channels=wav.getnchannels()
        out=args.output_dir / f"reference_{index:02d}.wav"
        subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-y","-i",
            str(source),"-vn","-ac","1","-ar","24000","-af","highpass=f=65",
            "-c:a","pcm_s16le",str(out)],check=True)
        rms,gain=normalize(out)
        records.append({
            "id":f"reference_{index:02d}",
            "source_sha256":hashlib.sha256(source.read_bytes()).hexdigest(),
            "duration_s":round(duration,3),
            "source_sample_rate":rate,
            "source_channels":channels,
            "rms_dbfs":round(20*math.log10(max(rms,1e-9)),2),
            "normalization_gain_db":round(20*math.log10(max(gain,1e-9)),2),
            "transcript":"",
            "single_speaker_confirmed":"",
            "clean_speech_confirmed":"",
            "rights_confirmed":"",
        })
    with (args.output_dir/"manifest.csv").open("w",encoding="utf-8",newline="") as target:
        writer=csv.DictWriter(target,fieldnames=list(records[0]))
        writer.writeheader();writer.writerows(records)
    print(f"Prepared {len(records)} clips, {sum(r['duration_s'] for r in records):.2f} seconds")
    print("Review each clip by ear before use as a voice prompt; keep source and output private.")

if __name__ == "__main__":
    main()
