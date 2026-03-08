import { Link } from "react-router-dom";

export default function Home() {
  return (
    <div className="home-page home-prime">
      <header className="home-prime-header">
        <Link to="/" className="app-logo" aria-label="Araro Home">
          <img src="/araro-logo.svg" alt="" className="app-logo-img" />
          <span className="app-logo-text">ஆராரோ</span>
        </Link>
        <nav className="home-prime-nav">
          <Link to="/login" className="home-prime-nav-link">Sign in</Link>
          <Link to="/register" className="btn btn-primary btn-sm">Create account</Link>
        </nav>
      </header>

      <section className="home-prime-hero">
        <div className="home-prime-hero-content">
          <h1 className="home-prime-title">Stories for kids</h1>
          <p className="home-prime-tagline">
            Create personalized Tamil stories for your child — with their name, favorite themes, and a voice they love.
          </p>
          <div className="home-ctas">
            <Link to="/login" className="btn btn-primary home-cta-primary">
              Sign in
            </Link>
            <Link to="/register" className="btn btn-outline home-cta-secondary">
              Create account
            </Link>
          </div>
        </div>
      </section>

      <section className="prime-row">
        <div className="prime-row-header">
          <h2 className="prime-row-title">Why Araro</h2>
        </div>
        <div className="home-prime-features">
          <div className="home-prime-feature">
            <span className="home-feature-icon" aria-hidden>📖</span>
            <h3>Curated & AI stories</h3>
            <p className="muted">Browse the library or generate new stories with your child&apos;s name and interests.</p>
          </div>
          <div className="home-prime-feature">
            <span className="home-feature-icon" aria-hidden>🎧</span>
            <h3>Listen together</h3>
            <p className="muted">Stream audio narration and optional soundscapes for story time.</p>
          </div>
          <div className="home-prime-feature">
            <span className="home-feature-icon" aria-hidden>🏠</span>
            <h3>For parents & kids</h3>
            <p className="muted">Safe, professional, and designed for both kids and parents to enjoy.</p>
          </div>
        </div>
      </section>

      <footer className="home-footer">
        <nav className="nav-links">
          <Link to="/login">Sign in</Link>
          <span>·</span>
          <Link to="/register">Register</Link>
          <span>·</span>
          <Link to="/privacy">Privacy</Link>
          <span>·</span>
          <Link to="/terms">Terms</Link>
        </nav>
      </footer>
    </div>
  );
}
