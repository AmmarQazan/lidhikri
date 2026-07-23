#!/usr/bin/env python3
"""Final capture with debug-exported overlay/services."""
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
D = "emulator-5554"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store-assets" / "screenshots" / "phone"
VIDEO = ROOT / "store-assets" / "video" / "promo.mp4"
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
PREFS = ROOT / "scripts" / "adhkar_settings_skip.xml"
TASBIH = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ"
AZKAR = "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ"
SECTION = "أذكار الصباح"


def adb(args):
    subprocess.run(["adb", "-s", D, *args], check=False)


def wait(s=2.5):
    time.sleep(s)


def tap(x, y):
    adb(["shell", "input", "tap", str(x), str(y)])


def shot(name):
    OUT.mkdir(parents=True, exist_ok=True)
    remote = f"/sdcard/final_{name}.png"
    adb(["shell", "screencap", "-p", remote])
    adb(["pull", remote, str(OUT / f"{name}.png")])
    print("shot", name)


def prep():
    adb(["shell", "pm", "disable-user", "--user", "0", "com.android.vending"])
    adb(["shell", "am", "force-stop", PKG])
    adb(["install", "-r", str(APK)])
    adb(["shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow"])
    for p in (
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
    ):
        adb(["shell", "pm", "grant", PKG, p])
    adb(["push", str(PREFS), "/data/local/tmp/adhkar_settings.xml"])
    adb(["shell", "run-as", PKG, "mkdir", "-p", "shared_prefs"])
    adb(["shell", "run-as", PKG, "cp", "/data/local/tmp/adhkar_settings.xml", "shared_prefs/adhkar_settings.xml"])
    adb(["shell", "am", "start", "-n", f"{PKG}/.MainActivity"])
    wait(4)
    tap(540, 1280)
    wait(1)


def overlay_tasbih():
    adb([
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", TASBIH,
    ])


def overlay_azkar():
    adb([
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", AZKAR,
        "--es", "extra_section_title", SECTION,
        "--ez", "extra_auto_azkar", "true",
    ])


def trigger_tasbih_audio():
    adb(["shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "start"])
    wait(0.5)
    adb(["shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "trigger"])


def trigger_azkar_audio():
    adb([
        "shell", "am", "start-foreground-service",
        "-n", f"{PKG}/.service.AzkarCollectionPlayService",
        "--es", "collection_id", "morning",
    ])


def main():
    prep()

    tap(405, 2280)
    wait(2)
    tap(540, 520)
    wait(2.5)
    shot("03_azkar_morning_cards")

    overlay_tasbih()
    wait(3)
    shot("07_popup_auto_tasbih")

    adb(["shell", "input", "keyevent", "4"])
    wait(1)
    overlay_azkar()
    wait(3)
    shot("08_popup_auto_azkar")

    VIDEO.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/final_promo.mp4"
    adb(["shell", "rm", "-f", remote])
    proc = subprocess.Popen(["adb", "-s", D, "shell", "screenrecord", "--time-limit", "45", remote])
    wait(2)
    overlay_tasbih()
    wait(2)
    trigger_tasbih_audio()
    wait(12)
    overlay_azkar()
    wait(2)
    trigger_azkar_audio()
    wait(25)
    proc.wait(timeout=70)
    wait(2)
    adb(["pull", remote, str(VIDEO)])
    print("video", VIDEO.stat().st_size)


if __name__ == "__main__":
    main()
