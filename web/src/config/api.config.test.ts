import { describe, expect, it } from "vitest";
import { isLoopbackHostname, shouldUseSameOriginApi } from "./api.config";

describe("api.config loopback routing", () => {
  it("treats localhost and 127.0.0.1 as loopback", () => {
    expect(isLoopbackHostname("localhost")).toBe(true);
    expect(isLoopbackHostname("127.0.0.1")).toBe(true);
    expect(isLoopbackHostname("::1")).toBe(true);
    expect(isLoopbackHostname("dev.web.tamixa.in")).toBe(false);
  });

  it("uses same-origin API when both page and API are local", () => {
    expect(shouldUseSameOriginApi("localhost", "http://localhost:8080")).toBe(true);
    expect(shouldUseSameOriginApi("127.0.0.1", "http://127.0.0.1:8080")).toBe(true);
    expect(shouldUseSameOriginApi("localhost", "https://dev.tamixa.in")).toBe(false);
    expect(shouldUseSameOriginApi("dev.web.tamixa.in", "http://localhost:8080")).toBe(false);
  });
});
