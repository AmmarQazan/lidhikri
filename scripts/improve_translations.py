#!/usr/bin/env python3
"""Improve FR/ES (and a few EN) user-facing strings, add missing keys, pretty-print XML."""
from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"

EN_UPDATES = {
    "display_lock": "Lock screen",
    "popup_position_x": "Horizontal position",
    "popup_position_y": "Vertical position",
}

FR_UPDATES = {
    "admin_reciter_audio_pick_section": "Choisissez une section pour gérer les enregistrements de ce récitateur",
    "audio_not_available_message": "Aucun enregistrement n'est disponible pour cet élément. Ajoutez un audio depuis les paramètres du récitateur ou en modifiant le dhikr.",
    "auto_tasbih_audio_when_hint": "Un tasbih court par intervalle — pas une chaîne complète. Les adhkar automatiques : un dhikr à l'heure prévue (voir les réglages des adhkar auto).",
    "azkar_auto_display_hint": "Comment apparaissent les adhkar automatiques",
    "azkar_auto_display_section": "Affichage des adhkar",
    "azkar_auto_lock_screen": "Adhkar sur l'écran verrouillé",
    "azkar_auto_lock_screen_hint": "Carte d'adhkar avec le titre de la section — séparée du tasbih",
    "azkar_hub_hint": "Adhkar de Hisnul Muslim — lisez-les ou programmez la lecture automatique",
    "azkar_voice_section": "Voix des adhkar",
    "azkar_voice_section_hint": "Récitateur et volume pour les adhkar automatiques — séparés du tasbih",
    "builtin_jawami": "Jawami du tasbih",
    "category_eid": "Aïd",
    "content_remote_version_label": "Dernière version sur Firebase",
    "dhikr_of_day_channel_hint": "Rappel silencieux sur l'écran de verrouillage avec le dhikr du jour",
    "dhikr_of_day_lock_hint": "Affiche le dhikr du jour sur l'écran de verrouillage en notification silencieuse. Activez les notifications si demandé.",
    "dhikr_of_day_text_color_auto": "Automatique",
    "dhikr_of_day_text_color_teal": "Turquoise",
    "dhikr_of_day_widget_description": "Affiche un dhikr quotidien aléatoire parmi vos adhkar",
    "flip_to_stop_playback": "Retourner le téléphone pour arrêter",
    "flip_to_stop_playback_hint": "Retournez le téléphone face contre table pendant l'écoute pour arrêter la lecture.",
    "misbaha_widget_background_hint": "S'applique aux widgets traditionnel et électronique. Le fond transparent laisse voir le fond d'écran.",
    "nav_azkar": "Sections d'adhkar",
    "nav_tasbih": "Jawami du tasbih",
    "pause_during_calls": "Pause pendant les appels",
    "pause_during_calls_hint": "Pas de son de l'application pendant un appel téléphonique ou audio/vidéo (WhatsApp, Telegram, Signal, Messenger, Meet, Skype, et tout appel en ligne).",
    "pause_during_media": "Couper le son si une autre application diffuse de l'audio",
    "pause_during_media_hint": "Activé : silence si une autre application diffuse de l'audio — YouTube, musique, notes vocales WhatsApp/Telegram/Signal/Messenger, ou appels dans ces applications. Désactivé : les autres applications peuvent s'interrompre puis reprendre.",
    "popup_position_x": "Position horizontale",
    "popup_position_y": "Position verticale",
    "respect_quiet_mode": "Respecter le mode silencieux / Ne pas déranger",
    "respect_quiet_mode_hint": "Pas de son de rappel si le téléphone est silencieux ou en mode Ne pas déranger.",
    "settings_app_general_subtitle": "Langue, lecture, batterie, affichage par-dessus",
    "settings_app_general_title": "Application et autorisations",
    "settings_display_title": "Affichage et lecture",
    "settings_misbaha_subtitle": "Type, apparence des perles ou électronique, son et vibration",
    "settings_widgets_title": "Widgets",
    "today_stats_azkar_title": "Adhkar auto aujourd'hui",
    "today_stats_hint": "Les compteurs automatiques augmentent à chaque rappel réussi. La misbaha compte les appuis manuels. Remise à zéro chaque jour.",
}

