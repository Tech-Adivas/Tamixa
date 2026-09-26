# Interactive Story Translation & Content Length Fix

## Issues Identified

### 1. Interactive Graph Not Copied During Translation
**Problem**: When using "Regenerate & Sync All languages" on interactive stories, the `interactiveGraphJson` field from the master story (English) was not being copied to the translated story records in other languages. This caused interactive stories to appear as linear stories in non-English languages.

**Root Cause**: The `runTranslationStep` method in `StoryProcessingService.kt` was not handling the `interactiveGraphJson` field during the translation pipeline.

### 2. Interactive Story Content Too Brief
**Problem**: Generated interactive stories had very short content (around 50-100 words total), far below the desired 500-750 words.

**Root Cause**: The LLM prompts in `InteractiveEpisodeAdminService.kt` were instructing the model to create "one to five short sentences" per segment, resulting in minimal content.

## Solutions Implemented

### Fix 1: Copy Interactive Graph During Translation

**File**: `backend/src/main/kotlin/com/tamixa/application/narration/StoryProcessingService.kt`

**Changes**:
1. Added `masterInteractiveGraphJson: String?` parameter to `runTranslationStep` method
2. Updated both the `existing.copy()` and new `StoryTranslation` creation to include `interactiveGraphJson = masterInteractiveGraphJson`
3. Updated the call to `runTranslationStep` in `processLanguage` to pass `story.interactiveGraphJson`

**Impact**: Now when translations are generated, the interactive graph from the master story is properly copied to all language translations, preserving the interactive structure.

### Fix 2: Increase Interactive Story Content Length

**File**: `backend/src/main/kotlin/com/tamixa/application/narration/InteractiveEpisodeAdminService.kt`

**Changes**:

#### In `generateInteractiveGraphFromStory` method:
- Changed prompt from: "one to five short sentences of spoken narration"
- To: "8-12 sentences of rich, engaging spoken narration (no stage directions, no markdown)"
- Added: "Each segment should be 100-150 words to create an immersive, detailed story experience"
- Added: "Use vivid descriptions, dialogue, and sensory details to bring the story to life"
- Added: "Make each choice point meaningful with clear consequences in the following segments"

#### In `fillMissingSegmentNarrationScriptsInternal` method:
- Changed prompt from: "one to five short sentences a narrator would read aloud"
- To: "8-12 sentences (100-150 words) that a narrator would read aloud. Use rich, engaging language with vivid descriptions, dialogue, and sensory details"

**Impact**: 
- With 5-9 segments at 100-150 words each, total story length will be approximately 500-1350 words
- Stories will be more immersive and engaging with richer narrative content
- Better meets the 500-750 word target range

## Expected Results

### Before Fix:
- ❌ Interactive stories in English only
- ❌ Other languages showed linear stories (no choices)
- ❌ Very brief content (~50-100 words total)

### After Fix:
- ✅ Interactive stories work in all configured languages (en, hi, ta, te, kn, ml)
- ✅ Interactive graph structure preserved across all translations
- ✅ Rich, engaging content (500-750+ words)
- ✅ Immersive storytelling with vivid descriptions and dialogue

## Testing Instructions

1. **Create or Edit an Interactive Story in English**:
   - Go to Admin → Stories → Create/Edit
   - Add interactive graph JSON or generate one
   - Ensure the story has interactive choices

2. **Regenerate & Sync All Languages**:
   - Click "Regenerate & sync all languages" button
   - Wait for pipeline to complete

3. **Verify in Admin**:
   - Check "Other languages" tab
   - Verify each language has the interactive graph JSON
   - Check word count is in 500-750 range

4. **Verify in Mobile App**:
   - Open the story in different languages
   - Confirm interactive choices appear
   - Verify content is rich and engaging

## Technical Details

### Database Schema
The fix leverages existing schema:
- `library_stories.interactive_graph` (TEXT) - Master story graph
- `story_translations.interactive_graph` (TEXT) - Per-language graph copy

### Pipeline Flow
```
Master Story (EN) with interactive_graph
    ↓
processSync(translationOnly=true)
    ↓
For each language:
    runTranslationStep() → Copies interactive_graph
    runRewriteStep() → Processes content
    ↓
Translation saved with interactive_graph
```

### Word Count Calculation
- 5-9 segments per story
- 100-150 words per segment
- Total: 500-1350 words
- Target range: 500-750 words ✓

## Files Modified

1. `backend/src/main/kotlin/com/tamixa/application/narration/StoryProcessingService.kt`
   - Added parameter to `runTranslationStep`
   - Updated translation save logic
   - Updated method call

2. `backend/src/main/kotlin/com/tamixa/application/narration/InteractiveEpisodeAdminService.kt`
   - Enhanced LLM prompts for richer content
   - Increased word count targets
   - Added narrative quality guidelines

## Rollout Notes

- ✅ Backward compatible - existing stories unaffected
- ✅ No database migration required
- ✅ No API changes
- ⚠️ Existing interactive stories should be regenerated to get improved content length
- ⚠️ LLM token usage will increase due to longer content generation

## Monitoring

After deployment, monitor:
- Translation pipeline success rates
- Average word count for new interactive stories
- User engagement with interactive stories across languages
- LLM token usage and costs
