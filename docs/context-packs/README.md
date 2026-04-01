# Language context packs (multilingual advantage)

**Context packs** carry **linguistic**, **cultural**, **tone**, and **TTS** constraints per language or locale. They complement generic i18n strings and feed both **generation** and **evaluation** ([evaluation-agent.mdc](../../.cursor/rules/evaluation-agent.mdc)).

## Pack contents (each language)

- **Script & typography** — expected script, common pitfalls, mixed-language UI rules  
- **Linguistic rules** — formality, honorifics, child-directed tone, things to avoid  
- **Cultural norms** — festivals, settings, family idioms (reviewed by native speakers)  
- **Tone patterns** — Tamixa voice in this language  
- **TTS constraints** — sentence length, pause markers, words that TTS mispronounces  
- **Glossary** — product terms (character names, “story”, “parent”) — consistent translations  

## Layout

Use one file per primary locale, e.g. `ta.md`, `hi.md`, `te.md`, `kn.md`, or region variants if needed.

Start from [_TEMPLATE.md](_TEMPLATE.md).

## Ownership

Each pack has a **locale owner** (native or professional reviewer). Set `Review-by:` per [CONTEXT_LIFECYCLE.md](../CONTEXT_LIFECYCLE.md).

## Integration

- **Product:** Reference pack id in prompts or admin metadata when generating/evaluating stories.  
- **SDLC:** Evaluation agent compares drafts against the pack.  

## Related

- [.github/instructions/localization.instructions.md](../../.github/instructions/localization.instructions.md)  
- [TAMIXA_AI_CONTROL_PLANE.md](../TAMIXA_AI_CONTROL_PLANE.md)  
