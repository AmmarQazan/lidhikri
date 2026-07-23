# أصول متجر Google Play — Sabbih

## المحتويات

```
store-assets/
├── graphics/
│   ├── icon-512.png          # أيقونة المتجر 512×512
│   └── feature-graphic.png   # صورة الغلاف 1024×500
├── screenshots/phone/        # لقطات الهاتف 1080×2400
├── video/
│   └── promo.mp4             # فيديو ترويجي (~40 ث)
└── listings.md               # نصوص العرض بأربع لغات
```

## إعادة الالتقاط

```bash
python scripts/stable_capture.py
```

يتطلب محاكياً أو جهازاً متصلاً عبر ADB.

## رفع Play Console

1. **أيقونة التطبيق:** `graphics/icon-512.png`
2. **Feature Graphic:** `graphics/feature-graphic.png`
3. **لقطات الشاشة:** كل ملفات `screenshots/phone/`
4. **الفيديو (اختياري):** `video/promo.mp4`
5. **النصوص:** انسخ من `listings.md` لكل لغة
