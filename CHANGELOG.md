# Changelog — خِزانة (Khizana)

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/), and the
project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Security

- **A blank excluded folder no longer hides the entire library.** `""`, `"/"`
  or any all-slashes entry trimmed to the empty prefix, and the subpath test
  then read `startsWith("/")` — true of every absolute path on the device. One
  such row made every scan return zero files and mark every book MISSING, with
  no error anywhere because the scan had genuinely succeeded. Backup files are
  where such a row comes from: `excludedFolders` was the one section read with
  no validation at all. Both ends are fixed — the filter ignores empty
  exclusions, and the importer drops entries that are not absolute paths.
- **Tag references in a backup file must carry a real fingerprint**, the rule
  books and bookmarks already followed. `book_tags` has no foreign key, so a
  reference to an impossible book id sat in the table permanently — and
  `pruneUnused` keeps a tag alive for exactly that kind of row, leaving tags in
  the filter row with no book behind them. `BackupRestorer` also refuses to
  write a reference for a book that does not exist after the restore's own
  inserts, as a second belt.
- **The FileProvider no longer maps the filesystem root.** Sharing needs SD
  cards and USB volumes, which `external-path` alone does not reach, so the
  provider maps `/storage` as well — but not `/`, which made every private file
  the app owns, `khizana.db` included, expressible as a URI. `BookSharing`
  enforces the same boundary in code, on the canonicalised path, so widening
  the XML alone cannot hand out the database.
- **A CBZ entry with an absurd aspect ratio is downsampled instead of
  survived.** The sample size came from the width alone, so a 10 x 2,000,000
  entry — narrower than the target, and the shape a decompression bomb takes —
  passed through at full size and asked the decoder for eighty gigabytes. The
  `OutOfMemoryError` catch did stop the crash, but only after a full round of
  allocation pressure, once per page. Decoded *area* is now bounded too.
- **CI hardening.** The keystore secret reaches the decode step through the
  environment instead of being interpolated into a shell command, and the
  minified release build (`assembleRelease`, R8 and resource shrinking) runs on
  every PR — it used to be exercised for the first time on a release tag.
  A new `dependency-submission` workflow publishes the resolved Gradle
  dependency graph for `main` on every push, which is what gives dependency
  review anything to compare a pull request against: GitHub reads Maven's
  `pom.xml` statically but reaches Gradle only through the submission API, and
  this project's version catalog would defeat a static parser anyway. Review
  itself stays advisory until the dependency graph is switched on for the
  account that owns the repository — not the repository page the action's error
  message links to — and the workflow comment records both facts and the
  condition for removing the line.
- The backup size cap dropped from 32 MB to 8 MB. A real library of several
  thousand books serializes to two or three; the restore holds one transaction
  for its whole duration, so the cap is about how long the database stays
  locked, not about disk.

### Added

- **Opening a book is finally reachable without a touchscreen.** The cover
  responded only to `detectTapGestures`, which never enters the semantics tree:
  TalkBack did not announce it as a button, and a keyboard could not focus or
  activate it. Screen-reader and keyboard users could move, share, tag, hide
  and delete a book from its overflow menu — everything except read it. The
  cover is now a real `clickable` with an "Open «title»" label, and the long
  press stays on the drag source. The overflow button names its book too,
  instead of a shelf of identical "Book options".
- **The reader keeps the screen on.** Reading a page is the one activity in
  this app that involves not touching the screen, so the idle timer dimmed it
  mid-page. Scoped to the reader and released on the way out.
- **Cover generation reports progress.** `CoverWorker` had always emitted it;
  nothing collected it, so after a scan the shelves filled with grey
  placeholders that quietly turned into covers minutes later.
- **A failed scan says so.** `WorkInfo.State.FAILED` collapsed onto the same
  blank state as "no scan has ever run", so an unreadable volume was
  indistinguishable from an empty library. Both workers now log the failure,
  and the library screen shows an error line under the button.
- **Migrations are tested.** The v1 schema JSON was reconstructed by re-running
  the annotation processor over the v1 entity shape — `exportSchema` was off
  when v1 shipped — so its DDL and identity hash are Room's own rather than
  hand-written. `app/schemas/` now holds v1, v2 and v3, and `MigrationTest`
  replays v1 to v3 and v2 to v3 on the JVM, asserting that reading position,
  title, manual order, hidden flag and bookmarks all survive.
- **An architecture section in the README**, stating the dependency rule the
  code already follows: `ui` to `data` to `domain`, and `domain` knows nothing
  about Android.

### Fixed

- **Restoring a backup no longer flattens the hand-made shelf order.**
  `manualOrder` was written into the backup file and applied to books the
  device did not have, but the update path for books it already had left it
  out — so restoring onto the same device returned every shelf and lost the
  arrangement inside it. The restore test had re-implemented `applyRestore`
  beside the real one, which is why a field missing from both still read as a
  passing contract; the logic now lives in `BackupRestorer`, which needs no
  Context, and the test drives production code.
