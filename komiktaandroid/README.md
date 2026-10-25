# Komikta Android Native (Kotlin + Jetpack Compose)

Native Android client for the existing Komikta Node.js/Express API. This is not a WebView wrapper.

## Included
- Native Compose UI with dark visual language aligned with the website's current design tokens.
- Home feed and latest updates from `/api/home` and `/api/update`.
- Search, genre filters, latest/popular sorting and catalog grid from `/api/explore`, loading multiple pages (up to 20 pages of 200 titles each).
- Comic detail, synopsis, genres, chapter list and vertical chapter reader.
- Persistent local saved list, native bottom navigation, loading/error/empty states.
- Coil image loading, Retrofit + Gson, coroutine-based requests.

## Configure backend URL

The app uses a **placeholder default** (the website README lists this domain as an example, not a confirmed deployment). You can override it with a Gradle property:

```properties
KOMIKTA_API_BASE_URL=https://your-real-backend-domain.example/
```

Put that property in your local `gradle.properties` (do not commit private values), or pass `-PKOMIKTA_API_BASE_URL=https://your-real-backend-domain.example/` to Gradle. Keep the trailing slash.

Replace it with the actual deployed HTTPS URL for the Node/Express backend, keeping the trailing slash. The website ZIP's README mentions `komikta.vercel.app` only as an example; verify that this is really where your backend is deployed before building.

Expected endpoints: `/api/home`, `/api/update`, `/api/explore`, `/api/detail?url=...`, `/api/read?url=...`.

## Build

Open this folder in Android Studio, sync Gradle, set the backend URL, then run the `app` configuration. Build APK from **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Important limitations in this first native port
- The backend remains hosted separately and must be reachable over HTTPS.
- Bookmarks are persisted locally with SharedPreferences. Reading history and offline chapter downloads are not implemented yet.
- Reader navigation currently renders the chapter images vertically. Previous/next chapter controls, advanced reader settings, and download management remain follow-up work.
- Real API compatibility should be verified against the deployed backend before release.
