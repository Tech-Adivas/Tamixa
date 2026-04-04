import { outlineInteractiveGraphJson } from "./interactive-graph-outline";
import { DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE } from "./edu-simulator-template";

describe("outlineInteractiveGraphJson", () => {
  it("orders segments from start and lists choices", () => {
    const outline = outlineInteractiveGraphJson(DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE);
    expect(outline).not.toBeNull();
    expect(outline!.map((s) => s.id)).toEqual([
      "intro",
      "high_stress_path",
      "analytical_path",
      "collaborative_path",
    ]);
    expect(outline![0].choices).toHaveLength(3);
    expect(outline![1].isEnd).toBe(true);
  });

  it("returns null for invalid graph", () => {
    expect(outlineInteractiveGraphJson("{")).toBeNull();
  });
});
