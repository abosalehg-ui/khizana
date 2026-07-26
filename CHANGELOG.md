# Changelog — خِزانة (Khizana)

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/), and the
project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### M8 — Search, natural ordering, manual ordering & Continue Reading

- `NaturalOrderComparator`: digit runs compare as numbers («المجلد 2» before «المجلد 10»), understanding Western, Arabic-Indic (٠-٩) and Eastern (۰-۹) digits, case-insensitive text, no overflow on long numbers. Applied to shelf ordering AND CBZ page order (page_2 before page_10 without zero padding).
- Library search field: case- and diacritic-insensitive, Arabic-aware normalization (tashkeel stripped; أ/إ/آ→ا, ة→ه, ى→ي) across title, file name and author; composes with the tag filter.
- Manual ordering by drag & drop: dropping a book **onto another book** inserts it before that book (moving shelves if needed); a trailing drop slot and empty shelves append. The shelf's `manualOrder` is rewritten 1..n; unordered books follow in natural title order.
- «أكمل القراءة» synthetic shelf: up to 10 unfinished books, most recently read first, shown above everything only when non-empty; never a drop target.
- Unit tests: natural comparator (Arabic/Latin/Arabic-Indic digits, leading zeros, case), search normalization, Continue-Reading shelf rules, within-shelf ordering, CBZ natural page order.

### M7 — Settings: deep scan, excluded folders, delete & backup

- Settings screen (theme moved here from the library header, plus a hidden-books entry): the library header now has a single «الإعدادات» button.
- Deep scan toggle (DataStore): scans walk all of external storage instead of MediaStore — slower but catches unindexed files; the scan button honors it automatically.
- Excluded folders management: add via the system folder picker (SAF tree → filesystem path for primary/SD volumes; unsupported providers politely refused), remove with one tap; scans skip them (M1 filter, now user-editable).
- Permanent delete from device («حذف من الجهاز» in the book menu, error-red): confirm dialog states it's irreversible; deletes the file, cover, DB row and tag links. If the file can't be deleted, nothing is touched.
- Backup & restore to a user-chosen JSON file: shelves, tags, reading positions, hidden flags and excluded folders — keyed by content fingerprint, with **no paths stored**. Restoring on a new device inserts unknown books as MISSING; the first scan re-attaches them by fingerprint automatically. Topics/tags are merged by name.
- Unit tests: backup JSON round-trip (nulls, Arabic text, empty/missing sections), SAF tree-id → path conversion.

### M6 — Shelf management, tags & hidden books

- Shelf menu (⋮ on every topic shelf header): rename in place, or delete with a clear promise — the shelf's books return to «جديد ⭐», no book is ever deleted. The New shelf itself is built-in and unmanaged.
- Tags: a ⋮ menu on each book cover opens Hide / Tags. The tags dialog shows all existing tags as chips, toggles membership, and creates new tags inline; unused tags are pruned automatically.
- Tag filter row above the shelves («الكل» + one chip per tag): selecting a tag narrows every shelf to matching books while keeping the shelf structure (and drop targets) visible.
- Hidden books: hiding removes a book from all shelves and filters instantly; the «الكتب المخفية» screen (from the library header) lists them with covers and one-tap unhide. Hidden state survives rescans — guaranteed since M1's DAO contract.
- Unit tests: tag filtering preserves shelf structure; null filter is a no-op.

### M5 — Landscape spreads & pinch zoom

- Landscape now shows two-page spreads like an open book: the cover always alone, then (1,2), (3,4)…, with a trailing odd page alone. Pure `buildSpreads`/`spreadIndexOfPage` mapping, fully unit-tested.
- RTL correctness for free: the spread Row places the lower page at the layout start, which is the right side under the RTL pager — the same model reads correctly in both directions.
- Pinch zoom 1x–5x with clamped one-finger panning while zoomed; double-tap toggles 1x/2.5x; zoom resets on page turn. Single-finger swipes still page normally at 1x — the zoom gesture handler only consumes events while pinching or zoomed.
- Rotation keeps the exact reading position: the ViewModel tracks the last settled page and both pager modes reopen from it (persisted `locator` unchanged).

### M4 — The reader: PDF/CBZ pages, direction & saved position

