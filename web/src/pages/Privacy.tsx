/**
 * Privacy Policy — compliance-ready summary.
 * Full policy: docs/COMPLIANCE.md (Data Classification, Retention, Child Data, etc.)
 */
import { Link } from "react-router-dom";

export default function Privacy() {
  return (
    <main className="page" role="main" aria-label="Privacy Policy">
      <div style={{ maxWidth: 720, margin: "0 auto", padding: "1.5rem 1rem" }}>
        <h1>Privacy Policy</h1>
        <p className="muted">Last updated: March 2026. Araro is an AI storytelling app for children (age 1–12).</p>

        <section aria-labelledby="your-rights">
          <h2 id="your-rights">Your rights</h2>
          <p>
            <strong>Request a copy of your data (data export).</strong> You can request a downloadable copy of the
            personal data we hold about you and your child profiles. In the app, go to <strong>Settings</strong> and use
            <strong> “Request data export”</strong>. We will prepare your data and provide a secure download link
            (typically within a few minutes). This supports your right to data portability under GDPR and similar laws.
          </p>
        </section>

        <section aria-labelledby="retention">
          <h2 id="retention">Data retention summary</h2>
          <p>We keep your data only as long as needed for the service or as required by law.</p>
          <ul>
            <li>Account and child profiles: until you delete your account or the child profile.</li>
            <li>Story content: up to 24 months, or until you delete the account/child (whichever is first).</li>
            <li>Voice recordings (if you use voice features): until you remove them or delete your account.</li>
            <li>Audit and security logs: 90 days for operations; up to 12 months for compliance.</li>
          </ul>
          <p>
            For the full retention policy and deletion process, see our Data Retention Policy (Section 6 in our
            compliance documentation, available on request or via in-app support).
          </p>
        </section>

        <section aria-labelledby="minimisation">
          <h2 id="minimisation">Data minimization</h2>
          <p>
            We collect and keep only what is necessary to provide the service and comply with the law. We do not use
            your or your child’s data for behavioural advertising, and we do not sell personal data. Child data is used
            only for age-appropriate story generation and optional voice features, as described below.
          </p>
        </section>

        <section aria-labelledby="child-ai">
          <h2 id="child-ai">How we use your child’s data for AI (story generation)</h2>
          <p>
            To create personalised stories, we send to our AI provider (OpenAI) only the minimum needed: your child’s{" "}
            <strong>first name</strong>, <strong>age</strong> (from date of birth), <strong>language</strong>, and the{" "}
            <strong>story theme</strong> you choose. We do not send your email, your child’s full identity, or any other
            identifiers. Our contract with the provider states that they do not use our data to train their models; we do
            not store your data on their systems beyond the immediate request and response. Story content may also be
            checked by a moderation API for safety; the same minimal-use and no-training rules apply.
          </p>
        </section>

        <section aria-labelledby="contact">
          <h2 id="contact">Contact and other rights</h2>
          <p>
            For access, correction, deletion, or complaints, use the in-app Settings (data export, account deletion) or
            contact us at the support address in the app. For the full compliance framework (GDPR, COPPA, DPDP, etc.),
            see <code>docs/COMPLIANCE.md</code> in our repository.
          </p>
        </section>

        <p className="muted" style={{ marginTop: "2rem" }}>
          <Link to="/">Back to home</Link> · <Link to="/login">Log in</Link> · <Link to="/register">Register</Link>
        </p>
      </div>
    </main>
  );
}
