#!/usr/bin/env python3
"""Record a complete Play promo: overlays+audio, azkar views, misbaha, home widget."""
from __future__ import annotations

import json
import shlex
import subprocess
import sys
import time
from pathlib import Path

import imageio_ffmpeg

sys.path.insert(0, str(Path(__file__).resolve().parent))
from capture_locale_screens import (  # noqa: E402
    DHIKR_TEXT,
    PKG,
    ROOT,
    adb,
    prep,
    tap_xy,
    wait,
)
from setup_promo_widgets import reset_launcher_home, split_widget_pages  # noqa: E402

DEVICE = "emulator-5554"
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
VIDEO_DIR = ROOT / "store-assets" / "video"
RAW = VIDEO_DIR / "promo-raw.mp4"
OUT = VIDEO_DIR / "promo.mp4"
MARKS = ROOT / ".tmp-export" / "promo-marks.json"
AUDIO_TASBIH = ROOT / "remote-content" / "audio" / "sou_tasbhamd.mp3"
AUDIO_AZKAR = ROOT / "remote-content" / "audio" / "azkar" / "qt_02.mp3"
AUDIO_TRAD = ROOT / "remote-content" / "audio" / "sou_tasbeeh.mp3"
AZKAR_LISTEN_SEC = 8.0
TRAD_LISTEN_SEC = 5.0
CALC_PKG = "org.fossify.math"
CALC_ACTIVITY = f"{CALC_PKG}/.activities.SplashActivity.Green"
CALC_APK = ROOT / ".tmp-export" / "calculator-promo.apk"
CALENDAR = "com.google.android.calendar/com.android.calendar.AllInOneActivity"
# 8, 5 on Fossify calculator (1080x2400) — short taps then overlay
CALC_KEYS = ((405, 1361), (405, 1640))
NAV_X_RTL = (945, 675, 405, 135)
NAV_Y = 2274
OVERLAY_CLOSE = (541, 1268)
AZKAR_EVENING = (898, 1141)
AZKAR_PLAY = (364, 636)
AZKAR_OK = (259, 1402)
AZKAR_NEXT = (540, 455)
TRAD_STYLE = (655, 951)
TRAD_PLAY = (153, 1447)
TRAD_BEAD = (540, 1550)


def ensure_calculator() -> bool:
    r = adb("shell", "pm", "path", CALC_PKG, check=False)
    if "package:" in (r.stdout or ""):
        return True
    if CALC_APK.exists():
        adb("install", "-r", str(CALC_APK), check=False)
        r = adb("shell", "pm", "path", CALC_PKG, check=False)
        return "package:" in (r.stdout or "")
    return False


def open_other_app() -> None:
    if ensure_calculator():
        adb("shell", "am", "start", "-n", CALC_ACTIVITY, check=False)
        wait(0.8)
        for x, y in CALC_KEYS:
            tap_xy(x, y)
            wait(0.12)
        return
    adb("shell", "am", "start", "-n", CALENDAR, check=False)
    wait(0.8)


def tap_nav_fast(index: int) -> None:
    tap_xy(NAV_X_RTL[index], NAV_Y)
    wait(0.55)


def show_tasbih_overlay() -> None:
    cmd = (
        f"am start -n {PKG}/.ui.overlay.OverlayActivity "
        f"--es extra_text {shlex.quote(DHIKR_TEXT)}"
    )
    adb("shell", cmd, check=False)
    wait(0.4)


def swipe_home_right() -> None:
    adb("shell", "input", "swipe", "900", "1100", "180", "1100", "320", check=False)
    wait(0.55)


def mark(events: list, name: str, t0: float) -> None:
    t = round(time.time() - t0, 2)
    events.append({"t": t, "name": name})
    print(f"{t:5.1f}s  {name}", flush=True)


def video_duration_sec(path: Path) -> float:
    p = subprocess.run([FFMPEG, "-i", str(path)], capture_output=True, text=True)
    for line in (p.stderr or "").splitlines():
        if "Duration:" in line:
            hms = line.split("Duration:")[1].split(",")[0].strip()
            h, m, s = hms.split(":")
            return int(h) * 3600 + int(m) * 60 + float(s)
    return 58.0


