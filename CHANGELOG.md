# Changelog

## 0.2.0 — 2026-10-27

- Add a full-screen screenshot viewer with pinch-to-zoom, bounded panning, and double-tap zoom/reset.
- Add accessible zoom-in, zoom-out, and Fit controls, plus loading, retry, and close actions.
- Preserve viewer transforms across configuration changes and clamp them when the available viewport changes.
- Bound viewer decoding and use a separate cache key to avoid magnifying a small gallery thumbnail.
- Hide the large archive header while searching, keeping stable keys for the search field and filters.
- Clear input focus when changing sections or opening details, and close old details when a new import starts.
- Add a separate Preview installation option and an explicit development-keystore option.
- Add geometry and Android UI tests for zoom, pan, reset, return navigation, and compact search.

## 0.1.0 — 2026-10-26

- Initial native Android release: photo-picker/share imports, duplicate detection, on-device OCR, full-text search, tags, keep/bury/revive, sorting ritual, and local storage.
