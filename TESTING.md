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

Planned (from the project spec): `NaturalOrderComparator` (M8),
spread mapping RTL/LTR (M5), progress with `pageCount = 0/1` (M4).

## Manual checklist

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
