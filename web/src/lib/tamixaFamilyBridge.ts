import type { InteractiveSegment } from "./interactiveStoryGraph";

/** Co-listening viewpoint: same scene, different narrative emphasis. */
export type FamilyPerspectiveRole = "Parent" | "Child";

const PERSPECTIVE_STORAGE_KEY = "tamixa_family_bridge_perspective_v1";

/**
 * Persist who is holding the device / whose lens to use for optional two-sided segment copy.
 * Call when the family switches listeners.
 */
export function setPerspective(role: FamilyPerspectiveRole): void {
  try {
    localStorage.setItem(PERSPECTIVE_STORAGE_KEY, role);
  } catch {
    /* private mode / quota */
  }
}

export function getPerspective(): FamilyPerspectiveRole {
  try {
    const v = localStorage.getItem(PERSPECTIVE_STORAGE_KEY);
    if (v === "Parent" || v === "Child") return v;
  } catch {
    /* */
  }
  return "Child";
}

type SegmentBodies = Pick<InteractiveSegment, "contentBody" | "contentBodyParent" | "contentBodyChild">;

/**
 * Resolve on-screen narrative for the current interactive segment.
 * Parent/Child bodies override the neutral `contentBody` when provided.
 */
export function segmentContentBodyForRole(
  segment: SegmentBodies | null | undefined,
  role: FamilyPerspectiveRole
): string {
  if (!segment) return "";
  if (role === "Parent") {
    const p = segment.contentBodyParent?.trim();
    if (p) return p;
  } else {
    const c = segment.contentBodyChild?.trim();
    if (c) return c;
  }
  return segment.contentBody?.trim() ?? "";
}
