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
| **Restore contract**: unknown book inserted as MISSING with no path, known book's position overwritten (documented behaviour), topics merged by name, tag ids remapped, excluded folders added | `BackupRestoreTest` |
| **Backup file validation**: missing/newer version rejected, traversal-style and non-fingerprint ids rejected, out-of-range numbers clamped | `BackupSerializerValidationTest` |
| Room SQL: relocation, MISSING round trip, visible/hidden queries, shelf-scoped query incl. the null New shelf, cover candidates, shelf deletion; also pins the SQL status literals to the `BookStatus` enum | `KhizanaDatabaseTest` |
| CBZ cover entry selection, image detection, downsample math, natural page order | `CbzCoverTest` |
| Shelf grouping, orphan fallback, tag filtering, Continue-Reading rules, within-shelf ordering | `ShelvesTest` |
| Progress math (pageCount 0/1, clamping) & direction resolution | `ReadingTest` |
| Spread building (cover alone, odd/even tails) & page<->spread mapping | `SpreadsTest` |
| Backup JSON round-trip (nulls, Arabic, empty sections) | `BackupSerializerTest` |
| SAF tree-id -> filesystem path (primary, SD, unsupported) | `TreePathsTest` |
| Natural ordering (Arabic/Latin/Arabic-Indic digits, zeros, case) | `NaturalOrderTest` |
| Search normalization (tashkeel, alef/ta-marbuta/maqsura variants) | `SearchTest` |

Known gaps, stated rather than implied:

- **No Compose UI tests.** The unused `ui-test-junit4` dependency was removed
  rather than left declared and unexercised.
- **No migration test.** `exportSchema` was off until now, so there is no v1
  schema JSON for `MigrationTestHelper` to migrate from. From v2 onwards the
  JSON under `app/schemas/` makes every future migration testable, and one
  should be added with the next schema change.
- ViewModels are untested; their logic is thin and the pure parts it delegates
  to (`buildShelves`, `normalizeForSearch`) are covered.

## Manual checklist

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
