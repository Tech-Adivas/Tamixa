# Audio Generation Screen

## Overview

The Audio Generation screen (`/dashboard/stories/audio`) provides a dedicated interface for content operations team members to trigger and monitor TTS (text-to-speech) audio generation for approved stories across all supported languages.

## Purpose

This screen is specifically designed for the audio generation workflow, separate from the content review and narration approval workflows. It allows admins to:

1. View stories that are approved and ready for audio generation (PUBLISHED status)
2. Trigger audio generation for all languages or specific languages
3. Monitor audio generation progress in real-time
4. Preview generated audio before approval
5. Approve narration for each language individually

## Features

### Story Filtering

- **Status Filter**: Automatically filters to show only stories with `status=PUBLISHED` (approved for audio generation)
- **Story ID Filter**: URL parameter `?storyId=123` to focus on a specific story
- **Pagination**: 20 stories per page with navigation controls

### Audio Generation

- **Generate All Languages**: Trigger TTS for all 6 supported languages (Tamil, English, Hindi, Telugu, Kannada, Malayalam)
- **Generate Individual Language**: Trigger TTS for a specific language only
- **Progress Monitoring**: Real-time progress updates with percentage completion
- **Auto-polling**: Automatically polls for status updates every 3 seconds during active processing

### Audio Preview

- **Language Selection**: Preview audio for any completed language
- **Playback Controls**: Play, pause, and stop controls
- **In-browser Playback**: Audio streams directly in the browser without downloads

### Audio Metadata

- **Duration**: Shows audio duration in minutes and seconds
- **Generation Timestamp**: Displays when audio was last generated (IST timezone)
- **File Size**: Shows estimated file size (if available)
- **Status Badges**: Visual indicators for each language's status (PENDING, TRANSLATING, REWRITING, TTS_PROCESSING, COMPLETED, FAILED)

### Narration Approval

- **Per-Language Approval**: Approve narration for each language individually
- **Approval Tracking**: Shows which languages have been approved
- **Approval Persistence**: Approvals are stored in the database and persist across sessions

## Workflow

### 1. Story Approval (Prerequisite)

Before a story appears on the Audio Generation screen:

1. Story must be created and saved as DRAFT
2. Story must be submitted for review (moves to PROCESSING)
3. Translation pipeline must complete (moves to READY)
4. Content reviewer must approve the story (moves to PUBLISHED)

### 2. Audio Generation

Once a story is in PUBLISHED status:

1. Navigate to `/dashboard/stories/audio`
2. Find the story in the list
3. Click "Generate" dropdown
4. Select "All Languages" or a specific language
5. Monitor progress in the "Audio Status" column
6. Wait for completion (typically 1-2 minutes per language)

### 3. Audio Preview

After audio generation completes:

1. Click the Play button in the "Preview" column
2. Select a language from the dropdown
3. Use playback controls to listen to the audio
4. Verify audio quality and accuracy

### 4. Narration Approval

After verifying audio quality:

1. Click "Approve" dropdown in the "Actions" column
2. Select the language to approve
3. Repeat for each language that passes quality check
4. Approved narrations are marked in the database

## API Endpoints

### Story List

```
GET /api/v1/admin/stories?status=PUBLISHED&page=0&size=20
```

Returns paginated list of stories ready for audio generation.

### Pipeline Status (Batch)

```
GET /api/v1/admin/pipeline/status-batch?ids=1&ids=2&ids=3
```

Returns pipeline status for multiple stories in a single request.

### Trigger Audio Generation

```
POST /api/v1/admin/stories/{id}/trigger-pipeline
```

Triggers audio generation for all languages.

### Regenerate Audio (Specific Languages)

```
POST /api/v1/admin/stories/{id}/regenerate-narration
Content-Type: application/json

{
  "languages": ["ta", "en"]
}
```

Regenerates audio for specific languages only.

### Preview Audio

```
GET /api/v1/admin/stories/{id}/preview-audio?language=ta
Authorization: Bearer {token}
```

