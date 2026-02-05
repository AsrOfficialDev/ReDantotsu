# Release Notes v1.0.4

## New Features & Fixes
- **Source Deduplication**: Fixed an issue where extending multiple repositories could result in duplicate extensions appearing in the source list. The app now intelligently deduplicates sources based on package names, ensuring a cleaner and more organized extension list.

## Internal Changes
- Updated internal deduplication logic in `ExtensionGithubApi.kt`.
