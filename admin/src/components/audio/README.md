# Audio Preview and Approval Components

This directory contains reusable components for audio preview and approval workflows across the admin dashboard.

**Task 15: Admin - Create audio preview and approval UI**

## Components

### AudioPreviewPlayer

A rich audio playback component with full controls.

**Features:**
- Play/pause/stop controls
- Seek bar with current time and duration display
- Speed control (0.75x, 1x, 1.25x, 1.5x)
- Volume control with mute toggle
- Skip forward/backward (10 seconds)
- Audio metadata display (duration, file size, generation timestamp)
- Loading and error states
- Responsive design

**Usage:**

```tsx
import { AudioPreviewPlayer } from "@/components/audio";

<AudioPreviewPlayer
  audioSource={audioBlob} // Blob or URL
  language="ta"
  languageLabel="Tamil"
  duration={180} // seconds
  fileSize={2048000} // bytes
  generatedAt="2025-01-15 10:30 AM IST"
  onEnded={() => console.log("Playback ended")}
  onError={(error) => console.error(error)}
/>
```

**Props:**

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `audioSource` | `Blob \| string` | Yes | Audio blob or URL to play |
| `language` | `string` | Yes | Language code (e.g., "ta", "en") |
| `languageLabel` | `string` | Yes | Language display name (e.g., "Tamil") |
| `duration` | `number` | No | Audio duration in seconds |
| `fileSize` | `number` | No | File size in bytes |
| `generatedAt` | `string` | No | Generation timestamp |
| `onEnded` | `() => void` | No | Callback when playback ends |
| `onError` | `(error: string) => void` | No | Callback when error occurs |
| `className` | `string` | No | Additional CSS classes |

---

### AudioApprovalDialog

A confirmation dialog for approving or rejecting audio narrations.

**Features:**
- Audio metadata display
- Quality checklist (informational)
- Approval notes (optional)
- Rejection reason (required for rejection)
- Approve/reject actions with confirmation
- Loading states during approval/rejection
- Error handling

**Usage:**

```tsx
import { AudioApprovalDialog } from "@/components/audio";

<AudioApprovalDialog
  open={isOpen}
  onOpenChange={setIsOpen}
  storyId={123}
  storyTitle="The Brave Fox"
  language="ta"
  languageLabel="Tamil"
  duration={180}
  fileSize={2048000}
  generatedAt="2025-01-15 10:30 AM IST"
  onApprove={async (storyId, language, notes) => {
    await api.approveNarration(storyId, language);
  }}
  onReject={async (storyId, language, reason) => {
    await api.rejectNarration(storyId, language, reason);
  }}
  isApproving={false}
  isRejecting={false}
/>
```

**Props:**

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `open` | `boolean` | Yes | Whether the dialog is open |
| `onOpenChange` | `(open: boolean) => void` | Yes | Callback when dialog open state changes |
| `storyId` | `number` | Yes | Story ID |
| `storyTitle` | `string` | Yes | Story title |
| `language` | `string` | Yes | Language code |
| `languageLabel` | `string` | Yes | Language display name |
| `duration` | `number` | No | Audio duration in seconds |
| `fileSize` | `number` | No | File size in bytes |
| `generatedAt` | `string` | No | Generation timestamp |
| `onApprove` | `(storyId, language, notes?) => Promise<void>` | Yes | Callback when audio is approved |
| `onReject` | `(storyId, language, reason) => Promise<void>` | No | Callback when audio is rejected |
| `isApproving` | `boolean` | No | Whether approval is in progress |
| `isRejecting` | `boolean` | No | Whether rejection is in progress |

---

### AudioPreviewModal

A modal dialog that displays the AudioPreviewPlayer with story context.

**Features:**
- Fetches audio from API automatically
- Full-featured audio player
- Story information display
- Loading and error states
- Responsive design

**Usage:**

```tsx
import { AudioPreviewModal } from "@/components/audio";

<AudioPreviewModal
  open={isOpen}
  onOpenChange={setIsOpen}
  storyId={123}
  storyTitle="The Brave Fox"
  language="ta"
  duration={180}
  fileSize={2048000}
  generatedAt="2025-01-15 10:30 AM IST"
  onError={(error) => console.error(error)}
/>
```

**Props:**

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `open` | `boolean` | Yes | Whether the modal is open |
| `onOpenChange` | `(open: boolean) => void` | Yes | Callback when modal open state changes |
| `storyId` | `number` | Yes | Story ID |
| `storyTitle` | `string` | Yes | Story title |
| `language` | `string` | Yes | Language code |
| `duration` | `number` | No | Audio duration in seconds |
| `fileSize` | `number` | No | File size in bytes |
| `generatedAt` | `string` | No | Generation timestamp |
| `onError` | `(error: string) => void` | No | Callback when error occurs |

---

## Integration Example

See `admin/src/app/(dashboard)/dashboard/stories/audio/page.tsx` for a complete integration example.

**Key integration points:**

1. **Preview Audio**: Click "Preview" dropdown → Select language → Opens AudioPreviewModal
2. **Approve Audio**: Click "Approve" dropdown → Select language → Opens AudioApprovalDialog
3. **State Management**: Modal/dialog state managed in parent component
4. **API Integration**: Components fetch audio and handle approval via API

**Workflow:**

```
1. User clicks "Preview" for a language
   ↓
2. AudioPreviewModal opens and fetches audio blob
   ↓
3. AudioPreviewPlayer displays with full controls
   ↓
4. User listens to audio and closes modal
   ↓
5. User clicks "Approve" for the same language
   ↓
6. AudioApprovalDialog opens with metadata
   ↓
7. User confirms approval (with optional notes)
   ↓
8. API call to approve narration
   ↓
9. Success message and page refresh
```

