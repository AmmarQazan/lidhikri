#!/usr/bin/env python3
"""Build Lidhikri logos, launcher icons, and Play graphics from the designer PDF/PNG."""
from __future__ import annotations

import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
DRAW = RES / "drawable"
GRAPHICS = ROOT / "store-assets" / "graphics"
PLAY_G = ROOT / "store-assets" / "play-upload" / "graphics"
WEB = ROOT / "store-assets" / "web"
UPLOAD_WEB = ROOT / "store-assets" / "play-upload"
VIDEO = ROOT / "store-assets" / "video"

PDF_RENDER = ROOT / ".tmp-export" / "pdf-1-1.png"
BANNER_JPG = Path(
    r"C:\Users\pc\.cursor\projects\c-Users-pc\assets"
    r"\c__Users_pc_AppData_Roaming_Cursor_User_workspaceStorage"
    r"_bbc9f20069a65463cd9aa48603600023_images_WhatsApp_Image_2026-09-06_at_6.26.03_PM-393a7814-47d1-4041-970c-e9c24fbe46a4.jpg"
)
NIGHT_PNG = Path(
    r"C:\Users\pc\.cursor\projects\c-Users-pc\assets"
    r"\c__Users_pc_AppData_Roaming_Cursor_User_workspaceStorage"
    r"_bbc9f20069a65463cd9aa48603600023_images_1_3_-3f6acd37-bf94-41f3-a86b-e8c2031ecf8b.png"
)
CREAM = (245, 241, 233)
GREEN_BG = (22, 61, 40)
NIGHT_BG = (22, 74, 46)


def save_png(im: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "PNG")
    print(f"  {path.relative_to(ROOT)}  {im.size}  {path.stat().st_size}")


def punch_near(im: Image.Image, ref: tuple[int, int, int], tol: int) -> Image.Image:
    arr = np.array(im.convert("RGBA")).astype(np.int16)
    r, g, b, a = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]
    dist = np.abs(r - ref[0]) + np.abs(g - ref[1]) + np.abs(b - ref[2])
    green = (g > r + 12) & (g > b + 8) & (g > 40)
    gold = (r > 140) & (g > 90) & (r > b + 18) & (r > 110)
    keep = green | gold
    new_a = a.copy()
    new_a[(dist < tol) & (~keep)] = 0
    ring = (dist >= tol) & (dist < tol + 36) & (~keep) & (a > 0)
    fade = ((tol + 36 - dist[ring]) / 36.0 * new_a[ring]).astype(np.int16)
    new_a[ring] = np.minimum(new_a[ring], fade)
    arr[:, :, 3] = np.clip(new_a, 0, 255)
    return Image.fromarray(arr.astype(np.uint8), "RGBA")


def trim(im: Image.Image, pad: int = 12, thr: int = 12) -> Image.Image:
    arr = np.array(im)
    ys, xs = np.where(arr[:, :, 3] > thr)
    if len(xs) == 0:
        return im
    l, r = max(0, int(xs.min()) - pad), min(im.width - 1, int(xs.max()) + pad)
    t, b = max(0, int(ys.min()) - pad), min(im.height - 1, int(ys.max()) + pad)
    return im.crop((l, t, r + 1, b + 1))


def split_mark(full: Image.Image) -> Image.Image:
    arr = np.array(full)
    rows = (arr[:, :, 3] > 20).sum(axis=1)
    h = full.height
    # Gap between mark (top) and word (bottom): low-ink band in the middle.
    lo, hi = int(h * 0.28), int(h * 0.72)
    band = rows[lo:hi]
    if band.size == 0:
        return full
    min_i = int(np.argmin(band)) + lo
    # expand to nearest near-empty rows
    top, bot = min_i, min_i
    while top > lo and rows[top] < max(8, rows.max() * 0.04):
        top -= 1
    while bot < hi - 1 and rows[bot] < max(8, rows.max() * 0.04):
        bot += 1
    cut = max(int(h * 0.22), top)
    return trim(full.crop((0, 0, full.width, cut)))


def fit_square(im: Image.Image, size: int, fill: float, bg: tuple[int, int, int] | None = None) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (*bg, 255) if bg else (0, 0, 0, 0))
    copy = im.copy()
    copy.thumbnail((int(size * fill), int(size * fill)), Image.Resampling.LANCZOS)
    x = (size - copy.width) // 2
    y = (size - copy.height) // 2
    canvas.paste(copy, (x, y), copy)
    return canvas


