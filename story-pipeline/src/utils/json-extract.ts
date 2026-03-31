/**
 * Models sometimes wrap JSON in markdown fences. Extract the first JSON object substring.
 */

export function stripMarkdownCodeFence(text: string): string {
  const trimmed = text.trim();
  const fence = /^```(?:json)?\s*\n?([\s\S]*?)\n?```$/im.exec(trimmed);
  if (fence) return fence[1].trim();
  return trimmed;
}

export function parseJsonObject(raw: string): unknown {
  const cleaned = stripMarkdownCodeFence(raw);
  try {
    return JSON.parse(cleaned);
  } catch {
    // Try to locate outermost { ... } for repair
    const start = cleaned.indexOf("{");
    const end = cleaned.lastIndexOf("}");
    if (start >= 0 && end > start) {
      return JSON.parse(cleaned.slice(start, end + 1));
    }
    throw new Error("Failed to parse JSON from model output");
  }
}
