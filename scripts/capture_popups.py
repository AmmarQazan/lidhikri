#!/usr/bin/env python3
"""Capture popup screenshots via foreground services."""
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
D = "emulator-5554"
OUT = Path(__file__).resolve().parents[1] / "store-assets" / "screenshots" / "phone"
PREFS = Path(__file__).resolve().parents[1] / "scripts" / "adhkar_settings_skip.xml"
APK = Path(__file__).resolve().parents[1] / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"


def adb(*a):
    subprocess.run(["adb", "-s", D, *a], check=False)


def wait(s=2.5):
    time.sleep(s)


def shot(name):
    r = f"/sdcard/sv_{name}.png"
    adb("shell", "screencap", "-p", r)
    adb("pull", r, str(OUT / f"{name}.png"))
    print(name, (OUT / f"{name}.png").stat().st_size)


def prep():
    adb("shell", "pm", "disable-user", "--user", "0", "com.android.vending")
    adb("shell", "am", "force-stop", PKG)
    adb("install", "-r", str(APK))
    adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
    for p in ("android.permission.POST_NOTIFICATIONS", "android.permission.RECORD_AUDIO", "android.permission.READ_PHONE_STATE"):
        adb("shell", "pm", "grant", PKG, p)
    adb("push", str(PREFS), "/data/local/tmp/adhkar_settings.xml")
    adb("shell", "run-as", PKG, "mkdir", "-p", "shared_prefs")
    adb("shell", "run-as", PKG, "cp", "/data/local/tmp/adhkar_settings.xml", "shared_prefs/adhkar_settings.xml")
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(4)
    adb("shell", "input", "tap", "540", "1280")
    wait(1)


def main():
    prep()
    # Morning cards
    adb("shell", "input", "tap", "405", "2280")  # azkar tab RTL
    wait(2)
    adb("shell", "input", "tap", "540", "520")
    wait(2.5)
    shot("03_azkar_morning_cards")

    # Tasbih popup via service
    adb("shell", "input", "tap", "945", "2280")  # home
    wait(1)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "start")
    wait(0.5)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "trigger")
    wait(4)
    shot("07_popup_auto_tasbih")

    # Azkar auto popup + audio
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AzkarCollectionPlayService", "--es", "collection_id", "morning")
    wait(4)
    shot("08_popup_auto_azkar")


if __name__ == "__main__":
    main()
