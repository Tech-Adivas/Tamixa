# Language-Agnostic Translation Pipeline - Spec Summary

## 📋 Overview

This spec addresses a critical architectural flaw where the translation pipeline assumes Tamil is always the source language, breaking stories created in other languages (like Story #136 in English).

## 🎯 Goals

1. **Respect source language**: Translate FROM the actual story language, not assume Tamil
2. **Support all formats**: Linear, interactive, and simulator stories
3. **Clear admin UX**: Separate "Regenerate Story" from "Translate Languages"
4. **Backward compatible**: Existing Tamil stories work unchanged
5. **Data quality**: Migration script fixes invalid language values

## 📁 Spec Documents

| Document | Purpose | Status |
|----------|---------|--------|
| `requirements.md` | Business and functional requirements | ✅ Approved |
| `DECISIONS.md` | Architectural decisions and rationale | ✅ Complete |
| `ANALYSIS.md` | Technical analysis and root cause | ✅ Complete |
| `tasks.md` | Implementation task breakdown | ✅ Ready |
| `design.md` | Technical design (detailed) | ⏳ Next |

## 🔑 Key Decisions

### 1. Use Existing `language` Field
- No schema changes needed
- Default to 'ta' for NULL values (backward compatible)
- Simpler and faster implementation

### 2. Migration Script Required
- Fix NULL/invalid language values
- Auto-detect from translations
- Audit trail for all changes

### 3. Split UI Actions
- **"Regenerate Story"** - Source language only (LLM improvement)
- **"Translate to All Languages"** - All other languages
- Clear separation of concerns

### 4. Multi-Layered Validation
- Migration script (one-time fix)
- API validation (runtime checks)
- Detection heuristic (fallback)
- Admin tool (manual override)

### 5. Format-Agnostic Logic
- Single pipeline for linear, interactive, and simulator stories
- Graph structure preserved, content translated
- No format-specific code paths

## 🚀 Implementation Phases

### Phase 1: Data Migration (Week 1)
- Create and run migration script
- Fix NULL/invalid language values
- Validate all stories

### Phase 2: Core Pipeline (Week 2)
- Update `resolvePipelineSourceText`
- Remove Tamil hardcoding
- Add language validation

### Phase 3: API & UI (Week 3)
- Split into two endpoints/actions
- Update admin UI
- Add confirmation dialogs

### Phase 4: Testing & Deployment (Week 4)
- Test all story formats
- Backward compatibility tests
- Production deployment

## 📊 Success Metrics

- ✅ Story #136 translates correctly from English
- ✅ Existing Tamil stories work unchanged
- ✅ All formats supported (linear, interactive, simulator)
- ✅ Clear admin UI with two actions
- ✅ 100% stories have valid language values
- ✅ No performance degradation

## 🎯 User Scenarios

### Scenario 1: English Interactive Story (Story #136)
```
Admin creates story in English with interactive graph
→ Clicks "Translate to All Languages"
→ System detects English as source
→ Translates EN → TA, HI, TE, KN, ML
→ Copies graph structure, translates segment text
→ All languages have consistent content
```

### Scenario 2: Tamil Linear Story (Existing)
```
Admin has Tamil story (existing behavior)
→ Clicks "Translate to All Languages"
→ System detects Tamil as source
→ Translates TA → EN, HI, TE, KN, ML
→ Works exactly as before (backward compatible)
```

### Scenario 3: Regenerate Source Only
```
Admin wants to improve English story content
→ Clicks "Regenerate Story"
→ System regenerates ONLY English content (LLM)
→ Other translations unchanged
→ Admin clicks "Translate to All Languages" separately
```

## ⚠️ Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking Tamil stories | Thorough testing, feature flag, gradual rollout |
| Invalid language data | Migration script, validation, admin tool |
| Admin confusion | Clear UI, tooltips, documentation, training |
| Performance issues | Parallel processing, monitoring, caching |

## 📝 Next Steps

1. ✅ Review and approve requirements
2. ⏳ Create technical design document
3. ⏳ Implement migration script
4. ⏳ Update core pipeline logic
5. ⏳ Split UI actions
6. ⏳ Test all scenarios
7. ⏳ Deploy with feature flag

## 🔗 Related Issues

- Story #136: English-origin story with wrong translations
- Interactive graph content mismatches
- Multi-language content team support
- Translation pipeline scalability

## 👥 Stakeholders

- **Content Admins**: Need clear UI and correct translations
- **Content Teams**: Want to create stories in any language
- **Engineering**: Need maintainable, scalable solution
- **Users**: Expect high-quality translations in all languages

---

**Spec Status**: ✅ Requirements Approved, Ready for Design  
**Priority**: Critical  
**Estimated Effort**: 4 weeks  
**Risk Level**: Medium (backward compatibility concerns)