Streams audio file for preview playback.

### Approve Narration

```
POST /api/v1/admin/stories/{id}/translations/{language}/approve-narration
```

Approves narration for a specific language.

## Status Indicators

### Pipeline Status per Language

- **PENDING**: Not started yet
- **TRANSLATING**: Translating content to target language
- **REWRITING**: Converting to conversational narration script
- **TTS_PROCESSING**: Generating audio with text-to-speech
- **COMPLETED**: Audio generation complete
- **FAILED**: Error occurred (with error message)

### Overall Status

- **PENDING**: No languages started
- **TRANSLATING_LANGUAGES**: One or more languages translating
- **TTS_PROCESSING**: One or more languages generating audio
- **COMPLETED**: All languages complete
- **FAILED**: One or more languages failed

## Error Handling

### Common Errors

1. **"Story must be in READY status"**
   - Solution: Approve the story in Content Review first

2. **"Session expired"**
   - Solution: Log in again

3. **"Audio is not ready for this language yet"**
   - Solution: Run Generate/Regenerate audio first

4. **"Preview failed"**
   - Solution: Check if audio file exists in S3, or regenerate audio

### Error Recovery

- **Failed Languages**: Use "Retry failed" button to retry only failed languages
- **Stuck Pipeline**: If progress stops for 15+ minutes, contact admin to clear stuck pipeline
- **Missing Audio**: Use "Reset narration status" in admin tools, then regenerate

## Performance

### Polling Strategy

- **Active Polling**: Every 3 seconds when stories are processing
- **Polling Duration**: Up to 6 minutes after trigger
- **Batch Requests**: Single API call for all visible stories
- **Auto-stop**: Polling stops when all stories reach stable state

### Optimization

- **Batch Status Fetching**: Reduces N requests to 1 for pipeline status
- **Conditional Polling**: Only polls when stories are actively processing
- **Silent Refresh**: Background refresh doesn't show loading spinner

## Relationship to Other Pages

### Content Review (`/dashboard/stories/review`)

- Stories in READY status
- Content approval workflow
- Moves stories to PUBLISHED status (ready for audio)

### Narration Tab (`/dashboard/stories/to-speech`)

- Stories with narration approved
- Final narration approval workflow
- Marks stories as available for mobile app

### Story Editor (`/dashboard/stories/{id}/edit`)

- Edit story content and metadata
- View detailed pipeline status
- Regenerate audio with custom prompts

## Configuration

### Backend Environment Variables

- `AUTO_TTS_ON_APPROVE`: When `true`, audio generation starts automatically on story approval (default: `false`)
- `AUDIO_AFTER_APPROVAL`: When `true`, audio generation is manual after approval (default: `true`)

### Feature Flags

- None currently (all features enabled by default)

## Permissions

### Required Permissions

- `MANAGE_STORIES`: Required to access the audio generation screen
- `MODERATE_STORIES`: Required to approve narrations

### Role Requirements

- **Content Manager**: Can generate audio and approve narrations
- **Admin**: Full access to all features
- **Viewer**: No access (read-only role)

## Best Practices

### Audio Generation

1. **Generate All Languages**: Use "All Languages" for new stories
2. **Regenerate Specific**: Use individual language regeneration for fixes
3. **Monitor Progress**: Watch progress bar to ensure completion
4. **Verify Quality**: Always preview audio before approval

### Quality Assurance

1. **Listen to Full Audio**: Don't approve without listening
2. **Check Pronunciation**: Verify proper names and technical terms
3. **Verify Emotion**: Ensure emotion mode matches story content
4. **Check Duration**: Verify duration matches expected length

### Troubleshooting

1. **Check Status First**: Review pipeline status before regenerating
2. **Wait for Completion**: Don't interrupt active processing
3. **Use Retry**: Use "Retry failed" instead of full regeneration
4. **Contact Support**: For persistent issues, contact backend team

## Future Enhancements

### Planned Features

