export interface TimePassageSummaryModalProps {
  open: boolean;
  title: string;
  lines: string[];
  onClose: () => void;
}

/** “3 months later” consequence skip summary. */
export default function TimePassageSummaryModal({ open, title, lines, onClose }: TimePassageSummaryModalProps) {
  if (!open) return null;
  return (
    <div className="time-passage-modal" role="dialog" aria-modal="true" aria-labelledby="time-passage-title">
      <div className="time-passage-modal__card">
        <h2 id="time-passage-title" className="time-passage-modal__title">
          {title}
        </h2>
        <ul className="time-passage-modal__list">
          {lines.map((line, i) => (
            <li key={i}>{line}</li>
          ))}
        </ul>
        <button type="button" className="btn btn-primary time-passage-modal__ok" onClick={onClose}>
          Back to the story
        </button>
      </div>
    </div>
  );
}
