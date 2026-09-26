-- Civic Survival pilot (interactive, English). DRAFT — not legal advice; prompts only.
-- story_owner seed:civic-survival-v1
-- Theme aligns with StoryLibraryValidation (Learn · Simulator prefix).

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Civic] The Traffic Stop — Keys & Papers (en)',
  $cv_0$# The Traffic Stop

**Civic survival:** a routine check escalates when you are asked for **vehicle keys**.

**Lesson:** Stay calm; verify identity; offer **licence, RC, insurance, PUC**—laws and local practice vary; this is a **practice story**, not legal advice.$cv_0$,
  'Learn · Simulator · Civic Survival',
  'Learn · Simulator · Civic Survival',
  'en',
  30,
  'Driver',
  118,
  2.0,
  $cv_0m$Fear makes people surrender control—know what you are being asked for before you comply.$cv_0m$,
  'DRAFT',
  'seed:civic-survival-v1',
  $cv_0g$ {"startSegmentId":"tts_n1","overlayStyle":"CARDS","segments":{"tts_n1":{"audioUrl":"https://cdn.tamixa.app/library/sim/civic-survival/v1/en/tts_n1.mp3","text":"Evening traffic. A uniformed officer waves you to the shoulder and asks for your vehicle keys—for safety, they say. Your phone buzzes with a payment reminder; your chest tightens. You have valid papers but grew up hearing that refusal always makes things worse.","choices":[{"id":"hand_keys","label":"Hand over the keys immediately to avoid trouble.","nextSegmentId":"tts_n2","skillDeltas":{"wisdom":-12,"social_capital":-8}},{"id":"calm_docs","label":"Stay polite; ask which document they need; do not hand keys—offer RC, insurance, licence, and PUC.","nextSegmentId":"tts_n3","skillDeltas":{"wisdom":22,"integrity":15}}]},"tts_n2":{"audioUrl":"https://cdn.tamixa.app/library/sim/civic-survival/v1/en/tts_n2.mp3","text":"Without the keys you cannot move if a secondary issue appears; the car sits wherever it was parked. Officers who follow procedure usually inspect papers and explain; surrendering control from fear is hard to unwind later.","choices":[]},"tts_n3":{"audioUrl":"https://cdn.tamixa.app/library/sim/civic-survival/v1/en/tts_n3.mp3","text":"You keep hands visible and documents ready. If something feels wrong, note time, place, and vehicle number calmly—escalate through proper channels after you are safe, not as a roadside argument. When in doubt, legal-aid helplines exist for the next step.","choices":[]}}}$cv_0g$,
  $cv_0p$Photograph your licence, RC, insurance, and PUC into a password-protected album you can show without handing over the phone.$cv_0p$,
  $cv_0j$["What would you teach a new driver about checks at night?", "Who would you call if you felt pressured beyond a normal document check?"]$cv_0j$::jsonb,
  $cv_0n$DRAFT pilot; verify Motor Vehicles Act / state rules with a qualified lawyer; audio CDN placeholders.$cv_0n$
);
