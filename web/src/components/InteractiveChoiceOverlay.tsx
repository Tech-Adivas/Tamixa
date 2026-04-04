import type { InteractiveChoice as InteractiveChoiceType } from "../lib/interactiveStoryGraph";

export interface InteractiveChoiceOverlayProps {
  choices: InteractiveChoiceType[];
  overlayStyle?: string | null;
  onChoice: (choice: InteractiveChoiceType) => void;
}

function normalizeStyle(raw: string | null | undefined): string {
  return raw?.trim().toUpperCase() || "WHATSAPP_CHAT";
}

/** WhatsApp-style or card-style bubbles — aligned with mobile `InteractiveChoiceOverlay`. */
export default function InteractiveChoiceOverlay({
  choices,
  overlayStyle,
  onChoice,
}: InteractiveChoiceOverlayProps) {
  const style = normalizeStyle(overlayStyle);
  const cards = style === "RESEARCH_CARDS" || style === "CARDS";

  return (
    <div
      className={`interactive-choice-overlay${cards ? " interactive-choice-overlay--cards" : ""}`}
      role="dialog"
      aria-modal="true"
      aria-labelledby="interactive-choice-heading"
    >
      <div className="interactive-choice-overlay__panel">
        <h2 id="interactive-choice-heading" className="interactive-choice-overlay__title">
          What should happen next?
        </h2>
        <div className="interactive-choice-overlay__choices">
          {choices.map((c) => (
            <button
              key={c.id}
              type="button"
              className="interactive-choice-overlay__bubble"
              onClick={() => onChoice(c)}
            >
              {c.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
