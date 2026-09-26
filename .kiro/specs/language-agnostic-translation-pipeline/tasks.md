# Tasks: Language-Agnostic Translation Pipeline

## Phase 1: Requirements & Design ✅
- [x] 1. Review and approve requirements document
- [x] 2. Make architectural decisions (see DECISIONS.md)
- [x] 3. Create technical design document
- [ ] 4. Review design with team

## Phase 2: Data Migration (Critical - Do First)
- [ ] 5. Create migration script to fix NULL/invalid language values
  - [ ] 5.1 Identify stories with NULL or empty language
  - [ ] 5.2 Identify stories with unsupported language values
  - [ ] 5.3 Implement auto-fix logic (NULL → 'ta', detect from translations)
  - [ ] 5.4 Create audit log table for language changes
  - [ ] 5.5 Test migration script on staging data
- [ ] 6. Run migration script on production (with backup)
- [ ] 7. Validate all stories have valid language values

## Phase 3: Core Pipeline Implementation
- [ ] 8. Update `resolvePipelineSourceText` to use story.language
  - [ ] 8.1 Remove hardcoded sourceLanguage assumption
  - [ ] 8.2 Add validation for supported languages
  - [ ] 8.3 Add default to 'ta' for empty values (backward compatibility)
  - [ ] 8.4 Update logging to show actual source language
- [ ] 9. Update `processLanguage` to pass correct source language
- [ ] 10. Update `runTranslationStep` to handle any source language
  - [ ] 10.1 Ensure interactive graph copies from master
  - [ ] 10.2 Test with linear stories (no graph)
  - [ ] 10.3 Test with interactive stories (with graph)
  - [ ] 10.4 Test with simulator stories (graph-based)

## Phase 4: API Layer Updates
- [ ] 11. Create new endpoint: `POST /api/admin/stories/{id}/regenerate`
  - [ ] 11.1 Regenerates source language content only
  - [ ] 11.2 Uses LLM to improve/rewrite content
  - [ ] 11.3 Does NOT trigger translations
- [ ] 12. Create new endpoint: `POST /api/admin/stories/{id}/translate-all`
  - [ ] 12.1 Translates from source to all other languages
  - [ ] 12.2 Copies interactive graph structure
  - [ ] 12.3 Preserves source content unchanged
- [ ] 13. Update existing `trigger-pipeline` endpoint for backward compatibility
- [ ] 14. Add language validation to story create/update endpoints

## Phase 5: Admin UI Updates
- [ ] 15. Split "Regenerate & Sync" into two buttons
  - [ ] 15.1 "Regenerate Story" button (source only)
  - [ ] 15.2 "Translate to All Languages" button (all others)
- [ ] 16. Display source language prominently in story edit form
- [ ] 17. Add tooltips explaining what each action does
- [ ] 18. Add confirmation dialogs showing affected languages
- [ ] 19. Disable "Translate" button if source content is empty
- [ ] 20. Update help text and documentation

## Phase 6: Admin Tools
- [ ] 21. Create language correction tool in Admin UI
  - [ ] 21.1 View current language for any story
  - [ ] 21.2 Manually correct language if wrong
  - [ ] 21.3 Bulk correction for categories
  - [ ] 21.4 Show audit log of changes
- [ ] 22. Add language validation warnings in story list
- [ ] 23. Add language filter in story search

## Phase 7: Testing & Validation
- [ ] 24. Unit tests for language detection logic
- [ ] 25. Test Story #136 (English origin, interactive)
  - [ ] 25.1 Regenerate story (English only)
  - [ ] 25.2 Translate to all languages
  - [ ] 25.3 Verify interactive graph integrity
- [ ] 26. Test existing Tamil-origin stories (backward compatibility)
  - [ ] 26.1 Linear stories
  - [ ] 26.2 Interactive stories
- [ ] 27. Test Hindi/Telugu/Kannada origin stories
- [ ] 28. Test all story formats:
  - [ ] 28.1 Linear stories (no graph)
  - [ ] 28.2 Interactive stories (with graph)
  - [ ] 28.3 Simulator stories (graph-based)
- [ ] 29. Performance testing with parallel translations
- [ ] 30. Load testing with multiple concurrent regenerations

## Phase 8: Deployment
- [ ] 31. Add feature flag for gradual rollout
- [ ] 32. Deploy migration script to staging
- [ ] 33. Deploy code changes to staging
- [ ] 34. Run smoke tests on staging
- [ ] 35. Deploy to production (migration first, then code)
- [ ] 36. Monitor translation quality and errors
- [ ] 37. Monitor performance metrics
- [ ] 38. Collect admin feedback

## Phase 9: Documentation & Training
- [ ] 39. Update admin user guide
- [ ] 40. Create video tutorial for new workflow
- [ ] 41. Update API documentation
- [ ] 42. Train content team on new actions
- [ ] 43. Create troubleshooting guide
