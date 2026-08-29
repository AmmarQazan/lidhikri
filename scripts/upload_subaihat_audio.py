#!/usr/bin/env python3
"""Upload Subaihat MP3s to Firebase Storage."""
from __future__ import annotations

import subprocess
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
AUDIO = ROOT / "remote-content" / "audio" / "subaihat"
BUCKET = "sabbih-dhikr-ammar.firebasestorage.app"


def access_token() -> str:
    auth_js = Path.home() / "AppData/Roaming/npm/node_modules/firebase-tools/lib/auth.js"
    if not auth_js.is_file():
        raise SystemExit("firebase-tools not found; run npm i -g firebase-tools")
    script = (
        "const auth=require(%r);"
        "(async()=>{"
        "const acc=auth.getGlobalDefaultAccount();"
        "if(!acc) throw new Error('not logged in');"
        "const t=await auth.getAccessToken(acc.tokens.refresh_token, acc.tokens.scopes||acc.tokens.scope||[]);"
        "process.stdout.write(t.access_token);"
        "})().catch(e=>{console.error(e); process.exit(1);});"
    ) % auth_js.as_posix()
    result = subprocess.run(
        ["node", "-e", script],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0 or not result.stdout.strip():
        err = (result.stderr or result.stdout or "token failed").strip()
        raise SystemExit(f"Firebase token refresh failed: {err[:400]}")
    return result.stdout.strip()


def upload(local: Path, object_name: str, token: str) -> None:
    q = urllib.parse.urlencode({"uploadType": "media", "name": object_name})
    url = f"https://storage.googleapis.com/upload/storage/v1/b/{BUCKET}/o?{q}"
    body = local.read_bytes()
    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "audio/mpeg",
        },
    )
    with urllib.request.urlopen(req) as resp:
        resp.read()


def main() -> None:
    if not AUDIO.is_dir():
        raise SystemExit(f"Missing {AUDIO}")
    files = sorted(AUDIO.rglob("*.mp3"))
    if not files:
        raise SystemExit("No mp3 files to upload")
    token = access_token()
    for i, path in enumerate(files, 1):
        rel = path.relative_to(AUDIO).as_posix()
        remote = f"content/audio/subaihat/{rel}"
        print(f"[{i}/{len(files)}] {rel}", flush=True)
        try:
            upload(path, remote, token)
        except urllib.error.HTTPError as exc:
            raise SystemExit(f"upload failed {exc.code} {rel}: {exc.read()[:300]!r}") from exc
    print(f"uploaded {len(files)} files")


if __name__ == "__main__":
    main()
