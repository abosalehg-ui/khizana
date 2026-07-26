# Changelog — خِزانة (Khizana)

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/), and the
project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
