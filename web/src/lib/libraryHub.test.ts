import { describe, expect, it } from "vitest";
import {
  LIBRARY_HUB_FUN,
  LIBRARY_HUB_LEARN,
  LIBRARY_HUB_LEARN_SAFETY,
  LIBRARY_HUB_SIMULATOR,
  normalizeLibraryHub,
} from "./libraryHub";

describe("normalizeLibraryHub", () => {
  it("defaults to browse", () => {
    expect(normalizeLibraryHub(null)).toBe("browse");
    expect(normalizeLibraryHub("")).toBe("browse");
    expect(normalizeLibraryHub("unknown")).toBe("browse");
  });

  it("maps known hubs", () => {
    expect(normalizeLibraryHub(LIBRARY_HUB_FUN)).toBe("fun");
    expect(normalizeLibraryHub(LIBRARY_HUB_LEARN)).toBe("learn");
    expect(normalizeLibraryHub(LIBRARY_HUB_LEARN_SAFETY)).toBe("learn");
    expect(normalizeLibraryHub(LIBRARY_HUB_SIMULATOR)).toBe("simulator");
    expect(normalizeLibraryHub("SIMULATOR")).toBe("simulator");
  });
});