---

## Design Principles

### Tamixa Design Tokens

All components follow Tamixa design tokens:
- **Spacing**: 8dp grid system (`spacing.cardPadding`, `spacing.sectionSpacing`)
- **Border Radius**: Consistent radii (`radius.card`, `radius.button`)
- **Elevation**: Card shadows (`elevation.card`, `elevation.cardHover`)
- **Colors**: Semantic colors (`primary`, `muted`, `destructive`)
- **Typography**: Consistent font sizes and weights

### Accessibility

- **Keyboard Navigation**: All controls are keyboard accessible
- **Screen Readers**: Proper ARIA labels and semantic HTML
- **Focus States**: Clear focus indicators on all interactive elements
- **Touch Targets**: Minimum 48dp touch targets for mobile
- **Color Contrast**: WCAG 2.1 AA compliant contrast ratios

### Professional Standards

- **Error Handling**: All API calls wrapped in try-catch with user-friendly error messages
- **Loading States**: Clear loading indicators during async operations
- **Validation**: Input validation with inline error messages
- **Defensive Coding**: Null checks, type guards, and fallback values
- **No Silent Failures**: All errors logged and displayed to user

### Security

- **No PII in Logs**: Audio metadata logged without sensitive information
- **Input Validation**: All user inputs validated before API calls
- **Blob Cleanup**: Audio object URLs properly revoked to prevent memory leaks
- **CSRF Protection**: API calls use authenticated session tokens

---

## Testing

### Manual Testing Checklist

**AudioPreviewPlayer:**
- [ ] Audio loads and plays without errors
- [ ] Play/pause/stop controls work correctly
- [ ] Seek bar updates during playback
- [ ] Speed control changes playback rate
- [ ] Volume control adjusts audio level
- [ ] Mute toggle works correctly
- [ ] Skip forward/backward works (10 seconds)
- [ ] Loading state displays while audio loads
- [ ] Error state displays on load failure
- [ ] Metadata displays correctly (duration, file size, timestamp)

**AudioApprovalDialog:**
- [ ] Dialog opens and closes correctly
- [ ] Story and language metadata displays correctly
- [ ] Quality checklist displays on initial view
- [ ] Approval flow: Initial → Approval → Confirm
- [ ] Rejection flow: Initial → Rejection → Confirm (requires reason)
- [ ] Approval notes are optional
- [ ] Rejection reason is required
- [ ] Loading states display during approval/rejection
- [ ] Error messages display on failure
- [ ] Success closes dialog and refreshes page

**AudioPreviewModal:**
- [ ] Modal opens and closes correctly
- [ ] Audio fetches automatically on open
- [ ] Loading state displays while fetching
- [ ] Error state displays on fetch failure
- [ ] AudioPreviewPlayer renders with correct props
- [ ] Story information displays correctly
- [ ] Close button works correctly

### Integration Testing

Test the complete workflow in the audio generation page:

1. Navigate to `/dashboard/stories/audio`
2. Find a story with completed audio (green checkmark)
3. Click "Preview" → Select a language
4. Verify AudioPreviewModal opens and audio plays
5. Test all player controls (play, pause, seek, speed, volume)
6. Close modal
7. Click "Approve" → Select the same language
8. Verify AudioApprovalDialog opens with correct metadata
9. Click "Approve" → Add optional notes → Confirm
10. Verify success message and page refresh
11. Verify narration is marked as approved

---

## Future Enhancements

### Potential Improvements

1. **Waveform Visualization**: Add visual waveform display during playback
2. **Audio Quality Indicators**: Display audio quality metrics (bitrate, sample rate)
3. **Narration Script Display**: Show narration script alongside audio player
4. **Compare Across Languages**: Side-by-side comparison of audio across languages
5. **Batch Approval**: Approve multiple languages at once
6. **Audio Editing**: Basic trim/fade controls for audio adjustments
7. **Playback History**: Track which audio files have been previewed
8. **Keyboard Shortcuts**: Add keyboard shortcuts for common actions (space = play/pause, arrow keys = seek)

### Performance Optimizations

1. **Audio Preloading**: Preload audio for next language in queue
2. **Caching**: Cache audio blobs in memory for faster replay
3. **Lazy Loading**: Load audio player components only when needed
4. **Virtual Scrolling**: For large lists of stories with audio players

---

## Troubleshooting

### Common Issues

**Audio doesn't play:**
- Check browser console for errors
- Verify audio blob/URL is valid
- Check browser audio permissions
- Try different browser (some browsers block autoplay)

**Preview modal doesn't open:**
- Check that story has completed audio (green checkmark)
- Verify API endpoint is accessible
- Check network tab for failed requests

**Approval fails:**
- Verify user has admin permissions
- Check that audio is in COMPLETED status
- Verify API endpoint is accessible
- Check backend logs for errors

**Audio controls don't work:**
- Check browser console for JavaScript errors
- Verify audio element is properly initialized
- Try refreshing the page

---

## Related Documentation

- [Audio Generation Page README](../../app/(dashboard)/dashboard/stories/audio/README.md)
- [Story Status System](../../../docs/STORY_STATUS_SYSTEM.md)
- [Design Tokens](../../lib/design-tokens.ts)
- [API Documentation](../../lib/api.ts)
- [Requirements: Admin Narration Workflow](../../../../.kiro/specs/tamixa-premium-ux-overhaul/requirements.md#requirement-12-admin-narration-workflow-simplification)
