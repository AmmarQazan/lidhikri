#!/usr/bin/env python3
"""Record a settings-focused Play promo: hub, tasbih, azkar, display, permissions, widgets."""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
import time
from pathlib import Path

import imageio_ffmpeg

sys.path.insert(0, str(Path(__file__).resolve().parent))
from capture_locale_screens import (  # noqa: E402
    PKG,
    ROOT,
    adb,
    back,
    ensure_app,
    prep,
    tap_nav,
    tap_text,
    tap_xy,
    wait,
)

DEVICE = "emulator-5554"
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
VIDEO_DIR = ROOT / "store-assets" / "video"
RAW = VIDEO_DIR / "promo-settings-raw.mp4"
OUT = VIDEO_DIR / "promo-settings-audio.mp4"
MARKS = ROOT / ".tmp-export" / "promo-settings-marks.json"
AUDIO_TASBIH = ROOT / "remote-content" / "audio" / "sou_tasbhamd.mp3"


def mark(events: list, name: str, t0: float) -> None:
    t = round(time.time() - t0, 2)
    events.append({"t": t, "name": name})
    print(f"{t:5.1f}s  {name}", flush=True)


def swipe(x1: int, y1: int, x2: int, y2: int, ms: int = 420) -> None:
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms), check=False)
    wait(0.7)


def video_duration_sec(path: Path) -> float:
    p = subprocess.run([FFMPEG, "-i", str(path)], capture_output=True, text=True)
    for line in (p.stderr or "").splitlines():
        if "Duration:" in line:
            hms = line.split("Duration:")[1].split(",")[0].strip()
            h, m, s = hms.split(":")
            return int(h) * 3600 + int(m) * 60 + float(s)
    return 50.0


def mix_audio(events: list, src: Path, dest: Path) -> Path:
    by = {e["name"]: e["t"] for e in events}
    t_tasbih = max(0, int(by.get("tasbih_settings", 8) * 1000))
    dur = video_duration_sec(src)
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_name("promo-settings-audio.mp4")
    cmd = [
        FFMPEG, "-y",
        "-i", str(src),
        "-i", str(AUDIO_TASBIH),
        "-filter_complex",
        (
            f"[1:a]aformat=channel_layouts=stereo,atrim=0:5,asetpts=PTS-STARTPTS,"
            f"adelay={t_tasbih}:all=true,volume=2.8,apad=whole_dur={dur:.2f}[a]"
        ),
        "-map", "0:v",
        "-map", "[a]",
        "-c:v", "copy",
        "-c:a", "aac",
        "-ar", "44100",
        "-b:a", "192k",
        "-t", f"{dur:.2f}",
        "-movflags", "+faststart",
        str(tmp),
    ]
    print("ffmpeg mix tasbih_ms", t_tasbih, "dur", dur, flush=True)
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print((r.stderr or "")[-3000:])
        print("mix failed, using silent video")
        return src
    print("mixed", tmp, tmp.stat().st_size)
    return tmp


def record() -> None:
    VIDEO_DIR.mkdir(parents=True, exist_ok=True)
    MARKS.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/promo_settings.mp4"
    adb("shell", "rm", "-f", remote, check=False)
    adb("shell", "media", "volume", "--stream", "3", "--set", "15", check=False)

    prep("ar", onboarding=True, reinstall=False)
    adb("shell", "am", "force-stop", PKG, check=False)
    wait(0.8)

    rec = subprocess.Popen(
        ["adb", "-s", DEVICE, "shell", "screenrecord", "--bit-rate", "12000000", "--time-limit", "180", remote]
    )
    t0 = time.time()
    events = []
    mark(events, "record_start", t0)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    mark(events, "splash", t0)
    wait(2.5)

    tap_nav("ar", 3)
    mark(events, "settings_hub", t0)
    wait(2.4)

    if not tap_text("التسبيح التلقائي", retries=2):
        tap_xy(540, 520)
        wait(1.4)
    mark(events, "tasbih_settings", t0)
    wait(2.2)
    swipe(540, 1650, 540, 780, 500)
    wait(1.8)
    swipe(540, 1650, 540, 780, 500)
    wait(2.0)
    back(1)
    wait(0.8)

    if not tap_text("الأذكار التلقائية", retries=2):
        tap_xy(540, 680)
        wait(1.4)
    mark(events, "azkar_settings", t0)
    wait(2.4)
    swipe(540, 1650, 540, 850, 450)
    wait(2.0)
    back(1)
    wait(0.8)

    if not tap_text("العرض والقراءة", retries=2):
        tap_xy(540, 840)
        wait(1.4)
    mark(events, "display_settings", t0)
    wait(1.6)
    tap_text("ليلي", retries=1)
    wait(1.8)
    tap_text("نهاري", retries=1)
    wait(1.2)
    swipe(540, 1650, 540, 900, 400)
    wait(1.6)
    back(1)
    wait(0.8)

    if not tap_text("التطبيق والأذونات", retries=2):
        tap_xy(540, 1000)
        wait(1.4)
    mark(events, "general_settings", t0)
    wait(2.2)
    swipe(540, 1650, 540, 800, 450)
    wait(2.2)
    back(1)
    wait(0.8)

    if not tap_text("ويدجت الشاشة", retries=2):
        tap_xy(540, 1320)
        wait(1.4)
    mark(events, "widgets_settings", t0)
    wait(2.6)
    tap_text("ذكر اليوم", retries=1)
    wait(2.2)

    adb("shell", "pkill", "-INT", "screenrecord", check=False)
    wait(2.5)
    rec.wait(timeout=90)
    wait(1.0)
    mark(events, "record_end", t0)
    MARKS.write_text(json.dumps(events, ensure_ascii=False, indent=2), encoding="utf-8")
    adb("pull", remote, str(RAW), check=False)
    print("raw", RAW.stat().st_size if RAW.exists() else 0)
    if RAW.exists() and RAW.stat().st_size > 10000:
        mixed = mix_audio(events, RAW, OUT)
        if mixed != OUT:
            try:
                shutil.copy2(mixed, OUT)
            except OSError:
                print("left mixed at", mixed)


if __name__ == "__main__":
    record()
