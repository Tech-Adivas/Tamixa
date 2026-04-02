import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

const navItems = [
  { to: "/dashboard", label: "Home" },
  { to: "/stories", label: "Stories" },
  { to: "/subscription", label: "Subscription" },
  { to: "/voice", label: "Voice" },
  { to: "/settings", label: "Settings" },
] as const;

export function AppLayout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  useEffect(() => {
    setMobileNavOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (mobileNavOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileNavOpen]);

  const displayEmail = user?.email
    ? user.email.length > 28
      ? `${user.email.slice(0, 26)}…`
      : user.email
    : "";

  return (
    <div className="app-shell">
      <button
        type="button"
        className="app-shell-nav-toggle"
        onClick={() => setMobileNavOpen((o) => !o)}
        aria-expanded={mobileNavOpen}
        aria-controls="app-sidebar-nav"
        aria-label={mobileNavOpen ? "Close menu" : "Open menu"}
      >
        <span className="app-shell-nav-toggle-bar" aria-hidden />
        <span className="app-shell-nav-toggle-bar" aria-hidden />
        <span className="app-shell-nav-toggle-bar" aria-hidden />
      </button>

      {mobileNavOpen ? (
        <button
          type="button"
          className="app-shell-scrim"
          aria-label="Close menu"
          onClick={() => setMobileNavOpen(false)}
        />
      ) : null}

      <aside
        id="app-sidebar-nav"
        className={`app-sidebar ${mobileNavOpen ? "app-sidebar--open" : ""}`}
        aria-label="Application"
      >
        <div className="app-sidebar-brand">
          <Link to="/dashboard" className="app-sidebar-logo" aria-label="Tamixa Home">
            <span className="app-sidebar-logo-mark">
              <img src="/tamixa-logo.svg" alt="" width={40} height={40} className="app-sidebar-logo-img" />
            </span>
            <span className="app-sidebar-logo-word">Tamixa</span>
          </Link>
          <p className="app-sidebar-tagline">Listen · Learn · Shine</p>
        </div>

        <nav className="app-sidebar-nav" aria-label="Main navigation">
          {navItems.map(({ to, label }) => {
            const active = location.pathname === to;
            return (
              <Link
                key={to}
                to={to}
                className={`app-sidebar-link ${active ? "app-sidebar-link--active" : ""}`}
              >
                <span className="app-sidebar-link-label">{label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="app-sidebar-footer">
          {displayEmail ? (
            <p className="app-sidebar-user" title={user?.email ?? ""}>
              {displayEmail}
            </p>
          ) : null}
          <button type="button" className="btn btn-ghost btn-sidebar-logout" onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>

      <div className="app-shell-stage">
        <header className="app-shell-topbar" role="banner">
          <div className="app-shell-topbar-inner">
            <span className="app-shell-topbar-brand">Tamixa</span>
            {displayEmail ? (
              <span className="app-shell-topbar-user" title={user?.email ?? ""}>
                {displayEmail}
              </span>
            ) : null}
          </div>
        </header>
        <main className="app-shell-main" id="main-content">
          {children}
        </main>
      </div>
    </div>
  );
}
