# TESTING — خِزانة (Khizana)

## Automated

- **Unit tests:** `./gradlew testDebugUnitTest` — this is the whole automated
  suite. Room/DAO tests run on the JVM through Robolectric, so CI executes
  them; there is no instrumented source set and no emulator step to forget.
- **Lint:** `./gradlew lintDebug`

Current coverage (grows with each milestone):

| Area | Test |
|---|---|
| ThemeMode parsing | `ThemeModeTest` |
| Fingerprint identity (same content two paths -> same id; same size different content -> different id) | `FileFingerprintTest` |
| Excluded-folder filtering (subpath vs similar-named sibling) | `ExcludedPathFilterTest` |
| **Rescan contract**: new file added, move/rename keeps the row and its progress, vanished file marked MISSING not deleted, returning file restored to OK, duplicate content collapses to one row, unreadable file skipped, progress throttled but exact at the end | `LibraryRescanTest` |
| **Restore contract**: unknown book inserted as MISSING with no path, known book's position overwritten (documented behaviour), topics merged by name, tag ids remapped, excluded folders added, bookmarks restored once per page even on a repeat restore | `BackupRestoreTest` |
| **Backup file validation**: missing/newer version rejected, traversal-style and non-fingerprint ids rejected (books and bookmarks alike), out-of-range numbers clamped | `BackupSerializerValidationTest` |
| Room SQL: relocation, MISSING round trip, visible/hidden queries, shelf-scoped query incl. the null New shelf, cover candidates, shelf deletion, bookmark scoping/ordering/note editing/cascade-by-hand; also pins the SQL status literals to the `BookStatus` enum | `KhizanaDatabaseTest` |
| CBZ cover entry selection, image detection, downsample math, natural page order | `CbzCoverTest` |
| Shelf grouping, orphan fallback, tag filtering, Continue-Reading rules, within-shelf ordering, the name/date/size sort modes and their title tie-break | `ShelvesTest` |
| Bookmark note normalization (blank → null, trimming, length cap) and page lookup | `BookmarksTest` |
| **One bookmark per page**: a second save edits the note, clearing a note keeps the bookmark, deletion is scoped | `ReaderBookmarksTest` |
| Share MIME type per format (a wrong type hides every reader app from the chooser) | `BookSharingTest` |
| Progress math (pageCount 0/1, clamping) & direction resolution | `ReadingTest` |
| Spread building (cover alone, odd/even tails) & page<->spread mapping | `SpreadsTest` |
| Backup JSON round-trip (nulls, Arabic, empty sections, bookmarks, a v1 file read by this v2 build) | `BackupSerializerTest` |
| SAF tree-id -> filesystem path (primary, SD, unsupported) | `TreePathsTest` |
| **Schema migrations**: v1 -> v3 in one hop and v2 -> v3 alone, each asserting the reading position, title, manual order, hidden flag and bookmarks all survive | `MigrationTest` |
| **Rescan fast path**: an unchanged file keeps its id without being re-read (proved by making it unreadable first), an in-place edit is fingerprinted again, and a pre-v3 row is stamped after one slow pass | `LibraryRescanTest` |
| Empty/slash-only excluded folders match nothing, and do not disarm real exclusions beside them | `ExcludedPathFilterTest` |
| Share-path boundary: storage volumes yes, app-private and system paths no, and traversal is normalised before the check | `BookSharingTest` |
| Sample-size bounded by decoded area, not width alone (the decompression-bomb aspect ratio) | `CbzCoverTest` |
| Arabic collation: hamza forms file under alef, ta marbuta and alef maqsura fold to their plain forms | `NaturalOrderTest` |
| Natural ordering (Arabic/Latin/Arabic-Indic digits, zeros, case) | `NaturalOrderTest` |
| Search normalization (tashkeel, alef/ta-marbuta/maqsura variants) | `SearchTest` |

Known gaps, stated rather than implied:

- **No Compose UI tests.** The unused `ui-test-junit4` dependency was removed
  rather than left declared and unexercised.
