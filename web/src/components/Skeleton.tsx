/** Loading skeleton for lists and cards */
export function Skeleton({ className = "", style }: { className?: string; style?: React.CSSProperties }) {
  return (
    <div
      className={`skeleton ${className}`}
      style={style}
      role="status"
      aria-label="Loading"
    />
  );
}

export function StoryCardSkeleton() {
  return (
    <li className="story-card story-card-flex">
      <div className="story-card-row" style={{ flex: 1 }}>
        <div className="story-card-cover">
          <div className="skeleton" style={{ width: "100%", height: "100%", margin: 0 }} />
        </div>
        <div className="story-card-main-text" style={{ flex: 1 }}>
          <Skeleton style={{ width: "70%", height: 18, marginBottom: 6 }} />
          <Skeleton style={{ width: "50%", height: 14 }} />
        </div>
      </div>
      <div className="story-card-actions">
        <Skeleton style={{ width: 72, height: 32 }} />
        <Skeleton style={{ width: 36, height: 32 }} />
      </div>
    </li>
  );
}

export function StoryListSkeleton({ count = 5 }: { count?: number }) {
  return (
    <ul className="story-list story-list-cards" role="list">
      {Array.from({ length: count }).map((_, i) => (
        <StoryCardSkeleton key={i} />
      ))}
    </ul>
  );
}

export function SubscriptionSkeleton() {
  return (
    <div className="subscription-skeleton" aria-label="Loading subscription">
      <section className="generate-section">
        <Skeleton style={{ width: 80, height: 24, marginBottom: 12 }} />
        <Skeleton style={{ width: "100%", height: 20, marginBottom: 8 }} />
        <Skeleton style={{ width: "60%", height: 20, marginBottom: 8 }} />
        <Skeleton style={{ width: 120, height: 20 }} />
      </section>
      <section className="generate-section">
        <Skeleton style={{ width: 100, height: 24, marginBottom: 12 }} />
        <Skeleton style={{ width: "100%", height: 20, marginBottom: 8 }} />
        <Skeleton style={{ width: "70%", height: 20 }} />
      </section>
    </div>
  );
}
