#!/usr/bin/env python3
"""Capture the real Android launch splash for the promo opening."""
import time
from pathlib import Path

from capture_locale_screens import DEVICE, PKG, ROOT, adb, wait

OUT = ROOT / "store-assets" / "video"
TMP = ROOT / ".tmp-export"


def shot(name: str) -> Path:
    TMP.mkdir(parents=True, exist_ok=True)
    remote = f"/sdcard/{name}.png"
    local = TMP / f"{name}.png"
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, str(local), check=False)
    return local


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    adb("shell", "am", "force-stop", PKG, check=False)
    wait(0.8)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    for i in range(6):
        time.sleep(0.28)
        p = shot(f"splash_{i}")
        print("shot", i, p.stat().st_size if p.exists() else 0)


if __name__ == "__main__":
    main()
