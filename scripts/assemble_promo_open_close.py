#!/usr/bin/env python3
"""Build promo: short branded intro (logo + features), then recorded footage. No end card."""
from __future__ import annotations

import shutil
import subprocess
from pathlib import Path

import arabic_reshaper
import imageio_ffmpeg
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
VIDEO = ROOT / "store-assets" / "video"
SRC_AUDIO = VIDEO / "promo-audio.mp4"
SRC_FALLBACK = VIDEO / "promo.mp4"
OUT = VIDEO / "promo.mp4"
PLAY = ROOT / "store-assets" / "play-upload" / "promo.mp4"
SPLASH_SRC = ROOT / ".tmp-export" / "splash_4.png"
INTRO = VIDEO / "intro-card.png"
TMP = VIDEO / "promo-assembled.mp4"
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
FONTS = Path(r"C:\Windows\Fonts")
GREEN = "#1F5A3F"
W, H = 1080, 2400
OPEN_SEC = 10.0
MID_SKIP = 3.15
INTRO_MP4 = VIDEO / "intro.mp4"


def ar(text: str) -> str:
    return get_display(arabic_reshaper.reshape(text))


def feature_font(size: int) -> ImageFont.FreeTypeFont:
    for path in (
        FONTS / "tradbdo.ttf",
        FONTS / "trado.ttf",
        FONTS / "tahomabd.ttf",
    ):
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def make_intro_card(splash: Path, dest: Path) -> None:
    im = Image.open(splash).convert("RGB").resize((W, H), Image.Resampling.LANCZOS)
    draw = ImageDraw.Draw(im)
    fnt = feature_font(50)
    lines = [
        ar("أذان ومواقيت الصلاة"),
        ar("أذكار ما بعد كل فرض"),
        ar("تسبيح تلقائي فوق التطبيقات"),
        ar("يعمل دون إنترنت · بلا إعلانات"),
    ]
    y = 1455
    for text in lines:
        box = draw.textbbox((0, 0), text, font=fnt)
        tw = box[2] - box[0]
        draw.text(((W - tw) // 2, y), text, fill=GREEN, font=fnt)
        y += 78
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest, optimize=True)
    print("intro card", dest, "font", fnt.path if hasattr(fnt, "path") else "")


def duration_sec(path: Path) -> float:
    p = subprocess.run([FFMPEG, "-i", str(path)], capture_output=True, text=True)
    for line in (p.stderr or "").splitlines():
        if "Duration:" in line:
            hms = line.split("Duration:")[1].split(",")[0].strip()
            h, m, s = hms.split(":")
            return int(h) * 3600 + int(m) * 60 + float(s)
    return 46.0


def make_intro_video(card: Path, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        FFMPEG, "-y",
        "-loop", "1", "-framerate", "8", "-t", str(OPEN_SEC), "-i", str(card),
        "-vf", f"scale={W}:{H},setsar=1,format=yuv420p",
        "-c:v", "libx264", "-preset", "fast", "-crf", "20",
        "-pix_fmt", "yuv420p", "-r", "8",
        "-an",
        str(dest),
    ]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr[-3000:])
        raise SystemExit(r.returncode)
    print("intro video", dest, "sec", OPEN_SEC)


def assemble() -> None:
    src = SRC_AUDIO if SRC_AUDIO.exists() and SRC_AUDIO.stat().st_size > 10000 else SRC_FALLBACK
    make_intro_card(SPLASH_SRC, INTRO)
    make_intro_video(INTRO, INTRO_MP4)
    mid_full = duration_sec(src)
    mid = max(1.0, mid_full - MID_SKIP)
    total = OPEN_SEC + mid + 2.4
    filt = (
        f"[0:v]scale={W}:{H},fps=8,setsar=1,format=yuv420p,setpts=PTS-STARTPTS[v0];"
        f"[1:v]scale={W}:{H},fps=8,setsar=1,format=yuv420p,"
        f"trim=start={MID_SKIP},setpts=PTS-STARTPTS[v1];"
        f"[v0][v1]concat=n=2:v=1:a=0,tpad=stop_mode=clone:stop_duration=2.4[v];"
        f"[1:a]atrim=start={MID_SKIP},asetpts=PTS-STARTPTS,"
        f"adelay={int(OPEN_SEC * 1000)}:all=true,apad=whole_dur={total:.2f}[a]"
    )
    cmd = [
        FFMPEG, "-y",
        "-i", str(INTRO_MP4),
        "-i", str(src),
        "-filter_complex", filt,
        "-map", "[v]", "-map", "[a]",
        "-c:v", "libx264", "-preset", "fast", "-crf", "22",
        "-pix_fmt", "yuv420p", "-r", "8",
        "-c:a", "aac", "-ar", "44100", "-b:a", "192k",
        "-t", f"{total:.2f}",
        "-movflags", "+faststart",
        str(TMP),
    ]
    print("ffmpeg assemble", "src", src.name, "intro", OPEN_SEC, "mid", mid, "total", total)
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr[-4000:])
        raise SystemExit(r.returncode)
    targets = [
        OUT,
        PLAY,
        PLAY.with_name("promo-v2.mp4"),
        PLAY.with_name("promo-final.mp4"),
    ]
    for dest in targets:
        try:
            shutil.copy2(TMP, dest)
            print("copied", dest)
        except OSError:
            print("locked", dest)
    print("ok", TMP)


if __name__ == "__main__":
    assemble()
