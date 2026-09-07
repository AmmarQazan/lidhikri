#!/usr/bin/env python3
"""Generate a 4096x2304 Play Console developer header (24-bit, <=1MB)."""
from pathlib import Path

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
GRAPHICS = ROOT / "store-assets" / "graphics"
LOGO_SRC = ROOT / "app" / "src" / "main" / "res" / "drawable" / "logo_sabbih.png"
BG_CANDIDATES = [
    Path(r"C:\Users\pc\.cursor\projects\c-Users-pc\assets\sabbih-header-bg.png"),
    GRAPHICS / "header-bg-source.png",
]
FONTS = Path(r"C:\Windows\Fonts")
W, H = 4096, 2304
MAX_BYTES = 1_000_000


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    path = FONTS / name
    if path.exists():
        return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def ar(text: str) -> str:
    return get_display(arabic_reshaper.reshape(text))


def cover(im: Image.Image, size: tuple[int, int]) -> Image.Image:
    tw, th = size
    scale = max(tw / im.width, th / im.height)
    nw, nh = int(im.width * scale), int(im.height * scale)
    im = im.resize((nw, nh), Image.Resampling.LANCZOS)
    left = (nw - tw) // 2
    top = max(0, (nh - th) // 2 - 80)
    return im.crop((left, top, left + tw, top + th))


def h_gradient(size: tuple[int, int], color: tuple[int, int, int], max_a: int) -> Image.Image:
    w, h = size
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = layer.load()
    stop = int(w * 0.52)
    for x in range(stop):
        a = int(max_a * (1 - x / stop) ** 1.15)
        for y in range(h):
            px[x, y] = (*color, a)
    return layer


def main() -> None:
    GRAPHICS.mkdir(parents=True, exist_ok=True)
    bg_src = next(p for p in BG_CANDIDATES if p.exists())
    src = Image.open(bg_src).convert("RGB")
    dest_bg = GRAPHICS / "header-bg-source.png"
    if bg_src.resolve() != dest_bg.resolve():
        dest_bg.write_bytes(bg_src.read_bytes())

    canvas = cover(src, (W, H))
    canvas = ImageEnhance.Color(canvas).enhance(1.08)
    canvas = ImageEnhance.Contrast(canvas).enhance(1.06)
    base = canvas.convert("RGBA")
    base = Image.alpha_composite(base, h_gradient((W, H), (8, 22, 14), 175))

    vignette = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    vdraw = ImageDraw.Draw(vignette)
    vdraw.ellipse([W * 0.28, H * 0.62, W * 0.72, H * 1.18], fill=(6, 16, 10, 110))
    vignette = vignette.filter(ImageFilter.GaussianBlur(48))
    base = Image.alpha_composite(base, vignette)

    logo = Image.open(LOGO_SRC).convert("RGBA")
    logo.thumbnail((720, 980), Image.Resampling.LANCZOS)
    shadow = Image.new("RGBA", (logo.width + 80, logo.height + 80), (0, 0, 0, 0))
    mask = logo.split()[-1]
    blob = Image.new("RGBA", logo.size, (0, 0, 0, 120))
    blob.putalpha(mask.point(lambda a: min(120, a)))
    shadow.paste(blob, (24, 30), blob)
    shadow = shadow.filter(ImageFilter.GaussianBlur(16))
    lx, ly = 260, 380
    base.paste(shadow, (lx - 24, ly - 16), shadow)
    base.paste(logo, (lx, ly), logo)

    draw = ImageDraw.Draw(base)
    title = ar("سَبِّح")
    tag = ar("أذان ومواقيت وأذكار بعد الفرض")
    f_title = font("tradbdo.ttf", 210)
    f_en = font("georgia.ttf", 78) if (FONTS / "georgia.ttf").exists() else font("segoeuib.ttf", 78)
    f_tag = font("tahoma.ttf", 52)

    tx = lx + logo.width + 70
    ty = ly + 210
    draw.text((tx + 2, ty + 3), title, fill=(0, 0, 0, 80), font=f_title)
    draw.text((tx, ty), title, fill="#F7F1E4", font=f_title)
    draw.text((tx, ty + 250), "Sabbih", fill="#E4D2A0", font=f_en)
    line_y = ty + 360
    draw.line([(tx, line_y), (tx + 640, line_y)], fill="#C9A227", width=3)
    draw.text((tx, line_y + 36), tag, fill="#E6C56A", font=f_tag)

    out_rgb = base.convert("RGB")
    png = GRAPHICS / "developer-header.png"
    jpg = GRAPHICS / "developer-header.jpg"
    out_rgb.save(png, format="PNG", optimize=True)
    quality = 88
    while quality >= 70:
        out_rgb.save(jpg, format="JPEG", quality=quality, optimize=True, subsampling=1)
        if jpg.stat().st_size <= MAX_BYTES:
            break
        quality -= 4
    # Play accepts JPEG; keep PNG only if it fits.
    if png.stat().st_size > MAX_BYTES:
        png.unlink()
        out = jpg
    else:
        out = png
    print("saved", out, out_rgb.size, out.stat().st_size, "q", quality)


if __name__ == "__main__":
    main()
