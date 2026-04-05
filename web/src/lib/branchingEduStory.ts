/**
 * Branching edu story graph + Life Bar (UserLifeProfile) schema.
 *
 * Distinct from {@link ./interactiveStoryGraph.ts} (CMS graph uses label/nextSegmentId/skillDeltas).
 */

// --- Metadata ---------------------------------------------------------------

export type BranchingEduStoryCategory = "Fun" | "Edu";

/** Learning axis tags (maps to curriculum / analytics dimensions). */
export type BranchingEduStorySubCategory =
  | "Tech"
  | "Business"
  | "Leadership"
  | "Ethics"
  | "Communication";

export interface BranchingEduStoryMetadata {
  category: BranchingEduStoryCategory;
  subCategory: BranchingEduStorySubCategory;
  /** Stable id for sync, analytics, or CMS reference. */
  storyId?: string;
  title?: string;
  version?: string;
}

// --- Life Bar / soft counters (0–100) ---------------------------------------

export const LIFE_STAT_KEYS = [
  "digitalWisdom",
  "fiscalMuscle",
  "socialCapital",
  "integrity",
  "emotionalBalance",
] as const;

export type LifeStatKey = (typeof LIFE_STAT_KEYS)[number];

/** Global player state: "soft counters" clamped to [0, 100]. */
export interface UserLifeProfile {
  /** Tech literacy & safe, intentional technology use */
  digitalWisdom: number;
  /** Business acumen & money sense */
  fiscalMuscle: number;
  /** Leadership & communication / relationship skills */
  socialCapital: number;
  /** Ethics, honesty, research & source literacy */
  integrity: number;
  /** Mental health, regulation, resilience */
  emotionalBalance: number;
}

/** Per-choice deltas applied to {@link UserLifeProfile} (missing keys = 0 delta). */
export type ImpactStats = Partial<Record<LifeStatKey, number>>;

export const DEFAULT_USER_LIFE_PROFILE: UserLifeProfile = {
  digitalWisdom: 0,
  fiscalMuscle: 0,
  socialCapital: 0,
  integrity: 0,
  emotionalBalance: 0,
};

export const LIFE_STAT_MIN = 0;
export const LIFE_STAT_MAX = 100;

export function clampLifeStat(value: number): number {
  if (!Number.isFinite(value)) return LIFE_STAT_MIN;
  return Math.min(LIFE_STAT_MAX, Math.max(LIFE_STAT_MIN, value));
}

export function normalizeUserLifeProfile(p: UserLifeProfile): UserLifeProfile {
  return {
    digitalWisdom: clampLifeStat(p.digitalWisdom),
    fiscalMuscle: clampLifeStat(p.fiscalMuscle),
    socialCapital: clampLifeStat(p.socialCapital),
    integrity: clampLifeStat(p.integrity),
    emotionalBalance: clampLifeStat(p.emotionalBalance),
  };
}

/** Add choice impact then clamp each stat to [0, 100]. */
export function applyImpactStats(profile: UserLifeProfile, impact: ImpactStats): UserLifeProfile {
  const next: UserLifeProfile = { ...profile };
  for (const key of LIFE_STAT_KEYS) {
    const d = impact[key];
    if (typeof d === "number" && Number.isFinite(d)) {
      next[key] = clampLifeStat(next[key] + d);
    }
  }
  return normalizeUserLifeProfile(next);
}

// --- Branching graph --------------------------------------------------------

export interface Choice {
  text: string;
  targetSegmentId: string;
  impactStats: ImpactStats;
}

export interface ChoiceNode {
  choices: Choice[];
}

export interface BranchingEduSegment {
  audioUrl: string;
  text: string;
  /** When absent, segment is a leaf (end of branch) or linear handoff is defined elsewhere. */
  choiceNode?: ChoiceNode | null;
}

export interface BranchingEduStory {
  metadata: BranchingEduStoryMetadata;
  startSegmentId: string;
  segments: Record<string, BranchingEduSegment>;
}

// --- Lenient JSON parsing ---------------------------------------------------

function isRecord(v: unknown): v is Record<string, unknown> {
  return v !== null && typeof v === "object" && !Array.isArray(v);
}

