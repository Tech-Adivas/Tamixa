import { describe, expect, it } from "vitest";
import { clampMeter, getSimulatorPlaybook } from "./categoryLogicFactory";

describe("getSimulatorPlaybook", () => {
  it("uses graph eduCategory when present", () => {
    expect(getSimulatorPlaybook("", null, "Ethics")).toBe("ethics");
    expect(getSimulatorPlaybook("", null, "Tech")).toBe("digitalSafety");
    expect(getSimulatorPlaybook("", null, "Business")).toBe("business");
  });

  it("infers digital safety from theme", () => {
    expect(getSimulatorPlaybook("Learn · Simulator · Digital Safety", null, null)).toBe("digitalSafety");
    expect(getSimulatorPlaybook("Scam awareness", null, null)).toBe("digitalSafety");
  });

  it("defaults when no match", () => {
    expect(getSimulatorPlaybook("Random tale", null, null)).toBe("default");
  });
});

describe("clampMeter", () => {
  it("clamps", () => {
    expect(clampMeter(-5)).toBe(0);
    expect(clampMeter(150)).toBe(100);
    expect(clampMeter(44)).toBe(44);
  });
});
