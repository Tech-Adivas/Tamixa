import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { register, authStorage, getMe } from "../lib/api";
import { useAuth } from "../contexts/AuthContext";

export default function Register() {
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false);
  const [acceptedParentalAttestation, setAcceptedParentalAttestation] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [validationError, setValidationError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setValidationError("");
    if (password.length < 8) {
      setValidationError("Password must be at least 8 characters");
      return;
    }
    if (!acceptedTerms || !acceptedPrivacy) {
      setValidationError("You must accept the Terms of Service and Privacy Policy to register");
      return;
    }
    if (!acceptedParentalAttestation) {
      setValidationError("You must confirm that you are the parent or guardian and at least 18 years old");
      return;
    }
    setLoading(true);
    try {
      const tokens = await register(email, password, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation);
      authStorage.setTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresInSeconds);
      const user = await getMe();
      setUser(user);
      navigate("/stories");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-shell" role="main" aria-label="Registration page">
      <div className="auth-shell-brand">
        <div className="auth-shell-brand-inner">
          <Link to="/" className="auth-shell-brand-logo">
            <img src="/tamixa-app-icon.png" alt="Tamixa" width={112} height={112} className="auth-shell-brand-logo-img" />
          </Link>
          <p className="auth-shell-brand-kicker">Join the fun</p>
          <h2 className="auth-shell-brand-headline">One cozy login for every bedtime adventure</h2>
          <p className="auth-shell-brand-copy">
            Create a family account to save favorites, voices, and your listening world—protected the Tamixa way, built for
            kids and grown-ups together.
          </p>
          <ul className="auth-shell-brand-list">
            <li>Password sign-in here on the web, or a magic code on the login page</li>
            <li>Stories, voices, and listening history in one place</li>
            <li>Simple consent steps so parents stay in charge</li>
          </ul>
        </div>
      </div>

      <div className="auth-shell-panel">
        <div className="auth-card auth-card--pro">
          <p className="auth-card-eyebrow">Tamixa</p>
          <h1 id="register-heading" className="auth-card-title">
            Create account
          </h1>
          <p id="register-desc" className="auth-card-subtitle muted">
            Set up your email and password to access the parent portal.
          </p>
          <form onSubmit={handleSubmit} className="form auth-form" aria-labelledby="register-heading" aria-describedby="register-desc">
            {(error || validationError) ? <p className="error">{error || validationError}</p> : null}
            <div className="field field--full" role="group" aria-label="Email">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                aria-required="true"
                placeholder="you@example.com"
              />
            </div>
            <div className="field field--full" role="group" aria-label="Password">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="new-password"
                minLength={8}
                aria-required="true"
                placeholder="At least 8 characters"
              />
            </div>
            <div className="field field--full">
              <label className="auth-check-label">
                <input
                  type="checkbox"
                  checked={acceptedTerms}
                  onChange={(e) => setAcceptedTerms(e.target.checked)}
                  aria-label="I accept the Terms of Service"
                />
                <span>
                  I accept the{" "}
                  <a href="/terms" target="_blank" rel="noopener noreferrer">
                    Terms of Service
                  </a>
                </span>
              </label>
            </div>
            <div className="field field--full">
              <label className="auth-check-label">
                <input
                  type="checkbox"
                  checked={acceptedPrivacy}
                  onChange={(e) => setAcceptedPrivacy(e.target.checked)}
                  aria-label="I accept the Privacy Policy"
                />
                <span>
                  I accept the{" "}
                  <a href="/privacy" target="_blank" rel="noopener noreferrer">
                    Privacy Policy
                  </a>
                </span>
              </label>
            </div>
            <div className="field field--full">
              <label className="auth-check-label">
                <input
                  type="checkbox"
                  checked={acceptedParentalAttestation}
                  onChange={(e) => setAcceptedParentalAttestation(e.target.checked)}
                  aria-label="I am the parent or guardian and am at least 18 years old"
                />
                <span>
                  I am the parent or guardian of the child(ren) who will use this account and I am at least 18 years old
                </span>
              </label>
            </div>
            <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
              {loading ? "Creating account…" : "Create account"}
            </button>
          </form>
          <p className="muted auth-footer-text">
            Already have an account?{" "}
            <Link to="/login" className="auth-inline-link">
              Sign in
            </Link>
          </p>
          <p className="auth-back">
            <Link to="/">← Back to home</Link>
          </p>
        </div>
      </div>
    </main>
  );
}
