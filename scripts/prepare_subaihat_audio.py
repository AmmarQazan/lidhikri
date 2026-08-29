#!/usr/bin/env python3
"""Copy Subaihat MP3s into remote-content/audio/subaihat with stable ASCII names."""
from __future__ import annotations

import json
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = Path(r"C:\Users\pc\Downloads\wetransfer_2026-08-29_0619\تطبيق سبح")
DEST = ROOT / "remote-content" / "audio" / "subaihat"

# (source_dir, source_filename) -> dest relative path under DEST
COPIES: list[tuple[str, str, str]] = [
    # tasbih
    ("تسبيحات", "سبحان الله.mp3", "tasbih/subhan_allah.mp3"),
    ("تسبيحات", "الحمد لله.mp3", "tasbih/alhamdulillah.mp3"),
    ("تسبيحات", "لا اله الا الله.mp3", "tasbih/tahlil.mp3"),
    ("تسبيحات", "الله اكبر.mp3", "tasbih/allahu_akbar.mp3"),
    ("تسبيحات", "لا حول ولا قوة الا بالله.mp3", "tasbih/hawqala.mp3"),
    ("تسبيحات", "سبحان الله والحمد و لا اله الا الله والله اكبر.mp3", "tasbih/baqiyat.mp3"),
    ("تسبيحات", "استغفر الله واتوب اليه.mp3", "tasbih/istighfar.mp3"),
    ("تسبيحات", "اللهم صل وسلم وبارك على نبينا محمد.mp3", "tasbih/salawat.mp3"),
    ("تسبيحات", "سبحان الله العظيم.mp3", "tasbih/subhan_azim.mp3"),
    ("تسبيحات", "لا اله الا الله وحده لا شريك له له الملك.mp3", "tasbih/tawhid.mp3"),
    ("تسبيحات", "لا اله الا انت سبحانك اني كنت من الظالمين.mp3", "tasbih/yunus.mp3"),
    ("تسبيحات", "يا ذا الجلال والاكرام.mp3", "tasbih/jalal.mp3"),
    ("تسبيحات", "حسبي الله ونعم الوكيل.mp3", "tasbih/hasbi.mp3"),
    ("تسبيحات", "الله اكبر الله اكبر لا اله الا الله الله اكبر ولله الحمد.mp3", "tasbih/eid_takbir.mp3"),
    # jawami — exact وبحمده formula
    ("جوامع التسبيح", "سبحان الله وبحمده عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته.mp3", "jawami/subhan_bihamd_adada.mp3"),
    # morning
    ("اذكار الصباح", "آية الكرسي.mp3", "morning/ayat_kursi.mp3"),
    ("اذكار الصباح", "سورة الأخلاص.mp3", "morning/ikhlas.mp3"),
    ("اذكار الصباح", "سورة الفلق1.mp3", "morning/falaq.mp3"),
    ("اذكار الصباح", "سورة الناس.mp3", "morning/nas.mp3"),
    ("اذكار الصباح", "اصبحنا واصبح الملك لله.mp3", "morning/asbahna_mulk.mp3"),
    ("اذكار الصباح", "اللهم بك اصبحنا.mp3", "morning/bika_asbahna.mp3"),
    ("اذكار الصباح", "اللهم انت ربي لا اله الا انت خلقتني وانا عبدك.mp3", "morning/sayyid_istighfar.mp3"),
    ("اذكار الصباح", "اللهم اعالم الغيب والشهادة فاطر السموات.mp3", "morning/alim_ghayb.mp3"),
    ("اذكار الصباح", "اللهم اني اصبحت اشهدك.mp3", "morning/ushhiduka.mp3"),
    ("اذكار الصباح", "اللهم اني ااعوذ بك ان اشرك بك وانا اعلم.mp3", "morning/shirk.mp3"),
    ("اذكار الصباح", "اللهم اما اصبح بي من نعمة.mp3", "morning/nima.mp3"),
    ("اذكار الصباح", "اللهم عافني في بدني.mp3", "morning/afini.mp3"),
    ("اذكار الصباح", "اللهم اني اعوذ بك من الهم والحزن.mp3", "morning/hamm.mp3"),
    ("اذكار الصباح", "اللهم اني اسالك العفو والعافية.mp3", "morning/afw.mp3"),
    ("اذكار الصباح", "بسم الله الذي لا يضر مع اسمه شيء.mp3", "morning/bismillah_la_yadurr.mp3"),
    ("اذكار الصباح", "يا حي يا قيوم برحمتك استغيث.mp3", "morning/ya_hayy.mp3"),
    ("اذكار الصباح", "اصبحنا واصبح الملك لله رب العالمين.mp3", "morning/asbahna_alamin.mp3"),
    ("اذكار الصباح", "اصبحنا على فطرة الاسلام1.mp3", "morning/fitrah.mp3"),
    ("اذكار الصباح", "اللهم اني اسالك علما نافعا.mp3", "morning/ilman.mp3"),
    ("اذكار الصباح", "اللهم صل وسلم وبارك على نبينا محمد.mp3", "morning/salawat.mp3"),
    # evening
    ("اذكار المساء", "آية الكرسي.mp3", "evening/ayat_kursi.mp3"),
    ("اذكار المساء", "سورة الأخلاص.mp3", "evening/ikhlas.mp3"),
    ("اذكار المساء", "سورة الفلق1.mp3", "evening/falaq.mp3"),
    ("اذكار المساء", "سورة الناس.mp3", "evening/nas.mp3"),
    ("اذكار المساء", "امسينا وامسى الملك لله والحمد لله.mp3", "evening/amsayna_mulk.mp3"),
    ("اذكار المساء", "اللهم بك امسينا.mp3", "evening/bika_amsayna.mp3"),
    ("اذكار المساء", "اللهم انت ربي لا اله الا انت خلقتني وانا عبدك.mp3", "evening/sayyid_istighfar.mp3"),
    ("اذكار المساء", "اللهم اعالم الغيب والشهادة فاطر السموات.mp3", "evening/alim_ghayb.mp3"),
    ("اذكار المساء", "اللهم اني امسيت اشهدك.mp3", "evening/ushhiduka.mp3"),
    ("اذكار المساء", "اللهم اني ااعوذ بك ان اشرك بك وانا اعلم.mp3", "evening/shirk.mp3"),
    ("اذكار المساء", "اللهم ما امسى بي من نعمة.mp3", "evening/nima.mp3"),
    ("اذكار المساء", "اللهم عافني في بدني.mp3", "evening/afini.mp3"),
    ("اذكار المساء", "اللهم اني اعوذ بك من الهم والحزن.mp3", "evening/hamm.mp3"),
    ("اذكار المساء", "اللهم اني اسالك العفو والعافية.mp3", "evening/afw.mp3"),
    ("اذكار المساء", "بسم الله الذي لا يضر مع اسمه شيء.mp3", "evening/bismillah_la_yadurr.mp3"),
    ("اذكار المساء", "يا حي يا قيوم برحمتك استغيث.mp3", "evening/ya_hayy.mp3"),
    ("اذكار المساء", "امسينا على فطرة الاسلام.mp3", "evening/fitrah.mp3"),
    ("اذكار المساء", "من قال استغفر الله العظيم.mp3", "evening/man_qala_istighfar.mp3"),
    ("اذكار المساء", "اللهم صل وسلم وبارك على نبينا محمد.mp3", "evening/salawat.mp3"),
    ("اذكار المساء", "اللهم اني اسالك علما نافعا.mp3", "evening/ilman.mp3"),
    # after prayer
    ("اذكار بعد الصلاة", "اسغفر الله 3.mp3", "after_prayer/istighfar_3.mp3"),
    ("اذكار بعد الصلاة", "آية الكرسي.mp3", "after_prayer/ayat_kursi.mp3"),
    ("اذكار بعد الصلاة", "سورة الأخلاص.mp3", "after_prayer/ikhlas.mp3"),
    ("اذكار بعد الصلاة", "سورة الفلق1.mp3", "after_prayer/falaq.mp3"),
    ("اذكار بعد الصلاة", "سورة الناس.mp3", "after_prayer/nas.mp3"),
    ("اذكار بعد الصلاة", "اللهم انت السلام ومنك السلام.mp3", "after_prayer/salam.mp3"),
    ("اذكار بعد الصلاة", "لا اله الا الله وحده لا شريك له له الملك.mp3", "after_prayer/tawhid.mp3"),
    ("اذكار بعد الصلاة", "سبحان الله.mp3", "after_prayer/subhan.mp3"),
    ("اذكار بعد الصلاة", "الحمد لله.mp3", "after_prayer/hamd.mp3"),
    ("اذكار بعد الصلاة", "الله اكبر.mp3", "after_prayer/takbir.mp3"),
    # sleep
    ("اذكار النوم", "بسمك ربي وضعت جنبي.mp3", "sleep/janbi.mp3"),
    ("اذكار النوم", "اللهم انت خلقت نفسي وانت توفاها.mp3", "sleep/khalaqta.mp3"),
    ("اذكار النوم", "اللهم قني عذابك يوم تبعث عبادك.mp3", "sleep/qini.mp3"),
    ("اذكار النوم", "بسمك اللهم اموت واحيا.mp3", "sleep/amutu.mp3"),
    ("اذكار النوم", "سبحان الله.mp3", "sleep/subhan.mp3"),
    ("اذكار النوم", "الحمد لله.mp3", "sleep/hamd.mp3"),
    ("اذكار النوم", "الله اكبر.mp3", "sleep/takbir.mp3"),
    # wake
    ("اذكار الاستيقاظ", "الحمد لله الذي أحيانا.mp3", "wake_up/ahyana.mp3"),
    ("اذكار الاستيقاظ", "لا اله الا الله وحده لا شريك له له الملك.mp3", "wake_up/tawhid.mp3"),
    ("اذكار الاستيقاظ", "الحمد لله الذي عافاني قي جسدي.mp3", "wake_up/afani.mp3"),
    # adhan
    ("اذكار عند سماع الأذان", "تقول مث ما يقول المؤذن.mp3", "adhan/repeat.mp3"),
    ("اذكار عند سماع الأذان", "اللهم رب هذه الدعوة التامة.mp3", "adhan/wasilah.mp3"),
    # home (folder has two spaces)
    ("اذكار دخول  المنزل", "بسم الله ولجنا وبسم الله خرجنا وعلى ربنا توكلنا.mp3", "home/walajna.mp3"),
    ("اذكار دخول  المنزل", "اللهم اني  أسألك خير المولج وخير المخرج.mp3", "home/mawlaj.mp3"),
    ("اذكار دخول  المنزل", "بسم الله توكلت على الله ولا حول ولا قوت الا بالله.mp3", "home/tawakkalt.mp3"),
    ("اذكار دخول  المنزل", "اللهم اني اعوذ بك ان اضل او اضل.mp3", "home/adilla.mp3"),
    # unmatched — uploaded for later binding
    ("تسبيحات", "استغفر الله اوانوب اليه.mp3", "unmatched/istighfar_awanub.mp3"),
    ("تسبيحات", "سبحان الله عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته.mp3", "unmatched/adada_khalqih_no_bihamd.mp3"),
    ("جوامع التسبيح", "سبحان الله عدد خلقه.mp3", "jawami/subhan_adada_khalqih.mp3"),
]

