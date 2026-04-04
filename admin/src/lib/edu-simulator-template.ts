/**
 * Scaffold for a Digital Safety branching episode. Replace placeholder audio URLs with real CDN paths after narration.
 * Matches mobile [InteractiveStoryGraph] and admin [lintInteractiveGraphJson].
 */

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

export const DIGITAL_SAFETY_SIMULATOR_THEME = "Learn · Simulator · Digital Safety";

export const DIGITAL_SAFETY_SIMULATOR_POST_MISSION =
  "At dinner tonight, pick one urgent message you would never act on without checking with a second person or official channel.";

/** Pretty-printed interactive_graph JSON (three-way fork → three outcomes). */
export const DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE = `{
  "startSegmentId": "intro",
  "overlayStyle": "WHATSAPP_CHAT",
  "segments": {
    "intro": {
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
      "audioUrl": "https://cdn.tamixa.app/library/sim/_REPLACE_/outcome_stress.mp3",
      "choices": []
    },
    "analytical_path": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/_REPLACE_/outcome_verify.mp3",
      "choices": []
    },
    "collaborative_path": {
      "audioUrl": "https://cdn.tamixa.app/library/sim/_REPLACE_/outcome_family.mp3",
      "choices": []
    }
  }
}`;
