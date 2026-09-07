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
        ("06_onboarding_welcome.png", "Start in minutes", "Arabic, English, French, Spanish, Turkish, Urdu, Indonesian, Hindi"),
        ("07_popup_auto_tasbih.png", "Over your apps", "A calm popup — then it steps aside"),
        ("08_popup_auto_azkar.png", "Auto adhkar", "Hisnul Muslim on your schedule"),
        ("09_widgets_hub.png", "Home widgets", "Dhikr of the day and misbaha"),
        ("10_home_widgets.png", "From the home screen", "Count without opening the app"),
        ("11_prayer_times.png", "Adhan & prayer times", "After-prayer adhkar after each fard"),
    ],
    "fr": [
        ("01_home_auto_tasbih.png", "Tasbih automatique", "Des rappels pendant l’usage du téléphone"),
        ("02_azkar_sections.png", "Hisnul Muslim", "Matin, soir, sommeil, et plus"),
        ("03_azkar_morning_cards.png", "Lire les adhkar", "Cartes ou liste — à votre rythme"),
        ("04_misbaha_digital.png", "Misbaha numérique", "Perles traditionnelles ou compteur"),
        ("05_settings_hub.png", "Réglages clairs", "Chaque fonction au même endroit"),
        ("06_onboarding_welcome.png", "Prêt en quelques minutes", "Arabe, anglais, français, espagnol, turc, ourdou, indonésien, hindi"),
        ("07_popup_auto_tasbih.png", "Par-dessus vos apps", "Une fenêtre calme, puis elle s’efface"),
        ("08_popup_auto_azkar.png", "Adhkar automatiques", "Hisnul Muslim selon votre horaire"),
        ("09_widgets_hub.png", "Widgets d’accueil", "Dhikr du jour et misbaha"),
        ("10_home_widgets.png", "Depuis l’écran d’accueil", "Comptez sans ouvrir l’application"),
        ("11_prayer_times.png", "Adhan et horaires", "Dhikr après chaque prière obligatoire"),
    ],
    "es": [
        ("01_home_auto_tasbih.png", "Tasbih automático", "Recordatorios mientras usas el teléfono"),
        ("02_azkar_sections.png", "Hisnul Muslim", "Mañana, tarde, sueño y más"),
        ("03_azkar_morning_cards.png", "Leer adhkar", "Tarjetas o lista — a tu ritmo"),
        ("04_misbaha_digital.png", "Misbaha digital", "Cuentas tradicionales o contador"),
        ("05_settings_hub.png", "Ajustes claros", "Cada función en un solo lugar"),
        ("06_onboarding_welcome.png", "Empieza en minutos", "Árabe, inglés, francés, español, turco, urdu, indonesio, hindi"),
        ("07_popup_auto_tasbih.png", "Sobre tus apps", "Una ventana serena que luego se aparta"),
        ("08_popup_auto_azkar.png", "Adhkar automáticos", "Hisnul Muslim en tu horario"),
        ("09_widgets_hub.png", "Widgets de inicio", "Dhikr del día y misbaha"),
        ("10_home_widgets.png", "Desde la pantalla de inicio", "Cuenta sin abrir la app"),
        ("11_prayer_times.png", "Adhan y horarios", "Adhkar después de cada fard"),
    ],
    "tr": [
        ("01_home_auto_tasbih.png", "Otomatik tesbih", "Telefonu kullanırken hatırlatmalar"),
        ("02_azkar_sections.png", "Hisnul Müslim", "Sabah, akşam, uyku ve daha fazlası"),
        ("03_azkar_morning_cards.png", "Zikir oku", "Kartlar veya liste — kendi hızınızda"),
        ("04_misbaha_digital.png", "Dijital tesbih", "Geleneksel taneler veya elektronik sayaç"),
        ("05_settings_hub.png", "Net ayarlar", "Her özellik bir yerde"),
        ("06_onboarding_welcome.png", "Dakikalar içinde başlayın", "Arapça, İngilizce, Fransızca, İspanyolca, Türkçe, Urduca, Endonezce, Hintçe"),
        ("07_popup_auto_tasbih.png", "Uygulamalarınızın üstünde", "Sakin bir pencere — sonra çekilir"),
        ("08_popup_auto_azkar.png", "Otomatik zikir", "Hisnul Müslim, sizin saatinizde"),
        ("09_widgets_hub.png", "Ana ekran widget'ları", "Günün zikri ve tesbih"),
        ("10_home_widgets.png", "Ana ekrandan", "Uygulamayı açmadan sayın"),
        ("11_prayer_times.png", "Ezan ve namaz vakitleri", "Her farzdan sonra zikir"),
    ],
    "ur": [
        ("01_home_auto_tasbih.png", "خودکار تسبیح", "فون استعمال کرتے ہوئے یاددہانی"),
        ("02_azkar_sections.png", "حصن المسلم", "صبح، شام، نیند اور مزید"),
        ("03_azkar_morning_cards.png", "اذکار پڑھیں", "کارڈز یا فہرست — اپنی رفتار سے"),
        ("04_misbaha_digital.png", "ڈیجیٹل تسبیح", "روایتی دانے یا الیکٹرانک کاؤنٹر"),
        ("05_settings_hub.png", "واضح ترتیبات", "ہر خصوصیت ایک جگہ"),
        ("06_onboarding_welcome.png", "چند منٹوں میں شروع کریں", "عربی، انگریزی، فرانسیسی، ہسپانوی، ترکی، اردو، انڈونیشیائی، ہندی"),
        ("07_popup_auto_tasbih.png", "ایپس کے اوپر", "پرسکون ونڈو — پھر ہٹ جاتی ہے"),
        ("08_popup_auto_azkar.png", "خودکار اذکار", "حصن المسلم آپ کے شیڈول پر"),
        ("09_widgets_hub.png", "ہوم وجٹس", "آج کا ذکر اور تسبیح"),
        ("10_home_widgets.png", "ہوم اسکرین سے", "ایپ کھولے بغیر گنیں"),
        ("11_prayer_times.png", "اذان اور نماز کے اوقات", "ہر فرض کے بعد اذکار"),
    ],
    "id": [
        ("01_home_auto_tasbih.png", "Tasbih otomatis", "Pengingat saat memakai ponsel"),
        ("02_azkar_sections.png", "Hisnul Muslim", "Pagi, petang, tidur, dan lainnya"),
        ("03_azkar_morning_cards.png", "Baca dzikir", "Kartu atau daftar — sesuai ritme Anda"),
        ("04_misbaha_digital.png", "Tasbih digital", "Manik tradisional atau penghitung elektronik"),
        ("05_settings_hub.png", "Pengaturan jelas", "Setiap fitur di satu tempat"),
        ("06_onboarding_welcome.png", "Mulai dalam hitungan menit", "Arab, Inggris, Prancis, Spanyol, Turki, Urdu, Indonesia, Hindi"),
        ("07_popup_auto_tasbih.png", "Di atas aplikasi Anda", "Jendela tenang — lalu menyingkir"),
        ("08_popup_auto_azkar.png", "Dzikir otomatis", "Hisnul Muslim sesuai jadwal Anda"),
        ("09_widgets_hub.png", "Widget beranda", "Dzikir hari ini dan tasbih"),
        ("10_home_widgets.png", "Dari layar utama", "Hitung tanpa membuka aplikasi"),
        ("11_prayer_times.png", "Adzan dan waktu shalat", "Dzikir setelah setiap fardu"),
    ],
    "hi": [
        ("01_home_auto_tasbih.png", "स्वतः तस्बीह", "फ़ोन इस्तेमाल करते हुए याददिहानी"),
        ("02_azkar_sections.png", "हिसनुल मुस्लिम", "सुबह, शाम, नींद और अन्य"),
        ("03_azkar_morning_cards.png", "अज़कार पढ़ें", "कार्ड या सूची — अपनी गति से"),
        ("04_misbaha_digital.png", "डिजिटल तस्बीह", "पारंपरिक मनके या इलेक्ट्रॉनिक काउंटर"),
        ("05_settings_hub.png", "स्पष्ट सेटिंग्स", "हर सुविधा एक जगह"),
        ("06_onboarding_welcome.png", "कुछ मिनटों में शुरू करें", "अरबी, अंग्रेज़ी, फ़्रेंच, स्पेनिश, तुर्की, उर्दू, इंडोनेशियाई, हिंदी"),
        ("07_popup_auto_tasbih.png", "आपकी ऐप्स के ऊपर", "शांत विंडो — फिर हट जाती है"),
        ("08_popup_auto_azkar.png", "स्वतः अज़कार", "हिसनुल मुस्लिम आपके समय पर"),
        ("09_widgets_hub.png", "होम विजेट", "आज का ज़िक्र और तस्बीह"),
        ("10_home_widgets.png", "होम स्क्रीन से", "ऐप खोले बिना गिनें"),
        ("11_prayer_times.png", "अज़ान और नमाज़ वक़्त", "हर फ़र्ज़ के बाद अज़कार"),
    ],
}

