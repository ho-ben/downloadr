# Downloadr

Downloadr is a private, mobile-first PWA for exporting complete Poe conversations as plain-text files.

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

## Development

```sh
npm test
npm run serve
```

Open <http://localhost:4173>.

## Deployment

Pushing `main` deploys the static PWA through GitHub Pages. In the repository settings, set Pages source to **GitHub Actions** if it is not already selected.

## Privacy and security

Downloadr has no backend, analytics, account system, or cookie collection. The service worker caches only the public app shell. Users authenticate directly with Poe; credentials remain under the `poe.com` origin.
