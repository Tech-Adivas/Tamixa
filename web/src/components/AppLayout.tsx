import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

const navItems = [
  { to: "/dashboard", label: "Home" },
  { to: "/stories", label: "Stories" },
  { to: "/children", label: "Children" },
  { to: "/subscription", label: "Subscription" },
  { to: "/voice", label: "Voice" },
  { to: "/settings", label: "Settings" },
] as const;

export function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const location = useLocation();

  return (
    <div className="app-layout">
      <header className="app-header" role="banner">
        <div className="app-header-inner">
          <Link to="/dashboard" className="app-logo" aria-label="Araro Home">
            <img src="/araro-logo.svg" alt="" className="app-logo-img" />
            <span className="app-logo-text">ஆராரோ</span>
          </Link>
          <nav className="app-nav" aria-label="Main navigation">
            {navItems.map(({ to, label }) => (
              <Link
                key={to}
                to={to}
                className={`app-nav-link ${location.pathname === to ? "active" : ""}`}
              >
                {label}
              </Link>
            ))}
          </nav>
          <div className="app-header-actions">
            <span className="app-user-email" title={user?.email ?? ""}>
              {user?.email ? (
                user.email.length > 24 ? `${user.email.slice(0, 22)}…` : user.email
              ) : null}
            </span>
            <button
              type="button"
              className="btn btn-outline btn-sm app-header-logout"
              onClick={logout}
              aria-label="Log out"
            >
              Logout
            </button>
          </div>
        </div>
      </header>
      <main className="app-main" id="main-content">{children}</main>
    </div>
  );
}
