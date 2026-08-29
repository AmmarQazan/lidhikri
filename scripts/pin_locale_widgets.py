#!/usr/bin/env python3
"""Pin misbaha widgets then recapture home for en/fr/es."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from capture_locale_screens import (
    ADD_WIDGET,
    MISBAHA_WIDGET,
    TRADITIONAL,
    WIDGETS_HUB,
    adb,
    pin_dhikr_of_day,
    prep,
    remove_extra_home_widgets,
    shot,
    tap_nav,
    tap_text,
    wait,
    OUT_ROOT,
)


def pin_and_shot(lang: str) -> None:
    out = OUT_ROOT / lang
    print("pin", lang, flush=True)
    prep(lang, onboarding=True, reinstall=False)
    remove_extra_home_widgets()
    tap_nav(lang, 3)
    wait(1)
    tap_text(WIDGETS_HUB[lang], contains=False)
    wait(1)
    tap_text(MISBAHA_WIDGET[lang], contains=True)
    wait(1.2)
    tap_text(TRADITIONAL[lang], contains=True)
    wait(0.5)
    adb("shell", "input", "swipe", "540", "1900", "540", "500", "500", check=False)
    wait(1.2)
    if not tap_text(ADD_WIDGET[lang], contains=True):
        tap_text("Add misbaha", contains=True) or tap_text("Ajouter", contains=True)
    wait(2.5)
    tap_text("Add to home screen", contains=True)
    wait(2)
    pin_dhikr_of_day(lang)
    adb("shell", "input", "keyevent", "3", check=False)
    wait(2.5)
    shot(out, "10_home_widgets")


if __name__ == "__main__":
    langs = sys.argv[1:] or ["en", "fr", "es"]
    for lang in langs:
        pin_and_shot(lang)
