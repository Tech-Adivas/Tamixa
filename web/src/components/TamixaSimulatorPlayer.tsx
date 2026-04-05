import { useEffect, useState } from "react";
import type { InteractiveChoice } from "../lib/interactiveStoryGraph";
import { prefetchInteractiveAudio } from "../lib/interactiveStoryGraph";
import type { SimulatorPlaybookKind } from "../lib/categoryLogicFactory";
import { clampMeter } from "../lib/categoryLogicFactory";

const REFLECTION_SECONDS = 10;

export interface SimulatorMetersState {
  authority: number;
  harmony: number;
  confidence: number;
}

export interface TamixaSimulatorPlayerProps {
  open: boolean;
  segmentKey: string;
  reflectionPoint: boolean;
  choices: InteractiveChoice[];
  overlayStyle?: string | null;
  branchPrefetchUrls: string[];
  onChoice: (choice: InteractiveChoice) => void;
  /** Category mini-game / UI kit */
  playbook: SimulatorPlaybookKind;
  /** Live meters (leadership + communication playbooks). */
  meters: SimulatorMetersState;
}

function normalizeStyle(raw: string | null | undefined): string {
  return raw?.trim().toUpperCase() || "WHATSAPP_CHAT";
}

function isCardsOverlay(overlayStyle: string | null | undefined): boolean {
  const s = normalizeStyle(overlayStyle);
  return s === "RESEARCH_CARDS" || s === "CARDS";
}

function defaultRiskWhy(): string {
  return "Think twice — this path can be risky online. When unsure, check with a trusted adult.";
}

/**
 * Learn · Simulator decision layer: reflection countdown, WhatsApp-style bubbles, and optional category UI.
 */
