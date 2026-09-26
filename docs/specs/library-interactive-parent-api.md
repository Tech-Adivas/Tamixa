# Library & interactive graph — parent API contract

This note is for client and integration authors. Admin and internal tooling may see additional fields.

## List catalog

- `GET /v1/stories/library?language={code}&page=&size=&theme=&learnHub=`
- `language` should be a supported pipeline code (e.g. `ta`, `en`, `hi`, `te`, `kn`, `ml`). Unknown codes may be normalized server-side or rejected depending on validation.
- Responses are paged; each item may include `interactiveGraph` when the story is an interactive episode (JSON object with `startSegmentId` and `segments`).

## Single story (parent)

- `GET /v1/stories/library/{id}?language={code}`
- Returns the story row for that language when approved for library delivery.
- **`interactiveGraph`**: merged graph for playback (base graph plus translation overlay applied server-side when present).
- **`translationInteractiveGraphOverlay`**: **not** returned on this route; it exists on admin DTOs for editors only. Parent apps must not depend on it.

## Client behavior

- Use **`interactiveGraph` only** for branching UI, segment audio URLs, and choices.
- If a segment is missing from the parsed graph or `audioUrl` is empty, show a user-visible error and do not fall back to the main story stream URL (that would be the wrong narration).

## Related

- Mobile normalizes library `language` via `LibraryPipelineLanguages.normalizeForApi` before API calls.
- Web listener helpers: `web/src/lib/storyListenerUi.ts` (`isInteractivePracticeLibraryStory`).