- Tapping a book opens the full-screen reader (Navigation Compose: `library` → `reader/{bookId}`).
- `CbzEngine` completes the engine pair: pages are the archive's image entries in reading order (alphabetical until natural ordering lands), decoded with downsampling; `EngineFactory` picks the engine by format.
- Swipe paging via `HorizontalPager` with neighbor pre-rendering; all engine work runs on a single dedicated thread (Pdfium isn't thread-safe, and close() can never race a render).
- Reading direction per book: RTL/LTR/AUTO, where AUTO detects Arabic characters in the title — RTL books page right-to-left.
- Position and progress persist on every page turn (`locator` as String, progress 0..1 with the pageCount 0/1 edge cases handled); covers now show a thin progress bar.
- Opening a book self-heals its status: PROTECTED/CORRUPT set on failure, restored to OK when it opens fine; page counts corrected from the real engine count.
- Page indicator always uses Western digits, per spec; tap toggles the reader top bar.
- Unit tests: progress math edge cases, direction resolution (Arabic/Latin/mixed/explicit), CBZ reading order.

### M3 — Wooden shelves, "New ⭐" shelf & drag-and-drop

- Real shelf UI: books stand as cover spines on wood-grain planks, grouped by topic, with the special "New ⭐" shelf always first (`topicId = null`).
- Drag & drop with Compose's dragAndDropSource/Target: long-press a book and drop it on any shelf to move it; the hovered shelf glows with a gold border. Empty shelves stay visible as drop targets («اسحب كتاباً إلى هنا»).
- Shelf creation from the library screen (رف جديد); books pointing at a deleted/unknown topic safely fall back to the New shelf.
- Unit tests for the pure shelf-grouping logic (`buildShelves`).

### M2 — PDF engine, covers & cache

- `BookEngine` abstraction (open/pageCount/renderPage/close) with `PdfEngine` backed by Pdfium (`com.github.mhiew:pdfium-android`); EPUB slots in behind the same interface later.
- Opening a file classifies it: password-protected PDFs → `PROTECTED`, unparseable files → `CORRUPT` — shown as badges, the app never crashes on them.
- `CoverGenerator` + `CoverWorker` (chained automatically after every scan): renders page 1 of each PDF at 480 px into `filesDir/covers/{id}.jpg` (atomic temp-file write), records page counts as a side effect, and marks unrenderable books `coverFailed` instead of retrying forever.
- CBZ covers without a full engine yet: first image entry (alphabetical) from the ZIP, downsampled decode; image count doubles as page count. `__MACOSX/`, hidden files, and directories are ignored.
- Library list now shows cover thumbnails (Coil), page counts, and protected/damaged badges.
- Unit tests: CBZ cover-entry selection, image detection, downsample math.

### M1 — Permissions, scanning, fingerprint & Room

- Room database (v1): books, topics, tags, book_tags, bookmarks, excluded_folders — schema exactly as specified, reading position stored as a String locator from day one.
- Content fingerprint identity: `SHA-256(fileSize + first 64 KB)`; moving or renaming a file never creates a duplicate row and never loses progress/hidden state.
- Manual library scan (never automatic): fast MediaStore query by extension, plus a recursive deep-scan path; excluded-folder filtering with exact-prefix semantics.
- Rescan contract: new fingerprints inserted to the "New ⭐" shelf (`topicId = null`), known fingerprints get their path refreshed in place, vanished files are silently marked `MISSING` — rows are never deleted.
- All Files Access permission flow: Android 11+ settings screen (`MANAGE_EXTERNAL_STORAGE`) with an in-app rationale, Android 10 legacy read-permission fallback.
- `ScanWorker` (Hilt + WorkManager, on-demand init): unique cancellable background scan with live progress.
- Temporary M1 library screen: permission gate, scan button with progress and report, plain list of found books (real shelves arrive in M3).
- Unit tests: fingerprint identity properties, excluded-path filtering. Instrumented tests: hidden state and progress survive relocation and MISSING round-trips.

### M0 — Project skeleton, theme & identity

- Gradle project with Kotlin 2.1, AGP 8.7, version catalog (`libs.versions.toml`).
- Jetpack Compose + Material 3, Hilt DI, Room/DataStore/WorkManager/Coil wired as dependencies.
- Warm wood + gold identity: full color palette, light/dark `ColorScheme`s, `WoodTokens` for shelf rendering.
- Amiri (headings) and Cairo (body) bundled locally in `res/font` — no network fonts.
- Arabic/English localization via `values/` and `values-ar/`; full RTL support.
- Theme mode setting (System / Light / Dark) persisted in Preferences DataStore.
- Adaptive launcher icon converted from `art/khizana-icon.svg` (wood gradient background, cabinet foreground, monochrome layer).
- Placeholder shelf screen showing the brand identity and a wooden plank preview.
- GitHub Actions: build + unit tests + lint on every push/PR; signed release APK on `v*` tags.
- MIT license, standard Android `.gitignore`, no keystore in the repo.
