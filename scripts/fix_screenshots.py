#!/usr/bin/env python3
"""Fix screenshot names (RTL nav) and capture missing shots."""
import shutil
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
DEVICE = "emulator-5554"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store-assets" / "screenshots" / "phone"
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
PREFS = ROOT / "scripts" / "adhkar_settings_skip.xml"
# RTL layout: Home rightmost
NAV = {"home": 945, "misbaha": 675, "azkar": 405, "settings": 135}


def adb(*a, check=True):
    subprocess.run(["adb", "-s", DEVICE, *a], check=check, capture_output=True)


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def wait(s=2.5):
    time.sleep(s)


def shot(name):
    r = f"/sdcard/fx_{name}.png"
    adb("shell", "screencap", "-p", r)
    adb("pull", r, str(OUT / f"{name}.png"))
    adb("shell", "rm", "-f", r)
    print("shot", name)


def prep():
    adb("shell", "pm", "disable-user", "--user", "0", "com.android.vending", check=False)
    adb("shell", "am", "force-stop", PKG, check=False)
    adb("install", "-r", str(APK), check=False)
    adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
    for p in ("android.permission.POST_NOTIFICATIONS", "android.permission.RECORD_AUDIO", "android.permission.READ_PHONE_STATE"):
        adb("shell", "pm", "grant", PKG, p, check=False)
    adb("push", str(PREFS), "/data/local/tmp/adhkar_settings.xml", check=False)
    adb("shell", "run-as", PKG, "mkdir", "-p", "shared_prefs", check=False)
    adb("shell", "run-as", PKG, "cp", "/data/local/tmp/adhkar_settings.xml", "shared_prefs/adhkar_settings.xml", check=False)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(5)
    tap(540, 1280)  # Wait on ANR if any
    wait(1.5)


def nav(tab):
    tap(NAV[tab], 2280)
    wait(2)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Reorganize good captures from misnamed files
    mapping = {
        "07_popup_auto_tasbih.png": "_home.png",
        "04_misbaha_digital.png": "_azkar.png",
        "03_azkar_morning_cards.png": "_misbaha.png",
    }
    tmp = OUT / "_tmp"
    tmp.mkdir(exist_ok=True)
    for src, dst in mapping.items():
        s = OUT / src
        if s.exists():
            shutil.copy(s, tmp / dst)

    prep()
    if (tmp / "_home.png").exists():
        shutil.copy(tmp / "_home.png", OUT / "01_home_auto_tasbih.png")
    else:
        nav("home"); shot("01_home_auto_tasbih")

    if (tmp / "_azkar.png").exists():
        shutil.copy(tmp / "_azkar.png", OUT / "02_azkar_sections.png")
    else:
        nav("azkar"); shot("02_azkar_sections")

    nav("azkar")
    tap(540, 520)
    wait(2.5)
    shot("03_azkar_morning_cards")

    if (tmp / "_misbaha.png").exists():
        shutil.copy(tmp / "_misbaha.png", OUT / "04_misbaha_digital.png")
    else:
        nav("misbaha"); shot("04_misbaha_digital")

    nav("settings")
    adb("shell", "input", "swipe", "540", "1900", "540", "700", "400", check=False)
    wait(1.5)
    shot("05_settings_hub")

    adb("shell", "pm", "clear", PKG, check=False)
    wait(1)
    adb("install", "-r", str(APK), check=False)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(5)
    tap(540, 1280)
    wait(1)
    shot("06_onboarding_welcome")

    prep()
    adb(
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        check=False,
    )
    wait(3)
    shot("07_popup_auto_tasbih")

    adb("shell", "input", "keyevent", "4", check=False)
    wait(1)
    adb(
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا",
        "--es", "extra_section_title", "أذكار الصباح",
        "--ez", "extra_auto_azkar", "true",
        check=False,
    )
    wait(3)
    shot("08_popup_auto_azkar")

    adb("shell", "input", "keyevent", "4", check=False)
    wait(1)
    nav("settings")
    tap(540, 700)
    wait(2)
    shot("09_dhikr_of_day_settings")

    shutil.rmtree(tmp, ignore_errors=True)
    print("done")


if __name__ == "__main__":
    main()
