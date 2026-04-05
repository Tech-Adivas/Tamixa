import { describe, expect, it } from "vitest";
import {
  lifeSkillCountersToLifeReadinessSnapshot,
  mergeLocalAndApiLifeReadiness,
  hasAnyLifeSkillCounterSignal,
} from "./lifeReadinessModel";

describe("lifeSkillCountersToLifeReadinessSnapshot", () => {
  it("maps pillars and derives ethics like Kotlin", () => {
    const s = lifeSkillCountersToLifeReadinessSnapshot({
      wisdom: 30,
      social: 60,
      money: 12,
      balance: 45,
    });
    expect(s.tech).toBe(30);
    expect(s.leadership).toBe(60);
    expect(s.business).toBe(12);
    expect(s.communication).toBe(45);
    expect(s.ethics).toBe(Math.round((30 + 60 + 45) / 3));
  });
});

describe("hasAnyLifeSkillCounterSignal", () => {
  it("is false for null or all zeros", () => {
    expect(hasAnyLifeSkillCounterSignal(null)).toBe(false);
    expect(
      hasAnyLifeSkillCounterSignal({ wisdom: 0, social: 0, money: 0, balance: 0 }),
    ).toBe(false);
  });
  it("is true when any pillar positive", () => {
    expect(
      hasAnyLifeSkillCounterSignal({ wisdom: 0, social: 0, money: 1, balance: 0 }),
    ).toBe(true);
  });
});

describe("mergeLocalAndApiLifeReadiness", () => {
  const local = {
    tech: 20,
    business: 10,
    leadership: 10,
    ethics: 80,
    communication: 10,
  };
  const api = lifeSkillCountersToLifeReadinessSnapshot({
    wisdom: 10,
    social: 10,
    money: 10,
    balance: 10,
  });

  it("uses API only when local has no practice", () => {
    const m = mergeLocalAndApiLifeReadiness(local, api, false, true);
    expect(m).toEqual(api);
  });

  it("uses local only when API has no practice", () => {
    const m = mergeLocalAndApiLifeReadiness(local, api, true, false);
    expect(m).toEqual(local);
  });

  it("averages each axis when both have practice", () => {
    const m = mergeLocalAndApiLifeReadiness(local, api, true, true);
    expect(m.tech).toBe(Math.round((local.tech + api.tech) / 2));
    expect(m.business).toBe(Math.round((local.business + api.business) / 2));
  });
});
