# Content Review Screen

## Overview

This is a dedicated content review screen for stories that have completed pipeline processing and are in READY status. Content operations team members can review, approve, reject, or request changes to stories from this screen.

## Location

`/dashboard/stories/review`

## Features

### Story Display
- Lists all stories with status=READY (pipeline processing complete)
- Shows story cover image, title, category, language, age, word count
- Displays pipeline status for all languages with visual indicators
- Shows content preview and moral

### Filtering
- Filter by category (Adventure, Animals, Friendship, etc.)
- Filter by language (Tamil, English, Hindi, Telugu, Kannada, Malayalam)

### Actions
- **View Full Story**: Opens a dialog with complete story details, metadata, and pipeline status
- **Approve**: Moves story to PUBLISHED status, ready for audio generation
- **Request Changes**: Moves story to CHANGES_REQUESTED status with reviewer notes
- **Reject**: Moves story to REJECTED status with rejection reason
- **Edit Story**: Navigate to story editor

### Real-time Updates
- Polls pipeline status every 5 seconds for active stories
- Updates UI automatically when pipeline completes
- Disables actions while pipeline is processing

## API Integration

### Endpoints Used
- `GET /api/v1/admin/stories?status=READY` - Fetch stories ready for review
- `POST /api/v1/admin/stories/{id}/approve` - Approve story
- `POST /api/v1/admin/stories/{id}/reject` - Reject story  
- `POST /api/v1/admin/stories/{id}/request-changes` - Request changes
- `GET /api/v1/admin/stories/{id}/pipeline-status` - Get pipeline status

### New API Methods Added
- `api.admin.approveLibraryStory(id)` - Approve story for publication
- `api.admin.requestChangesLibraryStory(id, notes)` - Request changes with notes

## Design Specifications

### Status Flow
```
READY → Approve → PUBLISHED (ready for audio generation)
READY → Request Changes → CHANGES_REQUESTED (back to content team)
READY → Reject → REJECTED (removed from queue)
```

### UI Components
- Uses `StoryStatusBadge` for consistent status display
- Uses `PipelineStatusBadges` for language-specific pipeline progress
- Card-based layout with hover effects
- Responsive design for mobile and desktop

### Security
- Requires `MODERATE_STORIES` permission
- Shows permission error for unauthorized users
- Validates all inputs before submission

## Workflow

1. Content team submits story for review (status → PUBLISHED)
2. Pipeline processes story (translation, rewriting, TTS prep)
3. Story moves to READY status when pipeline completes
4. Content review team reviews story in this screen
5. Reviewer approves, requests changes, or rejects
6. Approved stories move to Narration tab for audio generation

## Relationship to Existing Pages

- **Library Tab** (`/dashboard/stories`): All stories with filtering and bulk actions
- **Review Tab** (`/dashboard/stories/approve`): Stories in review queue (PUBLISHED, PROCESSING, READY) - focuses on narration approval
- **Content Review** (`/dashboard/stories/review`): Stories in READY status only - focuses on content approval
- **Narration Tab** (`/dashboard/stories/to-speech`): Approved stories ready for audio generation

## Notes

- This screen specifically targets READY status stories for content approval
- The existing `/dashboard/stories/approve` page handles narration approval for the broader review queue
- Both screens serve different purposes in the story workflow
- Consider consolidating or clarifying the distinction between content review and narration review in future iterations
