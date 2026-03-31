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
      // Backend no longer supports token-based magic link verification.
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

  if (status === "verifying") return <div className="page"><p>Signing you in…</p></div>;
  if (status === "error") {
    return (
      <div className="page">
        <p className="error">{errorMessage}</p>
        <p><Link to="/login">Go to login</Link></p>
      </div>
    );
  }
  return null;
}
