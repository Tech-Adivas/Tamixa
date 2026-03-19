# Story Retention and Completion Analytics

## Overview

Story retention and completion tracking for Tamixa. Privacy-first: only parent ID is stored, no child PII.

## Tracking Events

| Event | Description |
|-------|-------------|
| `story_started` | User began playback |
| `story_25_percent` | Playback crossed 25% |
| `story_50_percent` | Playback crossed 50% |
| `story_75_percent` | Playback crossed 75% |
| `story_completed` | User finished the story |
| `story_stopped_early` | User left before completing |

## Database: `story_analytics`

| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| parent_id | BIGINT | FK to parents; no child PII |
| story_id | BIGINT | Story or curated story ID |
| story_source | VARCHAR(20) | `curated` or `generated` |
| language | VARCHAR(10) | Story language |
| event_type | VARCHAR(30) | One of the 6 events above |
| timestamp | TIMESTAMPTZ | Event time |
| playback_position_seconds | INT | Position when event fired |

**Migration:** `V13__story_analytics.sql`

## Aggregated Metrics

- **completion_rate_per_story** – % of started sessions that completed, per story
- **average_listen_time** – Avg max playback position per session (seconds)
- **most_popular_category** – Theme/category with most `story_started` events
- **retention_by_language** – Completion rate grouped by language

## API Endpoints

### Tracking (parent-authenticated)

```
POST /api/v1/analytics/story-events
Authorization: Bearer <parent JWT>
Body: { storyId, storySource, language, eventType, playbackPositionSeconds }
```

### Admin (admin-authenticated)

```
GET /api/v1/admin/analytics/retention?days=30
GET /api/v1/admin/analytics/completion?days=30
```

## Mobile Integration

- **AnalyticsApi** – Posts events to the backend
- **StoryPlaybackTracker** – Tracks milestones per session, debounces duplicates
- **AudioPlayerScreen** – Emits `story_started` on enter, `onProgress` at 25/50/75%, `story_completed` at 99%+, `story_stopped_early` on back

When ExoPlayer/MediaSession is wired, pass real `progress` and `positionSeconds` into the analytics callbacks.

## Revenue Dashboard

Story engagement metrics are shown on the Revenue Analytics page:

- Completion rate
- Average listen time
- Most popular category
- Retention by language

These are fetched via `GET /admin/analytics/retention` and `GET /admin/analytics/completion`.
