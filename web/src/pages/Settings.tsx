import { useState, useEffect, useCallback } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {
  getPreferredVoiceProfile,
  setPreferredVoiceProfile,
  type PreferredVoiceProfile,
} from "../lib/listenerPreferences";
import { useAccessibility } from "../hooks/useAccessibility";
import { deleteAccount, updateStoryArtPersonalizationOptIn } from "../lib/api";
import {
  getConsentRecords,
  requestDataExport,
  getDataExportJobs,
  getListeningProgress,
  getProfile,
  getLifeSkillCounters,
  type ConsentRecord,
  type ExportJob,
  type ListeningProgress,
  type LifeSkillCountersResponse,
  type ProfileChildDto,
} from "../lib/api";
import { ROUTES } from "../lib/appRoutes";
import {
  LIFE_SKILL_CHILD_STORAGE_KEY,
  subscribeLifeSkillCountersRefresh,
} from "../lib/lifeSkillPreferences";
import {
  supportSafetySectionTitle,
  mentalHealthDisclaimer,
  eduInteractiveDisclaimer,
  helplinePlaceholderNote,
  getComplianceResourceLinesFromEnv,
} from "../lib/complianceCopy";

const LIFE_SKILL_PILLAR_MAX = 40;

function pillarBarPercent(value: number): number {
  if (!Number.isFinite(value) || value <= 0) return 0;
  return Math.min(100, (value / LIFE_SKILL_PILLAR_MAX) * 100);
}

