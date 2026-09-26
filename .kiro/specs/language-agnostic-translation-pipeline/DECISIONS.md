# Architectural Decisions: Language-Agnostic Translation Pipeline

## Decision Summary

As the senior technical lead, I've made the following architectural decisions for this critical fix:

---

## Decision 1: Use Existing `language` Field ✅

**Question**: Should we add a new `source_language` field or use existing `language` field?

**Decision**: Use existing `library_stories.language` field

**Rationale**:
- ✅ **Simplicity**: No schema changes, no data migration complexity
- ✅ **Semantic clarity**: `language` already represents the story's primary/source language
- ✅ **Backward compatible**: Can default to 'ta' for NULL values
- ✅ **Less confusion**: One language field is clearer than two
- ✅ **Faster implementation**: No DDL changes, no deployment coordination

**Trade-offs**:
- ⚠️ Assumes `language` field is accurate (requires validation)
- ⚠️ Cannot distinguish "display language" from "source language" (not needed in our case)

**Implementation**:
```kotlin
// Before (wrong)
val sourceLanguage = appProperties.translationPipeline.sourceLanguage // Always 'ta'

// After (correct)
val sourceLanguage = story.language.trim().lowercase().take(10)
    .ifEmpty { "ta" } // Backward compatibility only
```

---

## Decision 2: Require Migration Script ✅

**Question**: Do we need a migration script for existing stories?

**Decision**: Yes, comprehensive migration script required

**Rationale**:
- ✅ **Data quality**: Many old stories have NULL or invalid language values
- ✅ **Safety**: Validate data before new logic goes live
- ✅ **Transparency**: Audit trail shows what was changed
- ✅ **Confidence**: Know exactly which stories were affected

**Migration Strategy**:
1. **Phase 1**: Identify problem stories (NULL, empty, invalid)
2. **Phase 2**: Auto-fix with heuristics (NULL → 'ta', detect from translations)
3. **Phase 3**: Manual review for edge cases
4. **Phase 4**: Validate all stories have valid language

**Script Location**: `backend/src/main/resources/db/migration/V{next}__fix_story_language_values.sql`

---

## Decision 3: Split into Two Separate Actions ✅

**Question**: Keep "Regenerate & Sync" as one action or split into two?

**Decision**: Split into two separate actions

**New Actions**:
1. **"Regenerate Story"** - Regenerates content in source language only (LLM improvement)
2. **"Translate to All Languages"** - Translates existing source to all other languages

**Rationale**:
- ✅ **Clarity**: Each action has single, clear purpose
- ✅ **Control**: Admins can regenerate without triggering translations
- ✅ **Safety**: Prevents accidental overwrites of translations
- ✅ **Workflow**: Matches actual content workflow (create → improve → translate)
- ✅ **Cost**: Avoid unnecessary LLM calls when only translation needed

**UI Changes**:
```typescript
// Before
<Button onClick={regenerateAndSync}>Regenerate & Sync All Languages</Button>

// After
<Button onClick={regenerateStory}>Regenerate Story (Source Only)</Button>
<Button onClick={translateAllLanguages}>Translate to All Languages</Button>
```

**API Endpoints**:
- `POST /api/admin/stories/{id}/regenerate` - Source language only
- `POST /api/admin/stories/{id}/translate-all` - All other languages

---

## Decision 4: Multi-Layered Language Validation ✅

**Question**: How to handle stories where `language` field is wrong?

**Decision**: Multi-layered approach with auto-fix, detection, and manual override

**Validation Layers**:

### Layer 1: Migration Script (One-time)
- Auto-fix NULL/empty → 'ta'
- Detect from translations if available
- Log all changes to audit table

### Layer 2: API Validation (Runtime)
```kotlin
fun validateStoryLanguage(language: String): String {
    val normalized = language.trim().lowercase().take(10)
    
    if (normalized.isEmpty()) {
        log.warn("Empty language, defaulting to 'ta'")
        return "ta"
    }
    
    if (normalized !in supportedLanguages) {
        throw IllegalArgumentException("Unsupported language: $normalized")
    }
    
    return normalized
}
```