def adaptive_fg(mark: Image.Image, size: int = 432) -> Image.Image:
    # Safe zone ~66%; keep mark inside inner 72%.
    return fit_square(mark, size, 0.62, bg=None)


def pad_banner(src: Image.Image, w: int = 1024, h: int = 500) -> Image.Image:
    src = src.convert("RGB")
    canvas = Image.new("RGB", (w, h), src.getpixel((2, 2)))
    scaled = src.copy()
    scaled.thumbnail((w, h), Image.Resampling.LANCZOS)
    canvas.paste(scaled, ((w - scaled.width) // 2, (h - scaled.height) // 2))
    return canvas


def main() -> None:
    print("PDF render -> day logo")
    day_raw = Image.open(PDF_RENDER)
    corner = day_raw.getpixel((8, 8))[:3]
    day = trim(punch_near(day_raw, corner, tol=42), pad=16)
    mark = split_mark(day)
    print(f"  day {day.size}  mark {mark.size}  corner {corner}")

    print("Night PNG -> night logo")
    if NIGHT_PNG.exists():
        night_raw = Image.open(NIGHT_PNG)
        night = trim(punch_near(night_raw, (0, 0, 0), tol=48), pad=16)
    else:
        night = day
    print(f"  night {night.size}")

    # Full logos (in-app splash / home)
    day_fit = Image.new("RGBA", (1040, 1600), (0, 0, 0, 0))
    d = day.copy()
    d.thumbnail((1000, 1560), Image.Resampling.LANCZOS)
    day_fit.paste(d, ((1040 - d.width) // 2, (1600 - d.height) // 2), d)

    night_fit = Image.new("RGBA", (1040, 1600), (0, 0, 0, 0))
    n = night.copy()
    n.thumbnail((1000, 1560), Image.Resampling.LANCZOS)
    night_fit.paste(n, ((1040 - n.width) // 2, (1600 - n.height) // 2), n)

    save_png(day_fit, DRAW / "logo_lidhikri.png")
    save_png(night_fit, DRAW / "logo_lidhikri_night.png")
    # Keep filenames used by the app.

    icon_cream = fit_square(mark, 1024, 0.78, CREAM)
    icon_green = fit_square(mark, 1024, 0.78, GREEN_BG)
    save_png(icon_cream, DRAW / "logo_app_icon.png")
    save_png(fit_square(mark, 1024, 0.88), DRAW / "ic_launcher_logo.png")
    save_png(fit_square(day, 1024, 0.92), DRAW / "logo_splash.png")

    fg_day = adaptive_fg(mark, 432)
    fg_night = adaptive_fg(mark, 432)
    save_png(fg_day, RES / "mipmap-xxxhdpi" / "ic_launcher_foreground.png")
    save_png(fg_night, RES / "mipmap-night-xxxhdpi" / "ic_launcher_foreground.png")

    GRAPHICS.mkdir(parents=True, exist_ok=True)
    PLAY_G.mkdir(parents=True, exist_ok=True)
    icon512 = fit_square(mark, 512, 0.78, CREAM).convert("RGBA")
    save_png(icon512, GRAPHICS / "icon-512.png")
    save_png(icon512, PLAY_G / "icon-512.png")

    if BANNER_JPG.exists():
        feature = pad_banner(Image.open(BANNER_JPG))
        feature.save(GRAPHICS / "feature-graphic.png", "PNG")
        feature.save(PLAY_G / "feature-graphic.png", "PNG")
        print(f"  feature-graphic {feature.size}")
        shutil.copy2(BANNER_JPG, GRAPHICS / "feature-banner-source.jpg")

    if WEB.exists():
        save_png(icon512, WEB / "icon-512.png")
    if (UPLOAD_WEB / "privacy.html").exists():
        save_png(icon512, ROOT / "store-assets" / "play-upload" / "icon-512.png")

    intro = Image.new("RGB", (1080, 2400), CREAM)
    full = day.copy()
    full.thumbnail((820, 1200), Image.Resampling.LANCZOS)
    intro.paste(full, ((1080 - full.width) // 2, 560), full)
    intro.save(VIDEO / "intro-card.png", "PNG")
    print(f"  intro-card {intro.size}")

    # Developer icon (512)
    save_png(icon_cream, GRAPHICS / "developer-icon.png")
    print("done")


if __name__ == "__main__":
    main()
