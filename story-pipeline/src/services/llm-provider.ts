import type { StoryPipelineInput } from "../types/pipeline-input.js";
import type { PlannerPlan } from "../schemas/planner-output.schema.js";
import type { StoryOutput } from "../schemas/story-output.schema.js";

/**
 * Pipeline stages map to prompt templates; providers may use `ctx` for logging,
 * caching, or deterministic mocks without parsing prompt strings.
 */
export type PipelineStage = "planner" | "draft_writer" | "refiner" | "repair";

export interface LlmCallContext {
  stage: PipelineStage;
  input: StoryPipelineInput;
  plan?: PlannerPlan;
  draft?: StoryOutput;
  validationErrors?: string[];
}

export interface LlmCompletionOptions {
  temperature?: number;
  maxTokens?: number;
}

/**
 * Abstract LLM boundary — swap OpenAI, Azure, Groq, local OpenAI-compatible servers, etc.
 */
export interface LlmProvider {
  complete(prompt: string, ctx: LlmCallContext, options?: LlmCompletionOptions): Promise<string>;
}

const LONG_EN_STORY =
  `[Pause 500ms] Once upon a morning in Chennai, the sun painted the apartment balcony gold. [Warm tone] "Today we plant the herbs," Amma said. [Happy tone] Little Kavin clapped. [Pause 1s] They filled pots with soil, counted seeds like tiny treasures, and watered with a gentle sprinkle. [Calm] On the way downstairs, they met their neighbor who looked worried about a lost key. [Soft voice] Kavin offered to help search the community garden. [Excited] They found it beside a marigold—no drama, only relief. [Pause 500ms] Back home, the basil smelled like patience and teamwork. [Happy tone]`.repeat(
    4,
  );

function buildMockPlan(input: StoryPipelineInput): PlannerPlan {
  return {
    target_language_code: input.language,
    story_premise: `A gentle story about ${input.category} woven with: ${input.combined_situation}`,
    setting_type: "urban",
    regional_flavor: "tamil_nadu_south_india_primary",
    protagonist: {
      name: input.language === "en" ? "Kavin" : "கவின்",
      short_description:
        input.language === "en"
          ? "Curious child who likes helping neighbors"
          : "அண்டை வீட்டுக்காரர்களுக்கு உதவ விரும்பும் ஆர்வமுள்ள குழந்தை",
      age_band: "young_child",
    },
    supporting_characters: [
      {
        name: input.language === "en" ? "Amma" : "அம்மா",
        role: input.language === "en" ? "Caring parent" : "அக்கறையுள்ள பெற்றோர்",
      },
    ],
    conflict_type: "small_obstacle",
    emotional_arc:
      input.language === "en"
        ? "Curiosity → small worry → cooperative help → warm satisfaction"
        : "ஆர்வம் → சிறிய கவலை → ஒத்துழைப்பு → வெப்பமான திருப்தி",
    scene_outline: [
      { scene_id: 1, summary: "Home balcony gardening setup", emotional_beat: "cozy anticipation" },
      { scene_id: 2, summary: "Meeting worried neighbor", emotional_beat: "empathy" },
      { scene_id: 3, summary: "Searching garden together", emotional_beat: "teamwork" },
      { scene_id: 4, summary: "Finding key and shared smiles", emotional_beat: "relief and joy" },
    ],
    theme: input.language === "en" ? "Everyday courage through kindness" : "அன்றாட அன்பின் மூலம் துணிச்சல்",
    moral:
      input.language === "en"
        ? "Small helpful actions make the whole neighborhood brighter."
        : "சிறிய உதவிகள் அக்கம் பக்கத்தை பிரகாசமாக்கும்.",
    safety_notes: "No forbidden topics; conflict is a lost object, resolved peacefully.",
    variation_notes: `Differentiated from recent patterns: urban balcony, herb gardening, lost key beat; category ${input.category}.`,
  };
}

/** Mock-only: localize common admin category labels for script validation demos. */
function mockCategoryInLanguage(lang: string, category: string): string {
  if (lang === "en") return category;
  const ta: Record<string, string> = {
    Friendship: "நட்பு",
    Courage: "துணிச்சல்",
    Kindness: "அன்பு",
    Honesty: "நேர்மை",
    Adventure: "சாகசம்",
    Family: "குடும்பம்",
  };
  const hi: Record<string, string> = {
    Friendship: "मित्रता",
    Courage: "साहस",
    Kindness: "दयालुता",
    Honesty: "ईमानदारी",
    Adventure: "साहसिक",
    Family: "परिवार",
  };
  const te: Record<string, string> = {
    Friendship: "స్నేహం",
    Courage: "ధైర్యం",
    Kindness: "దయ",
    Honesty: "నిజాయితీ",
    Adventure: "సాహసం",
    Family: "కుటుంబం",
  };
  const ka: Record<string, string> = {
    Friendship: "ಸ್ನೇಹ",
    Courage: "ಧೈರ್ಯ",
    Kindness: "ದಯೆ",
    Honesty: "ಪ್ರಾಮಾಣಿಕತೆ",
    Adventure: "ಸಾಹಸ",
    Family: "ಕುಟುಂಬ",
  };
  const ml: Record<string, string> = {
    Friendship: "സൗഹൃദം",
    Courage: "ധൈര്യം",
    Kindness: "ദയ",
    Honesty: "സത്യസന്ധത",
    Adventure: "സാഹസികത",
    Family: "കുടുംബം",
  };
  const maps: Record<string, Record<string, string>> = { ta, hi, te, ka, ml };
  const m = maps[lang];
  return m?.[category] ?? category;
}

