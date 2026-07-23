#!/usr/bin/env python3
"""Re-capture failed store assets after dismissing Play Store dialog."""
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
DEVICE = "emulator-5554"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store-assets" / "screenshots" / "phone"
VIDEO_OUT = ROOT / "store-assets" / "video" / "promo.mp4"
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"


def adb(*args, check=True):
    return subprocess.run(["adb", "-s", DEVICE, *args], check=check, capture_output=True, text=True)


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def swipe(x1, y1, x2, y2, ms=400):
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))


def wait(s=2.0):
    time.sleep(s)


def shot(name):
    OUT.mkdir(parents=True, exist_ok=True)
    remote = f"/sdcard/rc_{name}.png"
    local = OUT / f"{name}.png"
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, str(local))
    adb("shell", "rm", "-f", remote)
    print("saved", local.name)


def dismiss_play_dialog():
    adb("shell", "pm", "disable-user", "--user", "0", "com.android.vending", check=False)
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(0.5)


def skip_onboarding():
    adb("shell", "pm", "clear", PKG, check=False)
    wait(1)
    adb("install", "-r", str(APK), check=False)
    adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
    for p in ("android.permission.POST_NOTIFICATIONS", "android.permission.RECORD_AUDIO", "android.permission.READ_PHONE_STATE"):
        adb("shell", "pm", "grant", PKG, p, check=False)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(3)
    dismiss_play_dialog()
    for _ in range(5):
        tap(900, 2220)
        wait(1.0)
    tap(540, 2220)
    wait(2)


def nav(i):
    tap([135, 405, 675, 945][i], 2280)
    wait(2)


def preview_tasbih_popup():
    nav(3)
    tap(540, 380)  # Auto tasbih card
    wait(2)
    swipe(540, 2000, 540, 400, 500)
    wait(1)
    swipe(540, 2000, 540, 400, 500)
    wait(1)
    tap(540, 1750)  # Preview on screen button area
    wait(2)


def preview_azkar_popup():
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(1)
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(1)
    nav(3)
    tap(540, 520)  # Auto azkar card
    wait(2)
    swipe(540, 2000, 540, 400, 500)
    wait(1)
    swipe(540, 2000, 540, 400, 500)
    wait(1)
    tap(540, 1750)
    wait(2)


def record_promo():
    VIDEO_OUT.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/sabbih_promo2.mp4"
    adb("shell", "rm", "-f", remote, check=False)
    proc = subprocess.Popen(["adb", "-s", DEVICE, "shell", "screenrecord", "--time-limit", "40", remote])
    wait(3)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "start", check=False)
    wait(0.5)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "trigger", check=False)
    wait(12)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AzkarCollectionPlayService", "--es", "collection_id", "morning", check=False)
    wait(25)
    proc.wait(timeout=60)
    wait(2)
    adb("pull", remote, str(VIDEO_OUT), check=False)
    adb("shell", "rm", "-f", remote, check=False)
    print("video", VIDEO_OUT, VIDEO_OUT.stat().st_size if VIDEO_OUT.exists() else 0)


def main():
    skip_onboarding()

    nav(0)
    shot("01_home_auto_tasbih")

    nav(2)
    shot("02_azkar_sections")
    tap(540, 560)
    wait(2.5)
    shot("03_azkar_morning_cards")

    nav(1)
    shot("04_misbaha_digital")

    nav(3)
    swipe(540, 1900, 540, 700, 400)
    wait(1.5)
    shot("05_settings_hub")

    adb("shell", "pm", "clear", PKG, check=False)
    wait(1)
    adb("install", "-r", str(APK), check=False)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(4)
    dismiss_play_dialog()
    shot("06_onboarding_welcome")

    skip_onboarding()
    preview_tasbih_popup()
    shot("07_popup_auto_tasbih")

    preview_azkar_popup()
    shot("08_popup_auto_azkar")

    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(1)
    nav(3)
    tap(540, 700)
    wait(2)
    shot("09_dhikr_of_day_settings")

    skip_onboarding()
    record_promo()


if __name__ == "__main__":
    main()
