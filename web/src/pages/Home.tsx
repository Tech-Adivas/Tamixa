import { Link } from "react-router-dom";

export default function Home() {
  return (
    <div className="marketing-page">
      <header className="marketing-header">
        <div className="marketing-header-inner">
          <Link to="/" className="app-logo marketing-logo" aria-label="Tamixa Home">
            <span className="tamixa-app-icon">
              <img src="/tamixa-app-icon.png" alt="" className="app-logo-img" />
            </span>
            <span className="app-logo-text">Tamixa</span>
          </Link>
          <nav className="marketing-nav" aria-label="Account">
            <Link to="/login" className="marketing-nav-link">
              Sign in
            </Link>
            <Link to="/register" className="btn btn-primary btn-sm marketing-nav-cta">
              Create account
            </Link>
          </nav>
        </div>
      </header>

      <section className="marketing-hero">
        <div className="marketing-hero-grid" aria-hidden />
        <div className="marketing-hero-inner">
          <p className="marketing-hero-eyebrow">Personalized stories · Tamil & more</p>
          <h1 className="marketing-hero-title">Stories that sound like they were written for your child</h1>
          <p className="marketing-hero-lead">
            Tamixa helps families create and listen to safe, engaging tales—with names, themes, and narration you
            control. Built for kids. Trusted by parents.
          </p>
          <div className="marketing-hero-ctas">
            <Link to="/register" className="btn btn-primary marketing-hero-btn-primary">
              Get started free
            </Link>
            <Link to="/login" className="btn btn-outline marketing-hero-btn-secondary">
              Sign in to your library
            </Link>
          </div>
        </div>
      </section>

      <section className="marketing-features" aria-labelledby="marketing-features-heading">
        <div className="marketing-section-head">
          <h2 id="marketing-features-heading" className="marketing-section-title">
            Everything you need for story time
          </h2>
          <p className="marketing-section-subtitle muted">
            One calm, focused experience—from discovery to playback.
          </p>
        </div>
        <div className="marketing-feature-grid">
          <article className="marketing-feature-card">
            <div className="marketing-feature-icon" aria-hidden>
              ✦
            </div>
            <h3 className="marketing-feature-title">Library & AI stories</h3>
            <p className="muted marketing-feature-copy">
              Browse curated tales or generate new ones with your child&apos;s name and favorite themes.
            </p>
          </article>
          <article className="marketing-feature-card">
            <div className="marketing-feature-icon" aria-hidden>
              ◉
            </div>
            <h3 className="marketing-feature-title">Listen together</h3>
            <p className="muted marketing-feature-copy">
              Stream narration designed for clarity and warmth—perfect for bedtime or quiet time.
            </p>
          </article>
          <article className="marketing-feature-card">
            <div className="marketing-feature-icon" aria-hidden>
              ◆
            </div>
            <h3 className="marketing-feature-title">Parent-first design</h3>
            <p className="muted marketing-feature-copy">
              Professional safeguards, clear consent, and controls you expect from a family product.
            </p>
          </article>
        </div>
      </section>

      <section className="marketing-cta-band">
        <div className="marketing-cta-inner">
          <h2 className="marketing-cta-title">Ready when you are</h2>
          <p className="marketing-cta-copy">Open the portal on any device—same account, same library.</p>
          <Link to="/login" className="btn btn-primary">
            Sign in with email code
          </Link>
        </div>
      </section>

      <footer className="marketing-footer">
        <nav className="marketing-footer-nav" aria-label="Footer">
          <Link to="/login">Sign in</Link>
          <span className="marketing-footer-dot" aria-hidden>
            ·
          </span>
          <Link to="/register">Register</Link>
          <span className="marketing-footer-dot" aria-hidden>
            ·
          </span>
          <Link to="/privacy">Privacy</Link>
          <span className="marketing-footer-dot" aria-hidden>
            ·
          </span>
          <Link to="/terms">Terms</Link>
        </nav>
        <p className="marketing-footer-tagline muted">Listen · Learn · Shine</p>
      </footer>
    </div>
  );
}
