<div dir="rtl">

# خِزانة 📚

**قارئ ومكتبة كتب لأندرويد — شخصي، بدون إعلانات، بدون إنترنت، بدون تتبّع.**

[English below ⬇](#khizana-)

خِزانة يفحص ملفات الكتب في جهازك (PDF و CBZ) ويعرضها على **رفوف خشبية واقعية** بأغلفة مولّدة تلقائياً، مع قارئ داخلي كامل بتمرير أفقي يدعم العربية (RTL) والإنجليزية (LTR).

## المزايا

- 📖 قارئ PDF و CBZ داخلي بمحرّك النظام فقط — بلا مكتبات خارجية.
- 🪵 رفوف خشبية دافئة بأغلفة 2:3 وأغلفة افتراضية أنيقة عند الفشل.
- 🔖 إشارات مرجعية بملاحظات، ومواضيع ووسوم لتنظيم المكتبة.
- 🌙 وضع ليلي بعكس ألوان الصفحة مع تقوية التباين للكتب الممسوحة ضوئياً.
- 🈯 واجهة عربية/إنجليزية تتبع لغة النظام، واتجاه قراءة مستقل لكل كتاب.

## إذن «الوصول لكل الملفات» — ولماذا هو ضروري

يطلب التطبيق إذن `MANAGE_EXTERNAL_STORAGE` لسبب واحد: فحص وحدة التخزين للعثور على ملفات كتبك أينما كانت، وفتحها مباشرة، وحذفها نهائياً إن طلبتَ ذلك. ملاحظات:

- مجلدا `Android/data` و `Android/obb` ممنوعان بقيد من نظام أندرويد نفسه (11+) ولا يمكن الوصول إليهما.
- **التطبيق لا يملك إذن الإنترنت أصلاً** — لا يستطيع إرسال أي بايت خارج جهازك حتى لو أراد. لا تحليلات، لا تتبّع، لا إعلانات.

## البناء

```
./gradlew assembleDebug
```

يتطلب JDK 17 و Android SDK 35. الإصدارات الموقّعة تُبنى آلياً عبر GitHub Actions عند دفع وسم `v*`.

## الرخصة

[MIT](LICENSE) © abosalehg-ui

</div>

---

# Khizana 📚

**A personal book reader & library for Android — no ads, no internet, no tracking.**

Khizana scans your device for book files (PDF and CBZ) and lays them out on **realistic wooden shelves** with auto-generated covers, plus a full built-in horizontal-swipe reader with first-class Arabic (RTL) and English (LTR) support.

## Features

- 📖 Built-in PDF & CBZ reader using only the OS engine — no third-party PDF libraries.
- 🪵 Warm wooden shelves with 2:3 covers and elegant deterministic fallback covers.
- 🔖 Bookmarks with notes; topics and tags to organize the library.
- 🌙 Night mode with page color inversion and contrast boosting for scanned books.
- 🈯 Arabic/English UI following the system language, with per-book reading direction.

## The "All Files Access" permission — and why it's needed

The app requests `MANAGE_EXTERNAL_STORAGE` for one purpose: scanning storage to find your book files wherever they are, opening them directly, and permanently deleting them when you ask it to. Notes:

- `Android/data` and `Android/obb` are blocked by Android itself (11+) and cannot be accessed.
- **The app holds no internet permission at all** — it cannot send a single byte off your device even if it wanted to. No analytics, no tracking, no ads.

## Building

```
./gradlew assembleDebug
```

Requires JDK 17 and Android SDK 35. Signed releases are built automatically by GitHub Actions when a `v*` tag is pushed.

## License

[MIT](LICENSE) © abosalehg-ui
