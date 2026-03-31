#!/usr/bin/env npx tsx
/**
 * Run: `npx tsx examples/run-example.ts` from the story-pipeline package root.
 */

import { StoryPipelineOrchestrator, MockTamixaLlmProvider } from "../src/index.js";
import { RecentStoryMemory } from "../src/utils/recent-story-memory.js";
import { exampleEnglishInput, exampleTamilInput } from "./test-inputs.js";

async function main() {
  const llm = new MockTamixaLlmProvider();
  const pipeline = new StoryPipelineOrchestrator(llm);
  const memory = new RecentStoryMemory();

  console.log("--- Run 1: English (mock LLM) ---");
  const r1 = await pipeline.run({
    ...exampleEnglishInput,
    recent_story_patterns: memory.getPatterns(),
  });
  console.log(JSON.stringify(r1.story, null, 2).slice(0, 1200) + "\n...");
  memory.record(r1.story, r1.plan.story_premise);

  console.log("\n--- Run 2: Tamil (mock LLM), with memory ---");
  const r2 = await pipeline.run({
    ...exampleTamilInput,
    recent_story_patterns: memory.getPatterns(),
  });
  console.log("title:", r2.story.title);
  console.log("duration_s:", r2.story.estimated_duration_seconds);
  console.log("repairAttempts:", r2.repairAttempts);
  console.log("warnings:", r2.validationWarnings);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