- [ ] Bulk audio generation for multiple stories
- [ ] Audio quality metrics (clarity, pronunciation score)
- [ ] Custom voice selection per language
- [ ] Audio waveform visualization
- [ ] Download audio files for offline review
- [ ] Audio comparison (before/after regeneration)
- [ ] Automated quality checks (duration, silence detection)

### Under Consideration

- [ ] Audio editing (trim, adjust speed)
- [ ] Multi-language preview (play all languages in sequence)
- [ ] Audio analytics (listen count, completion rate)
- [ ] Integration with external TTS providers
- [ ] Voice cloning support for custom voices

## Technical Notes

### Component Structure

```
AudioGenerationPage
├── Story List Table
│   ├── Story Row
│   │   ├── Story Info (ID, Title, Category)
│   │   ├── Audio Status (Pipeline badges, duration, timestamp)
│   │   ├── Preview Controls (Play, Pause, Stop)
│   │   └── Actions (Generate, Approve)
│   └── Pagination Controls
└── Error/Empty States
```

### State Management

- **Local State**: Component-level state with React hooks
- **Polling State**: Managed with `useEffect` and `setInterval`
- **Audio State**: Managed with `useRef` for audio element
- **Context**: Uses `useActionResult` for toast notifications and `usePipelineActive` for banner updates

### Performance Considerations

- **Batch API Calls**: Single request for all visible stories
- **Conditional Rendering**: Only renders visible rows
- **Memoization**: Callbacks memoized with `useCallback`
- **Cleanup**: Audio objects properly cleaned up on unmount

## Support

For issues or questions:

1. Check this README first
2. Review backend logs for API errors
3. Check browser console for client-side errors
4. Contact the development team with story ID and error details

## Changelog

### Version 1.0.0 (Initial Release)

- Created dedicated audio generation screen
- Implemented story filtering by PUBLISHED status
- Added audio generation for all languages and individual languages
- Implemented real-time progress monitoring with polling
- Added audio preview with playback controls
- Implemented per-language narration approval
- Added comprehensive error handling and user feedback


### Version 1.1.0 (Task 15: Audio Preview and Approval UI Enhancement)

**New Components:**

- **AudioPreviewPlayer**: Rich audio player with full playback controls
  - Play/pause/stop controls
  - Seek bar with time display
  - Speed control (0.75x, 1x, 1.25x, 1.5x)
  - Volume control with mute toggle
  - Skip forward/backward (10 seconds)
  - Metadata display (duration, file size, generation timestamp)

- **AudioPreviewModal**: Modal dialog for focused audio preview
  - Automatic audio fetching from API
  - Full-featured AudioPreviewPlayer integration
  - Story information display
  - Loading and error states

- **AudioApprovalDialog**: Confirmation dialog for audio approval
  - Multi-step approval process (Initial → Approval → Confirm)
  - Quality checklist (informational)
  - Optional approval notes field
  - Required rejection reason (if rejection is implemented)
  - Loading states during approval/rejection

**Enhanced Features:**

1. **Rich Audio Preview**:
   - Click "Preview" button → Select language → Opens AudioPreviewModal
   - Full-featured player with seek, speed, volume controls
   - Skip forward/backward 10 seconds
   - Metadata display (duration, file size, timestamp)

2. **Improved Approval Workflow**:
   - Click "Approve" button → Select language → Opens AudioApprovalDialog
   - Review quality checklist before approval
   - Add optional notes for approval documentation
   - Multi-step confirmation to prevent accidental approvals

3. **Better User Experience**:
   - Modal interface for focused listening
   - Clear visual feedback during all operations
   - Professional error handling with user-friendly messages
   - Consistent design following Tamixa design tokens

**Component Documentation:**

See [Audio Components README](../../../components/audio/README.md) for detailed component documentation, usage examples, and integration guide.

**Migration Notes:**

- Old inline audio player replaced with modal-based preview
- Old direct approval replaced with confirmation dialog
- All existing functionality preserved with enhanced UX
- No breaking changes to API or data structures