WAV_CONVERSIONS: list[tuple[str, str, str]] = [
    ("تسبيحات", "سبحان الله وبحمده.wav", "tasbih/subhan_bihamd.mp3"),
]


def ffmpeg_exe() -> str:
    import imageio_ffmpeg

    return imageio_ffmpeg.get_ffmpeg_exe()


def convert_wav(src: Path, dest: Path) -> int:
    dest.parent.mkdir(parents=True, exist_ok=True)
    result = subprocess.run(
        [
            ffmpeg_exe(),
            "-y",
            "-i",
            str(src),
            "-codec:a",
            "libmp3lame",
            "-b:a",
            "320k",
            "-ar",
            "44100",
            str(dest),
        ],
        capture_output=True,
        check=False,
    )
    if result.returncode != 0:
        err = (result.stderr or result.stdout or b"").decode("utf-8", "replace")[-400:]
        raise SystemExit(f"ffmpeg failed {src.name}: {err}")
    return dest.stat().st_size


def main() -> None:
    if not SRC.is_dir():
        raise SystemExit(f"source missing: {SRC}")
    copied = []
    missing = []
    for folder, name, dest_rel in COPIES:
        src = SRC / folder / name
        dest = DEST / dest_rel
        if not src.is_file():
            missing.append({"folder": folder, "name": name, "dest": dest_rel})
            continue
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
        copied.append({"src": f"{folder}/{name}", "dest": dest_rel, "size": src.stat().st_size})
    converted = []
    for folder, name, dest_rel in WAV_CONVERSIONS:
        src = SRC / folder / name
        dest = DEST / dest_rel
        if not src.is_file():
            missing.append({"folder": folder, "name": name, "dest": dest_rel})
            continue
        size = convert_wav(src, dest)
        converted.append({"src": f"{folder}/{name}", "dest": dest_rel, "size": size})
    report = ROOT / ".tmp-export" / "subaihat_copy_report.json"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(
        json.dumps(
            {"copied": copied, "converted": converted, "missing": missing},
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"copied {len(copied)}  converted {len(converted)}  missing {len(missing)}  -> {DEST}")
    if missing:
        print("MISSING:")
        for item in missing:
            print(" ", item)


if __name__ == "__main__":
    main()
