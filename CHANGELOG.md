# Changelog — خِزانة (Khizana)

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/), and the
project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

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
