# محتوى سَبِّح على الشبكة

هذا المجلد يُرفع إلى GitHub ويُزامَن مع التطبيق تلقائياً.

## النشر

1. عدّل المحتوى في التطبيق (وضع المدير) أو حدّث قاعدة البيانات محلياً.
2. صدّر الحزمة:

```bash
python scripts/export_remote_content.py --pull-emulator
```

أو من ملف قاعدة بيانات:

```bash
python scripts/export_remote_content.py --db path/to/adhkar.db
```

3. ارفع إلى GitHub:

```bash
git add remote-content/
git commit -m "تحديث حزمة المحتوى vN"
git push
```

4. التطبيقات المثبتة ستجلب `manifest.json` عند التشغيل وتُحدّث المحتوى إذا زاد رقم الإصدار.

## البنية

- `manifest.json` — إصدار الحزمة ورابط المحتوى وبصمة SHA-256
- `bundles/vN/content.json` — الأذكار، القرّاء، الأصوات، الأقسام
- `audio/` — ملفات MP3 (بدلاً من تضمينها داخل APK)

## الاستضافة المجانية

الرابط الافتراضي:

`https://raw.githubusercontent.com/AmmarQazan/sabbih/main/remote-content`

يمكن تغييره من `REMOTE_CONTENT_BASE_URL` في `app/build.gradle.kts`.
