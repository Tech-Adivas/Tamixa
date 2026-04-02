import { useState, useRef, useEffect } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import {
  authStorage,
  getMe,
  requestPasswordlessCode,
  verifyPasswordlessCode,
} from "../lib/api";
import { useAuth } from "../contexts/AuthContext";
import { logger } from "../lib/logger";

const OTP_LENGTH = 6;
const EMAIL_REGEX = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setUser } = useAuth();
  const from = (location.state as { from?: string })?.from ?? "/stories";
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
      audio.play().catch((err) => {
        logger.warn("login", "Ambient audio play failed", { message: err instanceof Error ? err.message : String(err) });
      });
    }
  };

  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      setError("Enter your email");
      return;
    }
    if (!EMAIL_REGEX.test(email)) {
      setError("Enter a valid email address");
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
      authStorage.setTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresInSeconds);
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
    <main className="auth-shell" role="main" aria-label="Login page">
      <audio
        ref={ambientRef}
        src="/audio/calm-ambient.mp3"
        loop
        playsInline
        className="auth-bg-music"
        preload="metadata"
      />

      <div className="auth-shell-brand">
        <div className="auth-shell-brand-inner">
          <button
            type="button"
            onClick={toggleAmbientSound}
            className="auth-shell-ambient"
            aria-label={ambientPlaying ? "Mute background sound" : "Play background sound"}
          >
            {ambientPlaying ? "Sound on" : "Ambient"}
          </button>
          <Link to="/" className="auth-shell-brand-logo">
            <img src="/tamixa-logo.svg" alt="Tamixa" width={180} height={48} className="auth-shell-brand-logo-img" />
          </Link>
          <p className="auth-shell-brand-kicker">Tamixa for families</p>
          <h2 className="auth-shell-brand-headline">Your family&apos;s story room, on any screen</h2>
          <p className="auth-shell-brand-copy">
            Sign in with a one-time code—no password to forget. Same cozy, trusted flow as the Tamixa app on your phone.
          </p>
          <ul className="auth-shell-brand-list">
            <li>Personalized library and AI-assisted stories</li>
            <li>Safe, age-appropriate content for family listening</li>
            <li>Voice and language options you control</li>
          </ul>
        </div>
      </div>

      <div className="auth-shell-panel">
        <div className="auth-card auth-card--pro">
          <p className="auth-card-eyebrow">Glad you&apos;re here</p>
          <h1 id="login-heading" className="auth-card-title">
            Sign in
          </h1>
          <p id="login-desc" className="auth-card-subtitle muted">
            Enter your email. We&apos;ll send a 6-digit code to sign in or create your account.
          </p>

          {!codeSentTo ? (
            <form onSubmit={handleSendCode} className="form auth-form" aria-labelledby="login-heading" aria-describedby="login-desc">
              {error ? <p className="error">{error}</p> : null}
              <div className="field field--full">
                <label htmlFor="email">Work or personal email</label>
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
              <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
                {loading ? "Sending…" : "Send sign-in code"}
              </button>
              <p className="muted auth-form-hint">
                New here? Use the same email to verify—we&apos;ll set up your account when you confirm the code.
              </p>
            </form>
          ) : (
            <form onSubmit={handleVerifyCode} className="form auth-form" aria-labelledby="login-heading">
              {error ? <p className="error">{error}</p> : null}
              <p className="auth-code-sent muted">
                Code sent to <strong className="auth-code-sent-email">{codeSentTo}</strong>
              </p>
              <div className="field field--full">
                <label htmlFor="code">6-digit code</label>
                <input
                  id="code"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={OTP_LENGTH}
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                  placeholder="000000"
                  autoComplete="one-time-code"
                  className="auth-input-code"
                />
              </div>
              <div className="field field--full">
                <label className="auth-check-label">
                  <input type="checkbox" checked={acceptedTerms} onChange={(e) => setAcceptedTerms(e.target.checked)} />
                  <span>
                    I agree to the{" "}
                    <a href="/terms" target="_blank" rel="noopener noreferrer">
                      Terms of Service
                    </a>
                  </span>
                </label>
              </div>
              <div className="field field--full">
                <label className="auth-check-label">
                  <input type="checkbox" checked={acceptedPrivacy} onChange={(e) => setAcceptedPrivacy(e.target.checked)} />
                  <span>
                    I agree to the{" "}
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
                  />
                  <span>I am the parent or guardian and am at least 18 years old</span>
                </label>
              </div>
              <p className="muted auth-form-hint auth-form-hint--small">Required for new accounts</p>
              <div className="auth-form-actions">
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? "Signing in…" : "Continue"}
                </button>
                <button type="button" className="btn btn-outline" onClick={resetToEmailStep} disabled={loading}>
                  Use different email
                </button>
              </div>
            </form>
          )}

          <p className="muted auth-footer-text">
            Prefer a password?{" "}
            <Link to="/register" className="auth-inline-link">
              Create an account with email and password
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
