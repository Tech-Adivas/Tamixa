# Requirements: Language-Agnostic Translation Pipeline

## Problem Statement

The current translation pipeline assumes Tamil (`ta`) is always the source language for all stories. This creates critical issues:

1. **Story #136 Case**: English-origin story with civic survival interactive graph gets wrong content when "Regenerate & Sync All Languages" is used
2. **Scalability**: Cannot support content teams creating stories in different languages
3. **Quality**: Translations from wrong source language produce incorrect content
4. **Interactive Stories**: Graph-content mismatches when source language is not Tamil

## Business Requirements

### BR-1: Respect Story Source Language
**Priority**: Critical  
**User Story**: As a content admin, when I create a story in English (or any language), the "Regenerate & Sync All Languages" should translate FROM that language, not assume Tamil.

**Acceptance Criteria**:
- System detects the actual source language of the story
- Translation pipeline uses the correct source language
- No hardcoded assumptions about Tamil being the source

### BR-2: Preserve Story Integrity
**Priority**: Critical  
**User Story**: As a content admin, when I regenerate translations, the story content, interactive graph, and metadata should remain consistent across all languages.

**Acceptance Criteria**:
- Interactive graph matches story content in all languages
- Title, moral, and content are properly translated from source
- Metadata (category, theme, age) preserved across languages

### BR-3: Support Multi-Language Content Teams
**Priority**: High  
**User Story**: As a content team lead, I want to create stories in Tamil, English, Hindi, or any supported language, and have the system handle translations correctly.

**Acceptance Criteria**:
- Stories can be created in any supported language
- System tracks which language is the "master" or "source"
- Translation pipeline adapts to the source language

### BR-4: Clear Admin UI Actions
**Priority**: High  
**User Story**: As a content admin, I want clear separation between "regenerating story content" and "translating to other languages" so I understand what each action does.

**Acceptance Criteria**:
- Two separate actions in Admin UI:
  - **"Regenerate Story"** - Regenerates content in the source language only (uses LLM to improve/rewrite)
  - **"Translate to All Languages"** - Translates existing source content to all other supported languages
- Each action has clear tooltip explaining what it does
- Confirmation dialogs show which languages will be affected
- "Translate to All Languages" is disabled if source content is empty

### BR-5: Backward Compatibility
**Priority**: Critical  
**User Story**: As a system admin, existing Tamil-origin stories should continue to work without any changes or data migration.

**Acceptance Criteria**:
- Existing stories with `language='ta'` work as before
- No breaking changes to existing translation records
- Gradual migration path if schema changes needed

## Functional Requirements

### FR-1: Source Language Detection
The system must automatically detect the source language of a story:
- Use `library_stories.language` field as the source language
- Default to `ta` only if language field is null/empty (backward compatibility)
- Validate language is in supported languages list

### FR-2: Translation Pipeline Source Resolution
The translation pipeline must:
- Resolve source text from the correct language
- Use master story content if translating FROM the master language
- Use translation record if translating FROM a non-master language
- Never assume Tamil is the source

### FR-3: Interactive Graph Propagation
When syncing languages:
- Copy `interactive_graph` from master story to all translations
- Preserve graph structure and segment IDs
- Only translate segment text content, not structure
- **Linear stories**: No special handling needed (interactive_graph is null)
- **Interactive stories**: Graph structure copied, segment text translated
- **Other formats** (e.g., simulator stories): Same logic applies - structure preserved, content translated

### FR-4: Translation Direction Logic
For each target language:
- If target == source: Use master story content (no translation needed)
- If target != source: Translate from source language to target
- Support source → target for any language pair

### FR-5: Admin UI Clarity
Update admin UI to show:
- Current source language of the story (prominently displayed)
- Two separate action buttons:
  - **"Regenerate Story"** - Only affects source language
  - **"Translate to All Languages"** - Translates to all other languages
- Which languages will be affected by each action
- Disable "Translate" if source content is empty or invalid

### FR-6: Data Migration and Validation
The system must include:
- **Migration script** to fix stories with missing/invalid language values
- **Validation rules**:
  - If `language` is NULL or empty → Set to 'ta' (historical default)
  - If `language` not in supported list → Detect from content or set to 'ta'
  - If `language` conflicts with existing translations → Use most common translation language
- **Admin tool** to manually correct language for specific stories
- **Audit log** of all language corrections made

## Non-Functional Requirements

### NFR-1: Performance
- Translation pipeline should not be slower than current implementation
- Parallel processing for multiple target languages
- Efficient source text resolution (no redundant queries)

### NFR-2: Maintainability
- Clear separation of concerns (source resolution, translation, sync)
- Well-documented language detection logic
- Easy to add new supported languages

### NFR-3: Data Integrity
- Atomic operations for translation updates
- Rollback capability if translation fails
- Audit trail for language changes

### NFR-4: Scalability
- Support for 10+ languages without performance degradation
- Configurable language list via environment variables
- No hardcoded language assumptions in core logic

## User Scenarios

### Scenario 1: English-Origin Interactive Story (Story #136)
**Given**: Story #136 created in English with civic survival interactive graph  
**When**: Admin clicks "Translate to All Languages"  
**Then**: 
- System detects English as source language
- Translates English → Tamil, Hindi, Telugu, Kannada, Malayalam
- Copies interactive graph structure to all translations
- Translates segment text within graph (preserves structure)
- All languages have consistent civic survival content

