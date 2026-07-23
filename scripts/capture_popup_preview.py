#!/usr/bin/env python3
"""Capture popups via in-app Preview on screen button."""
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


def wait(s=2.0):
    time.sleep(s)


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def swipe():
    adb("shell", "input", "swipe", "540", "2000", "540", "500", "450")


def shot(name):
    r = f"/sdcard/pv_{name}.png"
    adb("shell", "screencap", "-p", r)
    adb("pull", r, str(OUT / f"{name}.png"))
    print(name)


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
    tap(540, 1280)
    wait(1)


def open_preview(tasbih=True):
    tap(135, 2280)  # settings RTL
    wait(2)
    tap(540, 380 if tasbih else 520)
    wait(2)
    for _ in range(4):
        swipe()
        wait(0.7)
    tap(540, 2050)  # Preview on screen button
    wait(3)


def main():
    prep()
    open_preview(tasbih=True)
    shot("07_popup_auto_tasbih")
    tap(540, 1280)  # dismiss overlay if tap outside
    wait(1)
    adb("shell", "input", "keyevent", "4")
    wait(1)
    adb("shell", "input", "keyevent", "4")
    wait(1)
    open_preview(tasbih=False)
    shot("08_popup_auto_azkar")


if __name__ == "__main__":
    main()