- **A rescan no longer re-reads every file it already knows.** Fingerprinting
  opens each file and reads 64 KB, and it ran for every file on every scan — a
  few thousand books meant a couple of hundred megabytes off storage to arrive
  back at the ids we already had. A file whose path, size and mtime are all
  unchanged now keeps its id untouched. Identity is still the fingerprint:
  anything that moved, changed size or changed mtime takes the full path, and a
  deep scan skips the fast path entirely. This adds `books.lastModified` and
  schema v3; rows from before it carry 0, which no real mtime matches, so they
  are fingerprinted once more and then stamped.
- **The gold accents in the light theme are visible again.** The drop-target
  outline sat at 1.43:1 on parchment and the reading-progress bar at 2.05:1,
  against the 3:1 WCAG asks of a functional non-text element — so the only
  feedback a drag gives was invisible, and so was how far into a book you were.
  Both now use a darker gold (4.2:1 on the background, 3.7:1 on the dimmest
  surface) and the progress bar has a visible groove behind it. Dark mode
  already cleared the bar and is unchanged.
- **Arabic titles sort under alef.** Comparing raw code points put every
  hamza form ahead of plain alef, so «أحمد» and «إبراهيم» collected in a clump
  before «ابن خلدون» instead of interleaving with it. The shelf now folds the
  same letter variants the search already folded.
- **Shelves size themselves from the window, not the display.**
  `LocalConfiguration.screenWidthDp` reports the whole screen while the app
  owns a fraction of it, so 112 dp covers overflowed a split-screen row.
  Settings caps its column at 640 dp instead of stretching lines across a
  tablet.
- **Releases can be installed as upgrades.** `versionCode` was hardcoded to 1,
  so every signed release built from every `v*` tag claimed to be the same
  version. It is derived from the tag now.
- **"Move to shelf" closes with «تم».** Its only button read "Cancel" while
  picking a shelf had already moved the book.
- Status ribbons carry a lock or warning glyph beside the text, so the marker
  does not depend on colour alone. The fallback cover carries the book's title
  for a screen reader instead of a lone letter, and the loading spinner is
  labelled.
- The unused Amiri italic face was removed: no text style asked for it.
- Shared code instead of copies: the fingerprint format rule lives on
  `FileFingerprint`, the drop-target outline is one `Modifier.dropHighlight`,
  and the cover-or-first-letter fallback is one `BookCover` — which is also the
  single place the designed fallback cover on the roadmap will replace.
  `HiddenBooksViewModel` and `ScannerModule` moved into files of their own, and
  the shelf plank is drawn through `drawWithCache` rather than recomputing its
  grain on every recomposition.

### Added

- **Bookmarks with notes.** The reader's top bar carries a bookmark toggle for
  the current page and a list of the book's bookmarks; tapping one jumps to its
  page. A page holds at most one bookmark, so saving again on a saved page
  edits its note instead of stacking rows, and notes are capped at 500
  characters in the field rather than truncated on save. No migration was
  needed: the `bookmarks` table has shipped since schema v1 — it only lacked a
  DAO and a UI. Deleting a book from the device now deletes its bookmarks too;
  nothing here is a foreign key, so they would otherwise outlive their book.
- **Bookmarks travel in the backup file**, which moves the backup format to
  version 2. Version 1 files still restore (they simply carry no bookmarks);
  older builds refuse a version 2 file rather than half-reading it, which is
  what the version check is for. Restoring is idempotent: a bookmark that
  already exists on that page has its note updated instead of duplicated.
- **Shelf sort modes.** Books inside every shelf can be ordered by the manual
  drag-and-drop arrangement (the default, unchanged), by name in natural order,
  by newest addition, or by largest file. The choice is persisted in DataStore
  and applies to all shelves at once. Automatic sorts never rewrite
  `manualOrder`, so switching back to "my order" restores the hand-made
  arrangement — and while one is active, dropping a book onto another book
  moves it to that shelf without a meaningless reorder. Continue Reading keeps
  its most-recently-read order under every mode.
- **Share a book** from the ⋮ menu on its cover, next to Move/Hide/Tags/Delete.
  The file is handed to the chosen app as a `content://` URI from a
  non-exported `FileProvider` — a `file://` URI has thrown
  `FileUriExposedException` since Android 7 — with a read grant scoped to that
  one file. The provider maps `root-path`, because books legitimately live on
  SD cards and USB volumes that `external-path` does not cover. The app still
  has no INTERNET permission; the share sheet is the system's.

### Fixed

- The generated Room schema (`app/schemas/…/2.json`) is committed at last. It
  has been produced on every build since `exportSchema` was switched on but was
  never checked in, which is exactly what a future migration test needs to
  migrate *from* — the gap TESTING.md names. The schema itself is unchanged:
  adding a bookmarks DAO touches no table.

