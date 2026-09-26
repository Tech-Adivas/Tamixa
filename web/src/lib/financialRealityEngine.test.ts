import { describe, expect, it } from "vitest";
import {
  applyFinancialChoice,
  applyMonthlyBurn,
  computeMonthlyEmi,
  DEFAULT_FAMILY_ECONOMY,
  graphUsesFinancialReality,
  isLiquidityCrisis,
  simulateTimePassage,
} from "./financialRealityEngine";

describe("computeMonthlyEmi", () => {
  it("matches standard amortization shape", () => {
    const emi = computeMonthlyEmi(100_000, 0.12, 12);
    expect(emi).toBeGreaterThan(8800);
    expect(emi).toBeLessThan(8900);
  });
});

describe("applyFinancialChoice + crisis", () => {
  it("routes toward crisis when cash goes negative", () => {
    let e = { ...DEFAULT_FAMILY_ECONOMY, liquidCash: 5000 };
    e = applyFinancialChoice(
      e,
      {
        addLoan: { principal: 500_000, aprAnnual: 0.24, tenureMonths: 60, label: "Wedding loan" },
      },
      "loan"
    );
    expect(isLiquidityCrisis(e)).toBe(true);
  });
});

describe("simulateTimePassage", () => {
  it("advances months and returns summary", () => {
    const e = {
      ...DEFAULT_FAMILY_ECONOMY,
      outstandingBadDebtPrincipal: 50_000,
    };
    const r = simulateTimePassage(e, 3);
    expect(r.monthsAdvanced).toBe(3);
    expect(r.summaryLines.length).toBeGreaterThan(2);
    expect(r.economy.simulatedMonth).toBeGreaterThanOrEqual(e.simulatedMonth + 3);
  });
});

describe("graphUsesFinancialReality", () => {
  it("detects flag or choice impact", () => {
    expect(
      graphUsesFinancialReality({
        financialRealityEnabled: true,
        segments: {},
      })
    ).toBe(true);
    expect(
      graphUsesFinancialReality({
        segments: {
          a: { choices: [{ financialImpact: { addMonthlyEmi: 1000 } }] },
        },
      })
    ).toBe(true);
    expect(
      graphUsesFinancialReality({
        segments: { a: { choices: [{}] } },
      })
    ).toBe(false);
  });
});

describe("applyMonthlyBurn", () => {
  it("reduces liquid cash", () => {
    const next = applyMonthlyBurn(DEFAULT_FAMILY_ECONOMY);
    expect(next.liquidCash).toBeLessThan(DEFAULT_FAMILY_ECONOMY.liquidCash);
    expect(next.simulatedMonth).toBe(DEFAULT_FAMILY_ECONOMY.simulatedMonth + 1);
  });
});
