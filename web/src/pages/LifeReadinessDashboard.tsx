import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import LifeReadinessRadarChart from "../components/LifeReadinessRadarChart";
import { getProfile, getLifeSkillCounters, type ProfileChildDto } from "../lib/api";
import {
  createWebLocalStorageStore,
  DEFAULT_USER_LIFE_PROFILE,
  loadUserLifeProfileFromStore,
  USER_LIFE_PROFILE_STORAGE_KEY,
  type UserLifeProfile,
} from "../lib/branchingEduStory";
import { LIFE_SKILL_CHILD_STORAGE_KEY, subscribeLifeSkillCountersRefresh } from "../lib/lifeSkillPreferences";
import {
  LIFE_READINESS_AXES,
  hasAnyLifeSkillCounterSignal,
  hasAnyReadinessAxisSignal,
  lifeSkillCountersToLifeReadinessSnapshot,
  lowestReadinessAxis,
  mergeLocalAndApiLifeReadiness,
  profileToLifeReadinessSnapshot,
  readinessStatusLabel,
  recommendationForAxis,
  valueForAxis,
  wisdomBadgesUnlocked,
} from "../lib/lifeReadinessModel";

function hasAnyLocalPracticeSignal(profile: UserLifeProfile): boolean {
  return (
    profile.digitalWisdom > 0 ||
    profile.fiscalMuscle > 0 ||
    profile.socialCapital > 0 ||
    profile.integrity > 0 ||
    profile.emotionalBalance > 0
  );
}