- Dropping a book anywhere on a populated shelf now works: the whole row is an
  append target, with book covers keeping their precise insert-before
  behaviour on top. Previously only an empty shelf accepted drops everywhere —
  once it held books, the targets shrank to the covers themselves plus a
  narrow slot after the last one, so the obvious "drop it on the shelf"
  gesture did nothing. (Device feedback after v0.1.0 testing.)

### Hardening — review follow-up

Security, correctness, performance, accessibility and documentation fixes from
a full-repository review. No new features.

**Security & privacy**

- Dropped `com.github.mhiew:pdfium-android` (last published May 2022) for the
  platform `android.graphics.pdf.PdfRenderer`. PDF parsing is a memory-unsafe
  surface fed by untrusted files inside a process holding All Files Access; the
  platform renderer is patched through Play system updates, a vendored native
  parser is not. Password-protected files are still detected (`SecurityException`).
- Turned off Android's cloud backup (`allowBackup="false"` plus explicit
  exclusions in both rule files). The database and covers were being uploaded to
  Google Drive, contradicting the app's central promise — and the old comment
  claiming covers live in `cacheDir` was simply wrong, they live in `filesDir`.
- Backup files are validated before they touch the database: format version
  checked, book ids required to be bare SHA-256 fingerprints (they become file
  names in the cover store, so `../` was reachable), reads capped at 32 MB
  (`readBytes()` on a picked file could OOM the process), numeric fields clamped.
- `CoverStore` refuses any id that is not a fingerprint.
- CI validates the committed Gradle wrapper JAR before running it, and reviews
  dependencies on pull requests.

**Correctness**

- Every multi-step write now runs in a transaction (`TransactionRunner`):
  rescan (in batches), restore, shelf reorder, shelf delete, permanent delete.
- `OutOfMemoryError` is caught where images are decoded; it is an `Error`, so
  `catch (Exception)` let a single oversized page kill the whole worker.
- Logging added throughout — the app previously had none at all, so every
  swallowed failure was undiagnosable.
- Database v2 with a real migration adds indices on `topicId`,
  `(isHidden, status)` and `addedAt`; `exportSchema` is on so future migrations
  are testable.

**Performance**

- Shelf grouping, sorting and search filtering moved off the main thread, with
  the search text debounced and normalized once per book per database emission
  instead of three times per book per keystroke.
- Scan progress is reported on a stride rather than once per file, which was one
  WorkManager database write per file scanned.
- Reordering reads only the target shelf instead of the entire library.
- The reader caches rendered pages in an `LruCache` and re-renders at higher
  zoom levels, so 5x zoom shows detail instead of a stretched viewport-width bitmap.

**UX & accessibility**

- Shelf rows are no longer clipped: a spine with a status badge, or a large font
  scale, overflowed the fixed 120 dp row and cut the top off the covers.
- Covers now honour the 2:3 ratio their own design token specifies, and scale
  across compact/medium/expanded window widths.
- The book overflow button is a full 48 dp target with a scrim behind the glyph;
  it was 24 dp and invisible on pale covers.
- "Move to shelf…" in the book menu — dragging was the only way to organise a
  library, which excluded keyboard and screen-reader users entirely.
- Real `TopAppBar` on every screen (the library header used to scroll away with
  the settings button), snackbar with undo after hiding a book, an empty-library
  state distinct from a no-search-results state, and a loading state.
- Theme is a three-option segmented control instead of a blind cycling button.
- Restore asks for confirmation and explains that it overwrites reading
  positions; backup/restore disable their buttons and show progress.
- The reader has a page slider, announces page numbers to screen readers, and
  keeps the slider's direction matched to the book's.
- Arabic plurals for book counts, and Western digits everywhere instead of
  Arabic-Indic in the library and Western in the reader.

**Housekeeping**

- Repositories moved to `data.repo` (they import Room entities directly, so
  calling them a domain layer was a fiction); `LibraryScreen.kt` split from 928
  lines into six files.
- Removed dead code: the duplicate `cycleThemeMode`, seven unused `WoodTokens`
  fields, an unused string; extracted `TagDao.getOrCreate` and a generic
  `enumOrNull` in place of four copies each.
- README no longer advertises bookmarks, page inversion or designed fallback
  covers — none of which exist; they are listed under Roadmap instead.


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
- Backup & restore to a user-chosen JSON file: shelves, tags, reading positions, hidden flags and excluded folders — keyed by content fingerprint, with **no paths stored**. Restoring on a new device inserts unknown books as MISSING; the first scan re-attaches them by fingerprint automatically. Topics/tags are merged by name. For books the file already knows, the restored position/shelf/hidden flag **replace** the current ones — an earlier revision of this entry claimed nothing was lost, which was wrong; the Settings screen now warns before the picker opens.
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
