/**
 * Empty state for lists (library, my stories, etc.).
 * Illustration emoji + message + optional action.
 */
interface EmptyStateProps {
  emoji?: string;
  title: string;
  description?: string;
  action?: { label: string; onClick: () => void };
  className?: string;
}

export function EmptyState({
  emoji = "📖",
  title,
  description,
  action,
  className = "",
}: EmptyStateProps) {
  return (
    <div
      className={`empty-state ${className}`}
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "2.5rem 1.5rem",
        textAlign: "center",
      }}
    >
      <div
        style={{
          fontSize: "2.5rem",
          marginBottom: "0.75rem",
          lineHeight: 1,
        }}
        aria-hidden
      >
        {emoji}
      </div>
      <h3
        style={{
          margin: 0,
          fontSize: "1rem",
          fontWeight: 600,
          color: "var(--tamixa-cream, #f5f5f5)",
        }}
      >
        {title}
      </h3>
      {description && (
        <p className="muted" style={{ margin: "0.5rem 0 0", maxWidth: 280 }}>
          {description}
        </p>
      )}
      {action && (
        <button
          type="button"
          className="btn btn-primary"
          onClick={action.onClick}
          style={{ marginTop: "1.25rem" }}
        >
          {action.label}
        </button>
      )}
    </div>
  );
}