export default function LifeReadinessDashboard() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<UserLifeProfile>(DEFAULT_USER_LIFE_PROFILE);
  const [localLoaded, setLocalLoaded] = useState(false);
  const [lifeSkillChildren, setLifeSkillChildren] = useState<ProfileChildDto[]>([]);
  const [selectedChildId, setSelectedChildId] = useState<number | null>(null);
  const [apiCounters, setApiCounters] = useState<Awaited<ReturnType<typeof getLifeSkillCounters>>>(null);
  const [apiLoading, setApiLoading] = useState(false);

  const refreshLocal = useCallback(async () => {
    const store = createWebLocalStorageStore();
    try {
      const s = await loadUserLifeProfileFromStore(store);
      setProfile(s?.profile ?? DEFAULT_USER_LIFE_PROFILE);
    } catch {
      setProfile(DEFAULT_USER_LIFE_PROFILE);
    } finally {
      setLocalLoaded(true);
    }
  }, []);

  useEffect(() => {
    void refreshLocal();
    const onStorage = (e: StorageEvent) => {
      if (e.key === USER_LIFE_PROFILE_STORAGE_KEY || e.key === null) void refreshLocal();
    };
    window.addEventListener("storage", onStorage);
    const onVis = () => {
      if (document.visibilityState === "visible") void refreshLocal();
    };
    document.addEventListener("visibilitychange", onVis);
    return () => {
      window.removeEventListener("storage", onStorage);
      document.removeEventListener("visibilitychange", onVis);
    };
  }, [refreshLocal]);

  useEffect(() => {
    if (!user) {
      setLifeSkillChildren([]);
      setSelectedChildId(null);
      setApiCounters(null);
      setApiLoading(false);
      return;
    }
    setSelectedChildId(null);
    setLifeSkillChildren([]);
    setApiCounters(null);
    let cancelled = false;
    (async () => {
      try {
        const profileRes = await getProfile();
        if (cancelled) return;
        const children = profileRes.children ?? [];
        setLifeSkillChildren(children);
        if (children.length === 0) return;
        let pick = children[0].id;
        try {
          const stored = localStorage.getItem(LIFE_SKILL_CHILD_STORAGE_KEY);
          if (stored) {
            const n = Number(stored);
            if (Number.isFinite(n) && children.some((c) => c.id === n)) pick = n;
          }
        } catch {
          /* ignore */
        }
        setSelectedChildId(pick);
      } catch {
        if (!cancelled) {
          setLifeSkillChildren([]);
          setSelectedChildId(null);
          setApiCounters(null);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user]);

  const refreshApiCounters = useCallback(async () => {
    if (!user || selectedChildId == null) return;
    setApiLoading(true);
    try {
      const c = await getLifeSkillCounters(selectedChildId);
      setApiCounters(c);
    } catch {
      setApiCounters(null);
    } finally {
      setApiLoading(false);
    }
  }, [user, selectedChildId]);

  useEffect(() => {
    void refreshApiCounters();
  }, [refreshApiCounters]);

  useEffect(() => {
    return subscribeLifeSkillCountersRefresh(() => {
      void refreshApiCounters();
    });
  }, [refreshApiCounters]);

  useEffect(() => {
    const onVis = () => {
      if (typeof document !== "undefined" && !document.hidden) void refreshApiCounters();
    };
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, [refreshApiCounters]);

  const localSnapshot = profileToLifeReadinessSnapshot(profile);
  const hasLocal = hasAnyLocalPracticeSignal(profile);
  const apiSnapshot =
    apiCounters != null ? lifeSkillCountersToLifeReadinessSnapshot(apiCounters) : null;
  const hasApi = hasAnyLifeSkillCounterSignal(apiCounters ?? undefined);
  const snapshot = mergeLocalAndApiLifeReadiness(localSnapshot, apiSnapshot, hasLocal, hasApi);
  const badges = wisdomBadgesUnlocked(snapshot);
  const focusAxis = lowestReadinessAxis(snapshot);
  const rec = recommendationForAxis(focusAxis);
  const hasSignal = hasAnyReadinessAxisSignal(snapshot);

  const onChildChange = (id: number) => {
    setSelectedChildId(id);
    try {
      localStorage.setItem(LIFE_SKILL_CHILD_STORAGE_KEY, String(id));
    } catch {
      /* ignore */
    }
  };

  const showChildPicker = Boolean(user && lifeSkillChildren.length > 1);

  return (
    <div className="page prime-page life-readiness-page">
      <header className="prime-hero prime-hero-tamixa">
        <div className="prime-hero-content">
          <p className="dash-hero-eyebrow">Family view</p>
          <h1>Life readiness</h1>
          <p className="prime-hero-subtitle">
            Five gentle dimensions from interactive practice — not school marks.{" "}
            {user
              ? "We blend this browser’s practice snapshot with synced account signals for the child you pick below (same as Settings)."
              : "Signed out: you’ll only see practice stored in this browser. Sign in to merge account signals from the Tamixa app and web."}
          </p>
          <p className="muted" style={{ marginTop: "0.5rem", fontSize: "0.88rem" }}>
            <Link to="/stories?tab=library&hub=simulator">Practice (simulator)</Link>
            {" · "}
            <Link to="/dashboard">Home</Link>
            {user ? (
              <>
                {" · "}
                <Link to="/settings">Settings</Link>
              </>
            ) : null}
          </p>
        </div>
      </header>

      <div className="life-readiness-page__body">
        {!localLoaded ? (
          <p className="muted life-readiness-page__loading">Loading your practice snapshot…</p>
        ) : (
          <>
            {showChildPicker ? (
              <div className="life-readiness-page__child-row">
                <label htmlFor="life-readiness-child" className="life-readiness-page__child-label">
                  Child for synced signals
                </label>
                <select
                  id="life-readiness-child"
                  className="field-input life-readiness-page__child-select"
                  value={selectedChildId ?? ""}
                  onChange={(e) => {
                    const n = Number(e.target.value);
                    if (Number.isFinite(n)) onChildChange(n);
                  }}
                  disabled={apiLoading}
                >
                  {lifeSkillChildren.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name?.trim() || `Child ${c.id}`}
                    </option>
                  ))}
                </select>
                {apiLoading ? (
                  <p className="muted life-readiness-page__api-hint">Refreshing synced signals…</p>
                ) : null}
              </div>
            ) : null}

            {!hasSignal ? (
              <section className="life-readiness-page__empty" aria-live="polite">
                <h2 className="life-readiness-page__section-title">Start with a short practice tale</h2>
                <p className="muted">
                  When you finish interactive Learn episodes in the library, we carry forward a soft snapshot here.
                  {user
                    ? " If your account already has practice rows for the selected child, they’ll appear automatically."
                    : " Sign in to combine with synced signals from your Tamixa account."}
                </p>
                <Link to="/stories?tab=library&hub=simulator" className="btn btn-primary" style={{ marginTop: "1rem" }}>
                  Open practice hub
                </Link>
              </section>
            ) : null}

            <section className="life-readiness-page__chart-block" aria-labelledby="lr-radar-heading">
              <h2 id="lr-radar-heading" className="life-readiness-page__section-title">
                How things feel right now
              </h2>
              <div className="life-readiness-page__chart-row">
                <LifeReadinessRadarChart snapshot={snapshot} size={300} />
                <ul className="life-readiness-page__axis-list" aria-label="Readiness by area">
                  {LIFE_READINESS_AXES.map((a) => (
                    <li key={a.id}>
                      <span className="life-readiness-page__axis-label">{a.label}</span>
                      <span className="life-readiness-page__axis-status">
                        {readinessStatusLabel(valueForAxis(snapshot, a.id))}
                      </span>
                      <span className="muted life-readiness-page__axis-note">{a.sourceNote}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </section>

            {badges.length > 0 ? (
              <section className="life-readiness-page__badges" aria-labelledby="lr-badges-heading">
                <h2 id="lr-badges-heading" className="life-readiness-page__section-title">
                  Badges your family unlocked
                </h2>
                <ul className="life-readiness-page__badge-list">
                  {badges.map((b) => (
                    <li key={b.id} className="life-readiness-page__badge-card">
                      <p className="life-readiness-page__badge-title">{b.title}</p>
                      <p className="muted" style={{ margin: 0, fontSize: "0.9rem" }}>
                        {b.familyLine}
                      </p>
                    </li>
                  ))}
                </ul>
              </section>
            ) : null}

            <section className="life-readiness-page__next" aria-labelledby="lr-next-heading">
              <h2 id="lr-next-heading" className="life-readiness-page__section-title">
                Suggested next listen
              </h2>
              <div className="life-readiness-page__next-card">
                <p className="life-readiness-page__next-headline">{rec.headline}</p>
                <p className="muted" style={{ marginTop: 0 }}>
                  {rec.familyMessage}
                </p>
                <Link to={rec.storiesHref} className="btn btn-primary" style={{ marginTop: "0.75rem" }}>
                  Pick a story
                </Link>
              </div>
            </section>
          </>
        )}
      </div>
    </div>
  );
}
