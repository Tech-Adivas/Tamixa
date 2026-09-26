import {
  isLibraryStoryPipelineMetaKey,
  LIBRARY_STORY_PIPELINE_META_KEYS,
  adminStoryLanguageLabel,
  adminStoryEmotionModeLabel,
  getLibraryStoryMasterScriptContentError,
  hasScriptForLibraryStoryLanguage,
  isLibraryStoryInReviewQueue,
  canSubmitLibraryStoryForReview,
  buildLibraryStoryAdminProgressSteps,
} from "./library-story-workflow";

describe("library-story-workflow", () => {
  describe("isLibraryStoryPipelineMetaKey", () => {
    it("returns true for known meta keys", () => {
      for (const k of LIBRARY_STORY_PIPELINE_META_KEYS) {
        expect(isLibraryStoryPipelineMetaKey(k)).toBe(true);
      }
    });
    it("returns false for language-like keys", () => {
      expect(isLibraryStoryPipelineMetaKey("ta")).toBe(false);
      expect(isLibraryStoryPipelineMetaKey("en")).toBe(false);
    });
  });

  describe("adminStoryLanguageLabel", () => {
    it("normalizes case", () => {
      expect(adminStoryLanguageLabel("TA")).toBe("Tamil");
    });
    it("passes through unknown codes", () => {
      expect(adminStoryLanguageLabel("xx")).toBe("xx");
    });
  });

  describe("adminStoryEmotionModeLabel", () => {
    it("maps known modes", () => {
      expect(adminStoryEmotionModeLabel("CALM")).toBe("Calm");
      expect(adminStoryEmotionModeLabel("calm")).toBe("Calm");
    });
    it("returns raw value for unknown", () => {
      expect(adminStoryEmotionModeLabel("CUSTOM")).toBe("CUSTOM");
    });
  });

  describe("hasScriptForLibraryStoryLanguage / getLibraryStoryMasterScriptContentError", () => {
    const tamilStory = "ஒரு கதை இங்கே உள்ளது.";
    const englishStory = "Once upon a time there was a crow.";

    it("accepts English without script check", () => {
      expect(hasScriptForLibraryStoryLanguage(englishStory, "en")).toBe(true);
      expect(getLibraryStoryMasterScriptContentError(englishStory, "en")).toBeNull();
    });
    it("requires Tamil script for ta", () => {
      expect(hasScriptForLibraryStoryLanguage(tamilStory, "ta")).toBe(true);
      expect(hasScriptForLibraryStoryLanguage(englishStory, "ta")).toBe(false);
      expect(getLibraryStoryMasterScriptContentError(englishStory, "ta")).toContain("Tamil");
    });
  });

  describe("buildLibraryStoryAdminProgressSteps", () => {
    it("marks submit when submitReady and prior gates", () => {
      const steps = buildLibraryStoryAdminProgressSteps({
        minWordCount: 50,
        wordCount: 60,
        titleTrimmed: true,
        themeSet: true,
        contentTrimmed: true,
        simulatorSelected: false,
        simulatorGraphFieldsOk: true,
        onServer: true,
        regenerateBusy: false,
        hasCover: true,
        submitReady: true,
      });
      expect(steps.every((s) => s.done)).toBe(true);
    });
    it("blocks form when simulator graph invalid", () => {
      const steps = buildLibraryStoryAdminProgressSteps({
        minWordCount: 50,
        wordCount: 60,
        titleTrimmed: true,
        themeSet: true,
        contentTrimmed: true,
        simulatorSelected: true,
        simulatorGraphFieldsOk: false,
        onServer: true,
        regenerateBusy: false,
        hasCover: false,
        submitReady: false,
      });
      expect(steps[0]?.done).toBe(false);
    });
    it("allows form ready for simulator when word count below min but content non-empty and graph ok", () => {
      const steps = buildLibraryStoryAdminProgressSteps({
        minWordCount: 50,
        wordCount: 12,
        titleTrimmed: true,
        themeSet: true,
        contentTrimmed: true,
        simulatorSelected: true,
        simulatorGraphFieldsOk: true,
        onServer: true,
        regenerateBusy: false,
        hasCover: false,
        submitReady: false,
      });
      expect(steps[0]?.done).toBe(true);
    });
  });

  describe("review queue helpers", () => {
    it("isLibraryStoryInReviewQueue matches PUBLISHED / PROCESSING / READY", () => {
      expect(isLibraryStoryInReviewQueue("PUBLISHED")).toBe(true);
      expect(isLibraryStoryInReviewQueue("PROCESSING")).toBe(true);
      expect(isLibraryStoryInReviewQueue("READY")).toBe(true);
      expect(isLibraryStoryInReviewQueue("DRAFT")).toBe(false);
    });
    it("canSubmitLibraryStoryForReview blocks queued statuses", () => {
      expect(canSubmitLibraryStoryForReview("DRAFT", true)).toBe(true);
      expect(canSubmitLibraryStoryForReview("PUBLISHED", true)).toBe(false);
      expect(canSubmitLibraryStoryForReview("DRAFT", false)).toBe(false);
    });
  });
});
