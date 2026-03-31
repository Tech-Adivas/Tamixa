import type { StoryPipelineInput } from "../src/types/pipeline-input.js";

export const exampleEnglishInput: StoryPipelineInput = {
  language: "en",
  category: "Friendship",
  combined_situation: "A metro ride to a science fair mixed with helping a new classmate find courage to speak on stage",
  recent_story_patterns: {
    openings: ["Once there was a small village where everyone knew each other's names."],
    morals: ["Sharing is the best treasure of all."],
    characterNames: ["Ravi", "Meera"],
  },
  avoid_repeating: ["Panchatantra mouse", "golden mango tree"],
};

export const exampleTamilInput: StoryPipelineInput = {
  language: "ta",
  category: "Courage",
  combined_situation: "ஒரு பள்ளி அறிவியல் கண்காட்சி மற்றும் புதிய நண்பனுக்கு ஊக்கம் அளித்தல்",
  avoid_repeating: ["கோவில் தெரு"],
};

export const exampleHindiInput: StoryPipelineInput = {
  language: "hi",
  category: "Kindness",
  combined_situation: "शहरी पार्क में सफाई अभियान और छोटे पक्षी घोंसले की रक्षा",
};