- ~~No migration test.~~ Closed. The v1 schema JSON was reconstructed by
  re-running the annotation processor over the v1 entity shape, so its DDL and
  identity hash are Room's own rather than hand-written; `app/schemas/` now
  holds v1, v2 and v3 and `MigrationTest` replays every step on the JVM. The
  schema directory is registered as a *debug* asset source — `MigrationTestHelper`
  can only find schemas through assets, and a Robolectric test sees the merged
  assets of the variant under test, not the test source set's own.
- ViewModels are untested; their logic is thin and the pure parts it delegates
  to (`buildShelves`, `normalizeForSearch`, `shelfMetricsFor`) are covered.
- The restore test used to re-implement `applyRestore` beside the real one,
  which is how a field the restore silently dropped still read as a passing
  contract. The logic now lives in `BackupRestorer` — no Context, so the test
  drives production code — and the duplicate is gone.

## Manual checklist

### M10 — review fixes
- [ ] With TalkBack on, a book cover is announced as a button ("Open <title>") and double-tapping opens it; with a keyboard attached, Tab reaches a cover and Enter opens it.
- [ ] In the light theme, dragging a book shows a clearly visible gold outline on the shelf it would land on, and a part-read book shows a visible progress bar with a visible groove behind it.
- [ ] Turn off storage access in system settings, then scan: an error line appears under the button instead of the shelves silently staying empty.
- [ ] After a scan finds new books, a "generating covers" line with a live count appears and then disappears.
- [ ] Reading a page for two minutes without touching the screen: the screen stays on. Leaving the reader: it dims normally again.
- [ ] Rescan a large, unchanged library twice — the second run is noticeably faster than the first.
- [ ] Edit a book file in place (append a byte), rescan: it is picked up as a new book rather than silently ignored.
- [ ] Drag-order some books, back up, restore on the same device: the hand-made order comes back exactly.
- [ ] Hand-edit a backup file to add `""` to `excludedFolders`, restore, scan: the library is still there.
- [ ] "Move to shelf" now closes with a «تم» button, and picking a shelf has already moved the book.
- [ ] A book with a password-protected or damaged file shows a lock/warning glyph beside the ribbon text.
- [ ] Split-screen the app on a tablet: covers shrink to the compact size instead of overflowing the row.
- [ ] Books titled «أحمد», «ابن خلدون», «إبراهيم» sort interleaved under alef, not in a clump before it.

### M9 — bookmarks, shelf sort, sharing
- [ ] In the reader, the bookmark icon is outlined on a fresh page and filled after saving; it stays filled after leaving and reopening the book.
- [ ] Saving a note, then tapping the bookmark icon again on the same page, shows that note for editing — it does not create a second bookmark.
- [ ] Clearing the note text and saving keeps the page bookmarked (icon still filled).
- [ ] The bookmark list shows every bookmark page-ordered; tapping one jumps the pager to it, in both an RTL and an LTR book.
- [ ] Deleting a bookmark from the list empties the icon when you are standing on that page.
- [ ] The note field stops accepting input at 500 characters and the counter agrees.
- [ ] In landscape (two-page spread), bookmarking marks the spread's first page and the list jumps back to that spread.
- [ ] Back up, delete a bookmark, restore → the bookmark returns with its note; restoring twice does not duplicate it.
- [ ] Delete a book from the device → its bookmarks are gone (re-add the file, rescan: no stale notes).
- [ ] «الترتيب» → «الاسم» reorders every shelf; «الأحدث إضافةً» puts the newest scan first; «الأكبر حجماً» puts the biggest file first.
- [ ] The choice survives an app restart; «أكمل القراءة» stays newest-read-first under all four.
- [ ] Switching back to «ترتيبي اليدوي» restores the hand-made drag order exactly.
- [ ] Under an automatic sort, dragging a book onto a book on another shelf still moves it to that shelf.
- [ ] ⋮ on a book → «مشاركة» opens the system share sheet; sending to a file manager/email produces a working copy of the PDF/CBZ.
- [ ] Sharing a book whose file was deleted behind the app's back shows the failure message instead of an empty share sheet.

### M8
- [ ] Searching «تاريخ» finds «تَارِيخ الطبري» (diacritics ignored); «مكتبه» finds «مكتبة».
- [ ] Books titled المجلد 1 / المجلد 2 / المجلد 10 appear in that order on the shelf.
- [ ] Drag a book onto another book → it lands right before it; onto the end slot → it appends.
- [ ] The manual order survives app restart and rescans.
- [ ] Reading part of a book puts it in «أكمل القراءة» at the top; finishing it removes it.
- [ ] A CBZ with pages p1, p2, p10 (unpadded) shows them in the right order.

