/**
 * Branded loading state for route guards (auth check).
 * Uses Tamixa gradient and logo for consistent feel.
 */
export function AppLoading() {
  return (
    <div
      className="page prime-page"
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        minHeight: "60vh",
        gap: "1rem",
      }}
      aria-live="polite"
      aria-busy="true"
    >
      <div
        className="app-loading-spinner"
        style={{
          width: 40,
          height: 40,
          border: "3px solid rgba(124, 58, 237, 0.2)",
          borderTopColor: "var(--tamixa-purple, #7C3AED)",
          borderRadius: "50%",
          animation: "app-loading-spin 0.8s linear infinite",
        }}
        aria-hidden
      />
      <p className="muted" style={{ margin: 0, fontSize: "0.9rem" }}>
        Loading…
      </p>
      <style>{`
        @keyframes app-loading-spin {
          to { transform: rotate(360deg); }
        }
        @media (prefers-reduced-motion: reduce) {
          .app-loading-spinner { animation: none; opacity: 0.7; }
        }
      `}</style>
    </div>
  );
}
