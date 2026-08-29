#!/usr/bin/env python3
"""Capture Play Store screenshots with app UI in en / fr / es."""
from __future__ import annotations

import re
import shlex
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
DEVICE = "emulator-5554"
ROOT = Path(__file__).resolve().parents[1]
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
PREFS_DIR = ROOT / "scripts"
OUT_ROOT = ROOT / "store-assets" / "screenshots"
TMP = ROOT / ".tmp-export"

LANGS = ("en", "fr", "es")

NAV = {
    "ar": ("الرئيسية", "المسبحة", "الأذكار", "الإعدادات"),
    "en": ("Home", "Misbaha", "Adhkar sections", "Settings"),
    "fr": ("Accueil", "Misbaha", "Sections d'adhkar", "Paramètres"),
    "es": ("Inicio", "Misbaha", "Secciones de adhkar", "Ajustes"),
}
WIDGETS_HUB = {
    "ar": "ويدجت الشاشة",
    "en": "Home screen widgets",
    "fr": "Widgets",
    "es": "Widgets",
}
MISBAHA_WIDGET = {
    "ar": "مسبحة الشاشة",
    "en": "Misbaha widget",
    "fr": "Widget misbaha",
    "es": "Widget de misbaha",
}
ADD_WIDGET = {
    "ar": "إضافة مسبحة إلى الشاشة الرئيسية",
    "en": "Add misbaha widget to home screen",
    "fr": "Ajouter la misbaha à l'accueil",
    "es": "Añadir misbaha a la pantalla de inicio",
}
TRADITIONAL = {
    "ar": "تقليدية",
    "en": "Traditional",
    "fr": "Traditionnelle",
    "es": "Tradicional",
}
DHIKR_TEXT = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ"
AZKAR_TEXT = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ"
AZKAR_SECTION = {
    "ar": "أذكار الصباح",
    "en": "Morning adhkar",
    "fr": "Adhkar du matin",
    "es": "Adhkar de la mañana",
}
DHIKR_OF_DAY = {
    "ar": "ذكر اليوم",
    "en": "Dhikr of the day",
    "fr": "Dhikr du jour",
    "es": "Dhikr del día",
}
ADD_DHIKR = {
    "ar": "إضافة ذكر اليوم إلى الشاشة الرئيسية",
    "en": "Add dhikr of the day to home screen",
    "fr": "Ajouter le dhikr du jour à l'accueil",
    "es": "Añadir el dhikr del día a la pantalla de inicio",
}


def adb(*a, check=True):
    return subprocess.run(
        ["adb", "-s", DEVICE, *a],
        check=check,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )


