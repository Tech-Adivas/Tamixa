import { beforeEach, describe, expect, it } from "vitest";
import { getPerspective, segmentContentBodyForRole, setPerspective } from "./tamixaFamilyBridge";

describe("tamixaFamilyBridge", () => {
  beforeEach(() => {
    setPerspective("Child");
  });

  it("segmentContentBodyForRole prefers role-specific copy", () => {
    expect(
      segmentContentBodyForRole(
        {
          contentBody: "Neutral",
          contentBodyParent: "You worry about money.",
          contentBodyChild: "You just want ice cream.",
        },
        "Parent"
      )
    ).toBe("You worry about money.");
    expect(
      segmentContentBodyForRole(
        {
          contentBody: "Neutral",
          contentBodyParent: "Parent view",
          contentBodyChild: "Kid view",
        },
        "Child"
      )
    ).toBe("Kid view");
  });

  it("segmentContentBodyForRole falls back to contentBody", () => {
    expect(segmentContentBodyForRole({ contentBody: "Only neutral" }, "Parent")).toBe("Only neutral");
    expect(segmentContentBodyForRole({}, "Child")).toBe("");
  });

  it("setPerspective and getPerspective round-trip", () => {
    setPerspective("Parent");
    expect(getPerspective()).toBe("Parent");
    setPerspective("Child");
    expect(getPerspective()).toBe("Child");
  });
});
