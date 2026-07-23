#!/usr/bin/env python3
"""Generate 1024x500 feature graphic and copy app icon."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
GRAPHICS = ROOT / "store-assets" / "graphics"
GRAPHICS.mkdir(parents=True, exist_ok=True)

ICON_SRC = ROOT / "app" / "src" / "main" / "res" / "drawable" / "logo_app_icon.png"
LOGO_SRC = ROOT / "app" / "src" / "main" / "res" / "drawable" / "logo_sabbih.png"

# Copy icon
icon = Image.open(ICON_SRC).convert("RGBA")
icon.resize((512, 512), Image.Resampling.LANCZOS).save(GRAPHICS / "icon-512.png")

# Feature graphic 1024x500
W, H = 1024, 500
bg = Image.new("RGB", (W, H), "#1B5E3B")
draw = ImageDraw.Draw(bg)

# Gradient dome accent
for y in range(H):
    t = y / H
    c = int(27 + t * 20)
    draw.line([(0, y), (W, y)], fill=(c, 94 + int(t * 30), 59 + int(t * 20)))

# Gold arc
draw.ellipse([320, -80, 704, 280], outline="#D4AF37", width=6)

# Logo
logo = Image.open(LOGO_SRC).convert("RGBA")
logo.thumbnail((220, 300), Image.Resampling.LANCZOS)
bg.paste(logo, (80, (H - logo.height) // 2), logo)

try:
    font_lg = ImageFont.truetype("arial.ttf", 72)
    font_sm = ImageFont.truetype("arial.ttf", 32)
except OSError:
    font_lg = ImageFont.load_default()
    font_sm = ImageFont.load_default()

draw.text((360, 160), "Sabbih", fill="#F5F1E9", font=font_lg)
draw.text((360, 260), "Auto dhikr & adhkar — calm, focused, always with you", fill="#D4AF37", font=font_sm)
draw.text((360, 320), "Dhikr reminders that fit your day", fill="#E8E0D0", font=font_sm)

bg.save(GRAPHICS / "feature-graphic.png")
print(f"Saved {GRAPHICS / 'feature-graphic.png'}")