function parseCategory(v: unknown): BranchingEduStoryCategory | null {
  return v === "Fun" || v === "Edu" ? v : null;
}

function parseSubCategory(v: unknown): BranchingEduStorySubCategory | null {
  return v === "Tech" ||
    v === "Business" ||
    v === "Leadership" ||
    v === "Ethics" ||
    v === "Communication"
    ? v
    : null;
}

function parseImpactStats(raw: unknown): ImpactStats {
  if (!isRecord(raw)) return {};
  const out: ImpactStats = {};
  for (const key of LIFE_STAT_KEYS) {
    const n = raw[key];
    if (typeof n === "number" && Number.isFinite(n)) out[key] = n;
  }
  return out;
}

function parseChoiceNode(raw: unknown): ChoiceNode | null {
  if (!isRecord(raw)) return null;
  const arr = raw.choices;
  if (!Array.isArray(arr)) return null;
  const choices: Choice[] = [];
  for (const c of arr) {
    if (!isRecord(c)) continue;
    const text = c.text;
    const targetSegmentId = c.targetSegmentId;
    if (typeof text !== "string" || !text.trim()) continue;
    if (typeof targetSegmentId !== "string" || !targetSegmentId.trim()) continue;
    choices.push({
      text: text.trim(),
      targetSegmentId: targetSegmentId.trim(),
      impactStats: parseImpactStats(c.impactStats),
    });
  }
  return choices.length > 0 ? { choices } : null;
}

/** Parse and validate a {@link BranchingEduStory} from JSON (e.g. API or bundled asset). */
export function parseBranchingEduStory(raw: unknown): BranchingEduStory | null {
  if (!isRecord(raw)) return null;
  const metaRaw = raw.metadata;
  if (!isRecord(metaRaw)) return null;
  const category = parseCategory(metaRaw.category);
  const subCategory = parseSubCategory(metaRaw.subCategory);
  if (category === null || subCategory === null) return null;

  const metadata: BranchingEduStoryMetadata = {
    category,
    subCategory,
  };
  if (typeof metaRaw.storyId === "string" && metaRaw.storyId.trim()) metadata.storyId = metaRaw.storyId.trim();
  if (typeof metaRaw.title === "string" && metaRaw.title.trim()) metadata.title = metaRaw.title.trim();
  if (typeof metaRaw.version === "string" && metaRaw.version.trim()) metadata.version = metaRaw.version.trim();

  const start = raw.startSegmentId;
  if (typeof start !== "string" || !start.trim()) return null;
  const segmentsRaw = raw.segments;
  if (!isRecord(segmentsRaw)) return null;

  const segments: Record<string, BranchingEduSegment> = {};
  for (const [sid, segVal] of Object.entries(segmentsRaw)) {
    if (!sid.trim() || !isRecord(segVal)) continue;
    const audioUrl = segVal.audioUrl;
    const text = segVal.text;
    if (typeof audioUrl !== "string" || !audioUrl.trim()) continue;
    if (typeof text !== "string" || !text.trim()) continue;
    let choiceNode: ChoiceNode | null | undefined;
    if (segVal.choiceNode !== undefined && segVal.choiceNode !== null) {
      const cn = parseChoiceNode(segVal.choiceNode);
      if (cn) choiceNode = cn;
    }
    const seg: BranchingEduSegment = { audioUrl: audioUrl.trim(), text: text.trim() };
    if (choiceNode) seg.choiceNode = choiceNode;
    segments[sid.trim()] = seg;
  }

  const startId = start.trim();
  if (!segments[startId]) return null;

  return {
    metadata,
    startSegmentId: startId,
    segments,
  };
}

// --- Offline persistence (Web localStorage / RN AsyncStorage) ---------------

export const USER_LIFE_PROFILE_STORAGE_KEY = "tamixa_user_life_profile_v1";

/** Async key-value API compatible with React Native AsyncStorage. */
export interface AsyncKeyValueStore {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
}

export interface UserLifeProfilePersisted {
  schemaVersion: 1;
  updatedAtMs: number;
  profile: UserLifeProfile;
}

