#!/usr/bin/env python3
"""Final store asset capture — no pm clear, block Play Store overlay."""
import subprocess
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
DEVICE = "emulator-5554"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store-assets" / "screenshots" / "phone"
VIDEO_OUT = ROOT / "store-assets" / "video" / "promo.mp4"
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
PREFS = ROOT / "scripts" / "adhkar_settings_skip.xml"


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
    remote = f"/sdcard/fc_{name}.png"
    local = OUT / f"{name}.png"
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, str(local))
    adb("shell", "rm", "-f", remote)
    print("saved", local.name)


def block_play_store():
    for pkg in ("com.android.vending", "com.google.android.gms", "com.google.android.gsf"):
        adb("shell", "am", "force-stop", pkg, check=False)
    adb("shell", "pm", "disable-user", "--user", "0", "com.android.vending", check=False)


def setup_app(skip_onboarding=True):
    block_play_store()
    adb("install", "-r", str(APK), check=False)
    adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
    for p in (
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
    ):
        adb("shell", "pm", "grant", PKG, p, check=False)
    if skip_onboarding:
        adb("push", str(PREFS), "/data/local/tmp/adhkar_settings.xml", check=False)
        adb(
            "shell",
            "run-as",
            PKG,
            "cp",
            "/data/local/tmp/adhkar_settings.xml",
            "shared_prefs/adhkar_settings.xml",
            check=False,
        )
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(3)
    block_play_store()
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(0.5)


def nav(i):
    tap([135, 405, 675, 945][i], 2280)
    wait(2)


def capture_onboarding():
    adb("shell", "pm", "clear", PKG, check=False)
    wait(1)
    block_play_store()
    adb("install", "-r", str(APK), check=False)
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(4)
    block_play_store()
    shot("06_onboarding_welcome")


def preview_popup(tasbih=True):
    nav(3)
    tap(540, 380 if tasbih else 520)
    wait(2)
    for _ in range(3):
        swipe(540, 2000, 540, 500, 450)
        wait(0.6)
    tap(540, 1900)
    wait(2.5)


def record_promo():
    VIDEO_OUT.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/fc_promo.mp4"
    adb("shell", "rm", "-f", remote, check=False)
    setup_app(skip_onboarding=True)
    proc = subprocess.Popen(["adb", "-s", DEVICE, "shell", "screenrecord", "--time-limit", "45", remote])
    wait(2)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "start", check=False)
    wait(0.5)
    adb("shell", "am", "start-foreground-service", "-n", f"{PKG}/.service.AdhkarReminderService", "-a", "trigger", check=False)
    wait(14)
    adb(
        "shell",
        "am",
        "start-foreground-service",
        "-n",
        f"{PKG}/.service.AzkarCollectionPlayService",
        "--es",
        "collection_id",
        "morning",
        check=False,
    )
    wait(28)
    proc.wait(timeout=70)
    wait(2)
    adb("pull", remote, str(VIDEO_OUT), check=False)
    adb("shell", "rm", "-f", remote, check=False)
    sz = VIDEO_OUT.stat().st_size if VIDEO_OUT.exists() else 0
    print(f"video bytes={sz}")


def main():
    setup_app(skip_onboarding=True)

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

    capture_onboarding()
    setup_app(skip_onboarding=True)

    preview_popup(tasbih=True)
    shot("07_popup_auto_tasbih")

    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(1)
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(1)
    preview_popup(tasbih=False)
    shot("08_popup_auto_azkar")

    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(1)
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    wait(1)
    nav(3)
    tap(540, 700)
    wait(2)
    shot("09_dhikr_of_day_settings")

    record_promo()


if __name__ == "__main__":
    main()
