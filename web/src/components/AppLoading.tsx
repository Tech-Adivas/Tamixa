/**
 * Branded loading state for route guards (auth check).
 */
export function AppLoading() {
  return (
    <div className="app-loading-screen" aria-live="polite" aria-busy="true">
      <div className="app-loading-screen-inner">
        <div className="app-loading-mark" aria-hidden>
          <img src="/tamixa-app-icon.png" alt="" width={48} height={48} className="app-loading-logo" />
        </div>
        <div className="app-loading-spinner" aria-hidden />
        <p className="app-loading-label">Setting the stage for story time…</p>
      </div>
    </div>
  );
}