### Scenario 2: Tamil-Origin Traditional Linear Story
**Given**: Story created in Tamil (existing behavior), no interactive graph  
**When**: Admin clicks "Translate to All Languages"  
**Then**:
- System detects Tamil as source language
- Translates Tamil → English, Hindi, Telugu, Kannada, Malayalam
- No interactive graph to copy (linear story)
- Behavior identical to current system (backward compatible)

### Scenario 3: Hindi-Origin Story (Future)
**Given**: Story created in Hindi by Hindi content team  
**When**: Admin clicks "Translate to All Languages"  
**Then**:
- System detects Hindi as source language
- Translates Hindi → Tamil, English, Telugu, Kannada, Malayalam
- All languages have correct content from Hindi source

### Scenario 4: Regenerate Story Content (Source Language Only)
**Given**: Story in English needs content improvement  
**When**: Admin clicks "Regenerate Story"  
**Then**:
- System regenerates ONLY English content using LLM
- Other language translations remain unchanged
- Admin must click "Translate to All Languages" separately to update translations

### Scenario 5: Story with Missing Language Field
**Given**: Old story with `language=NULL`  
**When**: Migration script runs  
**Then**:
- System sets `language='ta'` (historical default)
- Logs the change in audit trail
- Story continues to work with Tamil as source

## Success Metrics

1. **Correctness**: 100% of stories translate from their actual source language
2. **Interactive Graph Integrity**: 0 mismatches between graph and content
3. **Backward Compatibility**: 0 regressions in existing Tamil-origin stories
4. **Admin Satisfaction**: Clear understanding of what "Regenerate & Sync" does

## Out of Scope

- Automatic language detection from content (use explicit `language` field)
- Multi-source stories (one story, multiple source languages)
- Real-time translation (batch processing only)
- Custom translation rules per story

## Dependencies

- Translation API (OpenAI/Gemini) supports all language pairs
- Database schema has `library_stories.language` field
- Admin UI can display source language information

## Migration Script Requirements

### Pre-Deployment Data Validation
The migration script must:

1. **Identify Problem Stories**:
   ```sql
   -- Stories with NULL or empty language
   SELECT COUNT(*) FROM library_stories WHERE language IS NULL OR language = '';
   
   -- Stories with unsupported language
   SELECT COUNT(*) FROM library_stories 
   WHERE language NOT IN ('ta', 'en', 'hi', 'te', 'kn', 'ml');
   ```

2. **Auto-Fix Rules**:
   - `language IS NULL OR language = ''` → Set to 'ta' (historical default)
   - `language NOT IN supported_list` → Detect from translations or set to 'ta'
   - Log all changes to audit table

3. **Detection Heuristic** (for invalid languages):
   ```
   IF story has translations:
     language = most_common_translation_language
   ELSE IF content starts with Tamil script:
     language = 'ta'
   ELSE IF content starts with Devanagari script:
     language = 'hi'
   ELSE:
     language = 'ta' (safe default)
   ```

4. **Validation Report**:
   - Total stories processed
   - Stories fixed automatically
   - Stories requiring manual review
   - Audit log of all changes

5. **Rollback Capability**:
   - Backup original language values before migration
   - Provide rollback script if issues arise

### Post-Deployment Validation
- Verify all stories have valid language values
- Check translation pipeline works for all languages
- Monitor error rates by source language

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing Tamil stories | High | Thorough testing, feature flag, gradual rollout |
| Translation API doesn't support all pairs | Medium | Fallback to English as intermediate language |
| Performance degradation | Medium | Parallel processing, caching, monitoring |
| Admin confusion about new behavior | Low | Clear UI messaging, documentation, training |

## Open Questions - RESOLVED

### ✅ Q1: Should we add a new `source_language` field or use existing `language` field?
**Decision**: Use existing `language` field
**Rationale**: 
- Simpler implementation, no schema changes
- `language` field already exists and represents the story's primary language
- Backward compatible with default to 'ta' for null values
- Less confusion for admins (one language field, not two)

### ✅ Q2: Do we need a migration script for existing stories?
**Decision**: Yes, migration script required
**Rationale**:
- Many old stories have NULL or invalid language values
- Need to set default 'ta' for historical stories
- Validation ensures data integrity before new logic goes live
- Audit trail for transparency

### ✅ Q3: Should "Regenerate & Sync" be split into two separate actions?
**Decision**: Yes, split into two actions
**Rationale**:
- **"Regenerate Story"** - Clear intent: improve source content only
- **"Translate to All Languages"** - Clear intent: sync translations from source
- Prevents accidental regeneration when only translation is needed
- Gives admins more control over workflow
- Reduces confusion about what each action does

### ✅ Q4: How do we handle stories where `language` field is wrong?
**Decision**: Multi-layered approach
**Rationale**:
1. **Migration script**: Auto-fix NULL/empty → 'ta'
2. **Validation**: Reject unsupported languages at API level
3. **Detection heuristic**: If language conflicts with translations, use most common translation language
4. **Admin tool**: Manual correction UI for edge cases
5. **Audit log**: Track all corrections for accountability

### ✅ Q5: How to handle different story formats (linear, interactive, simulator)?
**Decision**: Format-agnostic translation logic
**Rationale**:
- **Linear stories**: No interactive graph, standard translation
- **Interactive stories**: Copy graph structure, translate segment text
- **Simulator stories**: Same as interactive (graph-based)
- **All formats**: Use same source language detection and translation pipeline
- No special-casing needed - logic works for all formats

## Next Steps

1. Review and approve requirements
2. Create technical design document
3. Implement source language detection
4. Update translation pipeline logic
5. Update admin UI
6. Test with Story #136 and existing stories
7. Deploy with feature flag
