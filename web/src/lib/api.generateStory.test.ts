import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { generateStory } from "./api";

function mockStoryJson() {
  return {
    id: 1,
    parentId: 1,
    childId: null as number | null,
    content: "Once…",
    theme: "space",
    language: "ta",
    age: 6,
    childName: "Listener",
    wordCount: 2,
    readingTimeMinutes: 0.1,
    title: "Star",
    moral: "Be kind.",
    status: "READY",
    audioFileUrl: null as string | null,
    createdAt: "2020-01-01T00:00:00Z",
  };
}

describe("generateStory", () => {
  beforeEach(() => {
    localStorage.setItem("tamixa_access_token", "test-access-token");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockStoryJson()),
      } as Response),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it("POST JSON body includes learningFocus when provided", async () => {
    await generateStory({
      age: 6,
      theme: "market day",
      language: "ta",
      learningFocus: "money_literacy",
    });
    expect(fetch).toHaveBeenCalled();
    const call = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    const init = call[1] as RequestInit;
    const body = JSON.parse(init.body as string);
    expect(body.learningFocus).toBe("money_literacy");
    expect(body.theme).toBe("market day");
    expect(body.language).toBe("ta");
  });

  it("POST JSON body omits learningFocus when not provided", async () => {
    await generateStory({
      age: 7,
      theme: "forest",
      language: "ta",
    });
    const call = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    const body = JSON.parse((call[1] as RequestInit).body as string);
    expect(body).not.toHaveProperty("learningFocus");
  });
});
