#!/usr/bin/env python3
"""Audit locale strings: keys, placeholders, leftover English, content overlay."""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
PLACEHOLDER = re.compile(r"%\d+\$[sd]|%\d+\$\.\d+f|%\d+\$02d|%\d+d%%")
ALLOW = {
    "Lidhikri", "Allah", "Allahu", "Akbar", "Hisnul", "Muslim", "Muhammad",
    "Firebase", "WhatsApp", "YouTube", "Telegram", "Signal", "Messenger",
    "Meet", "Skype", "TTS", "PIN", "OK", "Uthmani", "Indo-Pak", "Bengali",
    "Default", "Gold", "Teal", "Navy", "Maroon", "Amber", "JSON",
}

SCRIPTS = {
    "tr": ("latin", re.compile(r"[A-Za-zÇĞİÖŞÜçğıöşü]")),
    "ur": ("arabic", re.compile(r"[\u0600-\u06FF]")),
    "in": ("latin", re.compile(r"[A-Za-z]")),
    "hi": ("devanagari", re.compile(r"[\u0900-\u097F]")),
}


def load(folder: str) -> dict[str, str]:
    root = ET.parse(RES / folder / "strings.xml")
    return {el.get("name"): (el.text or "") for el in root.findall("string")}


def placeholders(text: str) -> list[str]:
    return sorted(PLACEHOLDER.findall(text))


def main() -> None:
    en = load("values")
    locales = {
        "ar": load("values-ar"),
        "fr": load("values-fr"),
        "es": load("values-es"),
        "tr": load("values-tr"),
        "ur": load("values-ur"),
        "in": load("values-in"),
        "hi": load("values-hi"),
    }
    print("keys en", len(en))
    issues = 0
    for loc, data in locales.items():
        missing = sorted(set(en) - set(data))
        extra = sorted(set(data) - set(en))
        ph = [k for k in en if k in data and placeholders(en[k]) != placeholders(data[k])]
        print(f"{loc}: {len(data)} keys, missing {len(missing)}, extra {len(extra)}, placeholder mismatch {len(ph)}")
        if missing:
            print("  missing", missing[:8])
            issues += len(missing)
        if ph:
            print("  placeholders", ph[:8])
            issues += len(ph)
        if loc in SCRIPTS:
            kind, rx = SCRIPTS[loc]
            weak = []
            for k, text in data.items():
                if k.startswith("font_style") or k in {"app_name", "onboarding_font_preview"}:
                    continue
                if not rx.search(text) and not any(a in text for a in ALLOW):
                    if loc in {"tr", "in"} and re.search(r"[A-Za-z]", text):
                        continue
                    weak.append(k)
            if loc in {"ur", "hi"} and weak:
                print(f"  no native script ({kind}) sample", weak[:12])

    print("overlay collections: morning/evening/sleep/adhan/home/jawami/favorites/wake_up/after_prayer")
    print("issues", issues)


if __name__ == "__main__":
    main()
