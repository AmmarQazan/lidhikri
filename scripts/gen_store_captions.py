#!/usr/bin/env python3
"""Build Play Store captioned screenshots (1080×1920) from phone captures."""
from __future__ import annotations

import shutil
from pathlib import Path

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
PHONE = ROOT / "store-assets" / "screenshots" / "phone"
CAP = ROOT / "store-assets" / "screenshots" / "captioned"
TMP = ROOT / ".tmp-export"
FONTS = Path(r"C:\Windows\Fonts")

W, H = 1080, 1920
BANNER = 300

# Extra feature shots not always produced by the ADB capture script.
IMPORTS = {
    "09_widgets_hub.png": TMP / "07-widgets-hub.png",
    "10_home_widgets.png": TMP / "19-widgets-page.png",
}

SHOTS = [
    ("01_home_auto_tasbih.png", "التسبيح التلقائي", "Auto tasbih while you use your phone"),
    ("02_azkar_sections.png", "حصن المسلم", "Morning, evening, and more"),
    ("03_azkar_morning_cards.png", "قراءة الأذكار", "Cards or list — at your pace"),
    ("04_misbaha_digital.png", "المسبحة الرقمية", "Traditional beads or electronic counter"),
    ("05_settings_hub.png", "كل شيء في مكانه", "Clear settings for every feature"),
    ("06_onboarding_welcome.png", "ابدأ خلال دقائق", "Arabic, English, French, Spanish"),
    ("07_popup_auto_tasbih.png", "فوق تطبيقاتك", "A calm popup — then it gets out of the way"),
    ("08_popup_auto_azkar.png", "الأذكار التلقائية", "Hisnul Muslim on your schedule"),
    ("09_widgets_hub.png", "ويدجت الشاشة", "Dhikr of the day and home misbaha"),
    ("10_home_widgets.png", "من الشاشة الرئيسية", "Count and remember without opening the app"),
]


def fnt(name: str, size: int) -> ImageFont.FreeTypeFont:
    path = FONTS / name
    if path.exists():
        return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def ar_text(text: str) -> str:
    return get_display(arabic_reshaper.reshape(text))


def import_extras() -> None:
    PHONE.mkdir(parents=True, exist_ok=True)
    for dest, src in IMPORTS.items():
        if src.exists():
            shutil.copy2(src, PHONE / dest)
            print("imported", dest)


def caption(src: Path, ar: str, en: str, dest: Path) -> None:
    shot = Image.open(src).convert("RGB")
    canvas = Image.new("RGB", (W, H), "#163D28")
    draw = ImageDraw.Draw(canvas)
    for y in range(BANNER):
        t = y / BANNER
        draw.line([(0, y), (W, y)], fill=(int(22 + t * 10), int(61 + t * 20), int(40 + t * 10)))
    draw.rectangle([0, BANNER - 6, W, BANNER], fill="#D4AF37")

    ar_draw = ar_text(ar)
    ar_font = fnt("tahomabd.ttf", 48)
    en_font = fnt("segoeui.ttf", 28)
    ar_bbox = draw.textbbox((0, 0), ar_draw, font=ar_font)
    en_bbox = draw.textbbox((0, 0), en, font=en_font)
    draw.text(((W - (ar_bbox[2] - ar_bbox[0])) // 2, 70), ar_draw, fill="#F5F1E9", font=ar_font)
    draw.text(((W - (en_bbox[2] - en_bbox[0])) // 2, 160), en, fill="#D4AF37", font=en_font)

    body_h = H - BANNER
    scale = min(W / shot.width, body_h / shot.height)
    nw, nh = int(shot.width * scale), int(shot.height * scale)
    shot = shot.resize((nw, nh), Image.Resampling.LANCZOS)
    x = (W - nw) // 2
    y = BANNER + (body_h - nh) // 2
    canvas.paste(shot, (x, y))
    dest.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(dest, optimize=True)
    print("captioned", dest.name)


def main() -> None:
    import_extras()
    for name, ar, en in SHOTS:
        src = PHONE / name
        if not src.exists():
            print("skip missing", name)
            continue
        caption(src, ar, en, CAP / name)


if __name__ == "__main__":
    main()
