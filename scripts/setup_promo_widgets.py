#!/usr/bin/env python3
"""Pin dhikr-of-day and traditional misbaha widgets, then screenshot home pages."""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from capture_locale_screens import (  # noqa: E402
    ADD_DHIKR,
    ADD_WIDGET,
    DHIKR_OF_DAY,
    MISBAHA_WIDGET,
    TRADITIONAL,
    WIDGETS_HUB,
    adb,
    dump_ui,
    go_home,
    pin_dhikr_of_day,
    prep,
    shot,
    tap_nav,
    tap_text,
    tap_xy,
    wait,
)

OUT = Path(__file__).resolve().parents[1] / ".tmp-export"


def split_widget_pages() -> None:
    """Keep dhikr-of-day and traditional misbaha on two separate launcher pages."""
    adb("shell", "input", "keyevent", "3", check=False)
    wait(0.7)
    adb("shell", "input", "swipe", "900", "1100", "180", "1100", "350", check=False)
    wait(1.1)
    xml = dump_ui()
    together = "ذكر اليوم" in xml and "متبقي" in xml
    if together:
        adb("shell", "input", "draganddrop", "400", "400", "1060", "420", "1800", check=False)
        wait(2.0)
        adb("shell", "input", "keyevent", "3", check=False)
        wait(0.8)
    reset_launcher_home()


def reset_launcher_home() -> None:
    """Leave the launcher on the Gmail/Photos page so later HOME + swipe hits widgets in order."""
    adb("shell", "input", "keyevent", "3", check=False)
    wait(0.45)
    for _ in range(4):
        adb("shell", "input", "swipe", "180", "1100", "900", "1100", "280", check=False)
        wait(0.28)
    for _ in range(4):
        xml = dump_ui()
        if "Gmail" in xml and "YouTube" in xml:
            return
        adb("shell", "input", "swipe", "900", "1100", "180", "1100", "320", check=False)
        wait(0.45)


def pin_traditional() -> None:
    tap_nav("ar", 3)
    wait(0.8)
    tap_text(WIDGETS_HUB["ar"], contains=False)
    wait(1.0)
    tap_text(MISBAHA_WIDGET["ar"], contains=True)
    wait(1.0)
    tap_text(TRADITIONAL["ar"], contains=False)
    wait(0.5)
    adb("shell", "input", "swipe", "540", "1900", "540", "500", "400", check=False)
    wait(0.8)
    if not tap_text(ADD_WIDGET["ar"], contains=True, retries=2):
        tap_xy(540, 2000)
        wait(1.0)
    wait(1.8)
    tap_text("Add to home screen", contains=True, retries=2) or tap_text("Add", contains=False, retries=1)
    wait(2.0)


def main() -> None:
    prep("ar", onboarding=True, reinstall=False)
    pin_traditional()
    pin_dhikr_of_day("ar")
    go_home()
    wait(1.2)
    split_widget_pages()
    go_home()
    wait(1.0)
    shot(OUT, "w-home0")
    adb("shell", "input", "swipe", "900", "1100", "180", "1100", "380", check=False)
    wait(1.2)
    shot(OUT, "w-homeR")
    adb("shell", "input", "swipe", "900", "1100", "180", "1100", "380", check=False)
    wait(1.2)
    shot(OUT, "w-homeR2")
    print("done")


if __name__ == "__main__":
    main()
