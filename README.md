<div dir="rtl">

# خِزانة 📚

**قارئ ومكتبة كتب لأندرويد — شخصي، بدون إعلانات، بدون إنترنت، بدون تتبّع.**

[English below ⬇](#khizana-)

خِزانة يفحص ملفات الكتب في جهازك (PDF و CBZ) ويعرضها على **رفوف خشبية واقعية** بأغلفة مولّدة تلقائياً، مع قارئ داخلي كامل بتمرير أفقي يدعم العربية (RTL) والإنجليزية (LTR).

## المزايا

- 📖 قارئ PDF و CBZ داخلي بمحرّك النظام فقط (`android.graphics.pdf.PdfRenderer`) — بلا مكتبات PDF خارجية، فترقيعات الأمان تصلك من النظام.
- 🪵 رفوف خشبية دافئة بأغلفة بنسبة 2:3، تتكيّف مقاساتها مع حجم الشاشة.
- 🗂️ مواضيع (أرفف) ووسوم لتنظيم المكتبة، وبحث عربي يتجاهل التشكيل وفروق الألف والتاء المربوطة.
- 🌙 وضع ليلي للواجهة (فاتح / غامق / تلقائي).
- 🈯 واجهة عربية/إنجليزية تتبع لغة النظام، واتجاه قراءة مستقل لكل كتاب.
- 💾 نسخ احتياطي واستعادة إلى ملف JSON تختاره أنت، مربوط ببصمة محتوى الملف لا بمساره.

## قيد التطوير (غير منفَّذ بعد)

هذه مذكورة هنا صراحةً لأنها **ليست** موجودة في التطبيق حالياً:

- 🔖 إشارات مرجعية بملاحظات — جدول `bookmarks` جاهز في قاعدة البيانات، بلا واجهة بعد.
- 🌗 عكس ألوان صفحات الكتاب وتقوية التباين للمسح الضوئي.
- 📚 دعم EPUB (الواجهة `BookEngine` مصمّمة لاستيعابه).
- 🎨 أغلفة افتراضية مولَّدة بتصميم — البديل الحالي هو الحرف الأول من العنوان.

## الخصوصية

- **التطبيق لا يملك إذن الإنترنت أصلاً** — لا يستطيع إرسال أي بايت خارج جهازك. لا تحليلات، لا تتبّع، لا إعلانات.
- **النسخ الاحتياطي السحابي من أندرويد معطَّل** (`allowBackup="false"` مع استثناء صريح لكل النطاقات). هذا مهم: نقل النسخ الاحتياطي يقوم به النظام لا التطبيق، فغياب إذن الإنترنت وحده لا يمنعه. بدونه كانت قاعدة البيانات — مسارات كتبك وعناوينها وسجل قراءتك — تُرفع إلى Google Drive.
- النسخة الاحتياطية الوحيدة هي ملف JSON تُنشئه أنت من الإعدادات وتضعه حيث تشاء.

## إذن «الوصول لكل الملفات» — ولماذا هو ضروري

يطلب التطبيق إذن `MANAGE_EXTERNAL_STORAGE` لسبب واحد: فحص وحدة التخزين للعثور على ملفات كتبك أينما كانت، وفتحها مباشرة، وحذفها نهائياً إن طلبتَ ذلك. مجلدا `Android/data` و `Android/obb` ممنوعان بقيد من نظام أندرويد نفسه (11+) ولا يمكن الوصول إليهما.

## البناء

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

يتطلب JDK 17 و Android SDK 35. الإصدارات الموقّعة تُبنى آلياً عبر GitHub Actions عند دفع وسم `v*`.

> عند تغيير مخطط قاعدة البيانات: اكتب ترحيلاً (`Migration`) وارفع ملفات المخطط المولَّدة في `app/schemas/` مع التغيير — لا يوجد `fallbackToDestructiveMigration`، وتقدّم القراءة يجب ألا يضيع.

## الرخصة

[MIT](LICENSE) © abosalehg-ui

</div>

---

# Khizana 📚

**A personal book reader & library for Android — no ads, no internet, no tracking.**

Khizana scans your device for book files (PDF and CBZ) and lays them out on **realistic wooden shelves** with auto-generated covers, plus a full built-in horizontal-swipe reader with first-class Arabic (RTL) and English (LTR) support.

## Features

- 📖 Built-in PDF & CBZ reader using only the OS engine (`android.graphics.pdf.PdfRenderer`) — no third-party PDF library, so security fixes arrive with the platform.
- 🪵 Warm wooden shelves with 2:3 covers that scale with the window size.
- 🗂️ Topics (shelves) and tags, plus Arabic-aware search that ignores diacritics and alef/ta-marbuta variants.
- 🌙 Light / dark / follow-system theming.
- 🈯 Arabic/English UI following the system language, with per-book reading direction.
- 💾 Backup & restore to a JSON file you choose, keyed by content fingerprint rather than file path.

## Roadmap (not implemented yet)

Listed explicitly because they are **not** in the app today:

- 🔖 Bookmarks with notes — the `bookmarks` table ships in the schema, the UI does not exist.
- 🌗 Page colour inversion and contrast boosting for scanned books.
- 📚 EPUB support (the `BookEngine` interface is shaped for it).
- 🎨 Designed fallback covers — today's placeholder is the title's first letter.

## Privacy

- **The app holds no internet permission at all** — it cannot send a single byte off your device. No analytics, no tracking, no ads.
- **Android's own cloud backup is disabled** (`allowBackup="false"` plus explicit exclusions for every domain). This matters: the backup transport is run by the system, not by the app, so lacking the internet permission does not stop it. Without this the database — every book path, title and reading position — was uploaded to Google Drive.
- The only backup is the JSON file you export yourself from Settings, to a location you pick.

## The "All Files Access" permission — and why it's needed

The app requests `MANAGE_EXTERNAL_STORAGE` for one purpose: scanning storage to find your book files wherever they are, opening them directly, and permanently deleting them when you ask it to. `Android/data` and `Android/obb` are blocked by Android itself (11+) and cannot be accessed.

## Building

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Requires JDK 17 and Android SDK 35. Signed releases are built automatically by GitHub Actions when a `v*` tag is pushed.

> When changing the database schema: write a `Migration` and commit the generated files under `app/schemas/`. There is no `fallbackToDestructiveMigration` — reading progress must never be dropped.

## License

[MIT](LICENSE) © abosalehg-ui
