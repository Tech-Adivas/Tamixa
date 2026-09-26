# Interactive Stories Content Review

## Overview
This document lists all interactive stories that need content verification and improvement.

## Interactive Story Categories

### 1. Life Skills Simulators ([LifeSim])
**Migration File**: `V94__life_skills_interactive_stories_seed.sql` and `V95__life_skills_interactive_stories_seed_batch2.sql`

Stories:
1. **The Phantom OTP** - Digital safety, OTP scams
2. **The Relative's Request** - Financial boundaries, family loans
3. **The Interview Accent** - Professional communication, bias
4. **The Deepfake Call** - Digital fraud awareness
5. **The Discount Trap** - Consumer awareness, impulse buying
6. **The Exam Pressure** - Academic stress, mental health
7. **The WhatsApp Forward** - Misinformation, critical thinking
8. **The Society Meeting** - Civic participation, community
9. **The First Credit Card** - Financial literacy, credit management
10. **The Customer Is Wrong** - Service industry, conflict resolution
11. **The AI Career Fear** - Technology adaptation, career planning
12. **The Wedding Budget** - Financial planning, family expectations
13. **The Neighborhood Bully** - Conflict resolution, safety
14. **The Health Insurance Fine Print** - Insurance literacy
15. **The Lost Receipt** - Consumer rights, documentation
16. **The Grandparent's Wallet — UPI** - Digital payments, elder safety

### 2. Survival/Adulting Stories ([Survival])
**Migration File**: `V97__survival_adulting_stories_seed.sql`

Stories:
1. **The Hospital Nightmare — Room Rent** - Healthcare costs, insurance
2. **The Secret Loan — Spouse & Relative** - Financial transparency, relationships
3. **The Pink Slip — Voluntary Resignation** - Employment rights, documentation
4. **The Vanishing Minimum — Fees & Auto-Sweep** - Banking, hidden fees
5. **The Surgery Denied — Proposal Truth** - Insurance claims, disclosure
6. **The Tax Notice — Nil GST Return** - Tax compliance, business
7. **Term vs Money-Back — Twenty Years** - Insurance products, long-term planning
8. **The SIP Leak Hunt** - Investment management, fees
9. **Survival 101 — Three Drills** - Emergency preparedness

### 3. Civic Survival ([Civic])
**Migration File**: `V98__civic_survival_pilot_story_seed.sql`

Stories:
1. **The Traffic Stop — Keys & Papers** - Legal rights, police interaction

## Content Review Checklist

For each interactive story, verify:

### Story Structure
- [ ] **Opening Hook** - Engaging, relatable scenario
- [ ] **Context Setup** - Clear situation description
- [ ] **Decision Points** - 2-4 meaningful choices per branch
- [ ] **Consequences** - Realistic outcomes for each choice
- [ ] **Learning Moments** - Educational insights embedded naturally
- [ ] **Resolution** - Satisfying conclusion with takeaway

### Interactive Graph Quality
- [ ] **Branch Logic** - All paths lead to valid endpoints
- [ ] **Choice Labels** - Clear, concise, age-appropriate
- [ ] **Audio Segments** - Proper CDN paths referenced
- [ ] **Timing** - Appropriate pacing between choices
- [ ] **Replay Value** - Multiple paths worth exploring

### Educational Content
- [ ] **Accuracy** - Factually correct information
- [ ] **Relevance** - Applicable to target age group
- [ ] **Cultural Sensitivity** - Appropriate for Indian context
- [ ] **Safety Focus** - Emphasizes child/teen safety
- [ ] **Actionable Advice** - Practical takeaways

### Parent-Facing Content
- [ ] **Discussion Prompts** - Thoughtful conversation starters
- [ ] **Content Note** - Clear summary of themes
- [ ] **Post-Story Mission** - Engaging follow-up activity
- [ ] **Resource Links** - Helpful external resources (if applicable)

### Technical Quality
- [ ] **Audio Files** - All segments uploaded to CDN
- [ ] **JSON Validation** - interactive_graph is valid JSON
- [ ] **Path References** - All audio paths are correct
- [ ] **Translations** - Available in target languages
- [ ] **Narration Approval** - Audio reviewed and approved

## Review Process

### Step 1: Database Query
Run this query to get all interactive stories:

\`\`\`sql
SELECT 
    id,
    title,
    category,
    status,
    LENGTH(interactive_graph) as graph_size,
    post_story_mission IS NOT NULL as has_mission,
    parent_discussion_prompts IS NOT NULL as has_prompts,
    narration_approved_at IS NOT NULL as narration_approved
FROM library_stories
WHERE interactive_graph IS NOT NULL
ORDER BY category, title;
\`\`\`

### Step 2: Content Review
For each story:
1. Read the full content
2. Review the interactive_graph JSON structure
3. Test all decision branches
4. Verify educational accuracy
5. Check parent-facing content
6. Test audio playback (if available)

### Step 3: Improvements
Document needed improvements:
- Content edits (clarity, accuracy, engagement)
- Branch logic fixes
- Additional choices or paths
- Enhanced learning moments
- Better parent resources

### Step 4: Implementation
- Update story content in database
- Regenerate narration if text changed
- Update interactive_graph JSON
- Add/improve parent-facing content
- Test in mobile app

## Priority Stories for Review

Based on educational impact and usage:

### High Priority (Review First)
1. **The Phantom OTP** - Critical digital safety
2. **The Deepfake Call** - Emerging threat awareness
3. **The First Credit Card** - Financial foundation
4. **The Hospital Nightmare** - Healthcare literacy
5. **The Traffic Stop** - Legal rights awareness

### Medium Priority
- All other LifeSim stories
- Survival/Adulting stories
- Additional Civic stories

### Low Priority
- Stories with less critical topics
- Duplicate themes (consolidate if needed)

## Next Steps

1. ✅ **Unpublish DSG Pilot stories** - Run `scripts/sql/unpublish_dsg_pilot_stories.sql`
2. ⏳ **Review interactive stories** - Use this checklist
3. ⏳ **Document improvements** - Create improvement tickets
4. ⏳ **Implement changes** - Update content and test
5. ⏳ **Publish approved stories** - Change status to PUBLISHED

## Notes

- All interactive stories are currently in DRAFT status
- Audio segments need to be uploaded to CDN before publishing
- Each story should be tested in the mobile app before publishing
- Parent feedback should be collected and incorporated
- Consider A/B testing different branch structures