def tap_xy(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def wait(s=2.0):
    time.sleep(s)


def focused() -> str:
    r = adb("shell", "dumpsys", "window", check=False)
    for line in (r.stdout or "").splitlines():
        if "mCurrentFocus" in line or "mFocusedApp" in line:
            return line
    return ""


def ensure_app():
    if PKG in focused():
        return
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    wait(4)


def dump_ui() -> str:
    TMP.mkdir(parents=True, exist_ok=True)
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", check=False)
    dest = TMP / "ui.xml"
    adb("pull", "/sdcard/ui.xml", str(dest), check=False)
    if dest.exists():
        return dest.read_text(encoding="utf-8", errors="replace")
    return ""


def _nodes(xml: str):
    pat = re.compile(
        r'(?:text|content-desc)="([^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
        r'|bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*(?:text|content-desc)="([^"]*)"'
    )
    seen = set()
    for m in pat.finditer(xml):
        if m.group(1) is not None:
            text, x1, y1, x2, y2 = m.group(1), *map(int, m.group(2, 3, 4, 5))
        else:
            x1, y1, x2, y2 = map(int, m.group(6, 7, 8, 9))
            text = m.group(10)
        if text:
            seen.add((text, x1, y1, x2, y2))
            yield text, (x1 + x2) // 2, (y1 + y2) // 2, y1, y2
    desc = re.compile(
        r'content-desc="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
    )
    for m in desc.finditer(xml):
        text, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
        key = (text, x1, y1, x2, y2)
        if key not in seen:
            yield text, (x1 + x2) // 2, (y1 + y2) // 2, y1, y2


def find_text(xml: str, needle: str, *, contains=False, min_y=0, max_y=9999):
    needle_l = needle.lower()
    for text, x, y, y1, y2 in _nodes(xml):
        if y1 < min_y or y2 > max_y:
            continue
        if contains:
            if needle_l in text.lower():
                return x, y, text
        elif text == needle:
            return x, y, text
    return None


def tap_text(needle: str, *, contains=False, min_y=0, max_y=9999, retries=4) -> bool:
    for _ in range(retries):
        xml = dump_ui()
        hit = find_text(xml, needle, contains=contains, min_y=min_y, max_y=max_y)
        if hit:
            x, y, text = hit
            shown = text.encode("ascii", "replace").decode("ascii")
            print(f"TAP {shown!r} -> {x},{y}", flush=True)
            tap_xy(x, y)
            wait(1.6)
            return True
        wait(0.8)
    print("MISS", needle.encode("ascii", "replace").decode("ascii"), flush=True)
    return False


NAV_X = (127, 405, 678, 945)
NAV_X_RTL = (945, 675, 405, 135)
NAV_Y = 2274


def tap_nav(lang: str, index: int) -> bool:
    ensure_app()
    label = NAV[lang][index]
    if tap_text(label, min_y=2000, retries=2):
        return True
    xs = NAV_X_RTL if lang == "ar" else NAV_X
    print("NAV fallback", lang, index, label)
    tap_xy(xs[index], NAV_Y)
    wait(1.6)
    return True


def back(n=1):
    for _ in range(n):
        if PKG not in focused():
            ensure_app()
            return
        adb("shell", "input", "keyevent", "4", check=False)
        wait(0.8)


def shot(out: Path, name: str):
    out.mkdir(parents=True, exist_ok=True)
    r = f"/sdcard/st_{name}.png"
    adb("shell", "screencap", "-p", r)
    adb("pull", r, str(out / f"{name}.png"))
    adb("shell", "rm", "-f", r, check=False)
    print(out.name, name)


def write_prefs(lang: str, onboarding: bool) -> Path:
    path = PREFS_DIR / f"adhkar_settings_{lang}.xml"
    done = "true" if onboarding else "false"
    path.write_text(
        "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n"
        "<map>\n"
        f'    <boolean name="onboarding_completed" value="{done}" />\n'
        f'    <string name="app_language">{lang}</string>\n'
        '    <boolean name="auto_azkar_enabled" value="true" />\n'
        '    <boolean name="service_enabled" value="true" />\n'
        '    <string name="number_digit_style">LATIN</string>\n'
        '    <float name="popup_font_scale" value="0.82" />\n'
        '    <long name="azkar_selected_reciter" value="1" />\n'
        "</map>\n",
        encoding="utf-8",
    )
    return path


def push_prefs(path: Path):
    adb("push", str(path), "/data/local/tmp/adhkar_settings.xml", check=False)
    adb("shell", "run-as", PKG, "mkdir", "-p", "shared_prefs", check=False)
    adb("shell", "run-as", PKG, "cp", "/data/local/tmp/adhkar_settings.xml", "shared_prefs/adhkar_settings.xml", check=False)


def prep(lang: str, onboarding: bool = True, reinstall: bool = False):
    prefs = write_prefs(lang, onboarding)
    adb("shell", "pm", "disable-user", "--user", "0", "com.android.vending", check=False)
    adb("shell", "am", "force-stop", PKG, check=False)
    if reinstall and APK.exists():
        adb("install", "-r", str(APK), check=False)
    adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow", check=False)
    for p in (
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
    ):
        adb("shell", "pm", "grant", PKG, p, check=False)
    push_prefs(prefs)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    wait(5)
    ensure_app()
    xml = dump_ui()
    if "isn't responding" in xml.lower() or "ANR" in xml:
        tap_text("Wait", contains=True) or tap_text("Close app", contains=True)
        wait(1)
        ensure_app()


def overlay(lang: str, azkar: bool):
    text = AZKAR_TEXT if azkar else DHIKR_TEXT
    cmd = (
        f"am start -n {PKG}/.ui.overlay.OverlayActivity "
        f"--es extra_text {shlex.quote(text)}"
    )
    if azkar:
        section = AZKAR_SECTION.get(lang, AZKAR_SECTION["ar"])
        cmd += f" --es extra_section_title {shlex.quote(section)} --ez extra_auto_azkar true"
    adb("shell", cmd, check=False)
    wait(2.8)


def azkar_sections_ready(lang: str) -> bool:
    xml = dump_ui()
    markers = ("Morning adhkar", "Adhkar du matin", "Adhkar de la mañana", "أذكار الصباح", "Evening")
    return any(m in xml for m in markers) and "Navigate between" not in xml


def open_azkar_list(lang: str):
    tap_nav(lang, 2)
    wait(1)
    for _ in range(4):
        if azkar_sections_ready(lang):
            return
        back(1)
        wait(0.6)
    tap_nav(lang, 2)


def open_settings_hub(lang: str):
    tap_nav(lang, 3)
    wait(1)
    xml = dump_ui()
    hint = {
        "ar": "اختر القسم الذي تريد ضبطه",
        "en": "Choose a section to configure",
        "fr": "Choisissez une section à configurer",
        "es": "Elige la sección que quieres configurar",
    }.get(lang)
    if hint and hint not in xml:
        back(3)
        tap_nav(lang, 3)
        wait(1.2)


def go_home():
    adb("shell", "input", "keyevent", "3", check=False)
    wait(1.8)


def remove_extra_home_widgets():
    """Keep one traditional misbaha; drop extra copies so dhikr of day can fit."""
    go_home()
    for _ in range(6):
        xml = dump_ui()
        hits = [
            (x, y, text)
            for text, x, y, y1, y2 in _nodes(xml)
            if "متبقي" in text or "remaining" in text.lower() or "restant" in text.lower()
        ]
        if len(hits) <= 1:
            return
        _, y, _ = sorted(hits, key=lambda h: h[1])[-1]
        adb("shell", "input", "swipe", "540", str(y), "540", str(y), "900", check=False)
        wait(1.2)
        if not (
            tap_text("Remove", retries=2)
            or tap_text("إزالة", retries=1)
            or tap_text("Supprimer", retries=1)
            or tap_text("Eliminar", retries=1)
        ):
            tap_xy(540, 180)
            wait(1.2)
        wait(1.0)


def pin_dhikr_of_day(lang: str):
    open_settings_hub(lang)
    tap_text(WIDGETS_HUB[lang], contains=False)
    wait(1.0)
    tap_text(DHIKR_OF_DAY[lang], contains=True)
    wait(1.2)
    adb("shell", "input", "swipe", "540", "1900", "540", "400", "400", check=False)
    wait(1.0)
    if not tap_text(ADD_DHIKR[lang], contains=True):
        tap_text("Add dhikr", contains=True)
    wait(2.0)
    tap_text("Add to home screen", contains=True) or tap_text("Add", contains=False)
    wait(2.0)


def pin_home_widgets(lang: str, out: Path):
    remove_extra_home_widgets()
    open_settings_hub(lang)
    tap_text(WIDGETS_HUB[lang], contains=False)
    wait(1.2)
    shot(out, "09_widgets_hub")
    tap_text(MISBAHA_WIDGET[lang], contains=True)
    wait(1.2)
    tap_text(TRADITIONAL[lang], contains=False)
    wait(0.6)
    tap_text(ADD_WIDGET[lang], contains=True)
    wait(2)
    tap_text("Add", contains=False) or tap_text("ADD", contains=False)
    wait(2)
    pin_dhikr_of_day(lang)
    go_home()
    wait(2)
    shot(out, "10_home_widgets")
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    wait(2)


def capture_lang(lang: str, reinstall: bool = False) -> None:
    out = OUT_ROOT / lang
    print("===", lang)
    prep(lang, onboarding=True, reinstall=reinstall)
    tap_nav(lang, 0)
    wait(1)
    shot(out, "01_home_auto_tasbih")

    open_azkar_list(lang)
    shot(out, "02_azkar_sections")
    if not tap_text("Morning adhkar", contains=True):
        tap_text("Adhkar du matin", contains=True) or tap_text("Adhkar de la mañana", contains=True) or tap_text("أذكار الصباح", contains=True)
    wait(2)
    shot(out, "03_azkar_morning_cards")

    tap_nav(lang, 1)
    wait(1.5)
    shot(out, "04_misbaha_digital")

    open_settings_hub(lang)
    shot(out, "05_settings_hub")
    pin_home_widgets(lang, out)

    adb("shell", "am", "force-stop", PKG, check=False)
    wait(0.8)
    push_prefs(write_prefs(lang, onboarding=False))
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", check=False)
    wait(5)
    shot(out, "06_onboarding_welcome")

    prep(lang, onboarding=True, reinstall=False)
    overlay(lang, azkar=False)
    shot(out, "07_popup_auto_tasbih")
    back(1)
    wait(0.8)
    overlay(lang, azkar=True)
    shot(out, "08_popup_auto_azkar")
    back(1)


def main() -> None:
    capture_lang("en", reinstall=True)
    capture_lang("fr", reinstall=False)
    capture_lang("es", reinstall=False)


if __name__ == "__main__":
    main()
