import { useState, useEffect, useRef } from "react";
import { useAuth } from "../contexts/AuthContext";
import { getVoiceProfiles, uploadVoiceProfile, type VoiceProfile } from "../lib/api";

export default function Voice() {
  useAuth();
  const [profiles, setProfiles] = useState<VoiceProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    getVoiceProfiles()
      .then(setProfiles)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError("");
    try {
      const profile = await uploadVoiceProfile(file);
      setProfiles((p) => [...p, profile]);
      if (fileRef.current) fileRef.current.value = "";
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="page">
      <header className="page-header dashboard-header">
        <div>
          <h1>Voice profiles</h1>
          <p className="page-subtitle muted">Upload voice samples for personalized story narration</p>
        </div>
      </header>

      {error && <p className="error">{error}</p>}

      <section className="page-section generate-section">
        <h2 className="page-section-title">Add voice profile</h2>
        <div className="page-section-card">
          <p className="muted" style={{ marginTop: 0 }}>Record a short audio sample (parent voice) for story narration.</p>
          <input
            ref={fileRef}
            type="file"
            accept="audio/*"
            onChange={handleFile}
            disabled={uploading}
            style={{ marginTop: "1rem", marginBottom: "0.5rem" }}
          />
          {uploading && <p className="muted">Uploading…</p>}
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
