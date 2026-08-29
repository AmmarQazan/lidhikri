#!/usr/bin/env python3
"""Re-capture azkar hub, settings hub, widgets hub for en/fr/es."""
import subprocess
import time
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(Path(__file__).resolve().parent))

from capture_locale_screens import adb, nav, prep, shot, tap, wait

OUT_ROOT = Path(__file__).resolve().parents[1] / "store-assets" / "screenshots"


def recapture(lang: str) -> None:
    out = OUT_ROOT / lang
    print("retry", lang)
    prep(lang, onboarding=True)
    nav(0)
    wait(1)
    nav(2)
    wait(1.5)
    adb("shell", "input", "keyevent", "4", check=False)
    wait(1)
    adb("shell", "input", "keyevent", "4", check=False)
    wait(1.2)
    shot(out, "02_azkar_sections")

    nav(0)
    wait(1)
    nav(3)
    wait(2)
    shot(out, "05_settings_hub")

    adb("shell", "input", "swipe", "540", "2000", "540", "700", "500", check=False)
    wait(1.4)
    # Widgets row — lower half of settings after scroll
    tap(540, 1180)
    wait(2)
    shot(out, "09_widgets_hub")


def main() -> None:
    for lang in ("en", "fr", "es"):
        recapture(lang)


if __name__ == "__main__":
    main()
