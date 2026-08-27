#!/usr/bin/env python3
"""Write paste-ready Play listing files + copy privacy into play-upload."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "store-assets" / "listings.md"
OUT = ROOT / "store-assets" / "play-upload" / "listings"
PRIVACY = ROOT / "store-assets" / "privacy.html"


def main() -> None:
    text = SRC.read_text(encoding="utf-8")
    blocks = {
        "ar": ("## العربية (ar)", "## English (en)"),
        "en": ("## English (en)", "## Français (fr)"),
        "fr": ("## Français (fr)", "## Español (es)"),
        "es": ("## Español (es)", "## لقطات الشاشة"),
    }
    OUT.mkdir(parents=True, exist_ok=True)
    for loc, (start, end) in blocks.items():
        i = text.index(start)
        j = text.index(end)
        (OUT / f"{loc}.txt").write_text(text[i:j].strip() + "\n", encoding="utf-8")
        print("wrote", loc)

    shutil.copy2(PRIVACY, ROOT / "store-assets" / "play-upload" / "privacy.html")
    print("privacy copied")


if __name__ == "__main__":
    main()
