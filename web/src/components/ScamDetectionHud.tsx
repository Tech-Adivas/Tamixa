import type { ScamSignal } from "../lib/interactiveStoryGraph";

const LABELS: Record<ScamSignal, { title: string; detail: string }> = {
  urgency: {
    title: "Urgency",
    detail: "Rushing you to pay, share OTP, or decide in seconds.",
  },
  hidden_fees: {
    title: "Hidden fees",
    detail: "Extra charges buried in the fine print or “processing.”",
  },
  permissions: {
    title: "Permissions",
    detail: "Asking for SMS, contacts, or screen access they should not need.",
  },
};

export interface ScamDetectionHudProps {
  signals: ScamSignal[] | undefined;
}

/** Live red-flag strip for Digital Survival — sits under the player during the segment. */
export default function ScamDetectionHud({ signals }: ScamDetectionHudProps) {
  if (!signals?.length) return null;
  return (
    <div className="scam-detection-hud" role="region" aria-label="Scam detection hints">
      <div className="scam-detection-hud__title-row">
        <span className="scam-detection-hud__pulse" aria-hidden />
        <span className="scam-detection-hud__title">Scam-detection</span>
        <span className="scam-detection-hud__subtitle muted">Watch these red flags in this scene</span>
      </div>
      <ul className="scam-detection-hud__chips">
        {signals.map((s) => (
          <li key={s} className="scam-detection-hud__chip">
            <strong>{LABELS[s].title}</strong>
            <span className="scam-detection-hud__chip-detail">{LABELS[s].detail}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
