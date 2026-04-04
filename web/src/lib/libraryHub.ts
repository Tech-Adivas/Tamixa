/**
 * Web library lane deep links — align with mobile {@code Screen.Library} hub query values.
 * Examples: /stories?tab=library&hub=simulator
 */
export type LibraryHub = "browse" | "fun" | "learn" | "simulator";

export const LIBRARY_HUB_FUN = "fun";
export const LIBRARY_HUB_LEARN = "learn";
export const LIBRARY_HUB_LEARN_SAFETY = "learn_safety";
export const LIBRARY_HUB_SIMULATOR = "simulator";

export function normalizeLibraryHub(raw: string | null | undefined): LibraryHub {
  const v = raw?.trim().toLowerCase() ?? "";
  if (v === LIBRARY_HUB_FUN) return "fun";
  if (v === LIBRARY_HUB_LEARN || v === LIBRARY_HUB_LEARN_SAFETY) return "learn";
  if (v === LIBRARY_HUB_SIMULATOR) return "simulator";
  return "browse";
}
