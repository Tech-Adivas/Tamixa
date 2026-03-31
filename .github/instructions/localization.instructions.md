---
applyTo: "mobile/**,backend/**,web/**,admin/**"
---

# Localization and languages

- Treat **Indian languages** as first-class: layout, fonts, string resources, and API locale fields — not as afterthought translations.
- Backend: validate **language/locale** against **allowlists**; avoid passing raw user input into queries or commands.
- Mobile/Web/Admin: preserve **RTL/LTR** and **script** correctness when touching copy or formatting.
- Story and narration flows: coordinate with **content safety** and **moderation** rules ([.cursor/rules/prompt-story-agent.mdc](../../.cursor/rules/prompt-story-agent.mdc)).
