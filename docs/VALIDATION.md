# Delivery validation

Verified on October 27, 2026. Development release 0.2.0 Preview (version code 2).

| Check | Result |
| --- | --- |
| Debug app and instrumentation APK builds | Passed |
| Kotlin/JVM tests | 21 passed, 0 failed |
| Android device tests | 6 passed, 0 failed |
| Total tests | 27 passed |
| Android Lint | 0 errors; 1 advisory warning |
| APK signature verification | Passed |
| Source/APK correspondence | Built from the delivered app source snapshot |
| Merged manifest | No Internet or broad media-library read permission |
| Side-by-side install | Original app and Preview coexist; original private-data sentinel preserved |

The downloadable APK uses `com.zelun.screenshotgraveyard.preview`. Default source
builds retain `com.zelun.screenshotgraveyard`; the optional Preview flag selects
the separate package. The apps have independent archives. The v0.1 signing key
was temporary and unavailable, so this delivery deliberately avoids replacing
that installation. A private development-signing backup is supplied separately
for future Preview updates; it is excluded from the source archive.

The device run used Android 9 / API 28, x86_64, with software rendering. The
screenshots are actual native emulator captures using fictional sample data or
a generated test image.

The Android tests exercised:

- Room FTS4 prefix and multiword matching, index updates after title edits,
  index cleanup after deletion, and stale status-update protection.
- Preservation of manually edited titles and intentionally cleared tags when OCR finishes.
- Unique content hashes and database persistence after close/reopen.
- Real image import, duplicate detection, actual bundled-model OCR, body-text
  prefix search, keep/bury/revive, and permanent archive-copy removal while
  preserving the input image.
- FileProvider image/png type reporting, sample collection rendering, and sorting navigation.
- Full-screen viewer open/close, actual two-finger pinch, horizontal and vertical
  pan, double-tap zoom/reset, zoom buttons, and Fit.
- Compact search results with the archive hero hidden, and restoration when cleared.

JVM tests cover safe FTS queries, normalization, classification, and image
viewport geometry: focal anchoring, pan and zoom bounds, aspect fit, resizing,
double-tap behavior, and invalid inputs.

The tested APK SHA-256 is:

```
cb6fb2c8cddda7ae1e043d350b4a57d17140e32b3b3c1b6601e9fe38fd9e25de
```

The development signing certificate SHA-256 is:

```
cae20081182f8ae123039636d4a7628605eaf71d39cfc5e3ef3c133665aab4d7
```

Build and test commands (use your private key path):

```sh
./gradlew -PgraveyardPreviewInstall=true \
  -PgraveyardDebugKeystore=/path/debug.keystore \
  assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
./gradlew -PgraveyardPreviewInstall=true \
  -PgraveyardDebugKeystore=/path/debug.keystore connectedDebugAndroidTest
```

This delivery invoked the instrumentation runner directly against the same
built app and test APKs. The final run completed all six Android tests in
141.529 seconds. Raw reports are in `docs/verification/`. Historical v0.1
reports and preview are retained under `docs/history/v0.1.0/`.

The single lint advisory is a tooling/dependency recommendation, not a failing
check. The GitHub Actions build/lint/unit and API 35 emulator workflow is
included but has not run on GitHub for this delivery. Physical devices, a modern
API 35/36 device matrix, store signing, and store publication have not been verified.
