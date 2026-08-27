#!/usr/bin/env python3
"""Record promo video from a running emulator without reinstalling."""
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
DEVICE = "emulator-5554"
VIDEO = Path(__file__).resolve().parents[1] / "store-assets" / "video" / "promo.mp4"


def adb(*a):
    subprocess.run(["adb", "-s", DEVICE, *a], check=False, capture_output=True)


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def wait(s):
    time.sleep(s)


def overlay(text, section=None, auto=False):
    args = [
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", text,
    ]
    if section:
        args += ["--es", "extra_section_title", section]
    if auto:
        args += ["--ez", "extra_auto_azkar", "true"]
    adb(*args)


def main():
    VIDEO.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/st_promo.mp4"
    adb("shell", "rm", "-f", remote)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(2)
    tap(953, 2274)  # home
    wait(1)

    p = subprocess.Popen(["adb", "-s", DEVICE, "shell", "screenrecord", "--time-limit", "45", remote])
    wait(2)
    overlay("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ")
    wait(7)
    adb("shell", "input", "keyevent", "4")
    wait(1)
    overlay(
        "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا",
        section="أذكار الصباح",
        auto=True,
    )
    wait(8)
    adb("shell", "input", "keyevent", "4")
    wait(1)
    tap(677, 2274)  # misbaha
    wait(6)
    tap(127, 2274)  # settings
    wait(6)
    tap(953, 2274)  # home
    wait(5)
    p.wait(timeout=70)
    wait(1)
    adb("pull", remote, str(VIDEO))
    print("video", VIDEO.stat().st_size if VIDEO.exists() else 0)


if __name__ == "__main__":
    main()
