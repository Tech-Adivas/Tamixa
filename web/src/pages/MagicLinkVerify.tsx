import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { authStorage, getMe } from "../lib/api";
import { useAuth } from "../contexts/AuthContext";
import { API_PATH } from "../../../config/api.config";

const API_BASE = API_PATH;

export default function MagicLinkVerify() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [status, setStatus] = useState<"verifying" | "success" | "error">("verifying");

  useEffect(() => {
    const token = searchParams.get("token");
    if (!token) {
      setStatus("error");
      return;
    }
    fetch(`${API_BASE}/auth/magic-link/verify?token=${encodeURIComponent(token)}`)
      .then((res) => {
        if (!res.ok) throw new Error("Invalid or expired link");
        return res.json();
      })
      .then((data) => {
        authStorage.setTokens(data.accessToken, data.refreshToken);
        return getMe();
      })
      .then((user) => {
        setUser(user);
        setStatus("success");
        navigate("/dashboard");
      })
      .catch(() => setStatus("error"));
  }, [searchParams, setUser, navigate]);

  if (status === "verifying") return <div className="page"><p>Signing you in…</p></div>;
  if (status === "error") return <div className="page"><p className="error">Invalid or expired link. Please try again.</p></div>;
  return null;
}
