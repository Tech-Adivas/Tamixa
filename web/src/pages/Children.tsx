import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { getChildren, createChild, updateChild, type Child } from "../lib/api";

export default function Children() {
  useAuth();
  const [children, setChildren] = useState<Child[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [name, setName] = useState("");
  const [dateOfBirth, setDateOfBirth] = useState("");
  const [language, setLanguage] = useState("ta");
  const [interests, setInterests] = useState("");
  const [favoriteColor, setFavoriteColor] = useState("");
  const [favoriteAnimal, setFavoriteAnimal] = useState("");
  const [characterTraits, setCharacterTraits] = useState("");
  const [avatarChoice, setAvatarChoice] = useState("");
  const [creating, setCreating] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [childProfileConsent, setChildProfileConsent] = useState(false);

  useEffect(() => {
    getChildren()
      .then(setChildren)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const resetForm = () => {
    setName("");
    setDateOfBirth("");
    setLanguage("ta");
    setInterests("");
    setFavoriteColor("");
    setFavoriteAnimal("");
    setCharacterTraits("");
    setAvatarChoice("");
    setChildProfileConsent(false);
    setShowForm(false);
    setEditingId(null);
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !dateOfBirth) return;
    if (!childProfileConsent) {
      setError("You must confirm that you consent to collect your child's data before adding a profile.");
      return;
    }
    setCreating(true);
    setError("");
    try {
      const child = await createChild({
        name: name.trim(),
        dateOfBirth,
        languagePreference: language,
        interests: interests.trim() || undefined,
        favoriteColor: favoriteColor.trim() || undefined,
        favoriteAnimal: favoriteAnimal.trim() || undefined,
        characterTraits: characterTraits.trim() || undefined,
        avatarChoice: avatarChoice.trim() || undefined,
        childProfileConsent: true,
      });
      setChildren((prev) => [...prev, child]);
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add child");
    } finally {
      setCreating(false);
    }
  };

  const startEdit = (c: Child) => {
    setEditingId(c.id);
    setLanguage(c.languagePreference ?? "ta");
    setInterests(c.interests ?? "");
    setFavoriteColor(c.favoriteColor ?? "");
    setFavoriteAnimal(c.favoriteAnimal ?? "");
    setCharacterTraits(c.characterTraits ?? "");
    setAvatarChoice(c.avatarChoice ?? "");
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editingId == null) return;
    setUpdating(true);
    setError("");
    try {
      const child = await updateChild(editingId, {
        languagePreference: language,
        interests: interests.trim() || null,
        favoriteColor: favoriteColor.trim() || null,
        favoriteAnimal: favoriteAnimal.trim() || null,
        characterTraits: characterTraits.trim() || null,
        avatarChoice: avatarChoice.trim() || null,
      });
      setChildren((prev) => prev.map((x) => (x.id === editingId ? child : x)));
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update child");
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="page">
      <header className="page-header dashboard-header">
        <div>
          <h1>Children</h1>
          <p className="page-subtitle muted">Manage child profiles for personalized stories</p>
        </div>
      </header>

      {error && <p className="error">{error}</p>}

      {loading ? (
        <p className="muted">Loading…</p>
      ) : (
        <section className="page-section">
          <h2 className="page-section-title">Profiles</h2>
          <ul className="story-list story-list-cards">
          {children.map((c) => (
            <li key={c.id} className="story-card story-card-flex">
              <div className="story-card-main">
                <span className="story-card-title"><strong>{c.name}</strong></span>
                <span className="muted story-card-meta">DOB: {c.dateOfBirth} · {c.languagePreference ?? "—"}</span>
                {(c.favoriteColor || c.favoriteAnimal || c.characterTraits || c.avatarChoice) && (
                  <span className="muted" style={{ display: "block", marginTop: "0.25rem", fontSize: "0.875rem" }}>
                    {[c.favoriteColor, c.favoriteAnimal, c.characterTraits, c.avatarChoice].filter(Boolean).join(" · ")}
                  </span>
                )}
              </div>
              <div className="story-card-actions">
                <button type="button" className="btn btn-sm btn-outline" onClick={() => startEdit(c)}>
                  Edit
                </button>
              </div>
            </li>
          ))}
          </ul>
        </section>
      )}

      {editingId != null && (() => {
        const editingChild = children.find((c) => c.id === editingId);
        return (
        <section className="page-section generate-section">
          <h2 className="page-section-title">Edit profile{editingChild ? ` — ${editingChild.name}` : ""}</h2>
          <div className="page-section-card">
          <form onSubmit={handleUpdate} className="form">
            <div className="field">
              <label htmlFor="editLang">Language preference</label>
              <select id="editLang" value={language} onChange={(e) => setLanguage(e.target.value)}>
                <option value="ta">Tamil</option>
                <option value="en">English</option>
                <option value="hi">Hindi</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="editInterests">Interests</label>
              <input
                id="editInterests"
                value={interests}
                onChange={(e) => setInterests(e.target.value)}
                placeholder="e.g. dinosaurs, space, animals"
              />
            </div>
            <div className="field">
              <label htmlFor="editColor">Favorite color</label>
              <input id="editColor" value={favoriteColor} onChange={(e) => setFavoriteColor(e.target.value)} placeholder="e.g. blue" />
            </div>
            <div className="field">
              <label htmlFor="editAnimal">Favorite animal</label>
              <input id="editAnimal" value={favoriteAnimal} onChange={(e) => setFavoriteAnimal(e.target.value)} placeholder="e.g. lion" />
            </div>
            <div className="field">
              <label htmlFor="editTraits">Character traits</label>
              <input
                id="editTraits"
                value={characterTraits}
                onChange={(e) => setCharacterTraits(e.target.value)}
                placeholder="e.g. brave, curious, kind"
              />
            </div>
            <div className="field">
              <label htmlFor="editAvatar">Avatar / creature in stories</label>
              <input id="editAvatar" value={avatarChoice} onChange={(e) => setAvatarChoice(e.target.value)} placeholder="e.g. dragon, unicorn" />
            </div>
            <button type="submit" className="btn btn-primary" disabled={updating}>
              {updating ? "Saving…" : "Save"}
            </button>
            <button type="button" className="btn btn-outline" onClick={resetForm} style={{ marginLeft: "0.5rem" }}>
              Cancel
            </button>
          </form>
          </div>
        </section>
        );
      })()}

      {showForm ? (
        <section className="page-section generate-section">
          <h2 className="page-section-title">Add child</h2>
          <div className="page-section-card">
          <form onSubmit={handleCreate} className="form">
            <div className="field">
              <label htmlFor="childName">Name</label>
              <input
                id="childName"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                placeholder="Child's name"
              />
            </div>
            <div className="field">
              <label htmlFor="dob">Date of birth</label>
              <input
                id="dob"
                type="date"
                value={dateOfBirth}
                onChange={(e) => setDateOfBirth(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="lang">Language preference</label>
              <select id="lang" value={language} onChange={(e) => setLanguage(e.target.value)}>
                <option value="ta">Tamil</option>
                <option value="en">English</option>
                <option value="hi">Hindi</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="interests">Interests</label>
              <input id="interests" value={interests} onChange={(e) => setInterests(e.target.value)} placeholder="e.g. dinosaurs, space" />
            </div>
            <div className="field">
              <label htmlFor="favColor">Favorite color</label>
              <input id="favColor" value={favoriteColor} onChange={(e) => setFavoriteColor(e.target.value)} placeholder="e.g. blue" />
            </div>
            <div className="field">
              <label htmlFor="favAnimal">Favorite animal</label>
              <input id="favAnimal" value={favoriteAnimal} onChange={(e) => setFavoriteAnimal(e.target.value)} placeholder="e.g. lion" />
            </div>
            <div className="field">
              <label htmlFor="traits">Character traits</label>
              <input id="traits" value={characterTraits} onChange={(e) => setCharacterTraits(e.target.value)} placeholder="e.g. brave, curious" />
            </div>
            <div className="field">
              <label htmlFor="avatar">Avatar in stories</label>
              <input id="avatar" value={avatarChoice} onChange={(e) => setAvatarChoice(e.target.value)} placeholder="e.g. dragon" />
            </div>
            <div className="field">
              <label style={{ display: "flex", alignItems: "flex-start", gap: 8, cursor: "pointer" }}>
                <input
                  type="checkbox"
                  id="childConsent"
                  checked={childProfileConsent}
                  onChange={(e) => setChildProfileConsent(e.target.checked)}
                  aria-describedby="childConsentDesc"
                />
                <span id="childConsentDesc">
                  I consent to the collection of my child&apos;s data (name, date of birth, preferences) for personalized storytelling, as described in the Privacy Policy.
                </span>
              </label>
            </div>
            <div className="field">
              <button type="submit" className="btn btn-primary" disabled={creating}>
                {creating ? "Adding…" : "Add child"}
              </button>
                <button type="button" className="btn btn-outline" onClick={resetForm} style={{ marginLeft: "0.5rem" }}>
                  Cancel
                </button>
            </div>
          </form>
          </div>
        </section>
      ) : (
        <section className="page-section">
          <button type="button" className="btn btn-primary" onClick={() => setShowForm(true)}>
            Add child
          </button>
        </section>
      )}
    </div>
  );
}
