#!/usr/bin/env python3
"""Upload promo.mp4 via Studio Create menu; publish Unlisted."""
from pathlib import Path

from playwright.sync_api import sync_playwright

VIDEO = Path(__file__).resolve().parents[1] / "store-assets" / "play-upload" / "promo.mp4"
OUT = Path(__file__).resolve().parents[1] / ".tmp-export"
TITLE = "Lidhikri — auto tasbih, Hisnul Muslim, misbaha"
DESC = (
    "Lidhikri: auto dhikr, Hisnul Muslim, digital misbaha and home widgets. No ads.\n"
    "https://sabbih.web.app/"
)
CHANNEL = "https://studio.youtube.com/channel/UCvgGwOj95Fdo4ojEORPc3pA"


def dump(page, name):
    OUT.mkdir(exist_ok=True)
    try:
        page.screenshot(path=str(OUT / f"{name}.png"), timeout=6000)
    except Exception as e:
        print("shot skip", name, e, flush=True)
    try:
        (OUT / f"{name}.txt").write_text(page.url + "\n" + page.inner_text("body")[:15000], encoding="utf-8")
    except Exception:
        pass
    print("DUMP", name, page.url, flush=True)


with sync_playwright() as p:
    browser = p.chromium.connect_over_cdp("http://127.0.0.1:9222")
    ctx = browser.contexts[0]
    page = ctx.new_page()
    page.goto(CHANNEL + "/videos/upload", wait_until="domcontentloaded", timeout=90000)
    page.wait_for_timeout(5000)
    dump(page, "yt4-content")

    page.goto(CHANNEL, wait_until="domcontentloaded", timeout=90000)
    page.wait_for_timeout(4000)

    create = page.get_by_role("button", name="Create")
    print("create", create.count(), flush=True)
    if create.count():
        create.first.click(force=True)
        page.wait_for_timeout(1200)
    dump(page, "yt4-create")

    item = page.get_by_role("menuitem", name="Upload videos")
    if item.count() == 0:
        item = page.get_by_text("Upload videos", exact=True)
    print("menu upload", item.count(), flush=True)
    if item.count():
        item.last.click(force=True)
        page.wait_for_timeout(2500)
    dump(page, "yt4-dialog")

    inp = page.locator("input[type=file]").first
    inp.wait_for(state="attached", timeout=20000)
    inp.set_input_files(str(VIDEO))
    print("file set", flush=True)
    page.wait_for_timeout(8000)
    dump(page, "yt4-after-file")

    page.evaluate(
        """([title, desc]) => {
          const boxes = document.querySelectorAll('ytcp-uploads-dialog #textbox');
          const set = (el, val) => {
            el.focus();
            el.innerText = val;
            el.dispatchEvent(new InputEvent('input', { bubbles: true, data: val }));
          };
          if (boxes[0]) set(boxes[0], title);
          if (boxes[1]) set(boxes[1], desc);
        }""",
        [TITLE, DESC],
    )
    nk = page.get_by_text("No, it's not made for kids", exact=False)
    if nk.count():
        nk.first.click(force=True)
        print("not kids", flush=True)

    for step in range(8):
        if page.get_by_text("Unlisted", exact=True).count():
            print("visibility step", step, flush=True)
            break
        nxt = page.locator("ytcp-uploads-dialog #next-button")
        if nxt.count() == 0:
            break
        try:
            nxt.first.click(force=True, timeout=10000)
            print("next", step, flush=True)
            page.wait_for_timeout(2000)
        except Exception as e:
            print("next skip", e, flush=True)
            break

    dump(page, "yt4-vis")
    un = page.get_by_text("Unlisted", exact=True)
    clicked = False
    for i in range(un.count()):
        el = un.nth(i)
        box = el.bounding_box()
        if el.is_visible() and box and box["width"] >= 20 and box["height"] >= 10:
            el.click(force=True)
            print("unlisted", i, flush=True)
            clicked = True
            break
    if not clicked and un.count():
        page.mouse.click(620, 430)
        print("unlisted mouse fallback", flush=True)
    page.wait_for_timeout(800)
    done = page.locator("#done-button")
    save = page.get_by_role("button", name="Save")
    print("done", done.count(), "save", save.count(), flush=True)
    if done.count():
        done.first.click(force=True)
        page.wait_for_timeout(10000)
    elif save.count():
        save.first.click(force=True)
        page.wait_for_timeout(10000)
    dump(page, "yt4-done")
    body = ""
    try:
        body = page.inner_text("body")
    except Exception:
        pass
    for line in body.splitlines():
        if "youtu.be" in line or "watch?v=" in line or "/shorts/" in line:
            print("LINK", line.strip(), flush=True)
    print("URL", page.url, flush=True)
