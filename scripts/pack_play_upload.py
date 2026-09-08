#!/usr/bin/env python3
"""Pack the 8 Play Console screenshots + graphics into store-assets/play-upload/."""
from __future__ import annotations

import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "store-assets" / "screenshots" / "captioned"
GFX = ROOT / "store-assets" / "graphics"
VIDEO = ROOT / "store-assets" / "video" / "promo.mp4"
OUT = ROOT / "store-assets" / "play-upload"

# Play Console phone: 2–8 images. Keep adhan / prayer times / after-prayer in the set.
ORDER = [
    ("01_home_auto_tasbih.png", "01-auto-tasbih-home.png"),
    ("07_popup_auto_tasbih.png", "02-popup-over-apps.png"),
    ("08_popup_auto_azkar.png", "03-auto-azkar-popup.png"),
    ("11_prayer_times.png", "04-prayer-times.png"),
    ("02_azkar_sections.png", "05-hisnul-muslim.png"),
    ("04_misbaha_digital.png", "06-digital-misbaha.png"),
    ("10_home_widgets.png", "07-home-widgets.png"),
    ("05_settings_hub.png", "08-settings-hub.png"),
]
EXTRAS = [
    ("09_widgets_hub.png", "09-widgets-settings.png"),
    ("12_display_theme.png", "10-display-theme.png"),
]


KEEP_NAMES = {dest for _, dest in ORDER + EXTRAS}


def _copy_shots(src_dir: Path, dest_dir: Path, label: str) -> None:
    dest_dir.mkdir(parents=True, exist_ok=True)
    for src_name, dest_name in ORDER + EXTRAS:
        src = src_dir / src_name
        if not src.exists():
            raise SystemExit(f"missing {src}")
        dest = dest_dir / dest_name
        shutil.copy2(src, dest)
        im = Image.open(dest)
        print(f"{label} {dest_name}: {im.size[0]}x{im.size[1]} {im.mode}")
    for stale in dest_dir.glob("*.png"):
        if stale.name not in KEEP_NAMES:
            stale.unlink()
            print("removed stale", stale.name)


def main() -> None:
    phone = OUT / "phone-screenshots"
    graphics = OUT / "graphics"
    phone.mkdir(parents=True, exist_ok=True)
    graphics.mkdir(parents=True, exist_ok=True)

    _copy_shots(SRC, phone, "day")
    night_src = SRC / "ar-night"
    if night_src.exists():
        _copy_shots(night_src, OUT / "phone-screenshots-night", "night")

    for name in ("icon-512.png", "feature-graphic.png"):
        shutil.copy2(GFX / name, graphics / name)
        im = Image.open(graphics / name)
        print(f"{name}: {im.size[0]}x{im.size[1]}")

    listings_src = ROOT / "store-assets" / "listings.md"
    shutil.copy2(listings_src, OUT / "listings.md")
    privacy = ROOT / "store-assets" / "privacy.html"
    if privacy.exists():
        shutil.copy2(privacy, OUT / "privacy.html")
    readme_src = ROOT / "store-assets" / "play-upload-README.md"
    if readme_src.exists():
        shutil.copy2(readme_src, OUT / "README.md")
    if VIDEO.exists():
        shutil.copy2(VIDEO, OUT / "promo.mp4")
        print("promo.mp4", VIDEO.stat().st_size)

    sys.path.insert(0, str(Path(__file__).resolve().parent))
    from pack_play_listings import main as pack_listings

    pack_listings()
    print("packed", OUT)


if __name__ == "__main__":
    main()