export default function Settings() {
  const complianceResourceLines = getComplianceResourceLinesFromEnv();
  const { user, setUser, logout } = useAuth();
  const location = useLocation();
  const { fontScale, setFontScale, highContrast, setHighContrast } = useAccessibility();
  const [consent, setConsent] = useState<ConsentRecord[]>([]);
  const [exportJobs, setExportJobs] = useState<ExportJob[]>([]);
  const [progress, setProgress] = useState<ListeningProgress | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [exporting, setExporting] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [storyArtOptIn, setStoryArtOptIn] = useState<boolean>(user?.storyArtPersonalizationOptIn === true);
  const [savingStoryArt, setSavingStoryArt] = useState(false);
  const [preferredVoice, setPreferredVoiceState] = useState<PreferredVoiceProfile>(() =>
    getPreferredVoiceProfile()
  );
  const [lifeSkillCounters, setLifeSkillCounters] = useState<LifeSkillCountersResponse | null>(null);
  const [lifeSkillChildren, setLifeSkillChildren] = useState<ProfileChildDto[]>([]);
  const [lifeSkillSelectedId, setLifeSkillSelectedId] = useState<number | null>(null);
  const [lifeSkillBlockLoading, setLifeSkillBlockLoading] = useState(false);
  const navigate = useNavigate();

  const refreshLifeSkillCounters = useCallback(async () => {
    if (!user) return;
    const id = lifeSkillSelectedId;
    if (id == null) return;
    try {
      const c = await getLifeSkillCounters(id);
      setLifeSkillCounters(c);
    } catch {
      /* ignore — block shows stale or empty */
    }
  }, [user, lifeSkillSelectedId]);

  useEffect(() => {
    return subscribeLifeSkillCountersRefresh(() => {
      if (location.pathname === "/settings") void refreshLifeSkillCounters();
    });
  }, [refreshLifeSkillCounters, location.pathname]);

  useEffect(() => {
    const onVis = () => {
      if (
        typeof document !== "undefined" &&
        !document.hidden &&
        location.pathname === "/settings"
      ) {
        void refreshLifeSkillCounters();
      }
    };
    document.addEventListener("visibilitychange", onVis);
    return () => document.removeEventListener("visibilitychange", onVis);
  }, [refreshLifeSkillCounters, location.pathname]);

  useEffect(() => {
    setStoryArtOptIn(user?.storyArtPersonalizationOptIn === true);
  }, [user?.storyArtPersonalizationOptIn]);

  useEffect(() => {
    Promise.all([getConsentRecords(), getDataExportJobs(), getListeningProgress()])
      .then(([c, e, p]) => {
        setConsent(c);
        setExportJobs(e);
        setProgress(p);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!user) {
      setLifeSkillChildren([]);
      setLifeSkillSelectedId(null);
      setLifeSkillCounters(null);
      return;
    }
    if (location.pathname !== "/settings") return;
    let cancelled = false;
    setLifeSkillBlockLoading(true);
    (async () => {
      try {
        const profile = await getProfile();
        const children = profile.children ?? [];
        if (cancelled) return;
        if (children.length === 0) {
          setLifeSkillChildren([]);
          setLifeSkillSelectedId(null);
          setLifeSkillCounters(null);
          return;
        }
        setLifeSkillChildren(children);
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
        setLifeSkillSelectedId(pick);
        const counters = await getLifeSkillCounters(pick);
        if (!cancelled) setLifeSkillCounters(counters);
      } catch {
        if (!cancelled) {
          setLifeSkillChildren([]);
          setLifeSkillSelectedId(null);
          setLifeSkillCounters(null);
        }
      } finally {
        if (!cancelled) setLifeSkillBlockLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [user, location.pathname]);

  const onLifeSkillChildChange = async (id: number) => {
    setLifeSkillSelectedId(id);
    try {
      localStorage.setItem(LIFE_SKILL_CHILD_STORAGE_KEY, String(id));
    } catch {
      /* ignore */
    }
    setLifeSkillBlockLoading(true);
    setError("");
    try {
      const c = await getLifeSkillCounters(id);
      setLifeSkillCounters(c);
    } catch (err) {
      setLifeSkillCounters(null);
      setError(err instanceof Error ? err.message : "Failed to load practice signals");
    } finally {
      setLifeSkillBlockLoading(false);
    }
  };

  const handleExport = async () => {
    setExporting(true);
    setError("");
    try {
      await requestDataExport();
      const jobs = await getDataExportJobs();
      setExportJobs(jobs);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to request export");
    } finally {
      setExporting(false);
    }
  };

  const handleDeleteAccount = async () => {
    if (deleteConfirm !== "DELETE") return;
    setDeleting(true);
    setError("");
    try {
      await deleteAccount();
      logout();
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Account deletion failed");
    } finally {
      setDeleting(false);
    }
  };

  const handleStoryArtToggle = async (next: boolean) => {
    const previous = storyArtOptIn;
    setStoryArtOptIn(next);
    setSavingStoryArt(true);
    setError("");
    try {
      const saved = await updateStoryArtPersonalizationOptIn(next);
      setStoryArtOptIn(saved);
      if (user) {
        setUser({ ...user, storyArtPersonalizationOptIn: saved });
      }
    } catch (err) {
      setStoryArtOptIn(previous);
      setError(err instanceof Error ? err.message : "Failed to update setting");
    } finally {
      setSavingStoryArt(false);
    }
  };

  return (
    <div className="page app-surface-page">
      <header className="app-page-header">
        <h1>Settings & Privacy</h1>
        <p className="page-subtitle">Consent, data export, listening progress</p>
      </header>

      {error && <p className="error">{error}</p>}

      <section className="page-section generate-section">
        <h2 className="page-section-title">Default story voice</h2>
        <div className="page-section-card">
          <p className="muted" style={{ marginTop: 0 }}>
            When a story has no saved voice yet, we start with this choice (same idea as the Tamixa app).{" "}
            <Link to="/stories">Per-story voice and play mode</Link> in the player override this.
          </p>
          <div className="field" style={{ marginTop: "0.75rem" }}>
            <label htmlFor="pref-voice-default" className="muted" style={{ display: "block", marginBottom: "0.35rem" }}>
              Preferred voice
            </label>
            <select
              id="pref-voice-default"
              className="field-input"
              value={preferredVoice}
              onChange={(e) => {
                const v = e.target.value as PreferredVoiceProfile;
                setPreferredVoiceProfile(v);
                setPreferredVoiceState(v);
              }}
            >
              <option value="default">Tamixa default</option>
              <option value="calm">Calm (needs plan where premium)</option>
              <option value="family">Family / my voice (when available)</option>
            </select>
          </div>
        </div>
      </section>

      <section className="page-section generate-section">
        <h2 className="page-section-title">Accessibility</h2>
        <div className="page-section-card">
        <div className="field">
          <label>Text size</label>
          <input
            type="range"
            min="0.8"
            max="1.4"
            step="0.1"
            value={fontScale}
            onChange={(e) => setFontScale(parseFloat(e.target.value))}
          />
          <span>{Math.round(fontScale * 100)}%</span>
        </div>
        <div className="field">
          <label>
            <input
              type="checkbox"
              checked={highContrast}
              onChange={(e) => setHighContrast(e.target.checked)}
            />
            {" "}High contrast
          </label>
        </div>
        </div>
      </section>

      <section className="page-section generate-section">
        <h2 className="page-section-title">{supportSafetySectionTitle}</h2>
        <div className="page-section-card">
          <p className="muted" style={{ marginTop: 0 }}>
            {mentalHealthDisclaimer}
          </p>
          <p className="muted" style={{ marginBottom: 0 }}>
            {eduInteractiveDisclaimer}
          </p>
          {complianceResourceLines.length > 0 ? (
            <ul className="muted" style={{ marginTop: "0.75rem", marginBottom: 0, paddingLeft: "1.25rem", fontSize: "0.9rem" }}>
              {complianceResourceLines.map((line) => {
                const urlMatch = line.match(/\bhttps?:\/\/\S+/i);
                if (urlMatch) {
                  const url = urlMatch[0].replace(/[),.;]+$/, "");
                  const label = line.replace(url, "").trim() || url;
                  return (
                    <li key={line} style={{ marginBottom: "0.35rem" }}>
                      {label}{" "}
                      <a href={url} target="_blank" rel="noopener noreferrer">
                        {url}
                      </a>
                    </li>
                  );
                }
                return (
                  <li key={line} style={{ marginBottom: "0.35rem" }}>
                    {line}
                  </li>
                );
              })}
            </ul>
          ) : null}
          <p className="muted" style={{ marginTop: "0.75rem", marginBottom: 0, fontSize: "0.88rem" }}>
            {helplinePlaceholderNote}{" "}
            <Link to="/privacy">Privacy</Link>
            {" · "}
            <Link to="/terms">Terms</Link>
          </p>
        </div>
      </section>

      {loading ? (
        <p>Loading…</p>
      ) : (
        <>
          {lifeSkillChildren.length > 0 && lifeSkillSelectedId != null && (
            <section className="page-section generate-section">
              <h2 className="page-section-title">Practice signals from story choices</h2>
              <div className="page-section-card life-skill-card">
                <p className="muted" style={{ marginTop: 0, marginBottom: "0.65rem" }}>
                  Same summary as the Tamixa app Profile card — updates after interactive Learn episodes on web or mobile.{" "}
                  <Link to={ROUTES.lifeReadiness}>Open the life readiness view</Link> for a family-friendly chart and story
                  ideas (web practice snapshot).
                </p>
                {lifeSkillChildren.length > 1 ? (
                  <div className="field" style={{ marginBottom: "0.75rem" }}>
                    <label htmlFor="life-skill-child" className="muted" style={{ display: "block", marginBottom: "0.35rem" }}>
                      Child
                    </label>
                    <select
                      id="life-skill-child"
                      className="field-input"
                      value={lifeSkillSelectedId}
                      onChange={(e) => onLifeSkillChildChange(Number(e.target.value))}
                      disabled={lifeSkillBlockLoading}
                    >
                      {lifeSkillChildren.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name?.trim() || `Child ${c.id}`}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : null}
                {lifeSkillBlockLoading && !lifeSkillCounters ? (
                  <p className="muted" style={{ marginTop: 0 }}>
                    Loading…
                  </p>
                ) : lifeSkillCounters ? (
                  <>
                    <p className="life-skill-card__disclaimer muted">
                      {lifeSkillCounters.copyForParents?.trim() ||
                        "Not scores or school grades — private hints from interactive story choices only."}
                    </p>
                    <ul className="life-skill-pillars" aria-label="Practice pillars">
                      <li className="life-skill-pillar">
                        <span className="life-skill-pillar__label">Wisdom / digital judgment</span>
                        <span className="life-skill-pillar__value">{lifeSkillCounters.wisdom}</span>
                        <span className="life-skill-pillar__track" aria-hidden>
                          <span
                            className="life-skill-pillar__fill"
                            style={{ width: `${pillarBarPercent(lifeSkillCounters.wisdom)}%` }}
                          />
                        </span>
                      </li>
                      <li className="life-skill-pillar">
                        <span className="life-skill-pillar__label">Social</span>
                        <span className="life-skill-pillar__value">{lifeSkillCounters.social}</span>
                        <span className="life-skill-pillar__track" aria-hidden>
                          <span
                            className="life-skill-pillar__fill"
                            style={{ width: `${pillarBarPercent(lifeSkillCounters.social)}%` }}
                          />
                        </span>
                      </li>
                      <li className="life-skill-pillar">
                        <span className="life-skill-pillar__label">Money habits</span>
                        <span className="life-skill-pillar__value">{lifeSkillCounters.money}</span>
                        <span className="life-skill-pillar__track" aria-hidden>
                          <span
                            className="life-skill-pillar__fill"
                            style={{ width: `${pillarBarPercent(lifeSkillCounters.money)}%` }}
                          />
                        </span>
                      </li>
                      <li className="life-skill-pillar">
                        <span className="life-skill-pillar__label">Balance / focus</span>
                        <span className="life-skill-pillar__value">{lifeSkillCounters.balance}</span>
                        <span className="life-skill-pillar__track" aria-hidden>
                          <span
                            className="life-skill-pillar__fill"
                            style={{ width: `${pillarBarPercent(lifeSkillCounters.balance)}%` }}
                          />
                        </span>
                      </li>
                    </ul>
                  </>
                ) : (
                  <p className="muted" style={{ marginTop: 0 }}>
                    Couldn’t load counters for this child.
                  </p>
                )}
              </div>
            </section>
          )}

          {progress && (
            <section className="page-section generate-section">
              <h2 className="page-section-title">Listening progress</h2>
              <div className="page-section-card">
              <p style={{ margin: "0 0 0.25rem" }}>Stories started: {progress.storiesStarted}</p>
              <p>Stories completed: {progress.storiesCompleted}</p>
              <p style={{ margin: 0 }}>Completion rate: {(progress.completionRate * 100).toFixed(0)}%</p>
              </div>
            </section>
          )}

          <section className="page-section generate-section">
            <h2 className="page-section-title">Story personalization</h2>
            <div className="page-section-card">
              <div className="field">
                <label>
                  <input
                    type="checkbox"
                    checked={storyArtOptIn}
                    onChange={(e) => handleStoryArtToggle(e.target.checked)}
                    disabled={savingStoryArt}
                  />
                  {" "}Allow Tamixa to personalize story artwork from my preferences
                </label>
              </div>
              <p className="muted" style={{ marginTop: "0.5rem" }}>
                {savingStoryArt ? "Saving..." : "You can change this anytime."}
              </p>
            </div>
          </section>

          <section className="page-section generate-section">
            <h2 className="page-section-title">Consent history</h2>
            {consent.length === 0 ? (
              <p className="muted">No consent records yet.</p>
            ) : (
              <ul className="story-list" style={{ marginTop: "0.75rem" }}>
                {consent.map((c, i) => (
                  <li key={i} className="story-card">
                    {c.consentType} (v{c.version}) — {new Date(c.grantedAt).toLocaleString()}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="page-section generate-section">
            <h2 className="page-section-title">Data export</h2>
            <div className="page-section-card">
            <p className="muted" style={{ marginTop: 0 }}>Request a copy of your data (GDPR/DPDP).</p>
            <button type="button" className="btn btn-primary" onClick={handleExport} disabled={exporting} style={{ marginTop: "0.5rem" }}>
              {exporting ? "Requesting…" : "Request data export"}
            </button>
            {exportJobs.length > 0 && (
              <ul className="story-list" style={{ marginTop: "1rem" }}>
                {exportJobs.map((j) => (
                  <li key={j.id} className="story-card">
                    Job #{j.id}: {j.status} — {new Date(j.requestedAt).toLocaleString()}
                    {j.downloadUrl && <a href={j.downloadUrl} download> Download</a>}
                  </li>
                ))}
              </ul>
            )}
            </div>
          </section>

          <section className="page-section generate-section">
            <h2 className="page-section-title">Delete account</h2>
            <div className="page-section-card" style={{ borderColor: "var(--color-destructive, #dc3545)" }}>
              <p className="muted" style={{ marginTop: 0 }}>Permanently delete your account and all associated data. This cannot be undone.</p>
              <p style={{ marginTop: "0.5rem", fontSize: "0.9rem" }}>Type <strong>DELETE</strong> to confirm:</p>
              <input
                type="text"
                className="field-input"
                value={deleteConfirm}
                onChange={(e) => setDeleteConfirm(e.target.value)}
                placeholder="DELETE"
                aria-label="Confirmation"
                style={{ marginTop: "0.25rem", maxWidth: "12rem" }}
              />
              <button
                type="button"
                className="btn"
                style={{ marginTop: "0.5rem", background: "var(--color-destructive, #dc3545)", color: "#fff" }}
                onClick={handleDeleteAccount}
                disabled={deleting || deleteConfirm !== "DELETE"}
              >
                {deleting ? "Deleting…" : "Delete my account"}
              </button>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
