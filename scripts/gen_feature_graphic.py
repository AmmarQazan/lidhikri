#!/usr/bin/env python3
"""Generate 1024x500 feature graphic (bilingual, no overflow) and 512 icon."""
from pathlib import Path

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
GRAPHICS = ROOT / "store-assets" / "graphics"
GRAPHICS.mkdir(parents=True, exist_ok=True)

ICON_SRC = ROOT / "app" / "src" / "main" / "res" / "drawable" / "logo_app_icon.png"
LOGO_NIGHT = ROOT / "app" / "src" / "main" / "res" / "drawable" / "logo_lidhikri_night.png"
LOGO_SRC = LOGO_NIGHT if LOGO_NIGHT.exists() else (
    ROOT / "app" / "src" / "main" / "res" / "drawable" / "logo_lidhikri.png"
)
FONTS = Path(r"C:\Windows\Fonts")


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    path = FONTS / name
    if path.exists():
        return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def ar_text(text: str) -> str:
    return get_display(arabic_reshaper.reshape(text))


def fit_text(draw: ImageDraw.ImageDraw, text: str, max_w: int, names: list[str], start: int) -> ImageFont.FreeTypeFont:
    for size in range(start, 18, -2):
        for name in names:
            f = font(name, size)
            bbox = draw.textbbox((0, 0), text, font=f)
            if bbox[2] - bbox[0] <= max_w:
                return f
    return font(names[0], 20)


def main() -> None:
    icon = Image.open(ICON_SRC).convert("RGBA")
    icon.resize((512, 512), Image.Resampling.LANCZOS).save(GRAPHICS / "icon-512.png")
    play_g = ROOT / "store-assets" / "play-upload" / "graphics"
    play_g.mkdir(parents=True, exist_ok=True)
    icon.resize((512, 512), Image.Resampling.LANCZOS).save(play_g / "icon-512.png")

    banner = GRAPHICS / "feature-banner-source.jpg"
    if banner.exists():
        src = Image.open(banner).convert("RGB")
        w, h = 1024, 500
        canvas = Image.new("RGB", (w, h), src.getpixel((2, 2)))
        scaled = src.copy()
        scaled.thumbnail((w, h), Image.Resampling.LANCZOS)
        canvas.paste(scaled, ((w - scaled.width) // 2, (h - scaled.height) // 2))
        out = GRAPHICS / "feature-graphic.png"
        canvas.save(out)
        canvas.save(play_g / "feature-graphic.png")
        print(f"Saved banner feature {out}")
        return

    w, h = 1024, 500
    bg = Image.new("RGB", (w, h), "#163D28")
    draw = ImageDraw.Draw(bg)
    for y in range(h):
        t = y / h
        r = int(22 + t * 18)
        g = int(61 + t * 28)
        b = int(40 + t * 16)
        draw.line([(0, y), (w, y)], fill=(r, g, b))

    draw.ellipse([300, -90, 780, 260], outline="#D4AF37", width=4)

    logo = Image.open(LOGO_SRC).convert("RGBA")
    logo.thumbnail((240, 320), Image.Resampling.LANCZOS)
    bg.paste(logo, (56, (h - logo.height) // 2), logo)

    title_ar = ar_text("لذكري")
    tag_ar_raw = ar_text("أذان  •  مواقيت صلاة  •  أذكار بعد الفرض")
    ar_font = fit_text(draw, title_ar, 620, ["tradbdo.ttf", "tahoma.ttf"], 78)
    en_font = font("segoeuib.ttf", 44)
    tag_ar = fit_text(draw, tag_ar_raw, 620, ["tahoma.ttf", "arial.ttf"], 30)
    tag_en = fit_text(draw, "Adhan  ·  Prayer times  ·  After-prayer dhikr", 620, ["segoeui.ttf"], 26)

    x = 340
    draw.text((x, 118), title_ar, fill="#F5F1E9", font=ar_font)
    draw.text((x, 210), "Lidhikri", fill="#F5F1E9", font=en_font)
    draw.text((x, 282), tag_ar_raw, fill="#D4AF37", font=tag_ar)
    draw.text((x, 332), "Adhan  ·  Prayer times  ·  After-prayer dhikr", fill="#E8E0D0", font=tag_en)


    out = GRAPHICS / "feature-graphic.png"
    bg.save(out)
    print(f"Saved {out}")


if __name__ == "__main__":
    main()
