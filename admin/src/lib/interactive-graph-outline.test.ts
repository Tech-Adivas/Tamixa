import { DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE } from "./edu-simulator-template";
import { lintInteractiveGraphJson } from "./interactive-graph-lint";
import { outlineInteractiveGraphJson } from "./interactive-graph-outline";
import { validateInteractiveSegmentUrls } from "./interactive-segment-audio-url";

describe("lintInteractiveGraphJson", () => {
  it("allows segments with empty audioUrl (pending Segment Audio Studio)", () => {
    const raw = JSON.stringify({
      startSegmentId: "hook",
      segments: {
        hook: { text: "Hello", audioUrl: "", choices: [] },
      },
    });
    expect(lintInteractiveGraphJson(raw).ok).toBe(true);
  });

  it("rejects non-string audioUrl", () => {
    const raw = JSON.stringify({
      startSegmentId: "hook",
      segments: {
        hook: { text: "Hello", audioUrl: 1, choices: [] },
      },
    });
    const r = lintInteractiveGraphJson(raw);
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.errors.some((e) => e.includes("audioUrl"))).toBe(true);
    }
  });
});

describe("validateInteractiveSegmentUrls", () => {
  it("allows http audioUrl on localhost and private LAN (dev)", () => {
    const raw = JSON.stringify({
      startSegmentId: "ta_ep01_hook",
      segments: {
        ta_ep01_hook: {
          audioUrl: "http://192.168.0.9:8080/audio/stories/1/ta/file.mp3",
          choices: [],
        },
      },
    });
    expect(validateInteractiveSegmentUrls(raw)).toEqual([]);
  });

  it("rejects http on public hosts", () => {
    const raw = JSON.stringify({
      startSegmentId: "a",
      segments: {
        a: { audioUrl: "http://evil.com/x.mp3", choices: [] },
      },
    });
    const errs = validateInteractiveSegmentUrls(raw);
    expect(errs.length).toBeGreaterThan(0);
    expect(errs[0]).toMatch(/HTTPS/i);
  });
});

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
