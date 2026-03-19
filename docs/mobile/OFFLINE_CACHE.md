# Offline & Cache Behavior

Overview of what is cached, what requires network, and what happens when offline.

## Cached Data

| Data | Storage | TTL / Limits | Offline behavior |
|------|---------|--------------|------------------|
| Auth tokens | DataStore / SecureStorage | Until expired or logout | App remains "logged in" until token expires. API calls fail with 401 when expired. |
| Generated stories | StoryCache (in-memory / DataStore) | ~50 stories (configurable) | Cached stories are playable if audio URL is accessible. New generation requires network. |
| Settings (language, dark mode) | DataStore | Persistent | All settings work offline. |
| Favorite IDs | Server-only | — | Favorites list requires network. Toggling favorite fails offline (shows snackbar). |

## Requires Network

- **Login / Register** – Always requires network
- **Story generation** – OpenAI + TTS pipeline
- **Curated stories** – Fetched from server
- **Search** – Server-side search
- **Favorites list** – Fetched from server
- **Recent playback** – Server-side
- **Streaming audio** – Requires network to stream (no full offline download by default)
- **Voice upload** – Requires network
- **Subscription status** – Requires network

## Offline Behavior

1. **Dashboard** – Shows empty lists if never loaded. Cached stories (from `StoryCache`) appear if previously loaded.
2. **Story playback** – Audio streaming fails offline. TTS fallback (device synthesis) may work for some content if text is cached.
3. **Search** – Returns empty; snackbar: "No internet connection" or "Connection problem"
4. **Favorites** – Empty list; snackbar on load failure
5. **Pull-to-refresh** – Same as initial load; failures show snackbar

## Future Improvements

- [ ] Persist curated story metadata for offline browsing
- [ ] Optional offline audio download before play
- [ ] Queue favorite toggles and sync when back online
- [ ] Offline indicator in UI (e.g. banner when network unavailable)
