# Delivery validation

Verified on October 26, 2026. Development release 0.1.0.

| Check | Result |
| --- | --- |
| Debug app build | Passed |
| Instrumentation APK build | Passed |
| Kotlin/JVM tests | 11 passed, 0 failed |
| Android device tests | 4 passed, 0 failed |
| Android Lint | 0 errors; 13 advisory warnings |
| APK signature verification | Passed; APK Signature Scheme v2 |
| Source/APK correspondence | Built from the delivered app source snapshot |
| Merged manifest | No Internet or broad media-library read permission |

The device run used Android 9 / API 28, x86_64, with software rendering. The
screenshots show the app and its bundled fictional samples on that emulator.

The Android tests exercised:

- Room FTS4 prefix and multiword matching, index updates after title edits,
  index cleanup after deletion, and stale status-update protection.
- Preservation of manually edited titles and intentionally cleared tags when OCR finishes.
- Unique content hashes and database persistence after close/reopen.
- Real image import, duplicate detection, actual bundled-model OCR, body-text
  prefix search, keep/bury/revive, and permanent archive-copy removal while
  preserving the input image.
- FileProvider image/png type reporting for shared PNG images.
- Sample collection rendering and navigation to the sorting ritual.

The tests ran against the development APK whose SHA-256 is:

```
0a3bdcd9dd84dde89b81cda8d6a035861f0e9933253a77c988bb11088a8da3c0
```

Build and test commands:

```sh
./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
./gradlew connectedDebugAndroidTest
```

This delivery ran the instrumentation runner directly against the same APKs on
the connected emulator. Raw device results and JVM test results are in
`docs/verification/`.

Lint advisories include available dependency upgrades and annotation-processing
tooling recommendations. They do not fail the configured lint check.

A GitHub Actions workflow for build/lint/unit checks and API 35 emulator checks
is included. It has not been run on GitHub as part of this delivery. Physical
devices, a modern API 35/36 device matrix, store signing, and store publication
remain outside this development delivery.
