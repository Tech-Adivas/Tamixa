# Tamixa story pipeline (TypeScript)

Production-oriented **multilingual story generation** with a **four-stage prompt chain**, **Zod** schema validation, **safety / language / TTS-marker / title / repetition** checks, and an **LLM repair** loop with retries.

## Layout

```
story-pipeline/
├── README.md
├── package.json
├── tsconfig.json
├── src/
│   ├── config/languages.ts       # Supported langs + script hints
│   ├── constants/markers.ts      # Allowed TTS markers only
│   ├── types/pipeline-input.ts
│   ├── schemas/                  # Zod: PlannerPlan + StoryOutput
│   ├── prompts/                  # Template builders per stage
│   ├── validators/               # Deterministic checks + aggregate
│   ├── utils/                    # JSON extract, duration, memory, repair helpers
│   ├── services/
│   │   ├── llm-provider.ts       # Interface + OpenAI-compatible + mock
│   │   └── story-pipeline-orchestrator.ts
│   └── index.ts
└── examples/
    ├── test-inputs.ts
    ├── api-handler.example.ts
    └── run-example.ts
```

## Stages

1. **Planner** — structured `PlannerPlan` JSON (premise, setting type, arc, scenes, moral, safety/variation notes).
2. **Draft writer** — full story JSON (`title`, `category`, `theme`, `story_text`, `moral`, `estimated_duration_seconds`).
3. **Refiner** — improves flow and TTS readability; preserves meaning and safety.
4. **Validator / repair** — Zod + heuristics; on failure, **Repair** prompt with error list; retries up to `maxRepairAttempts`.

## Final JSON schema (exact keys)

```json
{
  "title": "",
  "category": "",
  "theme": "",
  "story_text": "",
  "moral": "",
  "estimated_duration_seconds": 0
}
```

- Keys stay **English**; **values** must be in the **target language** (`ta`, `en`, `hi`, `te`, `ka`, `ml`).
- **Kannada** code is `ka` (product convention); Unicode validation uses the Kannada script.
- Inline markers in `story_text` must be **exactly** the allowed English tokens (see `src/constants/markers.ts`).

## Install & build

```bash
cd story-pipeline
npm install
npm run build
```

## Run the mock example (no API key)

```bash
npx tsx examples/run-example.ts
```

Uses `MockTamixaLlmProvider` to exercise parsing, validation, and `RecentStoryMemory`.

## Use with OpenAI-compatible APIs

Set:

- `OPENAI_API_KEY` — required for live model
- `OPENAI_BASE_URL` — optional (default `https://api.openai.com/v1`)
- `STORY_MODEL` — optional (default `gpt-4o`)

```typescript
import {
  StoryPipelineOrchestrator,
  OpenAiCompatibleProvider,
} from "@tamixa/story-pipeline";

const llm = new OpenAiCompatibleProvider({
  apiKey: process.env.OPENAI_API_KEY!,
  baseUrl: process.env.OPENAI_BASE_URL,
  model: process.env.STORY_MODEL ?? "gpt-4o",
});

const orchestrator = new StoryPipelineOrchestrator(llm);
const result = await orchestrator.run({
  language: "ta",
  category: "Friendship",
  combined_situation: "…",
  recent_story_patterns: memory.getPatterns(),
  avoid_repeating: ["phrase to avoid"],
});
```

## Bulk generation & anti-repetition

- Pass **`recent_story_patterns`** from a session store or `RecentStoryMemory` (sliding window of openings, morals, names, plot summaries).
- Pass **`avoid_repeating`** for explicit strings that must not appear.
- Planner prompts include both; **repetition validator** scores openings/morals vs recent items.

## Integration notes

- **Safety** and **language** validators are **heuristic**; keep human moderation for children’s content.
- Extend **`validateSafetyText`** with Tamil/Hindi/etc. keyword lists as you gather false negatives/positives.
- **`examples/api-handler.example.ts`** shows Zod body parsing and status mapping for a generic HTTP API.

## License

Private package for the Tamixa monorepo; align with your project license.
