#!/usr/bin/env python3
"""Fix unescaped apostrophes in Android string resources."""
import re
from pathlib import Path

def fix_file(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    lines = []
    for line in text.splitlines():
        m = re.match(r'(\s*<string name="[^"]+">)(.*)(</string>\s*)$', line)
        if not m:
            lines.append(line)
            continue
        prefix, content, suffix = m.groups()
        # Escape apostrophes not already escaped
        fixed = re.sub(r"(?<!\\)'", r"\\'", content)
        lines.append(prefix + fixed + suffix)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Fixed {path}")

for locale in ("values-fr", "values-es"):
    fix_file(Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / locale / "strings.xml")
