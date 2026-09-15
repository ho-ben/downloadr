# Downloadr

Downloadr is a private, mobile-first tool for exporting complete Poe conversations as plain-text files. It now has two versions:

- an Android browser app that signs in to Poe directly and exports the open chat with one tap;
- the original installable PWA, which uses a bookmark helper in Chrome.

## Android browser app (experimental)

The Android app in [`android/`](android/) embeds a secure WebView. Sign in to Poe inside the app, open a conversation, and tap **Export**. The transcript moves from Poe's page to the Android app in small on-device chunks; nothing is uploaded to Downloadr. You can copy it or save it directly to `Downloads/Downloadr/`.

Because Google blocks OAuth sign-in inside embedded browsers, use Poe's email sign-in option if the Google button refuses to continue. The app targets Android 10 or newer.

Every push that changes `android/` produces an installable debug APK in the repository's **Actions → Build Downloadr for Android → Artifacts** section. Android will ask you to permit installation from your browser or GitHub app the first time.

## Why it uses a bookmark helper

Poe renders conversation content inside a protected `poe-app://` frame. Browser same-origin rules correctly prevent an unrelated PWA from reading that frame, and Android Chrome does not support ordinary browser extensions. Downloadr therefore uses a one-time bookmark helper that the user explicitly runs on the selected `poe.com/chat/...` page.

The helper:

- runs only on `https://poe.com/chat/...`;
- uses the session that Poe already owns in that tab;
- calls Poe's read-only, paginated message feed from the Poe origin;
- filters results to the selected chat in memory;
- never sends the Poe cookie, messages, or transcript to Downloadr or another server;
- offers both clipboard copy and a Blob-based `.txt` download.

The file-download path avoids Android clipboard-size limits. Poe's internal web query can change; the helper discovers current operation metadata from Poe's loaded client when possible and has a tested fallback for the current release.

## PWA development

```sh
npm test
npm run serve
```

Open <http://localhost:4173>.

## Android development

Open the `android` directory in Android Studio, or run the **Build Downloadr for Android** GitHub Action. The app uses no analytics, ad SDK, backend, or stored credentials. Poe owns its WebView cookies, just as Poe owns cookies in a normal browser.

## Deployment

Pushing `main` deploys the static PWA through GitHub Pages. In the repository settings, set Pages source to **GitHub Actions** if it is not already selected.

## Privacy and security

Downloadr has no backend, analytics, account system, or cookie collection. The service worker caches only the public app shell. Users authenticate directly with Poe; credentials remain under the `poe.com` origin.
