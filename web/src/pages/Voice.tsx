import { useState, useEffect, useRef } from "react";
import { useAuth } from "../contexts/AuthContext";
import { getVoiceProfiles, uploadVoiceProfile, type VoiceProfile } from "../lib/api";

export default function Voice() {
  useAuth();
  const [profiles, setProfiles] = useState<VoiceProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [uploading, setUploading] = useState(false);
  const [userConsent, setUserConsent] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const consentRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    getVoiceProfiles()
      .then(setProfiles)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) {
      setError("Choose a reference audio file first.");
      return;
    }
    if (!userConsent) {
      setError("Please confirm that you consent to your voice being used to create a synthetic voice.");
      return;
    }
    setUploading(true);
    setError("");
    try {
      const consentFile = consentRef.current?.files?.[0] ?? null;
      const profile = await uploadVoiceProfile(file, consentFile, true);
      setProfiles((p) => [...p, profile]);
      if (fileRef.current) fileRef.current.value = "";
      if (consentRef.current) consentRef.current.value = "";
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="page app-surface-page">
      <header className="app-page-header">
        <h1>Clone your voice</h1>
        <p className="page-subtitle">Upload a voice sample to use your voice for story narration</p>
      </header>

      {error && <p className="error">{error}</p>}

      <section className="page-section generate-section">
        <h2 className="page-section-title">Add voice profile</h2>
        <div className="page-section-card">
          <p className="muted" style={{ marginTop: 0 }}>
            Upload a short audio sample (your voice, 5–10 seconds, clear and quiet). We’ll clone it so stories can be read in your voice. Processing usually takes under a minute.
          </p>
          <div style={{ marginTop: "1rem", display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            <div>
              <label className="muted" style={{ display: "block", marginBottom: "0.25rem", fontSize: "0.875rem" }}>Reference audio (required)</label>
              <input
                ref={fileRef}
                type="file"
                accept="audio/*,.mp3,.wav,.m4a"
                disabled={uploading}
                style={{ marginBottom: "0.5rem" }}
              />
            </div>
            <div>
              <label className="muted" style={{ display: "block", marginBottom: "0.25rem", fontSize: "0.875rem" }}>Consent audio (optional — required for Google voice cloning)</label>
              <input
                ref={consentRef}
                type="file"
                accept="audio/*,.mp3,.wav,.m4a"
                disabled={uploading}
                style={{ marginBottom: "0.5rem" }}
              />
              <p className="muted" style={{ fontSize: "0.8rem" }}>
                If your app uses Google voice cloning, also upload a recording of you reading: &quot;I am the owner of this voice and I consent to Google using this voice to create a synthetic voice model.&quot;
              </p>
            </div>
            <label className="consent-label" style={{ display: "flex", alignItems: "flex-start", gap: "0.5rem", marginTop: "0.5rem", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={userConsent}
                onChange={(e) => setUserConsent(e.target.checked)}
                disabled={uploading}
                aria-describedby="consent-description"
              />
              <span id="consent-description" className="muted" style={{ fontSize: "0.875rem" }}>
                I consent to my voice being used to create a synthetic voice for story narration in this app.
              </span>
            </label>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleUpload}
              disabled={uploading || !userConsent}
              style={{ alignSelf: "flex-start", marginTop: "0.75rem" }}
            >
              {uploading ? "Uploading and cloning…" : "Upload and clone voice"}
            </button>
          </div>
        </div>
      </section>

      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <section className="page-section generate-section">
          <h2 className="page-section-title">Your profiles</h2>
          {profiles.length === 0 ? (
            <p className="muted">No voice profiles yet. Upload one above.</p>
          ) : (
            <ul className="story-list" style={{ marginTop: "0.75rem" }}>
              {profiles.map((p) => (
                <li key={p.id} className="story-card">
                  Profile #{p.id} — Added {new Date(p.createdAt).toLocaleDateString()}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
}
