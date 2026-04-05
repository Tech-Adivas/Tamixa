import { useCallback, useState } from "react";
import {
  renderFamilyMissionCardPng,
  renderScamProofBadgePng,
  shareFamilyBridgeImage,
} from "../lib/familyBridgeShareImage";

export interface MissionCardOverlayProps {
  missionText?: string | null;
  resourceUrl?: string | null;
  /** Library story title for the share card header. */
  storyTitle?: string | null;
  /** When true, “Scam-Proof” badge share is offered (Life Readiness tech axis). */
  scamProofUnlocked?: boolean;
  onDismiss: () => void;
}

/** Post-episode Family Mission Card + Brag & Protect share (WhatsApp-friendly). */
export default function MissionCardOverlay({
  missionText,
  resourceUrl,
  storyTitle,
  scamProofUnlocked = false,
  onDismiss,
}: MissionCardOverlayProps) {
  const text = missionText?.trim() ?? "";
  const url = resourceUrl?.trim() ?? "";
  const [shareBusy, setShareBusy] = useState<"mission" | "badge" | null>(null);
  const [shareError, setShareError] = useState<string | null>(null);

  const shareMission = useCallback(async () => {
    if (!text) return;
    setShareError(null);
    setShareBusy("mission");
    try {
      const blob = await renderFamilyMissionCardPng({ missionLine: text, storyTitle });
      if (!blob) {
        setShareError("Could not create image on this device.");
        return;
      }
      await shareFamilyBridgeImage(
        blob,
        "We finished a Tamixa family mission — celebrating safe, curious choices together."
      );
    } catch (e) {
      setShareError(e instanceof Error ? e.message : "Share failed");
    } finally {
      setShareBusy(null);
    }
  }, [text, storyTitle]);

  const shareBadge = useCallback(async () => {
    setShareError(null);
    setShareBusy("badge");
    try {
      const blob = await renderScamProofBadgePng({
        badgeTitle: "Scam-Proof Senior",
        badgeSubtitle:
          "Building sharp instincts for messages, links, and “too urgent” calls — share the win with family.",
      });
      if (!blob) {
        setShareError("Could not create image on this device.");
        return;
      }
      await shareFamilyBridgeImage(
        blob,
        "Proud moment on Tamixa — we’re growing scam-smart habits as a family."
      );
    } catch (e) {
      setShareError(e instanceof Error ? e.message : "Share failed");
    } finally {
      setShareBusy(null);
    }
  }, []);

  return (
    <div className="mission-card-overlay" role="dialog" aria-modal="true" aria-labelledby="mission-card-title">
      <div className="mission-card-overlay__card mission-card-overlay__card--family-bridge">
        <h2 id="mission-card-title" className="mission-card-overlay__title">
          Family Mission Card
        </h2>
        <p className="mission-card-overlay__hint muted">
          One real-world challenge after the story — co-listen, then try it together. No grades, just connection.
        </p>
        {text ? (
          <div className="mission-card-overlay__body mission-card-overlay__body--card">
            <p className="mission-card-overlay__text">{text}</p>
          </div>
        ) : (
          <p className="muted mission-card-overlay__fallback">
            This episode does not include a mission line yet — check back after content is updated in the library.
          </p>
        )}
        <div className="mission-card-overlay__brag-row" aria-label="Share with family">
          {text ? (
            <button
              type="button"
              className="btn mission-card-overlay__brag-btn"
              disabled={shareBusy !== null}
              onClick={() => void shareMission()}
            >
              {shareBusy === "mission" ? "Preparing…" : "Brag & Protect — share mission card"}
            </button>
          ) : null}
          {scamProofUnlocked ? (
            <button
              type="button"
              className="btn btn-secondary mission-card-overlay__brag-btn"
              disabled={shareBusy !== null}
              onClick={() => void shareBadge()}
            >
              {shareBusy === "badge" ? "Preparing…" : "Share Scam-Proof badge"}
            </button>
          ) : null}
        </div>
        {shareError ? (
          <p className="mission-card-overlay__share-err muted" role="alert">
            {shareError}
          </p>
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