export function serializeUserLifeProfileState(profile: UserLifeProfile, nowMs: number = Date.now()): string {
  const payload: UserLifeProfilePersisted = {
    schemaVersion: 1,
    updatedAtMs: nowMs,
    profile: normalizeUserLifeProfile(profile),
  };
  return JSON.stringify(payload);
}

export function parseUserLifeProfileState(json: string): UserLifeProfilePersisted | null {
  try {
    const v = JSON.parse(json) as unknown;
    if (!isRecord(v)) return null;
    if (v.schemaVersion !== 1) return null;
    const updatedAtMs = v.updatedAtMs;
    if (typeof updatedAtMs !== "number" || !Number.isFinite(updatedAtMs)) return null;
    const p = v.profile;
    if (!isRecord(p)) return null;
    const profile: UserLifeProfile = { ...DEFAULT_USER_LIFE_PROFILE };
    for (const key of LIFE_STAT_KEYS) {
      const n = p[key];
      if (typeof n === "number" && Number.isFinite(n)) profile[key] = clampLifeStat(n);
    }
    return {
      schemaVersion: 1,
      updatedAtMs,
      profile: normalizeUserLifeProfile(profile),
    };
  } catch {
    return null;
  }
}

/** Merge strategy for offline-first: keep the document with the latest updatedAtMs. */
export function mergeUserLifeProfileStates(
  local: UserLifeProfilePersisted | null,
  remote: UserLifeProfilePersisted | null
): UserLifeProfilePersisted | null {
  if (!local) return remote;
  if (!remote) return local;
  return local.updatedAtMs >= remote.updatedAtMs ? local : remote;
}

export async function loadUserLifeProfileFromStore(
  store: AsyncKeyValueStore,
  key: string = USER_LIFE_PROFILE_STORAGE_KEY
): Promise<UserLifeProfilePersisted | null> {
  const raw = await store.getItem(key);
  if (raw === null || raw === undefined || !String(raw).trim()) return null;
  return parseUserLifeProfileState(String(raw));
}

export async function saveUserLifeProfileToStore(
  store: AsyncKeyValueStore,
  profile: UserLifeProfile,
  key: string = USER_LIFE_PROFILE_STORAGE_KEY,
  nowMs: number = Date.now()
): Promise<void> {
  await store.setItem(key, serializeUserLifeProfileState(profile, nowMs));
}

export async function clearUserLifeProfileFromStore(
  store: AsyncKeyValueStore,
  key: string = USER_LIFE_PROFILE_STORAGE_KEY
): Promise<void> {
  await store.removeItem(key);
}

/**
 * Browser {@link localStorage} adapter. No-ops get/set when `window` is unavailable (SSR).
 */
export function createWebLocalStorageStore(storage: Storage | null = typeof window !== "undefined" ? window.localStorage : null): AsyncKeyValueStore {
  return {
    async getItem(k: string): Promise<string | null> {
      if (!storage) return null;
      try {
        return storage.getItem(k);
      } catch {
        return null;
      }
    },
    async setItem(k: string, value: string): Promise<void> {
      if (!storage) return;
      try {
        storage.setItem(k, value);
      } catch {
        /* quota / private mode */
      }
    },
    async removeItem(k: string): Promise<void> {
      if (!storage) return;
      try {
        storage.removeItem(k);
      } catch {
        /* ignore */
      }
    },
  };
}

/**
 * Wrap `@react-native-async-storage/async-storage` (or any matching shape) for the same API as web.
 *
 * @example
 * ```ts
 * import AsyncStorage from "@react-native-async-storage/async-storage";
 * const store = createAsyncStorageBridge(AsyncStorage);
 * await saveUserLifeProfileToStore(store, profile);
 * ```
 */
export function createAsyncStorageBridge(asyncStorage: {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
  removeItem(key: string): Promise<void>;
}): AsyncKeyValueStore {
  return {
    getItem: (key) => asyncStorage.getItem(key),
    setItem: (key, value) => asyncStorage.setItem(key, value),
    removeItem: (key) => asyncStorage.removeItem(key),
  };
}
