export interface MissionCardOverlayProps {
  missionText?: string | null;
  resourceUrl?: string | null;
  onDismiss: () => void;
}

/** Post-episode family mission — aligned with mobile `MissionCardOverlay`. */
export default function MissionCardOverlay({
  missionText,
  resourceUrl,
  onDismiss,
}: MissionCardOverlayProps) {
  const text = missionText?.trim() ?? "";
  const url = resourceUrl?.trim() ?? "";

  return (
    <div className="mission-card-overlay" role="dialog" aria-modal="true" aria-labelledby="mission-card-title">
      <div className="mission-card-overlay__card">
        <h2 id="mission-card-title" className="mission-card-overlay__title">
          Family mission
        </h2>
        <p className="mission-card-overlay__hint muted">
          Try this together after the story — no wrong answers.
        </p>
        {text ? (
          <div className="mission-card-overlay__body">
            <p className="mission-card-overlay__text">{text}</p>
          </div>
        ) : null}
        {url ? (
          <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn-secondary mission-card-overlay__link"
          >
            Open linked resource
          </a>
        ) : null}
        <button type="button" className="btn btn-primary mission-card-overlay__done" onClick={onDismiss}>
          Done
        </button>
      </div>
    </div>
  );
}