PLAY_ORDER = [
    "01_home_auto_tasbih.png",
    "07_popup_auto_tasbih.png",
    "02_azkar_sections.png",
    "04_misbaha_digital.png",
    "10_home_widgets.png",
    "11_prayer_times.png",
    "08_popup_auto_azkar.png",
    "05_settings_hub.png",
]
PLAY_NAMES = [
    "01-auto-tasbih-home.png",
    "02-popup-over-apps.png",
    "03-hisnul-muslim.png",
    "04-digital-misbaha.png",
    "05-home-widgets.png",
    "06-prayer-times.png",
    "07-auto-azkar-popup.png",
    "08-settings-hub.png",
]


def fnt(name: str, size: int) -> ImageFont.FreeTypeFont:
    path = FONTS / name
    if path.exists():
        return ImageFont.truetype(str(path), size)
    bundled = ROOT / "store-assets" / "web" / "fonts" / name
    if bundled.exists():
        return ImageFont.truetype(str(bundled), size)
    return ImageFont.load_default()


def fonts_for(title: str, sub: str):
    sample = title + sub
    if any("\u0600" <= ch <= "\u06FF" for ch in sample):
        return fnt("scheherazade_new_regular.ttf", 40), fnt("scheherazade_new_regular.ttf", 26)
    if any("\u0900" <= ch <= "\u097F" for ch in sample):
        return fnt("NIRMALA.TTF", 40), fnt("NIRMALA.TTF", 24)
    return fnt("segoeuib.ttf", 42), fnt("segoeui.ttf", 26)


