# Evaluate AI output (per-output)

**Goal:** Score a **single** AI-produced artifact and produce actionable feedback for humans and for the **learning loop** (prompt/rule/context updates).

## Inputs

- The **output** to evaluate (paste or path)
- **Context**: task type, language/locale if multilingual, related spec or diff
- Optional: which rubrics apply (story, multilingual, code/tests, PR)

## Instructions

1. Classify **output type**: story | prompt | code | tests | PR | other.  
2. Score **each applicable dimension** 0–5 using the Evaluation agent rubrics in [.cursor/rules/evaluation-agent.mdc](../../.cursor/rules/evaluation-agent.mdc).  
3. List **blockers** (must fix) vs **nits** (optional).  
4. **Learning loop:** propose specific updates: prompt file, Cursor rule, context pack, or ADR—**one primary recommendation** minimum if any major dimension scores **below 4**.  
5. State **human-only** checks (cultural nuance, legal, brand).

## Output format (use this template)

```text
Type:
Scores: { dimension: 0-5, ... }
Blockers:
Nits:
Recommended durable update (learning loop):
Human follow-up:
```

## Related

- [docs/AI_EVALUATION_SYSTEM.md](../../docs/AI_EVALUATION_SYSTEM.md)  
- [docs/COST_GOVERNANCE.md](../../docs/COST_GOVERNANCE.md) (cheap model OK for routine eval; premium for high-stakes content)  
