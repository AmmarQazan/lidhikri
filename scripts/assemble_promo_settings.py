#!/usr/bin/env python3
"""Assemble settings promo: short branded intro, then recorded settings tour. No end card."""
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
SRC = VIDEO / "promo-settings-audio.mp4"
SRC_FALLBACK = VIDEO / "promo-settings-raw.mp4"
OUT = VIDEO / "promo-settings.mp4"
PLAY = ROOT / "store-assets" / "play-upload" / "promo-settings.mp4"
SPLASH_SRC = ROOT / ".tmp-export" / "splash_4.png"
INTRO = VIDEO / "intro-settings.png"
TMP = VIDEO / "promo-settings-assembled.mp4"
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
FONTS = Path(r"C:\Windows\Fonts")
GREEN = "#1F5A3F"
W, H = 1080, 2400
OPEN_SEC = 1.5
MID_SKIP = 2.5


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
    fnt = feature_font(48)
    lines = [
        ar("فترة التسبيح والصوت"),
        ar("جدولة الأذكار اليومية"),
        ar("الخط والوضع الليلي"),
        ar("ويدجت والظهور فوق التطبيقات"),
    ]
    y = 1458
    for text in lines:
        box = draw.textbbox((0, 0), text, font=fnt)
        tw = box[2] - box[0]
        draw.text(((W - tw) // 2, y), text, fill=GREEN, font=fnt)
        y += 76
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest, optimize=True)
    print("intro card", dest)


def duration_sec(path: Path) -> float:
    p = subprocess.run([FFMPEG, "-i", str(path)], capture_output=True, text=True)
    for line in (p.stderr or "").splitlines():
        if "Duration:" in line:
            hms = line.split("Duration:")[1].split(",")[0].strip()
            h, m, s = hms.split(":")
            return int(h) * 3600 + int(m) * 60 + float(s)
    return 50.0


def assemble() -> None:
    src = SRC if SRC.exists() and SRC.stat().st_size > 10000 else SRC_FALLBACK
    make_intro_card(SPLASH_SRC, INTRO)
    mid_full = duration_sec(src)
    mid = max(1.0, mid_full - MID_SKIP)
    total = OPEN_SEC + mid
    filt = (
        f"[0:v]scale={W}:{H},fps=8,setsar=1,format=yuv420p,"
        f"trim=duration={OPEN_SEC},setpts=PTS-STARTPTS[v0];"
        f"[1:v]scale={W}:{H},fps=8,setsar=1,format=yuv420p,"
        f"trim=start={MID_SKIP},setpts=PTS-STARTPTS[v1];"
        f"[v0][v1]concat=n=2:v=1:a=0[v];"
        f"[1:a]atrim=start={MID_SKIP},asetpts=PTS-STARTPTS,"
        f"adelay={int(OPEN_SEC * 1000)}:all=true,apad=whole_dur={total:.2f}[a]"
    )
    cmd = [
        FFMPEG, "-y",
        "-loop", "1", "-t", str(OPEN_SEC), "-i", str(INTRO),
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
    print("ffmpeg assemble", "src", src.name, "mid", mid, "total", total)
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print((r.stderr or "")[-4000:])
        raise SystemExit(r.returncode)
    PLAY.parent.mkdir(parents=True, exist_ok=True)
    try:
        shutil.copy2(TMP, OUT)
    except OSError:
        print("promo-settings.mp4 locked, assembled file is", TMP)
    try:
        shutil.copy2(TMP, PLAY)
    except OSError:
        alt = PLAY.with_name("promo-settings-new.mp4")
        shutil.copy2(TMP, alt)
        print("play copy locked, wrote", alt)
    print("ok", OUT if OUT.exists() else TMP)


if __name__ == "__main__":
    assemble()
