import { useState, useRef, useEffect } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import {
  authStorage,
  getMe,
  requestPasswordlessCode,
  verifyPasswordlessCode,
} from "../lib/api";
import { useAuth } from "../contexts/AuthContext";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setUser } = useAuth();
  const from = (location.state as { from?: string })?.from ?? "/dashboard";
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [acceptedTerms, setAcceptedTerms] = useState(false);
  const [acceptedPrivacy, setAcceptedPrivacy] = useState(false);
  const [acceptedParentalAttestation, setAcceptedParentalAttestation] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [codeSentTo, setCodeSentTo] = useState<string | null>(null);
  const [ambientPlaying, setAmbientPlaying] = useState(false);
  const ambientRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    const audio = ambientRef.current;
    if (!audio) return;
    const onPlay = () => setAmbientPlaying(true);
    const onPause = () => setAmbientPlaying(false);
    audio.addEventListener("play", onPlay);
    audio.addEventListener("pause", onPause);
    return () => {
      audio.removeEventListener("play", onPlay);
      audio.removeEventListener("pause", onPause);
    };
  }, []);

  const toggleAmbientSound = () => {
    const audio = ambientRef.current;
    if (!audio) return;
    if (ambientPlaying) {
      audio.pause();
    } else {
      audio.play().catch(() => {});
    }
  };

  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      setError("Enter your email");
      return;
    }
    setError("");
    setLoading(true);
    try {
      const sent = await requestPasswordlessCode(email);
      if (sent) {
        setCodeSentTo(email);
      } else {
        setError("Failed to send code. Please try again.");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to send code");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!codeSentTo || !code) {
      setError("Enter the code from your email");
      return;
    }
    setError("");
    setLoading(true);
    try {
      const tokens = await verifyPasswordlessCode(
        codeSentTo,
        code,
        acceptedTerms,
        acceptedPrivacy,
        acceptedParentalAttestation
      );
      authStorage.setTokens(tokens.accessToken, tokens.refreshToken);
      const user = await getMe();
      setUser(user);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Invalid code or consent required");
    } finally {
      setLoading(false);
    }
  };

  const resetToEmailStep = () => {
    setCodeSentTo(null);
    setCode("");
    setAcceptedTerms(false);
    setAcceptedPrivacy(false);
    setAcceptedParentalAttestation(false);
    setError("");
  };

  return (
    <main className="page auth-page auth-page--bedtime" role="main" aria-label="Login page">
      <audio
        ref={ambientRef}
        src="/audio/calm-ambient.mp3"
        loop
        playsInline
        className="auth-bg-music"
        preload="metadata"
      />
      <button
        type="button"
        onClick={toggleAmbientSound}
        className="auth-ambient-toggle"
        aria-label={ambientPlaying ? "Mute background sound" : "Play background sound"}
      >
        {ambientPlaying ? "🔊 Mute" : "🔈 Play sound"}
      </button>
      <div className="auth-page__stars" aria-hidden="true" />
      <div className="auth-page__lantern auth-page__lantern--1" aria-hidden="true" />
      <div className="auth-page__lantern auth-page__lantern--2" aria-hidden="true" />
      <div className="auth-card auth-card--bedtime">
        <Link to="/" className="auth-logo-link">
          <img src="/tamixa-logo.svg" alt="Tamixa" className="auth-logo" />
        </Link>
        <p className="auth-welcome">Welcome to Tamixa</p>
        <p className="auth-tagline auth-tagline--bedtime">
          Listen your way—personalized for you.
        </p>
        <h1 id="login-heading">Sign in</h1>
        <p id="login-desc" className="muted">Sign in to access stories for your child.</p>

      {!codeSentTo ? (
        <form onSubmit={handleSendCode} className="form" aria-labelledby="login-heading" aria-describedby="login-desc">
          {error && <p className="error">{error}</p>}
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              placeholder="you@example.com"
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? "Sending…" : "Send code"}
          </button>
          <p className="muted" style={{ marginTop: "0.75rem" }}>
            We&apos;ll email you a 6-digit code. No password needed — same as the mobile app.
          </p>
        </form>
      ) : (
        <form onSubmit={handleVerifyCode} className="form">
          {error && <p className="error">{error}</p>}
          <p className="muted">Code sent to {codeSentTo}</p>
          <div className="field">
            <label htmlFor="code">Enter 6-digit code</label>
            <input
              id="code"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={6}
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
              placeholder="000000"
              autoComplete="one-time-code"
            />
          </div>
          <div className="field" style={{ marginTop: "0.5rem" }}>
            <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={acceptedTerms}
                onChange={(e) => setAcceptedTerms(e.target.checked)}
              />
              I agree to Terms of Service
            </label>
          </div>
          <div className="field">
            <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={acceptedPrivacy}
                onChange={(e) => setAcceptedPrivacy(e.target.checked)}
              />
              I agree to Privacy Policy
            </label>
          </div>
          <div className="field">
            <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={acceptedParentalAttestation}
                onChange={(e) => setAcceptedParentalAttestation(e.target.checked)}
              />
              I am the parent or guardian and am at least 18 years old
            </label>
          </div>
          <p className="muted" style={{ fontSize: "0.85rem", marginBottom: "0.5rem" }}>
            Required for new accounts
          </p>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? "Signing in…" : "Continue"}
          </button>
          <button
            type="button"
            className="btn btn-outline"
            style={{ marginLeft: "0.5rem" }}
            onClick={resetToEmailStep}
            disabled={loading}
          >
            Back
          </button>
        </form>
      )}

      <p className="muted auth-footer-text">
        Don&apos;t have an account? Enter your email above and we&apos;ll create one when you verify the code.
      </p>
      <p className="auth-back">
        <Link to="/">← Back to home</Link>
      </p>
      </div>
    </main>
  );
}
