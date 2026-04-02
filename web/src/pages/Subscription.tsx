import { useState, useEffect } from "react";
import { SubscriptionSkeleton } from "../components/Skeleton";
import {
  getSubscription,
  getUsage,
  cancelSubscription,
  validateReferralCode,
  createCheckoutSession,
  type Subscription,
  type Usage,
  type ReferralCodeValidateResponse,
} from "../lib/api";

export default function Subscription() {
  const [sub, setSub] = useState<Subscription | null>(null);
  const [usage, setUsage] = useState<Usage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [canceling, setCanceling] = useState(false);
  const [referralInput, setReferralInput] = useState("");
  const [appliedReferral, setAppliedReferral] = useState<ReferralCodeValidateResponse | null>(null);
  const [referralError, setReferralError] = useState<string | null>(null);
  const [upgrading, setUpgrading] = useState(false);
  const [cancelConfirm, setCancelConfirm] = useState(false);

  useEffect(() => {
    Promise.all([getSubscription(), getUsage()])
      .then(([s, u]) => {
        setSub(s);
        setUsage(u);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleApplyReferral = async () => {
    const code = referralInput.trim().toUpperCase();
    if (!code) {
      setAppliedReferral(null);
      setReferralError(null);
      return;
    }
    setReferralError(null);
    try {
      const result = await validateReferralCode(code);
      if (result.valid && result.shopName != null && result.offerPercent != null) {
        setAppliedReferral(result);
      } else {
        setAppliedReferral(null);
        setReferralError("Invalid or expired code");
      }
    } catch {
      setAppliedReferral(null);
      setReferralError("Could not validate code");
    }
  };

  const handleClearReferral = () => {
    setAppliedReferral(null);
    setReferralError(null);
  };

  const handleUpgrade = async () => {
    setUpgrading(true);
    setError("");
    try {
      const referralCode = (appliedReferral?.valid && appliedReferral.shortcode)
        ? appliedReferral.shortcode
        : (referralInput.trim().toUpperCase() || undefined);
      const checkoutUrl = await createCheckoutSession({ referralCode });
      if (checkoutUrl) {
        window.location.href = checkoutUrl;
        return;
      }
      setError("Checkout is not available. Try again later.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to start checkout");
    } finally {
      setUpgrading(false);
    }
  };

  const handleCancel = async () => {
    if (!cancelConfirm) {
      setCancelConfirm(true);
      return;
    }
    setCancelConfirm(false);
    setCanceling(true);
    setError("");
    try {
      await cancelSubscription();
      const s = await getSubscription();
      setSub(s);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to cancel");
    } finally {
      setCanceling(false);
    }
  };

  const showUpgrade = sub && (sub.plan === "FREE" || !sub.isEntitledToUnlimitedStories);

  return (
    <div className="page app-surface-page">
      <header className="app-page-header">
        <h1>Subscription</h1>
        <p className="page-subtitle">Your plan and usage</p>
      </header>

      {error && <p className="error">{error}</p>}

      {loading ? (
        <SubscriptionSkeleton />
      ) : (
        <>
          {sub && (
            <section className="page-section generate-section">
              <div className="section-card-tamixa">
                <div className="section-card-tamixa-header">
                  <h2>Plan</h2>
                  <p className="section-card-subtitle">Current plan and status</p>
                </div>
                <div className="section-card-tamixa-body subscription-plan-card" style={{ marginTop: 0, border: "none", borderRadius: 0 }}>
                  <p><strong>{sub.plan}</strong> — Status: {sub.status}</p>
                  {sub.currentPeriodEnd && <p className="muted">Period ends: {new Date(sub.currentPeriodEnd).toLocaleDateString()}</p>}
                  {sub.cancelAtPeriodEnd && <p className="error">Cancels at end of period</p>}
                  <p>Unlimited stories: {sub.isEntitledToUnlimitedStories ? "Yes" : "No"}</p>
                  <p>Max children: {sub.maxChildren}</p>
                  {showUpgrade ? (
                    <>
                      <div className="subscription-referral-panel">
                        <p className="section-card-subtitle" style={{ marginBottom: "0.5rem" }}>Referral code (e.g. AMAZ5, SHOPSTOP10)</p>
                        <div className="field" style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap", alignItems: "center" }}>
                          <input
                            type="text"
                            value={referralInput}
                            onChange={(e) => setReferralInput(e.target.value.toUpperCase().slice(0, 32))}
                            placeholder="AMAZ5"
                            style={{ maxWidth: "12rem", textTransform: "uppercase" }}
                          />
                          <button type="button" className="btn btn-outline" onClick={handleApplyReferral}>
                            Apply
                          </button>
                        </div>
                        {appliedReferral?.valid && appliedReferral.shopName != null && appliedReferral.offerPercent != null && (
                          <p style={{ marginTop: "0.5rem", color: "var(--primary)", display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: "0.5rem" }}>
                            <span>{appliedReferral.offerPercent}% off — {appliedReferral.shopName}</span>
                            <button type="button" className="btn btn-outline btn-sm" onClick={handleClearReferral}>Clear</button>
                          </p>
                        )}
                        {referralError && <p className="error" style={{ marginTop: "0.5rem", fontSize: "0.9rem" }}>{referralError}</p>}
                      </div>
                      <button
                        type="button"
                        className="btn btn-primary"
                        onClick={handleUpgrade}
                        disabled={upgrading}
                        style={{ marginTop: "1rem" }}
                      >
                        {upgrading ? "Opening checkout…" : "Upgrade — ₹99/month"}
                      </button>
                    </>
                  ) : null}
                  {!sub.cancelAtPeriodEnd && sub.plan !== "FREE" && (
                    <div style={{ marginTop: "1rem" }}>
                      {cancelConfirm ? (
                        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", flexWrap: "wrap" }}>
                          <span style={{ fontSize: "0.9rem" }}>Cancel at end of period? You'll keep access until then.</span>
                          <button type="button" className="btn btn-outline" onClick={handleCancel} disabled={canceling}>
                            {canceling ? "Canceling…" : "Yes, cancel"}
                          </button>
                          <button type="button" className="btn btn-outline" onClick={() => setCancelConfirm(false)} disabled={canceling}>
                            No, keep plan
                          </button>
                        </div>
                      ) : (
                        <button type="button" className="btn btn-outline" onClick={handleCancel} disabled={canceling}>
                          Cancel at period end
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </section>
          )}

          {usage && (
            <section className="page-section generate-section">
              <div className="section-card-tamixa">
                <div className="section-card-tamixa-header">
                  <h2>Usage ({usage.month})</h2>
                  <p className="section-card-subtitle">Stories and voice generations this month</p>
                </div>
                <div className="section-card-tamixa-body subscription-plan-card" style={{ marginTop: 0, border: "none", borderRadius: 0 }}>
                  <p>Stories: {usage.storiesUsed} {usage.storiesLimit != null ? `/ ${usage.storiesLimit}` : "(unlimited)"}</p>
                  <p>Voice generations: {usage.voiceUsed} / {usage.voiceLimit}</p>
                </div>
              </div>
            </section>
          )}
        </>
      )}
    </div>
  );
}
