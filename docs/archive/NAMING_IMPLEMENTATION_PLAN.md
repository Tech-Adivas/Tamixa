# Tamixa Naming Implementation Plan

## Current Status
✅ Documentation created (TAMIXA_CASUAL_NAMES.md)  
❌ Mobile app NOT updated yet  
❌ Database NOT updated yet

## Implementation Steps

### Phase 1: Bottom Navigation (HIGH PRIORITY - Do Now)
**Files to Update**: `mobile/composeApp/src/commonMain/kotlin/com/tamixa/ui/strings/Strings.kt`

| Current | New Casual Name | Status |
|---------|-----------------|--------|
| home() = "Home" / "முகப்பு" / "होम" | ✅ Already good! Keep as is | ✅ Done |
| library() = "Library" / "நூலகம்" / "लाइब्रेरी" | Change to "Stories" / "கதைகள்" / "कहानियाँ" | ⏳ TODO |
| profile() = "Profile" / "சுயவிவரம்" / "प்रोफ़ाइल" | Change to "Me" / "நான்" / "मैं" | ⏳ TODO |

### Phase 2: Dashboard Sections (MEDIUM PRIORITY)
**Files to Update**: `Strings.kt`

| Current Function | New Name | Status |
|------------------|----------|--------|
| dashboardSpotlightTitle() | Keep or simplify | ⏳ TODO |
| dashboardYourStoriesTitle() | "Your Stories" is good | ✅ Done |
| libraryBrowseTab() | Change to "All Stories" | ⏳ TODO |
| libraryFunCornerTab() | Keep "Fun Corner" | ✅ Done |
| libraryLearnSafetyTab() | Change to "Stay Safe" | ⏳ TODO |
| librarySimulatorTab() | Change to "Practice" | ⏳ TODO |

### Phase 3: Story Categories (LOW PRIORITY - Database)
**Files to Update**: Database migration SQL

Categories need database updates - will do separately.

### Phase 4: Story Titles (LOW PRIORITY - Database)
**Files to Update**: Database migration SQL

Story titles need database updates - will do separately.

## Quick Win: Update Bottom Navigation Now

Let me implement Phase 1 right now - it's the most visible change!
