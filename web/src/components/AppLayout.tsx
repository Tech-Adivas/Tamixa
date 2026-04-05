import { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { ROUTES } from "../lib/appRoutes";

const navItems = [
  { to: "/stories", label: "Stories" },
  { to: "/dashboard", label: "Home" },
  { to: ROUTES.lifeReadiness, label: "Life readiness" },
  { to: "/subscription", label: "Subscription" },
  { to: "/voice", label: "Voice" },
  { to: "/avatar", label: "Avatar" },
  { to: "/settings", label: "Settings" },
] as const;

const ROUTE_PAGE_TITLES: Record<string, string> = {
  "/stories": "Stories",
  "/dashboard": "Home",
  [ROUTES.lifeReadiness]: "Life readiness",
  "/subscription": "Subscription",
  "/voice": "Voice",
  "/avatar": "Avatar",
  "/settings": "Settings",
};

function pageTitleForPath(pathname: string): string {
  if (ROUTE_PAGE_TITLES[pathname]) {
    return ROUTE_PAGE_TITLES[pathname];
  }
  const match = Object.keys(ROUTE_PAGE_TITLES).find(
    (p) => p !== "/" && pathname.startsWith(`${p}/`),
  );
  return match ? ROUTE_PAGE_TITLES[match] : "Tamixa";
}

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

  const mobilePageTitle = useMemo(() => pageTitleForPath(location.pathname), [location.pathname]);

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-to-content">
        Skip to main content
      </a>
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
          <Link to="/stories" className="app-sidebar-logo" aria-label="Tamixa Stories">
            <span className="app-sidebar-logo-mark">
              <img src="/tamixa-app-icon.png" alt="" width={40} height={40} className="app-sidebar-logo-img" />
            </span>
            <span className="app-sidebar-logo-word">Tamixa</span>
          </Link>
          <p className="app-sidebar-tagline">Where every night needs a story</p>
        </div>

        <nav className="app-sidebar-nav" aria-label="Main navigation">
          {navItems.map(({ to, label }) => {
            const active = location.pathname === to;
            return (
              <Link
                key={to}
                to={to}
                className={`app-sidebar-link ${active ? "app-sidebar-link--active" : ""}`}
                aria-current={active ? "page" : undefined}
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
            <div className="app-shell-topbar-titles">
              <span className="app-shell-topbar-page">{mobilePageTitle}</span>
              <span className="app-shell-topbar-app">Tamixa</span>
            </div>
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
