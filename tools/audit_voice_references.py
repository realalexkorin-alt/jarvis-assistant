#!/usr/bin/env python3
"""Offline reference audit. Private audio must never be committed to GitHub."""
import argparse,csv,io,math,wave,zipfile
from array import array
from pathlib import Path

def inspect(data):
    with wave.open(io.BytesIO(data),"rb") as w:
        ch,sr,sw,n=w.getnchannels(),w.getframerate(),w.getsampwidth(),w.getnframes()
        if sw!=2: raise ValueError("PCM16 required")
        pcm=array("h");pcm.frombytes(w.readframes(n))
    samples=pcm[::ch] if ch>1 else pcm
    peak=max(map(abs,samples),default=0)/32768
    rms=math.sqrt(sum(v*v for v in samples)/max(1,len(samples)))/32768
    clipped=sum(abs(v)>=32500 for v in samples)/max(1,len(samples))
    return {"duration_sec":round(n/sr,2),"rate":sr,"channels":ch,
        "rms_dbfs":round(20*math.log10(max(rms,1e-8)),1),
        "peak_dbfs":round(20*math.log10(max(peak,1e-8)),1),
        "clipped_percent":round(100*clipped,2)}

def main():
    p=argparse.ArgumentParser();p.add_argument("private_zip",type=Path)
    p.add_argument("--output",type=Path,default=Path("reference-quality.csv"))
    args=p.parse_args()
    with zipfile.ZipFile(args.private_zip) as z:
        rows=[]
        for name in sorted(x for x in z.namelist() if x.endswith(".wav")):
            stats=inspect(z.read(name))
            notes=[]
            if stats["duration_sec"]<3: notes.append("short_reference")
            if stats["clipped_percent"]>0.2: notes.append("clipped")
            rows.append({"file":name,**stats,"review_notes":";".join(notes),
              "verified_single_speaker":"","verified_clean_speech":"",
              "transcript":"","reuse_permission_confirmed":""})
    if not rows: raise SystemExit("No WAV files found")
    args.output.parent.mkdir(parents=True,exist_ok=True)
    with args.output.open("w",newline="",encoding="utf-8") as out:
        w=csv.DictWriter(out,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
    print(f"Analyzed {len(rows)} clips; no recordings uploaded.")

if __name__=="__main__":main()