function buildMockStoryFromPlan(input: StoryPipelineInput, plan: PlannerPlan): StoryOutput {
  const isEn = input.language === "en";
  const body = isEn
    ? LONG_EN_STORY
    : `ஒரு சென்னை காலை [Pause 500ms] வானம் மென்மையான நீலம். [Warm tone] அம்மா சொன்னார், "இன்று நாம் மூலிகைகள் நடுவோம்." [Happy tone] கவின் கைதட்டினான். [Pause 1s] அவர்கள் மண்ணை நிரப்பி, விதைகளை எண்ணி, மென்மையாக நீர் தெளித்தனர். [Calm] கீழே செல்லும் வழியில் அண்டை வீட்டார் சாவி தொலைந்ததாக வருந்தினார். [Soft voice] கவின் உதவ முன்வந்தான். [Excited] சமூகத் தோட்டத்தில் மல்லிகை அருகே சாவி கிடந்தது. [Pause 500ms] வீட்டில் வந்த பிறகு பச்சை இலைகள் பொறுமையின் வாசனையைத் தந்தன. [Happy tone] `.repeat(
        5,
      );

  return {
    title: isEn ? "Balcony Seeds and the Garden Key" : "மாடத்தோட்ட விதைகளும் தோட்டச் சாவியும்",
    category: mockCategoryInLanguage(input.language, input.category),
    theme: plan.theme,
    story_text: body,
    moral: plan.moral,
    estimated_duration_seconds: 480,
  };
}

/**
 * Deterministic canned outputs for CI and local development without API keys.
 */
export class MockTamixaLlmProvider implements LlmProvider {
  async complete(_prompt: string, ctx: LlmCallContext): Promise<string> {
    switch (ctx.stage) {
      case "planner":
        return JSON.stringify(buildMockPlan(ctx.input));
      case "draft_writer": {
        const plan = ctx.plan ?? buildMockPlan(ctx.input);
        return JSON.stringify(buildMockStoryFromPlan(ctx.input, plan));
      }
      case "refiner": {
        const base = ctx.draft ?? buildMockStoryFromPlan(ctx.input, ctx.plan ?? buildMockPlan(ctx.input));
        const refined = { ...base, estimated_duration_seconds: 500 };
        return JSON.stringify(refined);
      }
      case "repair": {
        const base = ctx.draft ?? buildMockStoryFromPlan(ctx.input, ctx.plan ?? buildMockPlan(ctx.input));
        return JSON.stringify({ ...base, estimated_duration_seconds: Math.max(120, base.estimated_duration_seconds) });
      }
      default:
        return "{}";
    }
  }
}

export interface OpenAiCompatibleConfig {
  apiKey: string;
  baseUrl?: string;
  model: string;
  /** Optional extra headers (e.g. Azure) */
  headers?: Record<string, string>;
}

/**
 * OpenAI-compatible chat completions (GPT-4o, Groq, Together, vLLM, etc.).
 */
export class OpenAiCompatibleProvider implements LlmProvider {
  constructor(private readonly config: OpenAiCompatibleConfig) {}

  async complete(
    prompt: string,
    _ctx: LlmCallContext,
    options?: LlmCompletionOptions,
  ): Promise<string> {
    const url = `${(this.config.baseUrl ?? "https://api.openai.com/v1").replace(/\/$/, "")}/chat/completions`;
    const res = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${this.config.apiKey}`,
        ...this.config.headers,
      },
      body: JSON.stringify({
        model: this.config.model,
        temperature: options?.temperature ?? 0.7,
        max_tokens: options?.maxTokens ?? 8192,
        messages: [
          { role: "system", content: "You follow instructions exactly and return only the requested format." },
          { role: "user", content: prompt },
        ],
      }),
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(`OpenAI-compatible API error ${res.status}: ${errText.slice(0, 500)}`);
    }

    const data = (await res.json()) as {
      choices?: Array<{ message?: { content?: string } }>;
    };
    const content = data.choices?.[0]?.message?.content;
    if (!content) throw new Error("OpenAI-compatible API returned empty content");
    return content;
  }
}
