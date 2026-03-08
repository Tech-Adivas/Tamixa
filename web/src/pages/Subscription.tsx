import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { SubscriptionSkeleton } from "../components/Skeleton";
import { getSubscription, getUsage, cancelSubscription, type Subscription, type Usage } from "../lib/api";

export default function Subscription() {
  const {} = useAuth();
  const [sub, setSub] = useState<Subscription | null>(null);
  const [usage, setUsage] = useState<Usage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [canceling, setCanceling] = useState(false);

  useEffect(() => {
    Promise.all([getSubscription(), getUsage()])
      .then(([s, u]) => {
        setSub(s);
        setUsage(u);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const handleCancel = async () => {
    if (!confirm("Cancel at end of period? You'll keep access until then.")) return;
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

  return (
    <div className="page">
      <header className="page-header dashboard-header">
        <div>
          <h1>Subscription</h1>
          <p className="page-subtitle muted">Your plan and usage</p>
        </div>
      </header>

      {error && <p className="error">{error}</p>}

      {loading ? (
        <SubscriptionSkeleton />
      ) : (
        <>
          {sub && (
            <section className="page-section generate-section">
              <h2 className="page-section-title">Plan</h2>
              <div className="page-section-card subscription-plan-card">
                <p><strong>{sub.plan}</strong> — Status: {sub.status}</p>
              {sub.currentPeriodEnd && <p className="muted">Period ends: {new Date(sub.currentPeriodEnd).toLocaleDateString()}</p>}
              {sub.cancelAtPeriodEnd && <p className="error">Cancels at end of period</p>}
              <p>Unlimited stories: {sub.isEntitledToUnlimitedStories ? "Yes" : "No"}</p>
              <p>Max children: {sub.maxChildren}</p>
              {sub.plan === "FREE" || !sub.isEntitledToUnlimitedStories ? (
                <p className="muted" style={{ marginTop: "1rem" }}>
                  Upgrade to Premium for unlimited stories and voice cloning. Payment integration coming soon.
                </p>
              ) : null}
              {!sub.cancelAtPeriodEnd && sub.plan !== "FREE" && (
                <button type="button" className="btn btn-outline" onClick={handleCancel} disabled={canceling} style={{ marginTop: "1rem" }}>
                  {canceling ? "Canceling…" : "Cancel at period end"}
                </button>
              )}
              </div>
            </section>
          )}

          {usage && (
            <section className="page-section generate-section">
              <h2 className="page-section-title">Usage ({usage.month})</h2>
              <div className="page-section-card subscription-plan-card">
                <p>Stories: {usage.storiesUsed} {usage.storiesLimit != null ? `/ ${usage.storiesLimit}` : "(unlimited)"}</p>
                <p>Voice generations: {usage.voiceUsed} / {usage.voiceLimit}</p>
              </div>
            </section>
          )}
        </>
      )}
    </div>
  );
}
