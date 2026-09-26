# Status Components Quick Reference

## Import

```tsx
import {
  StoryStatusBadge,
  StoryStatusIcon,
  PipelineStatusBadges,
  PipelineProgressIndicator,
} from "@/components/design-system";
```

## Story Status Badge

### Basic Usage

```tsx
<StoryStatusBadge status="DRAFT" />
<StoryStatusBadge status="PUBLISHED" />
<StoryStatusBadge status="PROCESSING" />
<StoryStatusBadge status="READY" />
<StoryStatusBadge status="CHANGES_REQUESTED" />
<StoryStatusBadge status="REJECTED" />
```

### With Narration Approval (shows "Live")

```tsx
<StoryStatusBadge 
  status="PUBLISHED" 
  narrationApproved={true} 
/>
```

### Size Variants

```tsx
<StoryStatusBadge status="READY" size="sm" />
<StoryStatusBadge status="READY" size="md" />  {/* default */}
<StoryStatusBadge status="READY" size="lg" />
```

### Without Icon

```tsx
<StoryStatusBadge status="PROCESSING" showIcon={false} />
```

## Story Status Icon (Compact)

For dense table views:

```tsx
<StoryStatusIcon status="READY" />
<StoryStatusIcon status="PUBLISHED" narrationApproved={true} />
```

## Pipeline Status Badges

### Full Display

```tsx
<PipelineStatusBadges 
  status={pipelineStatusResponse}
  showProgress={true}
/>
```

### Compact Mode (short language codes)

```tsx
<PipelineStatusBadges 
  status={pipelineStatusResponse}
  compact={true}
/>
```

### Without Progress Label

```tsx
<PipelineStatusBadges 
  status={pipelineStatusResponse}
  showProgress={false}
/>
```

## Pipeline Progress Indicator

Simple completion count:

```tsx
<PipelineProgressIndicator status={pipelineStatusResponse} />
```

Shows: "3 / 6" or "Complete"

## Helper Functions

```tsx
import { 
  getStoryStatusLabel, 
  getStoryStatusDescription 
} from "@/components/design-system";

const label = getStoryStatusLabel("PUBLISHED", true);  // "Live"
const description = getStoryStatusDescription("READY");  // "Ready for human review..."
```

## Status Types

```typescript
type StoryStatus = 
  | "DRAFT"
  | "PUBLISHED"
  | "PROCESSING"
  | "READY"
  | "CHANGES_REQUESTED"
  | "REJECTED";
```

## Pipeline Status Response

```typescript
interface PipelineStatusResponse {
  // Per-language status
  ta?: string;
  en?: string;
  hi?: string;
  te?: string;
  kn?: string;
  ml?: string;
  
  // Meta fields
  processing?: string;
  overallStatus?: string;
  progress?: string;
  reviewedLanguages?: string;
  // ... more meta fields
}
```

## Common Patterns

### Table Row

```tsx
<tr>
  <td>{story.id}</td>
  <td>{story.title}</td>
  <td>
    <StoryStatusIcon 
      status={story.status} 
      narrationApproved={story.narrationApprovedAt != null}
    />
  </td>
  <td>
    <PipelineStatusBadges 
      status={pipelineStatus} 
      compact 
    />
  </td>
</tr>
```

### Card Header

```tsx
<CardHeader>
  <div className="flex items-center justify-between">
    <CardTitle>{story.title}</CardTitle>
    <StoryStatusBadge 
      status={story.status}
      narrationApproved={story.narrationApprovedAt != null}
    />
  </div>
</CardHeader>
```

### With Conditional Rendering

```tsx
{story.status !== "DRAFT" && (
  <PipelineStatusBadges 
    status={pipelineStatus}
    showProgress={story.status === "PROCESSING"}
  />
)}
```

### Disable Actions During Processing

```tsx
import { isLibraryStoryPipelineActivelyRunning } from "@/lib/library-story-workflow";

const isPipelineActive = isLibraryStoryPipelineActivelyRunning(pipelineStatus);

<Button disabled={isPipelineActive}>
  Edit Story
</Button>
```

## Color Reference

| Status | Color |
|--------|-------|
| DRAFT | Gray |
| PUBLISHED | Amber |
| PROCESSING | Blue (animated) |
| READY | Green |
| CHANGES_REQUESTED | Orange |
| REJECTED | Red |
| LIVE | Green |

| Pipeline Stage | Color |
|----------------|-------|
| PENDING | Gray |
| TRANSLATING | Orange (animated) |
| REWRITING | Orange (animated) |
| TTS_PROCESSING | Orange (animated) |
| COMPLETED | Green |
| FAILED | Red |

## Demo Page

View all variants: `/dashboard/stories/status-demo`

## Full Documentation

See [STORY_STATUS_SYSTEM.md](./STORY_STATUS_SYSTEM.md) for complete documentation.