### M7
- [ ] Deep scan toggle on → scan finds a PDF in a folder MediaStore doesn't index.
- [ ] Excluding a folder removes its books on the next scan (marked missing, not deleted); removing the exclusion brings them back.
- [ ] «حذف من الجهاز» asks for confirmation, then the file is really gone from storage.
- [ ] Back up, wipe app data, restore, scan → shelves, tags, progress and hidden flags all return.
- [ ] Restore an old backup over current data → nothing is lost, metadata merges.

### M6
- [ ] ⋮ on a shelf header renames it in place; delete moves its books to «جديد ⭐» (nothing lost).
- [ ] ⋮ on a book cover → Hide removes it from every shelf immediately.
- [ ] «الكتب المخفية» lists hidden books; «إظهار» returns the book to its original shelf.
- [ ] Hide a book, rescan → it stays hidden.
- [ ] Add a tag to a book; the tag chip row appears; selecting it filters all shelves.
- [ ] Removing a tag from its last book makes the tag disappear from the filter row.

### M5
- [ ] Rotating to landscape shows two facing pages; the cover stays alone.
- [ ] In an RTL book, the lower page number is on the right of the spread; in LTR, on the left.
- [ ] Rotating back and forth keeps the current page.
- [ ] Pinch zooms up to 5x; one finger pans while zoomed; swiping pages still works at 1x.
- [ ] Double-tap zooms in; double-tap again restores; turning the page resets zoom.

### M4
- [ ] Tapping a book opens it full-screen; swiping turns pages.
- [ ] An Arabic-titled book pages right-to-left; an English-titled one left-to-right.
- [ ] Close the app mid-book, reopen → the reader resumes on the same page.
- [ ] The cover shows a progress bar after reading; finishing the last page fills it.
- [ ] A CBZ opens and shows its images in order.
- [ ] Page indicator uses Western digits (1 / 250) even in the Arabic locale.
- [ ] Tap toggles the top bar; back returns to the shelves.

### M3
- [ ] Books appear standing on wooden planks, grouped by shelf, with «جديد ⭐» first.
- [ ] «رف جديد» creates an empty shelf that shows immediately with the drag hint.
- [ ] Long-press a book and drag it to another shelf → it moves there; the target shelf shows a gold highlight while hovering.
- [ ] Moving a book, then rescanning, keeps it on its shelf.
- [ ] RTL: shelf rows flow right-to-left in the Arabic locale.

### M2
- [ ] After a scan finishes, covers appear for PDF and CBZ books without further taps.
- [ ] A password-protected PDF shows the «محمي بكلمة مرور» badge and never crashes the app.
- [ ] A corrupt file (e.g. a renamed .txt → .pdf) shows the «ملف تالف» badge.
- [ ] Page counts appear on books once covers are generated.
- [ ] Covers survive app restart (cached on disk, not regenerated).

### M1
- [ ] Fresh install shows the permission card; granting from system settings and returning unlocks the scan UI without restarting.
- [ ] "Scan library" finds every PDF/CBZ on the device and lists them.
- [ ] Move a book file to another folder, rescan → same single row, path updated (no duplicate).
- [ ] Rename a book file, rescan → same single row.
- [ ] Delete a book file, rescan → it silently disappears from the list (no notification); restore the file, rescan → it returns with its data intact.
- [ ] Scanning is manual only — restarting the app never triggers a scan.

### M0
- [ ] App launches to the branded placeholder screen.
- [ ] Switching device dark mode flips the app between parchment and dark wood.
- [ ] The in-app theme button cycles System → Light → Dark and survives app restart.
- [ ] Switching device language to Arabic flips the whole UI to RTL with Arabic strings; English flips it back to LTR.
- [ ] Launcher icon renders correctly with round and squircle masks.

### Later milestones (placeholders from the spec)
- [ ] Rotate the screen while reading.
- [ ] Incoming call while reading.
- [ ] Press back while covers are being generated.
- [ ] Storage full while generating covers.
