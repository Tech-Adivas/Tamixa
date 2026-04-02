import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useSearchParams, useNavigate } from "react-router-dom";
import { authStorage, getMe, verifyPasswordlessCode } from "../lib/api";
import { useAuth } from "../contexts/AuthContext";

export default function MagicLinkVerify() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [status, setStatus] = useState<"verifying" | "success" | "error">("verifying");
  const [errorMessage, setErrorMessage] = useState("Invalid or expired link. Please try again.");

  useEffect(() => {
    const email = searchParams.get("email")?.trim() ?? "";
    const code = searchParams.get("code")?.trim() ?? "";
    const token = searchParams.get("token");
    if (!email || !code) {
      if (token) {
        setErrorMessage("This sign-in link format is no longer supported. Please request a new code.");
      }
      setStatus("error");
      return;
    }
    verifyPasswordlessCode(email, code, true, true, false)
      .then((data) => {
        authStorage.setTokens(data.accessToken, data.refreshToken, data.expiresInSeconds);
        return getMe();
      })
      .then((user) => {
        setUser(user);
        setStatus("success");
        navigate("/dashboard");
      })
      .catch((err) => {
        const msg = err instanceof Error ? err.message : "Invalid or expired link. Please try again.";
        setErrorMessage(msg);
        setStatus("error");
      });
  }, [searchParams, setUser, navigate]);

  if (status === "verifying") {
    return (
      <div className="auth-shell auth-shell--minimal">
        <div className="auth-shell-panel auth-shell-panel--solo">
          <div className="auth-card auth-card--pro auth-card--compact">
            <p className="auth-card-eyebrow">Tamixa</p>
            <h1 className="auth-card-title">Signing you in</h1>
            <p className="muted auth-card-subtitle">Verifying your link—this only takes a moment.</p>
            <div className="auth-verify-spinner-wrap" aria-busy="true" aria-live="polite">
              <div className="auth-verify-spinner" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="auth-shell auth-shell--minimal">
        <div className="auth-shell-panel auth-shell-panel--solo">
          <div className="auth-card auth-card--pro auth-card--compact">
            <p className="auth-card-eyebrow">Tamixa</p>
            <h1 className="auth-card-title">Couldn&apos;t sign you in</h1>
            <p className="error">{errorMessage}</p>
            <p className="muted auth-card-subtitle" style={{ marginBottom: "1rem" }}>
              Request a fresh code from the login page and try again.
            </p>
            <Link to="/login" className="btn btn-primary btn-block">
              Go to login
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return null;
}
