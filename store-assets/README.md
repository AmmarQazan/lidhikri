# أصول متجر Google Play — Sabbih

## المحتويات

```
store-assets/
├── graphics/
│   ├── icon-512.png          # أيقونة المتجر 512×512
│   └── feature-graphic.png   # صورة الغلاف 1024×500
├── screenshots/
│   ├── phone/                # لقطات خام 1080×2400
│   ├── ar/                   # لقطات عربية نهارية
│   ├── ar-night/             # لقطات عربية ليلية
│   └── captioned/            # لقطات بعناوين شرح (لرفع المتجر)
├── video/
│   ├── promo.mp4             # فيديو ترويجي
│   └── STORYBOARD.md         # مشاهد الفيديو
└── listings.md               # نصوص العرض
```

## أهم الميزات التي تشرحها الأصول

أي شرح أو غلاف أو لقطة أو فيديو **يجب** أن يوضّح الأذان ومواقيت الصلاة وأذكار ما بعد الفرض.

1. مواقيت الصلاة
2. الأذان
3. أذكار ما بعد الصلاة
4. التسبيح التلقائي فوق التطبيقات
5. الأذكار التلقائية من حصن المسلم
6. المسبحة الرقمية (تقليدية / إلكترونية)
7. ويدجت الشاشة الرئيسية وذكر اليوم
8. مظهر نهاري وليلي

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
python scripts/recapture_ar_and_night.py
```

يتطلب محاكياً أو جهازاً متصلاً عبر ADB، وAPK تجريبي مثبّت.

## رفع Play Console

1. **أيقونة التطبيق:** `graphics/icon-512.png`
2. **Feature Graphic:** `graphics/feature-graphic.png`
3. **حزمة جاهزة للرفع:** `play-upload/` — 8 لقطات مرقّمة + أيقونة + غلاف + فيديو + النصوص
   - نهار عربي: `play-upload/phone-screenshots/`
   - ليلي عربي: `play-upload/phone-screenshots-night/`
4. **الفيديو (اختياري):** ارفع `play-upload/promo.mp4` إلى يوتيوب ثم الصق الرابط في الكونسول
5. **النصوص:** انسخ من `play-upload/listings/` لكل لغة (ar / en / fr / es / tr / ur / id / hi)
6. **سياسة الخصوصية:** https://sabbih.web.app/privacy
7. **ملف المطوّر:** النصوص والحقول في `developer-profile.md` — الموقع والأيقونة والغلاف والنص الإنجليزي مُدخلة في الكونسول
