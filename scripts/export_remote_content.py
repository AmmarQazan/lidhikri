#!/usr/bin/env python3
"""تصدير قاعدة بيانات التطبيق إلى حزمة مزامنة على GitHub."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sqlite3
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REMOTE_ROOT = ROOT / "remote-content"
AUDIO_OUT = REMOTE_ROOT / "audio"
ASSETS_AUDIO = ROOT / "app" / "src" / "main" / "assets" / "audio"
DEFAULT_BASE_URL = "https://raw.githubusercontent.com/AmmarQazan/sabbih/main/remote-content"

TABLES = {
    "dhikr": "SELECT * FROM dhikr WHERE isDefault = 1 ORDER BY sortOrder, id",
    "reciters": "SELECT * FROM reciter ORDER BY id",
    "reciterAudio": """
        SELECT ra.* FROM reciter_audio ra
        INNER JOIN dhikr d ON d.id = ra.dhikrId AND d.isDefault = 1
        ORDER BY ra.id
    """,
    "reciterAzkarAudio": """
        SELECT raa.* FROM reciter_azkar_audio raa
        INNER JOIN azkar_item ai ON ai.id = raa.azkarItemId
        WHERE ai.collectionId != 'favorites'
        ORDER BY raa.id
    """,
    "collections": "SELECT * FROM adhkar_collection WHERE id != 'favorites' ORDER BY sortOrder",
    "azkarItems": """
        SELECT * FROM azkar_item
        WHERE collectionId != 'favorites'
        ORDER BY collectionId, sortOrder, id
    """,
}


def row_to_dict(cursor: sqlite3.Cursor, row: sqlite3.Row) -> dict:
    item = {key: row[key] for key in row.keys()}
    bool_fields = {
        "isEnabled", "isDefault", "isLongForm", "isDownloaded",
        "displayPopup", "displayNotification", "displayLockScreen",
        "displayAudioOnly", "displayAudioText", "isBuiltin", "isActive",
        "autoPlayAllowed", "autoPlayEnabled", "useTtsAutoPlay",
    }
    for key, value in list(item.items()):
        if isinstance(value, bytes):
            item[key] = value.decode("utf-8", errors="replace")
        elif key in bool_fields and isinstance(value, int):
            item[key] = bool(value)
    return item


def fetch_table(conn: sqlite3.Connection, query: str) -> list[dict]:
    conn.row_factory = sqlite3.Row
    return [row_to_dict(conn, row) for row in conn.execute(query)]


def asset_to_remote_url(asset_path: str, base_url: str) -> str:
    path = asset_path.replace("\\", "/").lstrip("/")
    if path.startswith("audio/"):
        path = path[len("audio/") :]
    return f"{base_url.rstrip('/')}/audio/{path}"


def copy_audio_assets() -> int:
    AUDIO_OUT.mkdir(parents=True, exist_ok=True)
    copied = 0
    if not ASSETS_AUDIO.exists():
        return copied
    for src in ASSETS_AUDIO.rglob("*.mp3"):
        rel = src.relative_to(ASSETS_AUDIO)
        dest = AUDIO_OUT / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        if not dest.exists() or dest.stat().st_size != src.stat().st_size:
            shutil.copy2(src, dest)
            copied += 1
    return copied


def transform_rows(table: str, rows: list[dict], base_url: str) -> list[dict]:
    out: list[dict] = []
    for row in rows:
        item = dict(row)
        if table == "dhikr":
            asset = item.get("audioPath")
            if asset and item.get("audioSourceType") in {"BUILTIN", "DOWNLOAD"}:
                item["remoteAudioUrl"] = asset_to_remote_url(str(asset), base_url)
        if table in {"reciterAudio", "reciterAzkarAudio"}:
            asset = item.get("assetPath")
            if asset:
                item["remoteUrl"] = asset_to_remote_url(str(asset), base_url)
                item["assetPath"] = None
            item["localPath"] = None
            item["isDownloaded"] = 0
        out.append(item)
    return out


def pull_emulator_db(dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["adb", "shell", "run-as com.greendome.adhkar sqlite3 databases/adhkar.db 'PRAGMA wal_checkpoint(FULL);'"],
        check=True,
    )
    result = subprocess.run(
        ["adb", "exec-out", "run-as", "com.greendome.adhkar", "cat", "databases/adhkar.db"],
        check=True,
        capture_output=True,
    )
    if len(result.stdout) < 1000:
        raise SystemExit("قاعدة البيانات المسحوبة فارغة — تأكد من تثبيت التطبيق وتشغيله")
    dest.write_bytes(result.stdout)


def _count_orphaned_reciter_audio(conn: sqlite3.Connection) -> int:
    total = conn.execute("SELECT COUNT(*) FROM reciter_audio").fetchone()[0]
    linked = conn.execute(
        """
        SELECT COUNT(*) FROM reciter_audio ra
        INNER JOIN dhikr d ON d.id = ra.dhikrId AND d.isDefault = 1
        """
    ).fetchone()[0]
    return max(0, total - linked)


def next_version() -> int:
    manifest_path = REMOTE_ROOT / "manifest.json"
    if manifest_path.exists():
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
        return int(data.get("version", 0)) + 1
    return 1


def export_bundle(db_path: Path, base_url: str, version: int | None = None) -> int:
    version = version or next_version()
    copy_audio_assets()

    conn = sqlite3.connect(db_path)
    try:
        bundle = {"version": version, "generatedAt": datetime.now(timezone.utc).isoformat()}
        for key, query in TABLES.items():
            rows = fetch_table(conn, query)
            bundle[key] = transform_rows(key, rows, base_url)
        skipped_audio = _count_orphaned_reciter_audio(conn)
        if skipped_audio:
            print(f"Skipped {skipped_audio} orphaned reciter_audio row(s)")
    finally:
        conn.close()

    bundle_dir = REMOTE_ROOT / "bundles" / f"v{version}"
    bundle_dir.mkdir(parents=True, exist_ok=True)
    content_path = bundle_dir / "content.json"
    content_text = json.dumps(bundle, ensure_ascii=False, separators=(",", ":"))
    content_path.write_text(content_text, encoding="utf-8")
    digest = hashlib.sha256(content_text.encode("utf-8")).hexdigest()

    manifest = {
        "version": version,
        "contentPath": f"bundles/v{version}/content.json",
        "sha256": digest,
        "generatedAt": bundle["generatedAt"],
        "minAppVersion": "1.0.4",
    }
    (REMOTE_ROOT / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return version


def main() -> int:
    parser = argparse.ArgumentParser(description="Export Sabbih remote content bundle")
    parser.add_argument("--db", type=Path, help="Path to adhkar.db")
    parser.add_argument("--pull-emulator", action="store_true", help="Pull DB from connected emulator")
    parser.add_argument("--version", type=int, help="Bundle version override")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    args = parser.parse_args()

    db_path = args.db or ROOT / ".tmp-export" / "adhkar.db"
    if args.pull_emulator:
        pull_emulator_db(db_path)
    if not db_path.exists():
        print("Database not found. Use --pull-emulator or --db PATH", file=sys.stderr)
        return 1

    version = export_bundle(db_path, args.base_url.rstrip("/"), args.version)
    print(f"Exported remote-content bundle v{version}")
    print(f"Manifest: {REMOTE_ROOT / 'manifest.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
