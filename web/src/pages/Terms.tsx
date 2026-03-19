/**
 * Terms of Service — short summary with link to Privacy Policy.
 */
import { Link } from "react-router-dom";

export default function Terms() {
  return (
    <main className="page" role="main" aria-label="Terms of Service">
      <div style={{ maxWidth: 720, margin: "0 auto", padding: "1.5rem 1rem" }}>
        <h1>Terms of Service</h1>
        <p className="muted">Last updated: March 2026.</p>
        <p>
          By creating an account, you agree to use Tamixa in accordance with these terms. You must be a parent or
          guardian (18 or older) to register. You are responsible for the use of the service under your account and for
          ensuring that any child data you provide is given with your consent.
        </p>
        <p>
          Our <Link to="/privacy">Privacy Policy</Link> describes how we collect, use, retain, and protect your and your
          child’s data, including your right to request a copy of your data and our data retention and minimization
          practices.
        </p>
        <p>
          We may update these terms and the Privacy Policy from time to time; we will notify you of material changes
          where required by law. Continued use of the service after changes constitutes acceptance.
        </p>
        <p className="muted" style={{ marginTop: "2rem" }}>
          <Link to="/">Back to home</Link> · <Link to="/privacy">Privacy Policy</Link> · <Link to="/login">Log in</Link>
        </p>
      </div>
    </main>
  );
}