ES_UPDATES = {
    "admin_reciter_audio_pick_section": "Elige una sección para gestionar las grabaciones de este recitador",
    "audio_not_available_message": "No hay audio grabado para este elemento. Añádelo desde los ajustes del recitador o al editar el dhikr.",
    "auto_reminder_display_hint": "Ajuste común para el tasbih y los adhkar automáticos",
    "auto_reminder_lock_screen_hint": "Muestra el texto del recordatorio en la pantalla de bloqueo — tasbih y adhkar",
    "auto_tasbih_audio_when_hint": "Un tasbih corto por intervalo — no una cadena completa. Adhkar automáticos: un dhikr a su hora (ver ajustes de adhkar auto).",
    "azkar_auto_display_hint": "Cómo aparecen los adhkar automáticos",
    "azkar_auto_display_section": "Visualización de adhkar",
    "azkar_auto_lock_screen": "Adhkar en pantalla de bloqueo",
    "azkar_auto_lock_screen_hint": "Tarjeta de adhkar con el título de la sección — separada del tasbih",
    "azkar_hub_hint": "Adhkar de Hisnul Muslim — léelos o programa la reproducción automática",
    "azkar_voice_section": "Voz de adhkar",
    "azkar_voice_section_hint": "Recitador y volumen para los adhkar automáticos — separados del tasbih",
    "category_eid": "Aid",
    "content_remote_version_label": "Última versión en Firebase",
    "dhikr_of_day_channel_hint": "Recordatorio silencioso en la pantalla de bloqueo con el dhikr del día",
    "dhikr_of_day_lock_hint": "Muestra el dhikr del día en la pantalla de bloqueo como notificación silenciosa. Activa las notificaciones si se te pide.",
    "flip_to_stop_playback": "Voltear el teléfono para detener",
    "flip_to_stop_playback_hint": "Voltea el teléfono boca abajo mientras escuchas para detener la reproducción.",
    "misbaha_widget_background_hint": "Se aplica a los widgets tradicional y electrónico. El fondo transparente deja ver el fondo de pantalla.",
    "nav_azkar": "Secciones de adhkar",
    "nav_tasbih": "Jawami del tasbih",
    "pause_during_calls_hint": "Sin sonido de la aplicación durante llamadas telefónicas o de audio/vídeo (WhatsApp, Telegram, Signal, Messenger, Meet, Skype y cualquier llamada en línea).",
    "pause_during_media": "Silenciar si otra aplicación reproduce audio",
    "pause_during_media_hint": "Activado: silencio si otra aplicación reproduce audio — YouTube, música, notas de voz de WhatsApp/Telegram/Signal/Messenger, o llamadas en esas apps. Desactivado: otras apps pueden pausarse y continuar después.",
    "popup_position_x": "Posición horizontal",
    "popup_position_y": "Posición vertical",
    "respect_quiet_mode": "Respetar silencio / No molestar",
    "respect_quiet_mode_hint": "Sin sonido de recordatorio si el teléfono está en silencio o en No molestar.",
    "settings_app_general_subtitle": "Idioma, reproducción, batería, mostrar sobre otras apps",
    "settings_app_general_title": "Aplicación y permisos",
    "settings_display_title": "Apariencia y lectura",
    "settings_misbaha_subtitle": "Tipo, estilo de las cuentas o electrónico, sonido y vibración",
    "settings_widgets_title": "Widgets",
    "today_stats_azkar_title": "Adhkar automático hoy",
    "today_stats_hint": "Los contadores automáticos suben con cada recordatorio correcto. La misbaha cuenta los toques manuales. Se reinicia cada día.",
}


def unescape_android(text: str | None) -> str:
    if not text:
        return ""
    out: list[str] = []
    i = 0
    while i < len(text):
        if text[i] == "\\" and i + 1 < len(text):
            nxt = text[i + 1]
            if nxt == "'":
                out.append("'")
                i += 2
                continue
            if nxt == '"':
                out.append('"')
                i += 2
                continue
            if nxt == "\\":
                out.append("\\")
                i += 2
                continue
            if nxt == "n":
                out.append("\n")
                i += 2
                continue
        out.append(text[i])
        i += 1
    return "".join(out)


def escape_android(text: str) -> str:
    return (
        text.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
    )


def load_map(path: Path) -> dict[str, str]:
    tree = ET.parse(path)
    items: dict[str, str] = {}
    for el in tree.getroot():
        if el.tag == "string" and el.get("name"):
            items[el.get("name", "")] = unescape_android(el.text)
    return items


def write_map(path: Path, items: dict[str, str]) -> None:
    # Keep original key order, append new keys alphabetically.
    tree = ET.parse(path)
    order = [el.get("name", "") for el in tree.getroot() if el.tag == "string" and el.get("name")]
    seen = set(order)
    extra = sorted(k for k in items if k not in seen)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for name in order + extra:
        if name not in items:
            continue
        lines.append(f'    <string name="{name}">{escape_android(items[name])}</string>')
    lines.append("</resources>")
    lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def patch(folder: str, updates: dict[str, str], en: dict[str, str]) -> None:
    path = RES / folder / "strings.xml"
    items = load_map(path)
    added = 0
    changed = 0
    for key, value in updates.items():
        if items.get(key) != value:
            if key not in items:
                added += 1
            else:
                changed += 1
            items[key] = value
    missing = [k for k in en if k not in items]
    for key in missing:
        if key in updates:
            continue
        items[key] = en[key]
        added += 1
        print(f"WARN fallback EN for {folder}: {key}")
    write_map(path, items)
    print(f"{folder}: +{added} new, ~{changed} improved, total {len(items)}")


def patch_en_in_place() -> dict[str, str]:
    path = RES / "values" / "strings.xml"
    text = path.read_text(encoding="utf-8")
    en = load_map(path)
    changed = 0
    for key, value in EN_UPDATES.items():
        if en.get(key) == value:
            continue
        pattern = rf'(<string name="{re.escape(key)}">)(.*?)(</string>)'
        text, n = re.subn(pattern, rf"\g<1>{escape_android(value)}\3", text, count=1)
        if n:
            en[key] = value
            changed += 1
    if changed:
        path.write_text(text, encoding="utf-8")
    print(f"values: ~{changed} improved, total {len(en)}")
    return en


def main() -> None:
    en = patch_en_in_place()
    patch("values-fr", FR_UPDATES, en)
    patch("values-es", ES_UPDATES, en)


if __name__ == "__main__":
    main()
