/**
 * Scaffold for a Digital Safety branching episode. Replace placeholder audio URLs with real CDN paths after narration.
 * Matches mobile [InteractiveStoryGraph] and admin [lintInteractiveGraphJson].
 */

import { SIMULATOR_THEME_PREFIX } from "./story-interactive-conventions";

/**
 * Full HTTPS URL to the printable decision journal (parent web or CDN).
 * Set in admin `.env.local`: `NEXT_PUBLIC_DECISION_JOURNAL_URL=https://your-parent-app/decision-journal.html`
 * When set, "Digital Safety template" can pre-fill post-episode resource link.
 */
export function getDefaultDecisionJournalUrl(): string {
  if (typeof process === "undefined") return "";
  const u = process.env.NEXT_PUBLIC_DECISION_JOURNAL_URL?.trim();
  if (!u) return "";
  return /^https?:\/\//i.test(u) ? u : "";
}

export const DIGITAL_SAFETY_SIMULATOR_THEME = `${SIMULATOR_THEME_PREFIX} · Digital Safety`;

export const DIGITAL_SAFETY_SIMULATOR_POST_MISSION =
  "At dinner tonight, pick one urgent message you would never act on without checking with a second person or official channel.";

/** Pretty-printed interactive_graph JSON (three-way fork → three outcomes). */
export const DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE = `{
  "startSegmentId": "intro",
  "overlayStyle": "WHATSAPP_CHAT",
  "segments": {
    "intro": {
      "text": "You get a message that feels urgent: your electricity might be cut off in an hour unless you act fast. Scammers use panic to make you skip thinking. What do you do next?",
      "audioUrl": "https://cdn.tamixa.app/library/sim/_REPLACE_/intro.mp3",
      "choices": [
        {
          "id": "panic_install",
          "label": "Install the app now — power will cut in an hour",
          "nextSegmentId": "high_stress_path",
          "skillDeltas": { "DIGITAL_WISDOM": -2 }
        },
        {
          "id": "verify_caller",
          "label": "Hang up and call the official electricity number from the bill",
          "nextSegmentId": "analytical_path",
          "skillDeltas": { "DIGITAL_WISDOM": 2 }
        },
        {
          "id": "ask_family",
          "label": "Ask a family member who handles bills to verify",
          "nextSegmentId": "collaborative_path",
          "skillDeltas": { "DIGITAL_WISDOM": 1 }
        }
      ]
    },
    "high_stress_path": {
      "text": "You rushed and installed the app. That often opens the door to stolen passwords and fake payments. Next time, pause—even when it feels urgent—and check with a trusted adult or the real number on your electricity bill.",
      "audioUrl": "https://cdn.tamixa.app/library/sim/_REPLACE_/outcome_stress.mp3",
      "choices": []
    },
    "analytical_path": {
      "text": "You hung up and used the official number from your bill. Scammers rely on panic; verifying through a channel you already trust keeps your account and money safer.",
      "audioUrl": "https://cdn.tamixa.app/library/sim/_REPLACE_/outcome_verify.mp3",
      "choices": []
    },
    "collaborative_path": {
      "text": "You asked your family. Talking it through with someone you trust is a strong habit. Together you can double-check urgent messages before anyone taps a link or installs anything new.",
      "audioUrl": "https://cdn.tamixa.app/library/sim/_REPLACE_/outcome_family.mp3",
      "choices": []
    }
  }
}`;
