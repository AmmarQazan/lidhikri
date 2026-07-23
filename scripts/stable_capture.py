#!/usr/bin/env python3
"""Stable capture: dismiss ANR, use overlay intents, record promo."""
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
DEVICE = "emulator-5554"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store-assets" / "screenshots" / "phone"
VIDEO = ROOT / "store-assets" / "video" / "promo.mp4"
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
PREFS = ROOT / "scripts" / "adhkar_settings_skip.xml"


def adb(*a, check=True):
    subprocess.run(["adb", "-s", DEVICE, *a], check=check, capture_output=True)


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def wait(s=2.5):
    time.sleep(s)


def dismiss_anr():
    tap(540, 1280)  # Wait
    wait(1)


def shot(name):
    OUT.mkdir(parents=True, exist_ok=True)
    r = f"/sdcard/st_{name}.png"
    adb("shell", "screencap", "-p", r)
    adb("pull", r, str(OUT / f"{name}.png"))
    adb("shell", "rm", "-f", r)
    print(name)


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
    dismiss_anr()
    wait(2)


def nav(i):
    tap([135, 405, 675, 945][i], 2280)
    wait(2)
    dismiss_anr()


def overlay_tasbih():
    adb(
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        check=False,
    )
    wait(3)


def overlay_azkar():
    adb(
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا",
        "--es", "extra_section_title", "أذكار الصباح",
        "--ez", "extra_auto_azkar", "true",
        check=False,
    )
    wait(3)


def main():
    prep()
    nav(0); shot("01_home_auto_tasbih")
    nav(2); shot("02_azkar_sections")
    tap(540, 560); wait(2.5); shot("03_azkar_morning_cards")
    nav(1); shot("04_misbaha_digital")
    nav(3); adb("shell", "input", "swipe", "540", "1900", "540", "700", "400", check=False); wait(1.5); shot("05_settings_hub")

    adb("shell", "pm", "clear", PKG, check=False); wait(1)
    adb("install", "-r", str(APK), check=False)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity"); wait(5)
    adb("shell", "pm", "disable-user", "--user", "0", "com.android.vending", check=False)
    shot("06_onboarding_welcome")

    prep()
    overlay_tasbih(); shot("07_popup_auto_tasbih")
    adb("shell", "input", "keyevent", "4", check=False); wait(1)
    overlay_azkar(); shot("08_popup_auto_azkar")

    adb("shell", "input", "keyevent", "4", check=False); wait(1)
    nav(3); tap(540, 700); wait(2); shot("09_dhikr_of_day_settings")

    # Promo video
    VIDEO.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/st_promo.mp4"
    adb("shell", "rm", "-f", remote, check=False)
    prep()
    p = subprocess.Popen(["adb", "-s", DEVICE, "shell", "screenrecord", "--time-limit", "42", remote])
    wait(2)
    overlay_tasbih()
    wait(3)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "start", check=False)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "trigger", check=False)
    wait(12)
    overlay_azkar()
    wait(2)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AzkarCollectionPlayService", "--es", "collection_id", "morning", check=False)
    wait(22)
    p.wait(timeout=60)
    wait(2)
    adb("pull", remote, str(VIDEO), check=False)
    print("video", VIDEO.stat().st_size if VIDEO.exists() else 0)


if __name__ == "__main__":
    main()
