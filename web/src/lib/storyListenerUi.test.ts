import { describe, expect, it } from "vitest";
import {
  interactivePracticeBadgeLabel,
  isFunStory,
  isInteractivePracticeLibraryStory,
  isLearnOrDigitalSafetyStory,
  isSimulatorStory,
} from "./storyListenerUi";

describe("storyListenerUi", () => {
  it("isFunStory detects fun categories", () => {
    expect(isFunStory("Adventure", "Fun stories")).toBe(true);
    expect(isFunStory("funny stories", null)).toBe(true);
    expect(isFunStory("Learn · Life Skills", null)).toBe(false);
  });

  it("isLearnOrDigitalSafetyStory", () => {
    expect(isLearnOrDigitalSafetyStory("Learn · Digital Safety", null)).toBe(true);
    expect(isLearnOrDigitalSafetyStory("Bedtime", "Digital Safety")).toBe(true);
    expect(isLearnOrDigitalSafetyStory("Fantasy", "Fun stories")).toBe(false);
  });

  it("isSimulatorStory", () => {
    expect(isSimulatorStory("Learn · Simulator · Digital Safety", null)).toBe(true);
    expect(isSimulatorStory("Learn · Life Skills", null)).toBe(false);
  });

  it("interactivePracticeBadgeLabel is non-empty", () => {
    expect(interactivePracticeBadgeLabel().length).toBeGreaterThan(0);
  });

  it("isInteractivePracticeLibraryStory uses graph or simulator prefix", () => {
    expect(
      isInteractivePracticeLibraryStory({
        theme: "Learn · Simulator · X",
        category: null,
        interactiveGraph: undefined,
      })
    ).toBe(true);
    expect(
      isInteractivePracticeLibraryStory({
        theme: "Other",
        category: null,
        interactiveGraph: { startSegmentId: "a" },
      })
    ).toBe(true);
    expect(
      isInteractivePracticeLibraryStory({
        theme: "Other",
        category: null,
        interactiveGraph: {},
      })
    ).toBe(false);
  });
});
