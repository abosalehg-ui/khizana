# TESTING — خِزانة (Khizana)

## Automated

- **Unit tests:** `./gradlew testDebugUnitTest`
- **Instrumented tests:** `./gradlew connectedDebugAndroidTest` (device/emulator required)

Current coverage (grows with each milestone):

| Area | Test | Milestone |
|---|---|---|
| ThemeMode parsing | `ThemeModeTest` | M0 |
| Fingerprint identity (same content two paths → same id; same size different content → different id) | `FileFingerprintTest` | M1 |
| Excluded-folder filtering (subpath vs similar-named sibling) | `ExcludedPathFilterTest` | M1 |
| Hidden state & progress survive relocation / MISSING round-trip (instrumented) | `BookDaoRescanTest` | M1 |
| CBZ cover entry selection, image detection, downsample math | `CbzCoverTest` | M2 |
| Shelf grouping: New-shelf-first, topic order, orphan fallback | `ShelvesTest` | M3 |
| Progress math (pageCount 0/1, clamping) & direction resolution | `ReadingTest` | M4 |
| Spread building (cover alone, odd/even tails) & page↔spread mapping | `SpreadsTest` | M5 |
| Tag filtering keeps shelf structure; null filter no-op | `ShelvesTest` | M6 |
| Backup JSON round-trip (nulls, Arabic, empty sections) | `BackupSerializerTest` | M7 |
| SAF tree-id → filesystem path (primary, SD, unsupported) | `TreePathsTest` | M7 |
| CBZ reading order (case-insensitive alphabetical) | `CbzCoverTest` | M4 |

Planned (from the project spec): `NaturalOrderComparator` (M8).

## Manual checklist

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
