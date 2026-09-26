# Quick Reference: Language-Agnostic Translation Pipeline

## 🎯 The Problem in 30 Seconds

**Before**: System assumes Tamil is always the source language  
**Issue**: Story #136 (English) gets wrong translations  
**Fix**: Use actual `story.language` as source, not hardcoded 'ta'

## 🔧 Key Code Changes

### 1. Source Language Detection
```kotlin
// ❌ BEFORE (Wrong)
val sourceLanguage = appProperties.translationPipeline.sourceLanguage // Always 'ta'

// ✅ AFTER (Correct)
val sourceLanguage = story.language.trim().lowercase().take(10)
    .ifEmpty { "ta" } // Backward compatibility only
```

### 2. Translation Direction
```kotlin
// ✅ NEW LOGIC
fun resolvePipelineSourceText(story: LibraryStory, targetLang: String): PipelineSourceText {
    val masterLang = story.language.trim().lowercase().take(10).ifEmpty { "ta" }
    
    // Validate
    if (masterLang !in supportedLanguages) {
        throw IllegalStateException("Unsupported language: $masterLang")
    }
    
    // If translating TO the master language, use master content
    if (targetLang.equals(masterLang, ignoreCase = true)) {
        return PipelineSourceText(masterLang, story.content, story.title, story.moral)
    }
    
    // If translating FROM master TO another language, use master content
    return PipelineSourceText(masterLang, story.content, story.title, story.moral)
}
```

### 3. Interactive Graph Handling
```kotlin
// ✅ COPY GRAPH STRUCTURE
val toSave = if (existing != null) {
    existing.copy(
        content = body,
        title = result.title,
        moral = result.moral,
        interactiveGraphJson = masterInteractiveGraphJson, // ← Copy from master
        // ...
    )
} else {
    StoryTranslation(
        // ...
        interactiveGraphJson = masterInteractiveGraphJson, // ← Copy from master
    )
}
```

## 📋 Migration Script Checklist

```sql
-- 1. Backup
CREATE TABLE library_stories_backup_language AS 
SELECT id, language FROM library_stories;

-- 2. Fix NULL/empty
UPDATE library_stories 
SET language = 'ta' 
WHERE language IS NULL OR language = '';

-- 3. Validate
SELECT id, title, language 
FROM library_stories 
WHERE language NOT IN ('ta', 'en', 'hi', 'te', 'kn', 'ml');

-- 4. Audit log
INSERT INTO admin_audit (action, resource_type, resource_id, details)
SELECT 'language_migration', 'library_story', id, 
       jsonb_build_object('old_language', NULL, 'new_language', 'ta')
FROM library_stories 
WHERE language = 'ta' AND id IN (SELECT id FROM library_stories_backup_language WHERE language IS NULL);
```

## 🎨 Admin UI Changes

### Before
```tsx
<Button onClick={regenerateAndSync}>
  Regenerate & Sync All Languages
</Button>
```

### After
```tsx
<div className="flex gap-2">
  <Button onClick={regenerateStory}>
    Regenerate Story (Source Only)
  </Button>
  <Button onClick={translateAllLanguages}>
    Translate to All Languages
  </Button>
</div>
```

## 🧪 Testing Checklist

- [ ] Story #136 (English, interactive) → Translates correctly
- [ ] Tamil story (linear) → Works as before
- [ ] Hindi story (new) → Translates from Hindi
- [ ] NULL language → Defaults to 'ta'
- [ ] Invalid language → Validation error
- [ ] Interactive graph → Structure preserved
- [ ] Linear story → No graph issues
- [ ] Parallel translations → No performance degradation

## 📊 Validation Queries

```sql
-- Check all stories have valid language
SELECT COUNT(*) FROM library_stories 
WHERE language IS NULL 
   OR language = '' 
   OR language NOT IN ('ta', 'en', 'hi', 'te', 'kn', 'ml');
-- Expected: 0

-- Check Story #136
SELECT id, title, language, 
       interactive_graph IS NOT NULL as has_graph 
FROM library_stories 
WHERE id = 136;
-- Expected: language='en', has_graph=true

-- Check translations have graphs
SELECT master_story_id, language, 
       interactive_graph IS NOT NULL as has_graph 
FROM story_translations 
WHERE master_story_id = 136;
-- Expected: All have has_graph=true (or false for linear)
```

## 🚨 Common Pitfalls

### ❌ Pitfall 1: Assuming Tamil is source
```kotlin
// DON'T
val source = "ta" // Hardcoded

// DO
val source = story.language.ifEmpty { "ta" }
```

### ❌ Pitfall 2: Not copying interactive graph
```kotlin
// DON'T
StoryTranslation(
    content = translated,
    // Missing: interactiveGraphJson
)

// DO
StoryTranslation(
    content = translated,
    interactiveGraphJson = story.interactiveGraphJson, // Copy from master
)
```

### ❌ Pitfall 3: Not validating language
```kotlin
// DON'T
val lang = story.language // Could be invalid

// DO
val lang = story.language.trim().lowercase().take(10)
if (lang !in supportedLanguages) {
    throw IllegalStateException("Unsupported: $lang")
}
```

## 🔍 Debugging Tips

### Check source language detection
```kotlin
log.info("Story ${story.id}: language='${story.language}' → source='$sourceLanguage'")
```

### Check translation direction
```kotlin
log.info("Translating story ${story.id}: $sourceLanguage → $targetLanguage")
```

### Check graph propagation
```kotlin
log.info("Story ${story.id}: master has graph=${story.interactiveGraphJson != null}, " +
         "translation has graph=${translation.interactiveGraphJson != null}")
```

## 📞 Support

- **Spec Location**: `.kiro/specs/language-agnostic-translation-pipeline/`
- **Requirements**: `requirements.md`
- **Decisions**: `DECISIONS.md`
- **Tasks**: `tasks.md`
- **Analysis**: `ANALYSIS.md`

## ✅ Definition of Done

- [ ] Migration script run successfully
- [ ] All stories have valid language values
- [ ] Core pipeline uses story.language
- [ ] Admin UI has two separate actions
- [ ] Story #136 works correctly
- [ ] Existing Tamil stories unchanged
- [ ] All tests passing
- [ ] Documentation updated
- [ ] Team trained on new workflow