### Layer 3: Detection Heuristic (Fallback)
```kotlin
fun detectLanguageFromContent(content: String, translations: List<Translation>): String {
    // 1. Use most common translation language
    if (translations.isNotEmpty()) {
        return translations.groupBy { it.language }
            .maxByOrNull { it.value.size }?.key ?: "ta"
    }
    
    // 2. Detect from script (Tamil, Devanagari, etc.)
    return when {
        content.matches(Regex(".*[\\u0B80-\\u0BFF].*")) -> "ta" // Tamil
        content.matches(Regex(".*[\\u0900-\\u097F].*")) -> "hi" // Hindi
        else -> "ta" // Safe default
    }
}
```

### Layer 4: Admin Tool (Manual)
- UI to view and correct language for specific stories
- Bulk correction tool for categories
- Audit log of manual changes

**Rationale**:
- ✅ **Defensive**: Multiple layers catch different error types
- ✅ **Automatic**: Most cases fixed without manual intervention
- ✅ **Flexible**: Manual override for edge cases
- ✅ **Auditable**: All changes logged

---

## Decision 5: Format-Agnostic Translation Logic ✅

**Question**: How to handle different story formats (linear, interactive, simulator)?

**Decision**: Single translation pipeline works for all formats

**Format Handling**:

### Linear Stories (No Interactive Graph)
```kotlin
// interactive_graph = NULL
// Standard translation: title, content, moral
// No special handling needed
```

### Interactive Stories (With Graph)
```kotlin
// interactive_graph = JSON with segments
// 1. Copy graph structure to translations
// 2. Translate segment text within graph
// 3. Preserve segment IDs, choices, structure
```

### Simulator Stories (Graph-based)
```kotlin
// Same as interactive stories
// Graph structure preserved
// Segment text translated
```

**Implementation**:
```kotlin
fun translateStory(story: LibraryStory, targetLang: String): StoryTranslation {
    // Translate core content (all formats)
    val translated = translationService.translate(
        sourceLang = story.language,
        targetLang = targetLang,
        content = story.content,
        title = story.title,
        moral = story.moral
    )
    
    // Copy interactive graph if present (interactive/simulator only)
    val graphJson = story.interactiveGraphJson?.let { graph ->
        copyGraphStructure(graph, targetLang) // Preserves structure, translates text
    }
    
    return StoryTranslation(
        content = translated.content,
        title = translated.title,
        moral = translated.moral,
        interactiveGraphJson = graphJson, // NULL for linear stories
        // ...
    )
}
```

**Rationale**:
- ✅ **Simplicity**: One pipeline for all formats
- ✅ **Maintainability**: No format-specific code paths
- ✅ **Extensibility**: New formats work automatically
- ✅ **Correctness**: Graph structure preserved, content translated

---

## Summary of Decisions

| Decision | Choice | Impact |
|----------|--------|--------|
| Language field | Use existing `language` | No schema changes, faster implementation |
| Migration script | Required | Data quality, safety, audit trail |
| UI actions | Split into two | Clarity, control, safety |
| Language validation | Multi-layered | Defensive, automatic, auditable |
| Format handling | Format-agnostic | Simple, maintainable, extensible |

---

## Implementation Priority

1. **Critical Path** (Week 1):
   - Migration script
   - Core pipeline logic (`resolvePipelineSourceText`)
   - Language validation

2. **High Priority** (Week 2):
   - Split UI actions
   - API endpoints
   - Interactive graph handling

3. **Medium Priority** (Week 3):
   - Admin language correction tool
   - Audit logging
   - Documentation

4. **Testing** (Week 4):
   - Story #136 validation
   - Backward compatibility tests
   - All format types (linear, interactive, simulator)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking Tamil stories | Low | High | Thorough testing, feature flag |
| Invalid language data | Medium | Medium | Migration script, validation |
| Admin confusion | Low | Low | Clear UI, documentation |
| Performance issues | Low | Medium | Parallel processing, monitoring |

---

## Success Criteria

✅ Story #136 translates correctly from English  
✅ Existing Tamil stories work unchanged  
✅ All story formats supported (linear, interactive, simulator)  
✅ Clear admin UI with two separate actions  
✅ Migration script fixes all invalid language values  
✅ No performance degradation  

---

**Decision Date**: 2026-04-17  
**Decision Maker**: Senior Technical Lead (AI)  
**Status**: Approved for Implementation
