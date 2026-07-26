# TESTING — خِزانة (Khizana)

## Automated

- **Unit tests:** `./gradlew testDebugUnitTest`
- **Instrumented tests:** `./gradlew connectedDebugAndroidTest` (device/emulator required)

Current coverage (grows with each milestone):

| Area | Test | Milestone |
|---|---|---|
| ThemeMode parsing | `ThemeModeTest` | M0 |

Planned (from the project spec): `FileFingerprint` (M1), `NaturalOrderComparator` (M8),
spread mapping RTL/LTR (M5), progress with `pageCount = 0/1` (M4), excluded-folder
filtering (M1), DAO hide/progress persistence across rescans (M1), `PdfEngine`
protected-file handling (M2).

## Manual checklist

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
