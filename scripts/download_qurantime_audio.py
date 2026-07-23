"""تحميل أصوات أذكار QuranTime إلى assets التطبيق."""
import json
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app" / "src" / "main" / "assets" / "audio" / "azkar"
SCRIPTS = Path(__file__).resolve().parent

needed = set()
for name in ("qurantime_morning.json", "qurantime_evening.json"):
    data = json.loads((SCRIPTS / name).read_text(encoding="utf-8"))
    for item in data:
        needed.add(item["audio_id"])

ASSETS.mkdir(parents=True, exist_ok=True)
base = "https://static.qurantime.net/adhkar"

for audio_id in sorted(needed):
    dest = ASSETS / f"qt_{audio_id:02d}.mp3"
    if dest.exists() and dest.stat().st_size > 1000:
        print(f"skip {dest.name}")
        continue
    url = f"{base}/{audio_id}.mp3"
    print(f"download {url} -> {dest.name}")
    urllib.request.urlretrieve(url, dest)

print(f"done: {len(list(ASSETS.glob('qt_*.mp3')))} files in {ASSETS}")
