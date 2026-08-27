#!/usr/bin/env python3
"""Caption locale screenshots for Play (1080×1920) with UI-language titles."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
FONTS = Path(r"C:\Windows\Fonts")
W, H = 1080, 1920
BANNER = 280

SHOTS = {
    "en": [
        ("01_home_auto_tasbih.png", "Auto tasbih", "Reminders while you use your phone"),
        ("02_azkar_sections.png", "Hisnul Muslim", "Morning, evening, sleep, and more"),
        ("03_azkar_morning_cards.png", "Read adhkar", "Cards or a list — at your pace"),
        ("04_misbaha_digital.png", "Digital misbaha", "Traditional beads or electronic counter"),
        ("05_settings_hub.png", "Clear settings", "Every feature in one place"),
        ("06_onboarding_welcome.png", "Start in minutes", "Arabic, English, French, Spanish"),
        ("07_popup_auto_tasbih.png", "Over your apps", "A calm popup — then it steps aside"),
        ("08_popup_auto_azkar.png", "Auto adhkar", "Hisnul Muslim on your schedule"),
        ("09_widgets_hub.png", "Home widgets", "Dhikr of the day and misbaha"),
        ("10_home_widgets.png", "From the home screen", "Count without opening the app"),
    ],
    "fr": [
        ("01_home_auto_tasbih.png", "Tasbih automatique", "Des rappels pendant l’usage du téléphone"),
        ("02_azkar_sections.png", "Hisnul Muslim", "Matin, soir, sommeil, et plus"),
        ("03_azkar_morning_cards.png", "Lire les adhkar", "Cartes ou liste — à votre rythme"),
        ("04_misbaha_digital.png", "Misbaha numérique", "Perles traditionnelles ou compteur"),
        ("05_settings_hub.png", "Réglages clairs", "Chaque fonction au même endroit"),
        ("06_onboarding_welcome.png", "Prêt en quelques minutes", "Arabe, anglais, français, espagnol"),
        ("07_popup_auto_tasbih.png", "Par-dessus vos apps", "Une fenêtre calme, puis elle s’efface"),
        ("08_popup_auto_azkar.png", "Adhkar automatiques", "Hisnul Muslim selon votre horaire"),
        ("09_widgets_hub.png", "Widgets d’accueil", "Dhikr du jour et misbaha"),
        ("10_home_widgets.png", "Depuis l’écran d’accueil", "Comptez sans ouvrir l’application"),
    ],
    "es": [
        ("01_home_auto_tasbih.png", "Tasbih automático", "Recordatorios mientras usas el teléfono"),
        ("02_azkar_sections.png", "Hisnul Muslim", "Mañana, tarde, sueño y más"),
        ("03_azkar_morning_cards.png", "Leer adhkar", "Tarjetas o lista — a tu ritmo"),
        ("04_misbaha_digital.png", "Misbaha digital", "Cuentas tradicionales o contador"),
        ("05_settings_hub.png", "Ajustes claros", "Cada función en un solo lugar"),
        ("06_onboarding_welcome.png", "Empieza en minutos", "Árabe, inglés, francés, español"),
        ("07_popup_auto_tasbih.png", "Sobre tus apps", "Una ventana serena que luego se aparta"),
        ("08_popup_auto_azkar.png", "Adhkar automáticos", "Hisnul Muslim en tu horario"),
        ("09_widgets_hub.png", "Widgets de inicio", "Dhikr del día y misbaha"),
        ("10_home_widgets.png", "Desde la pantalla de inicio", "Cuenta sin abrir la app"),
    ],
}

PLAY_ORDER = [
    "01_home_auto_tasbih.png",
    "07_popup_auto_tasbih.png",
    "02_azkar_sections.png",
    "04_misbaha_digital.png",
    "10_home_widgets.png",
    "09_widgets_hub.png",
    "08_popup_auto_azkar.png",
    "05_settings_hub.png",
]
PLAY_NAMES = [
    "01-auto-tasbih-home.png",
    "02-popup-over-apps.png",
    "03-hisnul-muslim.png",
    "04-digital-misbaha.png",
    "05-home-widgets.png",
    "06-widgets-settings.png",
    "07-auto-azkar-popup.png",
    "08-settings-hub.png",
]


def fnt(name: str, size: int) -> ImageFont.FreeTypeFont:
    path = FONTS / name
    if path.exists():
        return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def caption(src: Path, title: str, sub: str, dest: Path) -> None:
    shot = Image.open(src).convert("RGB")
    canvas = Image.new("RGB", (W, H), "#163D28")
    draw = ImageDraw.Draw(canvas)
    for y in range(BANNER):
        t = y / BANNER
        draw.line([(0, y), (W, y)], fill=(int(22 + t * 10), int(61 + t * 20), int(40 + t * 10)))
    draw.rectangle([0, BANNER - 6, W, BANNER], fill="#D4AF37")
    title_font = fnt("segoeuib.ttf", 42)
    sub_font = fnt("segoeui.ttf", 26)
    tb = draw.textbbox((0, 0), title, font=title_font)
    sb = draw.textbbox((0, 0), sub, font=sub_font)
    draw.text(((W - (tb[2] - tb[0])) // 2, 70), title, fill="#F5F1E9", font=title_font)
    draw.text(((W - (sb[2] - sb[0])) // 2, 150), sub, fill="#D4AF37", font=sub_font)
    body_h = H - BANNER
    scale = min(W / shot.width, body_h / shot.height)
    nw, nh = int(shot.width * scale), int(shot.height * scale)
    shot = shot.resize((nw, nh), Image.Resampling.LANCZOS)
    canvas.paste(shot, ((W - nw) // 2, BANNER + (body_h - nh) // 2))
    dest.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(dest, optimize=True)
    print("captioned", dest)


def pack(lang: str) -> None:
    import shutil

    cap = ROOT / "store-assets" / "screenshots" / "captioned" / lang
    dest_dir = ROOT / "store-assets" / "play-upload" / f"phone-screenshots-{lang}"
    dest_dir.mkdir(parents=True, exist_ok=True)
    for src_name, dest_name in zip(PLAY_ORDER, PLAY_NAMES):
        src = cap / src_name
        if src.exists():
            shutil.copy2(src, dest_dir / dest_name)
            print("packed", lang, dest_name)


def main() -> None:
    for lang, rows in SHOTS.items():
        phone = ROOT / "store-assets" / "screenshots" / lang
        cap = ROOT / "store-assets" / "screenshots" / "captioned" / lang
        for name, title, sub in rows:
            src = phone / name
            if not src.exists():
                print("skip", lang, name)
                continue
            caption(src, title, sub, cap / name)
        pack(lang)


if __name__ == "__main__":
    main()