def shape_text(text: str) -> str:
    if any("\u0600" <= ch <= "\u06FF" for ch in text):
        import arabic_reshaper
        from bidi.algorithm import get_display
        return get_display(arabic_reshaper.reshape(text))
    return text


def caption(src: Path, title: str, sub: str, dest: Path) -> None:
    shot = Image.open(src).convert("RGB")
    canvas = Image.new("RGB", (W, H), "#163D28")
    draw = ImageDraw.Draw(canvas)
    for y in range(BANNER):
        t = y / BANNER
        draw.line([(0, y), (W, y)], fill=(int(22 + t * 10), int(61 + t * 20), int(40 + t * 10)))
    draw.rectangle([0, BANNER - 6, W, BANNER], fill="#D4AF37")
    title_font, sub_font = fonts_for(title, sub)
    title_s, sub_s = shape_text(title), shape_text(sub)
    tb = draw.textbbox((0, 0), title_s, font=title_font)
    sb = draw.textbbox((0, 0), sub_s, font=sub_font)
    draw.text(((W - (tb[2] - tb[0])) // 2, 70), title_s, fill="#F5F1E9", font=title_font)
    draw.text(((W - (sb[2] - sb[0])) // 2, 150), sub_s, fill="#D4AF37", font=sub_font)
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
    en_phone = ROOT / "store-assets" / "screenshots" / "en"
    for lang, rows in SHOTS.items():
        phone = ROOT / "store-assets" / "screenshots" / lang
        cap = ROOT / "store-assets" / "screenshots" / "captioned" / lang
        for name, title, sub in rows:
            src = phone / name
            if not src.exists():
                src = en_phone / name
            if not src.exists():
                print("skip", lang, name)
                continue
            caption(src, title, sub, cap / name)
        pack(lang)


if __name__ == "__main__":
    main()
