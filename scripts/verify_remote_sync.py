#!/usr/bin/env python3
"""تحقق من سلامة حزمة المزامنة على GitHub واكتمال الربط مع جداول قاعدة البيانات."""
from __future__ import annotations

import hashlib
import json
import sqlite3
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_BASE = "https://raw.githubusercontent.com/AmmarQazan/lidhikri/main/remote-content"

EXPECTED_KEYS = {
    "dhikr",
    "reciters",
    "reciterAudio",
    "reciterAzkarAudio",
    "collections",
    "azkarItems",
}

COLLECTION_FIELDS = {
    "id", "titleAr", "titleEn", "sortOrder",
    "autoPlayAllowed", "autoPlayEnabled", "scheduleHour", "scheduleMinute",
    "weekDaysMask", "useTtsAutoPlay",
}

AZKAR_ITEM_FIELDS = {"id", "collectionId", "textAr", "virtueAr", "repeatCount", "sortOrder"}


def fetch(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=30) as resp:
        return resp.read()


def verify_github_bundle(base_url: str) -> dict:
    manifest = json.loads(fetch(f"{base_url}/manifest.json"))
    content_path = manifest["contentPath"]
    content_bytes = fetch(f"{base_url}/{content_path}")
    digest = hashlib.sha256(content_bytes).hexdigest()
    if digest != manifest["sha256"].lower():
        raise SystemExit(f"SHA256 mismatch: expected {manifest['sha256']}, got {digest}")
    bundle = json.loads(content_bytes.decode("utf-8"))
    if bundle["version"] != manifest["version"]:
        raise SystemExit("Bundle version != manifest version")
    missing = EXPECTED_KEYS - set(bundle.keys())
    if missing:
        raise SystemExit(f"Missing bundle keys: {sorted(missing)}")
    return manifest, bundle


def verify_referential_integrity(bundle: dict) -> list[str]:
    issues: list[str] = []
    collection_ids = {c["id"] for c in bundle["collections"]}
    dhikr_ids = {d["id"] for d in bundle["dhikr"]}
    reciter_ids = {r["id"] for r in bundle["reciters"]}
    azkar_ids = {a["id"] for a in bundle["azkarItems"]}

    for item in bundle["azkarItems"]:
        if item["collectionId"] not in collection_ids:
            issues.append(f"azkar_item {item['id']} -> unknown collection {item['collectionId']}")
        for field in AZKAR_ITEM_FIELDS:
            if field not in item:
                issues.append(f"azkar_item {item['id']} missing field {field}")

    for col in bundle["collections"]:
        for field in COLLECTION_FIELDS:
            if field not in col:
                issues.append(f"collection {col.get('id')} missing field {field}")

    for audio in bundle["reciterAudio"]:
        if audio["reciterId"] not in reciter_ids:
            issues.append(f"reciterAudio {audio['id']} -> unknown reciter {audio['reciterId']}")
        if audio["dhikrId"] not in dhikr_ids:
            issues.append(f"reciterAudio {audio['id']} -> unknown dhikr {audio['dhikrId']}")

    for audio in bundle["reciterAzkarAudio"]:
        if audio["reciterId"] not in reciter_ids:
            issues.append(f"reciterAzkarAudio {audio['id']} -> unknown reciter {audio['reciterId']}")
        if audio["azkarItemId"] not in azkar_ids:
            issues.append(f"reciterAzkarAudio {audio['id']} -> unknown azkar {audio['azkarItemId']}")

    return issues


def compare_with_local_export(bundle: dict, db_path: Path) -> list[str]:
    """Compare export query output with bundle if local DB exists."""
    if not db_path.exists():
        return []
    issues: list[str] = []
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    try:
        cols = conn.execute(
            "SELECT id, titleAr FROM adhkar_collection WHERE id != 'favorites' ORDER BY sortOrder"
        ).fetchall()
        bundle_titles = {c["titleAr"] for c in bundle["collections"]}
        for row in cols:
            if row["titleAr"] not in bundle_titles:
                issues.append(f"Local collection not in remote bundle: {row['titleAr']} ({row['id']})")
    finally:
        conn.close()
    return issues


def main() -> int:
    base = DEFAULT_BASE
    print("Checking remote bundle on GitHub...")
    manifest, bundle = verify_github_bundle(base)
    print(f"OK manifest v{manifest['version']} sha256 verified")
    print(
        "Counts:",
        {k: len(bundle[k]) for k in sorted(EXPECTED_KEYS)},
    )

    issues = verify_referential_integrity(bundle)
    if issues:
        print("INTEGRITY ISSUES:")
        for issue in issues:
            print(" -", issue)
        return 1
    print("OK referential integrity")

    local_db = ROOT / ".tmp-export" / "adhkar.db"
    local_issues = compare_with_local_export(bundle, local_db)
    if local_issues:
        print("LOCAL vs REMOTE gaps (admin changes not pushed yet):")
        for issue in local_issues:
            print(" -", issue)
    else:
        print("OK no obvious local/remote collection gaps (or no local DB)")

    print("SYNC COVERAGE:")
    print(" - Server -> App: dhikr, reciters, audio, collections, azkarItems")
    print(" - App -> Server: manual export only (admin changes NOT auto-upload)")
    print(" - Preserved on sync: favorites, custom dhikr (isDefault=0), user stats")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