export default function TamixaSimulatorPlayer({
  open,
  segmentKey,
  reflectionPoint,
  choices,
  overlayStyle,
  branchPrefetchUrls,
  onChoice,
  playbook,
  meters,
}: TamixaSimulatorPlayerProps) {
  const cards = isCardsOverlay(overlayStyle);
  const [phase, setPhase] = useState<"reflection" | "choices">("choices");
  const [reflectSecondsLeft, setReflectSecondsLeft] = useState(REFLECTION_SECONDS);
  const [hoverChoiceId, setHoverChoiceId] = useState<string | null>(null);
  const [focusChoiceId, setFocusChoiceId] = useState<string | null>(null);
  const [shortcutGate, setShortcutGate] = useState<InteractiveChoice | null>(null);

  useEffect(() => {
    if (!open) return;
    void prefetchInteractiveAudio(branchPrefetchUrls);
  }, [open, branchPrefetchUrls]);

  useEffect(() => {
    if (!open) {
      setPhase("choices");
      setReflectSecondsLeft(REFLECTION_SECONDS);
      setHoverChoiceId(null);
      setFocusChoiceId(null);
      setShortcutGate(null);
      return;
    }
    if (!reflectionPoint) {
      setPhase("choices");
      setReflectSecondsLeft(REFLECTION_SECONDS);
      return;
    }
    setPhase("reflection");
    setReflectSecondsLeft(REFLECTION_SECONDS);
    let remaining = REFLECTION_SECONDS;
    const id = window.setInterval(() => {
      remaining -= 1;
      setReflectSecondsLeft(remaining);
      if (remaining <= 0) {
        window.clearInterval(id);
        setPhase("choices");
      }
    }, 1000);
    return () => window.clearInterval(id);
  }, [open, segmentKey, reflectionPoint]);

  const activeTipId = hoverChoiceId ?? focusChoiceId;
  const activeChoice = choices.find((c) => c.id === activeTipId);
  const showMicroBudget =
    playbook === "business" &&
    choices.some((c) => c.capitalRemaining !== undefined || c.estimatedProfit !== undefined);

  const requestChoice = (ch: InteractiveChoice) => {
    if (playbook === "ethics" && ch.isShortcut === true) {
      setShortcutGate(ch);
      return;
    }
    onChoice(ch);
  };

  if (!open) return null;

  const overlayClass = cards
    ? "interactive-choice-overlay interactive-choice-overlay--cards tamixa-simulator-overlay tamixa-simulator-overlay--cards"
    : "interactive-choice-overlay tamixa-simulator-overlay tamixa-simulator-overlay--whatsapp";

  const headingId = phase === "reflection" ? "tamixa-simulator-reflection" : "tamixa-simulator-choices";

  const a = clampMeter(meters.authority);
  const h = clampMeter(meters.harmony);
  const conf = clampMeter(meters.confidence);

  return (
    <div
      className={overlayClass}
      role="dialog"
      aria-modal="true"
      aria-labelledby={headingId}
    >
      <div className="tamixa-simulator-overlay__thread">
        {!cards ? (
          <header className="tamixa-simulator-overlay__header" aria-hidden>
            <div className="tamixa-simulator-overlay__avatar" />
            <div>
              <div className="tamixa-simulator-overlay__title-row">
                <span className="tamixa-simulator-overlay__name">Tamixa</span>
              </div>
              <p className="tamixa-simulator-overlay__status">online · practice story</p>
            </div>
          </header>
        ) : null}

        <div className="tamixa-simulator-overlay__panel">
          {shortcutGate ? (
            <div className="tamixa-category-ethics-alert" role="alertdialog" aria-labelledby="ethics-shortcut-title">
              <h3 id="ethics-shortcut-title" className="tamixa-category-ethics-alert__title">
                Shortcut alert
              </h3>
              <p className="tamixa-category-ethics-alert__body">
                This choice skips careful research or honesty. Your <strong>Integrity</strong> may drop, and a{" "}
                <strong>consequence chapter</strong> can appear later in the story.
              </p>
              <div className="tamixa-category-ethics-alert__actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShortcutGate(null)}>
                  Go back
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={() => {
                    const ch = shortcutGate;
                    setShortcutGate(null);
                    if (ch) onChoice(ch);
                  }}
                >
                  I understand — continue
                </button>
              </div>
            </div>
          ) : null}

          {phase === "reflection" ? (
            <div className="tamixa-simulator-overlay__reflection-card" role="status" aria-live="polite">
              <p id="tamixa-simulator-reflection" className="tamixa-simulator-overlay__reflection-text">
                Discuss this with your family...
              </p>
              <div className="tamixa-simulator-overlay__reflection-count" aria-label="Seconds remaining">
                {reflectSecondsLeft}
              </div>
              <p className="tamixa-simulator-overlay__reflection-sub muted">A short pause before you choose what happens next.</p>
            </div>
          ) : (
            <>
              {playbook === "digitalSafety" ? (
                <div className="tamixa-category-redflag" role="region" aria-label="Red flag detector">
                  <span className="tamixa-category-redflag__badge">Red flag detector</span>
                  <span className="tamixa-category-redflag__hint">Hover or focus a choice to see why it might be risky.</span>
                </div>
              ) : null}

              {playbook === "leadership" ? (
                <div className="tamixa-category-meters tamixa-category-meters--dual" role="region" aria-label="Relationship meter">
                  <div className="tamixa-category-meter">
                    <div className="tamixa-category-meter__label-row">
                      <span>Authority</span>
                      <span className="tamixa-category-meter__value">{Math.round(a)}</span>
                    </div>
                    <div className="tamixa-category-meter__track" aria-hidden>
                      <div className="tamixa-category-meter__fill tamixa-category-meter__fill--authority" style={{ width: `${a}%` }} />
                    </div>
                  </div>
                  <div className="tamixa-category-meter">
                    <div className="tamixa-category-meter__label-row">
                      <span>Harmony</span>
                      <span className="tamixa-category-meter__value">{Math.round(h)}</span>
                    </div>
                    <div className="tamixa-category-meter__track" aria-hidden>
                      <div className="tamixa-category-meter__fill tamixa-category-meter__fill--harmony" style={{ width: `${h}%` }} />
                    </div>
                  </div>
                </div>
              ) : null}

              {playbook === "communication" ? (
                <div className="tamixa-category-meters" role="region" aria-label="Confidence meter">
                  <div className="tamixa-category-meter">
                    <div className="tamixa-category-meter__label-row">
                      <span>Confidence (clear, honest)</span>
                      <span className="tamixa-category-meter__value">{Math.round(conf)}</span>
                    </div>
                    <div className="tamixa-category-meter__track" aria-hidden>
                      <div
                        className="tamixa-category-meter__fill tamixa-category-meter__fill--confidence"
                        style={{ width: `${conf}%` }}
                      />
                    </div>
                  </div>
                  <p className="tamixa-category-meter__caption muted">Simple, clear answers build confidence faster than flashy or fake ones.</p>
                </div>
              ) : null}

              {showMicroBudget ? (
                <div className="tamixa-category-budget" role="region" aria-label="Micro budget">
                  <p className="tamixa-category-budget__title">Micro-budget</p>
                  <table className="tamixa-category-budget__table">
                    <thead>
                      <tr>
                        <th scope="col">Choice</th>
                        <th scope="col">Capital remaining</th>
                        <th scope="col">Est. profit</th>
                      </tr>
                    </thead>
                    <tbody>
                      {choices.map((c) => (
                        <tr key={c.id}>
                          <td>{c.label}</td>
                          <td>{c.capitalRemaining !== undefined ? c.capitalRemaining : "—"}</td>
                          <td>{c.estimatedProfit !== undefined ? c.estimatedProfit : "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}

              <h2 id="tamixa-simulator-choices" className="interactive-choice-overlay__title tamixa-simulator-overlay__choice-heading">
                What should happen next?
              </h2>

              {playbook === "digitalSafety" && activeChoice ? (
                <div className="tamixa-category-risk-tooltip" role="tooltip">
                  <strong>Why this can be a risk</strong>
                  <p>{activeChoice.riskWhy?.trim() ? activeChoice.riskWhy : defaultRiskWhy()}</p>
                </div>
              ) : null}

              <div className="interactive-choice-overlay__choices tamixa-simulator-overlay__choices">
                {choices.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    className="interactive-choice-overlay__bubble tamixa-simulator-overlay__bubble"
                    onClick={() => requestChoice(c)}
                    onMouseEnter={() => setHoverChoiceId(c.id)}
                    onMouseLeave={() => setHoverChoiceId(null)}
                    onFocus={() => setFocusChoiceId(c.id)}
                    onBlur={() => setFocusChoiceId(null)}
                  >
                    <span className="tamixa-simulator-overlay__bubble-tail" aria-hidden />
                    {playbook === "digitalSafety" ? (
                      <span className="tamixa-category-choice-row">
                        <span className="tamixa-category-redflag__dot" aria-hidden />
                        <span>{c.label}</span>
                      </span>
                    ) : (
                      c.label
                    )}
                    {playbook === "ethics" && c.isShortcut ? (
                      <span className="tamixa-category-shortcut-pill">Shortcut</span>
                    ) : null}
                  </button>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
