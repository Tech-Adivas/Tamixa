import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { ApiClientError, generateStory } from "./api";

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

  it("POST JSON body can send generationTopicId without theme", async () => {
    await generateStory({
      age: 14,
      language: "ta",
      generationTopicId: "pongal_gratitude",
      childName: "Listener",
    });
    const call = (fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    const body = JSON.parse((call[1] as RequestInit).body as string);
    expect(body.generationTopicId).toBe("pongal_gratitude");
    expect(body).not.toHaveProperty("theme");
    expect(body.age).toBe(14);
  });

  it("throws ApiClientError with stable code when backend returns 400 JSON", async () => {
    vi.unstubAllGlobals();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: () =>
          Promise.resolve({
            message: "Unknown generationTopicId",
            code: "UNKNOWN_GENERATION_TOPIC",
          }),
      } as Response),
    );
    const rejected = await generateStory({
      age: 8,
      language: "ta",
      generationTopicId: "not_real",
    }).catch((e) => e);
    expect(rejected).toBeInstanceOf(ApiClientError);
    const err = rejected as ApiClientError;
    expect(err.httpStatus).toBe(400);
    expect(err.code).toBe("UNKNOWN_GENERATION_TOPIC");
    expect(err.message).toBe("Unknown generationTopicId");
  });
});
