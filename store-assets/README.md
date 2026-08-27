# أصول متجر Google Play — Sabbih

## المحتويات

```
store-assets/
├── graphics/
│   ├── icon-512.png          # أيقونة المتجر 512×512
│   └── feature-graphic.png   # صورة الغلاف 1024×500
├── screenshots/
│   ├── phone/                # لقطات خام 1080×2400
│   └── captioned/            # لقطات بعناوين شرح (لرفع المتجر)
├── video/
│   ├── promo.mp4             # فيديو ترويجي
│   └── STORYBOARD.md         # مشاهد الفيديو
└── listings.md               # نصوص العرض بأربع لغات
```

## أهم الميزات التي تشرحها الأصول

1. التسبيح التلقائي فوق التطبيقات
2. الأذكار التلقائية من حصن المسلم
3. المسبحة الرقمية (تقليدية / إلكترونية)
4. ويدجت الشاشة الرئيسية وذكر اليوم
5. شاشة القفل والعرض المرن

## توليد الغلاف والأيقونة

```bash
python scripts/gen_feature_graphic.py
```

## لقطات مع عناوين شرح

```bash
python scripts/gen_store_captions.py
```

## إعادة الالتقاط من المحاكي

```bash
python scripts/stable_capture.py
```

يتطلب محاكياً أو جهازاً متصلاً عبر ADB، وAPK تجريبي مثبّت.

## رفع Play Console

1. **أيقونة التطبيق:** `graphics/icon-512.png`
2. **Feature Graphic:** `graphics/feature-graphic.png`
3. **حزمة جاهزة للرفع:** `play-upload/` — 8 لقطات مرقّمة + أيقونة + غلاف + فيديو + النصوص
4. **الفيديو (اختياري):** ارفع `play-upload/promo.mp4` إلى يوتيوب ثم الصق الرابط في الكونسول
5. **النصوص:** انسخ من `play-upload/listings/` لكل لغة (ar / en / fr / es)
6. **سياسة الخصوصية:** https://sabbih.web.app/privacy
7. **ملف المطوّر:** النصوص والحقول في `developer-profile.md` — الموقع والأيقونة والغلاف والنص الإنجليزي مُدخلة في الكونسول
