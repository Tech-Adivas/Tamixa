import { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {
  getParentAvatarUrl,
  uploadParentAvatar,
  deleteParentAvatar,
  resolveCoverUrl,
} from "../lib/api";

export default function Avatar() {
  useAuth();
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setLoading(true);
    getParentAvatarUrl()
      .then((url) => setAvatarUrl(url))
      .catch(() => setAvatarUrl(null))
      .finally(() => setLoading(false));
  }, []);

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) {
      setError("Choose an image first.");
      return;
    }
    setUploading(true);
    setError("");
    try {
      const url = await uploadParentAvatar(file);
      setAvatarUrl(url);
      if (fileRef.current) fileRef.current.value = "";
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  };

  const handleDelete = async () => {
    setUploading(true);
    setError("");
    try {
      await deleteParentAvatar();
      setAvatarUrl(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Remove failed");
    } finally {
      setUploading(false);
    }
  };

  const displayUrl = avatarUrl ? resolveCoverUrl(avatarUrl) ?? avatarUrl : null;

  return (
    <div className="page app-surface-page">
      <header className="app-page-header">
        <h1>Storytelling avatar</h1>
        <p className="page-subtitle">
          Upload a photo for the optional talking-head video when you use <strong>Avatar</strong> play mode on a story (premium).
        </p>
      </header>

      {error ? <p className="error">{error}</p> : null}

      <section className="page-section generate-section">
        <h2 className="page-section-title">Your avatar</h2>
        <div className="page-section-card">
          {loading ? (
            <p className="muted">Loading…</p>
          ) : displayUrl ? (
            <div style={{ marginBottom: "1rem" }}>
              <img
                src={displayUrl}
                alt="Your storytelling avatar"
                style={{ maxWidth: 220, borderRadius: 12, display: "block" }}
              />
            </div>
          ) : (
            <p className="muted" style={{ marginTop: 0 }}>
              No avatar yet. Add a clear, front-facing photo (JPEG or PNG). This pairs with voice cloning and{" "}
              <Link to="/voice">your voice profile</Link> in the story player.
            </p>
          )}
          <div style={{ marginTop: "1rem", display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            <div>
              <label className="muted" style={{ display: "block", marginBottom: "0.25rem", fontSize: "0.875rem" }}>
                Image file
              </label>
              <input ref={fileRef} type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" disabled={uploading} />
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
              <button type="button" className="btn btn-primary" onClick={handleUpload} disabled={uploading}>
                {uploading ? "Working…" : displayUrl ? "Replace avatar" : "Upload avatar"}
              </button>
              {displayUrl ? (
                <button type="button" className="btn" onClick={handleDelete} disabled={uploading}>
                  Remove
                </button>
              ) : null}
            </div>
            <p className="muted" style={{ fontSize: "0.8rem", margin: 0 }}>
              Requires an active Tamixa plan. Manage billing on{" "}
              <Link to="/subscription">Subscription</Link>.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