def mix_audio(events: list, src: Path, dest: Path) -> Path:
    by = {e["name"]: e["t"] for e in events}
    t_tasbih = max(0, int(by.get("tasbih_overlay", 3) * 1000))
    t_azkar_s = float(by.get("azkar_play", 22))
    t_misbaha_s = float(by.get("misbaha", 40))
    t_trad_s = float(by.get("trad_play", t_misbaha_s + 2))
    t_widgets_s = float(by.get("widget_dhikr", t_trad_s + 6))
    t_azkar = max(0, int(t_azkar_s * 1000))
    t_trad = max(0, int(t_trad_s * 1000))
    azkar_hold = max(1.5, min(AZKAR_LISTEN_SEC, t_misbaha_s - t_azkar_s - 0.4))
    trad_hold = max(1.5, min(TRAD_LISTEN_SEC, t_widgets_s - t_trad_s - 0.3))
    dur = video_duration_sec(src)
    dest.parent.mkdir(parents=True, exist_ok=True)
    tmp = dest.with_name("promo-audio.mp4")
    filt = (
        f"anullsrc=channel_layout=stereo:sample_rate=44100:d={dur:.2f}[base];"
        f"[1:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo,"
        f"atrim=0:4.5,asetpts=PTS-STARTPTS,volume=5,adelay={t_tasbih}|{t_tasbih}[a1];"
        f"[2:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo,"
        f"atrim=0:{azkar_hold:.2f},asetpts=PTS-STARTPTS,volume=10,"
        f"adelay={t_azkar}|{t_azkar}[a2];"
        f"[3:a]aformat=sample_fmts=fltp:sample_rates=44100:channel_layouts=stereo,"
        f"atrim=0:{trad_hold:.2f},asetpts=PTS-STARTPTS,volume=9,"
        f"adelay={t_trad}|{t_trad}[a3];"
        f"[base][a1][a2][a3]amix=inputs=4:duration=first:dropout_transition=0:normalize=0[a]"
    )
    cmd = [
        FFMPEG, "-y",
        "-i", str(src),
        "-i", str(AUDIO_TASBIH),
        "-i", str(AUDIO_AZKAR),
        "-stream_loop", "5", "-i", str(AUDIO_TRAD),
        "-filter_complex", filt,
        "-map", "0:v:0",
        "-map", "[a]",
        "-c:v", "copy",
        "-c:a", "aac",
        "-ar", "44100",
        "-b:a", "192k",
        "-t", f"{dur:.2f}",
        "-movflags", "+faststart",
        str(tmp),
    ]
    print(
        "ffmpeg mix tasbih_ms", t_tasbih,
        "azkar_ms", t_azkar, "azkar_hold", azkar_hold,
        "trad_ms", t_trad, "dur", dur,
        flush=True,
    )
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print(r.stderr[-5000:])
        raise SystemExit("mix failed")
    print("mixed audio", tmp, tmp.stat().st_size)
    return tmp


def record() -> None:
    VIDEO_DIR.mkdir(parents=True, exist_ok=True)
    MARKS.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/promo_full.mp4"
    adb("shell", "rm", "-f", remote, check=False)
    adb("shell", "media", "volume", "--stream", "3", "--set", "15", check=False)

    prep("ar", onboarding=True, reinstall=False)
    split_widget_pages()
    reset_launcher_home()
    adb("shell", "am", "force-stop", PKG, check=False)
    wait(0.6)

    rec = subprocess.Popen(
        ["adb", "-s", DEVICE, "shell", "screenrecord", "--bit-rate", "12000000", "--time-limit", "180", remote]
    )
    time.sleep(1.0)
    t0 = time.time()
    events = []
    mark(events, "record_start", t0)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    mark(events, "splash", t0)
    wait(2.4)
    mark(events, "home", t0)

    open_other_app()
    mark(events, "other_app", t0)
    show_tasbih_overlay()
    mark(events, "tasbih_overlay", t0)
    wait(2.2)
    tap_xy(*OVERLAY_CLOSE)
    wait(0.1)
    adb("shell", "input", "keyevent", "4", check=False)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    wait(0.5)

    tap_nav_fast(2)
    mark(events, "azkar_list", t0)
    wait(0.35)
    tap_xy(*AZKAR_EVENING)
    mark(events, "azkar_cards", t0)
    wait(0.65)
    tap_xy(*AZKAR_NEXT)
    wait(0.8)
    tap_xy(*AZKAR_PLAY)
    mark(events, "azkar_play", t0)
    wait(AZKAR_LISTEN_SEC)

    tap_nav_fast(1)
    mark(events, "misbaha", t0)
    wait(0.5)
    for _ in range(3):
        tap_xy(540, 1626)
        wait(0.22)
    tap_xy(*TRAD_STYLE)
    wait(0.75)
    tap_xy(*TRAD_PLAY)
    mark(events, "trad_play", t0)
    wait(0.35)
    for _ in range(5):
        tap_xy(*TRAD_BEAD)
        wait(0.32)
    wait(2.2)

    adb("shell", "input", "keyevent", "3", check=False)
    wait(0.5)
    swipe_home_right()
    mark(events, "widget_dhikr", t0)
    wait(2.3)
    swipe_home_right()
    mark(events, "widget_misbaha", t0)
    wait(4.2)

    adb("shell", "pkill", "-INT", "screenrecord", check=False)
    wait(2.5)

    rec.wait(timeout=90)
    wait(1.2)
    mark(events, "record_end", t0)
    MARKS.write_text(json.dumps(events, ensure_ascii=False, indent=2), encoding="utf-8")
    adb("pull", remote, str(RAW), check=False)
    print("raw", RAW.stat().st_size if RAW.exists() else 0)
    if RAW.exists() and RAW.stat().st_size > 10000:
        mix_audio(events, RAW, OUT)


if __name__ == "__main__":
    record()
