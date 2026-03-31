import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useAccessibility } from "../hooks/useAccessibility";
import { deleteAccount, updateStoryArtPersonalizationOptIn } from "../lib/api";
import {
  getConsentRecords,
  requestDataExport,
  getDataExportJobs,
  getListeningProgress,
  type ConsentRecord,
  type ExportJob,
  type ListeningProgress,
} from "../lib/api";

export default function Settings() {
  const { user, setUser, logout } = useAuth();
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
  const navigate = useNavigate();

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
    <div className="page">
      <header className="page-header-tamixa" style={{ marginTop: 0 }}>
        <h1>Settings & Privacy</h1>
        <p className="page-subtitle">Consent, data export, listening progress</p>
      </header>

      {error && <p className="error">{error}</p>}

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

      {loading ? (
        <p>Loading…</p>
      ) : (
        <>
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
