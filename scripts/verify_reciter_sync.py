#!/usr/bin/env python3
"""تحقق أن إضافة قارئ جديد تُصدَّر ضمن حزمة المزامنة."""
from __future__ import annotations

import json
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXPORT = ROOT / "scripts" / "export_remote_content.py"


def pull_db(dest: Path) -> None:
    result = subprocess.run(
        ["adb", "exec-out", "run-as", "com.greendome.adhkar", "cat", "databases/adhkar.db"],
        capture_output=True,
        check=True,
    )
    dest.write_bytes(result.stdout)
    if len(result.stdout) < 1000:
        raise SystemExit("قاعدة البيانات المسحوبة فارغة أو تالفة")


def main() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        db_path = Path(tmp) / "adhkar.db"
        pull_db(db_path)

        conn = sqlite3.connect(db_path)
        try:
            conn.execute(
                """
                INSERT OR REPLACE INTO reciter
                (id, nameAr, nameEn, nameFr, nameEs, isBuiltin, isActive)
                VALUES (99, 'قارئ تجريبي', 'Test Reciter', 'Test', 'Test', 0, 1)
                """
            )
            conn.commit()
            rows = conn.execute("SELECT id, nameAr FROM reciter WHERE id = 99").fetchall()
        finally:
            conn.close()
        if not rows:
            print("FAIL: لم يُدرج القارئ التجريبي", file=sys.stderr)
            return 1

        out = subprocess.run(
            [sys.executable, str(EXPORT), "--db", str(db_path), "--version", "999"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            check=True,
        )
        bundle_path = ROOT / "remote-content" / "bundles" / "v999" / "content.json"
        bundle = json.loads(bundle_path.read_text(encoding="utf-8"))
        found = [r for r in bundle["reciters"] if r["id"] == 99]
        if not found:
            print("FAIL: القارئ التجريبي غير موجود في حزمة التصدير", file=sys.stderr)
            return 1

        print("OK: القارئ الجديد يظهر في حزمة المزامنة:", found[0]["nameAr"])
        # تنظيف حزمة الاختبار
        import shutil
        shutil.rmtree(ROOT / "remote-content" / "bundles" / "v999", ignore_errors=True)
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
