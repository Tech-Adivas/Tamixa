import { describe, expect, it } from "vitest";
import {
  applyImpactStats,
  clampLifeStat,
  createWebLocalStorageStore,
  DEFAULT_USER_LIFE_PROFILE,
  loadUserLifeProfileFromStore,
  mergeUserLifeProfileStates,
  parseBranchingEduStory,
  parseUserLifeProfileState,
  saveUserLifeProfileToStore,
  serializeUserLifeProfileState,
} from "./branchingEduStory";

describe("branchingEduStory", () => {
  it("clamps life stats", () => {
    expect(clampLifeStat(-5)).toBe(0);
    expect(clampLifeStat(150)).toBe(100);
    expect(clampLifeStat(NaN)).toBe(0);
    expect(clampLifeStat(42)).toBe(42);
  });

  it("applies impact stats and clamps", () => {
    const base = { ...DEFAULT_USER_LIFE_PROFILE, digitalWisdom: 90 };
    const next = applyImpactStats(base, { digitalWisdom: 20, integrity: -3 });
    expect(next.digitalWisdom).toBe(100);
    expect(next.integrity).toBe(0);
  });

  it("parses BranchingEduStory", () => {
    const story = parseBranchingEduStory({
      metadata: { category: "Edu", subCategory: "Tech" },
      startSegmentId: "a",
      segments: {
        a: {
          audioUrl: "https://x/a.mp3",
          text: "Hello",
          choiceNode: {
            choices: [
              {
                text: "Go",
                targetSegmentId: "b",
                impactStats: { digitalWisdom: 5 },
              },
            ],
          },
        },
        b: { audioUrl: "https://x/b.mp3", text: "End" },
      },
    });
    expect(story).not.toBeNull();
    expect(story!.startSegmentId).toBe("a");
    expect(story!.segments.a.choiceNode?.choices[0].targetSegmentId).toBe("b");
  });

  it("round-trips persisted life profile via in-memory store", async () => {
    const mem = new Map<string, string>();
    const store = {
      async getItem(k: string) {
        return mem.has(k) ? mem.get(k)! : null;
      },
      async setItem(k: string, v: string) {
        mem.set(k, v);
      },
      async removeItem(k: string) {
        mem.delete(k);
      },
    };
    await saveUserLifeProfileToStore(store, applyImpactStats(DEFAULT_USER_LIFE_PROFILE, { fiscalMuscle: 10 }));
    const loaded = await loadUserLifeProfileFromStore(store);
    expect(loaded?.profile.fiscalMuscle).toBe(10);
  });

  it("mergeUserLifeProfileStates picks newer updatedAtMs", () => {
    const older = parseUserLifeProfileState(
      serializeUserLifeProfileState(DEFAULT_USER_LIFE_PROFILE, 1000)
    )!;
    const newer = parseUserLifeProfileState(
      serializeUserLifeProfileState(applyImpactStats(DEFAULT_USER_LIFE_PROFILE, { integrity: 7 }), 2000)
    )!;
    expect(mergeUserLifeProfileStates(older, newer)?.profile.integrity).toBe(7);
    expect(mergeUserLifeProfileStates(newer, older)?.profile.integrity).toBe(7);
  });

  it("createWebLocalStorageStore tolerates null storage", async () => {
    const store = createWebLocalStorageStore(null);
    expect(await store.getItem("k")).toBeNull();
    await store.setItem("k", "v");
    expect(await store.getItem("k")).toBeNull();
  });
});
