import { validateInteractiveStoryCategory } from "./story-interactive-conventions";

const validGraph = JSON.stringify({
  startSegmentId: "a",
  segments: { a: { audioUrl: "https://example.com/a.mp3", choices: [] } },
});

describe("validateInteractiveStoryCategory", () => {
  it("returns null when interactive graph is empty", () => {
    expect(validateInteractiveStoryCategory("Fantasy", null)).toBeNull();
    expect(validateInteractiveStoryCategory("Fantasy", "   ")).toBeNull();
  });

  it("returns null when graph JSON is invalid (lint handled separately)", () => {
    expect(validateInteractiveStoryCategory("Fantasy", "{")).toBeNull();
  });

  it("requires Learn · Simulator when graph is valid", () => {
    const msg = validateInteractiveStoryCategory("Learn · Digital Safety", validGraph);
    expect(msg).toBeTruthy();
    expect(msg).toMatch(/Learn · Simulator/i);
  });

  it("allows Learn · Simulator · Digital Safety", () => {
    expect(
      validateInteractiveStoryCategory("Learn · Simulator · Digital Safety", validGraph)
    ).toBeNull();
  });
});
