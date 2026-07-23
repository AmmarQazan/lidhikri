import re
import urllib.request
import json

def extract(page):
    url = f'https://www.qurantime.net/ar/adhkar/{page}/'
    html = urllib.request.urlopen(url).read().decode('utf-8')
    cards = re.findall(
        r'id="adhkar-card-(\d+)".*?'
        r'src="(https://static\.qurantime\.net/adhkar/(\d+)\.mp3)".*?'
        r'<p dir="rtl"[^>]*>(.*?)</p>',
        html, re.DOTALL
    )
    items = []
    for card_num, audio_url, audio_id, text_html in cards:
        text = re.sub(r'<[^>]+>', '', text_html)
        text = re.sub(r'\s+', ' ', text).strip()
        items.append({
            "card": int(card_num),
            "audio_url": audio_url,
            "audio_id": int(audio_id),
            "text": text,
        })
    return items

for page in ['morning-adhkar', 'evening-adhkar']:
    items = extract(page)
    out_name = f'qurantime_{page.replace("-adhkar","")}.json'
    with open(out_name, 'w', encoding='utf-8') as f:
        json.dump(items, f, ensure_ascii=False, indent=2)
    print(f"=== {page} ({len(items)} items) -> {out_name}")
