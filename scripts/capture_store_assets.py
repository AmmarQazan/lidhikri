#!/usr/bin/env python3
"""Capture Google Play screenshots and promo video via ADB."""
import subprocess
import threading
import time
from pathlib import Path

PKG = "com.greendome.adhkar"
DEVICE = "emulator-5554"
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store-assets" / "screenshots" / "phone"
VIDEO_OUT = ROOT / "store-assets" / "video" / "promo.mp4"
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
if not APK.exists():
    APK = ROOT / "preview-apk" / "adhkar-preview-v1.0.2.apk"

# 1080x2400 emulator
NAV_Y = 2280
NAV_X = [135, 405, 675, 945]
NEXT_BTN = (900, 2220)
START_BTN = (540, 2220)


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["adb", "-s", DEVICE, *args],
        check=check,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )


def tap(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(x), str(y))


def swipe(x1: int, y1: int, x2: int, y2: int, ms: int = 350) -> None:
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))


def wait(sec: float = 1.8) -> None:
    time.sleep(sec)


def shot(name: str) -> Path:
    OUT.mkdir(parents=True, exist_ok=True)
    remote = f"/sdcard/cap_{name}.png"
    local = OUT / f"{name}.png"
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, str(local))
    adb("shell", "rm", "-f", remote)
    print(f"screenshot: {local}")
    return local


def install_and_grant() -> None:
    adb("install", "-r", str(APK))
    adb("shell", "appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
    for perm in (
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
    ):
        adb("shell", "pm", "grant", PKG, perm, check=False)


def launch() -> None:
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(2.5)


def skip_onboarding() -> None:
    adb("shell", "pm", "clear", PKG, check=False)
    wait(1)
    install_and_grant()
    launch()
    # Welcome -> language -> font -> theme -> settings -> ready (6 next + start)
    for _ in range(5):
        tap(*NEXT_BTN)
        wait(1.2)
    tap(*START_BTN)
    wait(2.5)


def nav_tab(i: int) -> None:
    tap(NAV_X[i], NAV_Y)
    wait(1.8)


def start_tasbih_service() -> None:
    adb(
        "shell", "am", "start-foreground-service",
        "-n", f"{PKG}/.service.AdhkarReminderService",
        "-a", "start",
        check=False,
    )
    wait(0.5)


def trigger_tasbih_with_audio() -> None:
    start_tasbih_service()
    adb(
        "shell", "am", "start-foreground-service",
        "-n", f"{PKG}/.service.AdhkarReminderService",
        "-a", "trigger",
        check=False,
    )


def trigger_auto_azkar_with_audio() -> None:
    adb(
        "shell", "am", "start-foreground-service",
        "-n", f"{PKG}/.service.AzkarCollectionPlayService",
        "--es", "collection_id", "morning",
        check=False,
    )


def record_video() -> None:
    VIDEO_OUT.parent.mkdir(parents=True, exist_ok=True)
    remote = "/sdcard/sabbih_promo.mp4"

    def run_recorder() -> None:
        subprocess.run(
            ["adb", "-s", DEVICE, "shell", "screenrecord", "--time-limit", "50", remote],
            check=False,
        )

    t = threading.Thread(target=run_recorder, daemon=True)
    t.start()
    wait(2)

    # Tasbih popup + audio
    trigger_tasbih_with_audio()
    wait(10)

    # Auto azkar popup + TTS/audio
    trigger_auto_azkar_with_audio()
    wait(35)

    t.join(timeout=60)
    wait(2)
    adb("pull", remote, str(VIDEO_OUT), check=False)
    adb("shell", "rm", "-f", remote, check=False)
    print(f"video: {VIDEO_OUT}")


def capture_screenshots() -> None:
    skip_onboarding()

    nav_tab(0)
    shot("01_home_auto_tasbih")

    nav_tab(2)
    shot("02_azkar_sections")
    tap(540, 560)
    wait(2.2)
    shot("03_azkar_morning_cards")

    nav_tab(1)
    shot("04_misbaha_digital")

    nav_tab(3)
    swipe(540, 1900, 540, 700, 400)
    wait(1)
    shot("05_settings_hub")

    # Onboarding welcome (fresh install)
    adb("shell", "pm", "clear", PKG, check=False)
    wait(1)
    install_and_grant()
    launch()
    wait(4)
    shot("06_onboarding_welcome")

    # Overlay previews
    skip_onboarding()
    adb(
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
        check=False,
    )
    wait(2.5)
    shot("07_popup_auto_tasbih")

    adb(
        "shell", "am", "start",
        "-n", f"{PKG}/.ui.overlay.OverlayActivity",
        "--es", "extra_text",
        "اللَّهُمَّ بِكَ أَصْبَحْنَا وَبِكَ أَمْسَيْنَا وَبِكَ نَحْيَا وَبِكَ نَمُوتُ",
        "--es", "extra_section_title", "أذكار الصباح",
        "--ez", "extra_auto_azkar", "true",
        check=False,
    )
    wait(2.5)
    shot("08_popup_auto_azkar")

    # Dhikr of day settings
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    wait(1.5)
    nav_tab(3)
    tap(540, 420)
    wait(2)
    shot("09_dhikr_of_day_settings")


def main() -> None:
    print("=== Screenshots ===")
    capture_screenshots()
    print("=== Promo video ===")
    skip_onboarding()
    record_video()
    print("Done.")


if __name__ == "__main__":
    main()
