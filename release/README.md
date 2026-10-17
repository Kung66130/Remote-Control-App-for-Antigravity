# Google Play release preparation

Status: local preparation completed; NOT uploaded, submitted for review, or published.

## App identity

- Play Console account: Kunxxs Labs.
- App: Antigravity Remote; default language Thai.
- Application ID: `com.kunxxslabs.antigravityremote` (availability checked in Console).
- Version: 1.0 (1); minimum SDK 24; target SDK 36.
- The earlier USB debug installation uses `com.example.antigravityremote` and is a separate app. Do not uninstall it to migrate data without the user's permission.

## Files

- `Antigravity-Remote-1.0.aab`: signed upload bundle; not a final publication candidate until privacy and review requirements below are completed.
- `Antigravity-Remote-1.0.apk`: signed release APK for testing.
- `store-assets/icon-512.png`: 512 × 512 store icon derived from the user's chosen artwork.
- `store-assets/feature-graphic-1024x500.png`: feature graphic.
- `store-assets/screenshot-connect.png` and `screenshot-scanner.png`: actual emulator screenshots (scanner background is the emulator's virtual camera scene).
- `store-listing-th.md`: proposed Thai listing and release notes.
- `privacy-policy.html`: hosted at `https://kung66130.github.io/Remote-Control-App-for-Antigravity/` and linked from the app's connection screen. `privacy-policy-draft.md` remains the editable source for review.

## Validation

- `:app:bundleRelease :app:assembleRelease :app:testDebugUnitTest :app:lintRelease` succeeded.
- Existing unit tests: 2 passed. These are basic ViewModel tests, not a complete remote-connection test.
- Signed release installed and launched on the Pixel 7 Pro emulator; home screen and camera scanner inspected. Runtime camera permission prompt verified after revoking the test permission.
- Lint: 0 errors; existing warnings remain (including dependency update suggestions and absent monochrome icon).
- AAB JAR signature and APK signature verified. The upload certificate is intentionally self-signed.
- APK ZIP alignment checked with `zipalign -c -P 16 4`.
- All bundled arm64-v8a and x86_64 ELF LOAD alignments are at least 16 KB. No 16 KB device runtime test has been performed.
- Removed broad photo/storage permissions; existing system file picker remains.
- Added camera permission request before QR scanner navigation.
- Release WebView debugging disabled; spoken message content removed from debug logging.
- App data excluded from backups and device transfers.
- Home-screen logo uses the selected artwork; paste button shortened to prevent text clipping.

## Signing material — do not publish

The upload keystore is in `../release-private/antigravity-upload.jks`.
Local signing configuration and passwords are in `../keystore.properties`.
Both are git-ignored. Back them up privately; never upload them as store assets or commit them.
Google Play App Signing configuration remains to be completed in the Console.

## Publication blockers

1. Confirm the personal-account closed-test requirement: Google Play may require at least 12 opted-in testers for 14 continuous days before Production becomes available. The alpha track already contains version 1; tester opt-ins and elapsed days still need confirmation in Console.
2. Complete Data safety, target audience, content rating and app-access declarations. The remote web service and speech-engine processing must not be represented as entirely on-device.
3. Provide non-sensitive working review access/instructions to the remote functionality when required; no user tokens or private sessions have been submitted.
4. Verify actual store screenshots, remote connection and file upload behavior before submission.
5. The Play listing already has Thai title, full description, short description, icon, feature graphic, two phone screenshots, contact email `Kung66130@gmail.com`, phone, website, and the hosted privacy-policy URL.

## Current Console handoff

The app now exists in Play Console. API verification on 24 September 2026 found version 1 in Internal testing and Closed testing (alpha); Production has no release yet. The browser handoff page may need to be reopened because the Computer Use browser bridge is currently unavailable.

## Official references

- https://support.google.com/googleplay/android-developer/answer/14115180
- https://support.google.com/googleplay/android-developer/answer/9866151
- https://support.google.com/googleplay/android-developer/answer/9859455
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://developer.android.com/guide/practices/page-sizes
