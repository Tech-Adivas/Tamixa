import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { getMe, NON_PARENT_ACCOUNT_MESSAGE } from "./api";

function stubMe(body: unknown) {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ "content-type": "application/json" }),
      json: async () => body,
      text: async () => JSON.stringify(body),
      clone() {
        return this;
      },
    })
  );
}

describe("getMe role guard", () => {
  beforeEach(() => {
    localStorage.setItem("tamixa_access_token", "test-access-token");
  });
  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it("returns the user for PARENT accounts", async () => {
    stubMe({ email: "parent@example.com", role: "PARENT" });
    await expect(getMe()).resolves.toMatchObject({ role: "PARENT" });
    expect(localStorage.getItem("tamixa_access_token")).toBe("test-access-token");
  });

  it("rejects admin accounts and clears stored tokens", async () => {
    stubMe({ email: "admin@techadivas.com", role: "SUPER_ADMIN" });
    await expect(getMe()).rejects.toThrow(NON_PARENT_ACCOUNT_MESSAGE);
    expect(localStorage.getItem("tamixa_access_token")).toBeNull();
  });
});
