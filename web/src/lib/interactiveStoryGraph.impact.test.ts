import { describe, expect, it } from "vitest";
import { impactFromInteractiveChoice, resolveInteractiveChoiceNavigation } from "./interactiveStoryGraph";

describe("impactFromInteractiveChoice", () => {
  it("prefers impactStats over skillDeltas per key", () => {
    const im = impactFromInteractiveChoice({
      id: "a",
      label: "x",
      nextSegmentId: "b",
      skillDeltas: { digitalWisdom: 1, fiscalMuscle: 2 },
      impactStats: { digitalWisdom: 9 },
    });
    expect(im.digitalWisdom).toBe(9);
    expect(im.fiscalMuscle).toBe(2);
  });

  it("reads life keys from skillDeltas when impactStats omits them", () => {
    const im = impactFromInteractiveChoice({
      id: "a",
      label: "x",
      nextSegmentId: "b",
      skillDeltas: { digitalWisdom: 3, otherSkill: 99 },
    });
    expect(im.digitalWisdom).toBe(3);
    expect(im).not.toHaveProperty("otherSkill");
  });
});

describe("resolveInteractiveChoiceNavigation", () => {
  it("uses consequenceSegmentId for shortcuts when set", () => {
    expect(
      resolveInteractiveChoiceNavigation({
        id: "x",
        label: "y",
        nextSegmentId: "linear",
        isShortcut: true,
        consequenceSegmentId: "consequence",
      })
    ).toBe("consequence");
  });

  it("uses nextSegmentId when not a shortcut consequence", () => {
    expect(
      resolveInteractiveChoiceNavigation({
        id: "x",
        label: "y",
        nextSegmentId: "linear",
      })
    ).toBe("linear");
  });
});
