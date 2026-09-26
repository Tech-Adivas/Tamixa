-- Life skills interactive simulators batch 2 (English). DRAFT.
-- story_owner seed:life-skills-interactive-pack-v2; segment audio under https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Relative''s Request (en)',
  $b2_0$# The Relative's Request

Branching **finance and boundaries** simulator for young adults and parents: a beloved uncle asks for a large, urgent loan and requests secrecy from the rest of the family.
Choices contrast secret transfers, transparent family discussion, and structured non-cash help (formal credit, documentation).

**Lesson:** Love can include clear limits; secrecy plus money strains trust faster than a careful no.$b2_0$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  24,
  'You',
  135,
  2.0,
  $b2_0m$Financial boundaries with relatives are not betrayal—undocumented secrecy often damages relationships more than an honest, smaller yes.$b2_0m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_0g$ {"startSegmentId":"rel_n1_hook","overlayStyle":"CARDS","segments":{"rel_n1_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rel_n1_hook.mp3","text":"Your favorite uncle calls during lunch. He helped pay your school fees years ago, and he always remembers your birthday. Today his voice is warm but rushed: his supplier will stop shipments unless he wires eighty thousand rupees by evening. He says you are the only one who understands business pressure, and asks you not to alarm your parents—they worry too much and might refuse 'for no reason.' Your stomach tightens. You love him, but this is most of your emergency fund.","choices":[{"id":"wire_secretly","label":"Transfer the money now and keep it between you—he always stood by you.","nextSegmentId":"rel_n2_secret","skillDeltas":{"money":-20,"balance":-15}},{"id":"boundary_family","label":"Say you must discuss with parents or your spouse before any large loan—offer to talk together tonight.","nextSegmentId":"rel_n3_family_boundary","skillDeltas":{"wisdom":25,"social_capital":10}},{"id":"noncash_help","label":"Refuse a blind loan but offer to help him draft a bank or MFI application and go with him tomorrow.","nextSegmentId":"rel_n4_structured","skillDeltas":{"wisdom":20,"social_capital":15}}]},"rel_n2_secret":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rel_n2_secret.mp3","text":"You move the funds quietly. He thanks you with a long voice note. For six weeks he is affectionate on the family group chat. Then replies slow down. When your mother sees a bounced ECS alert you were debugging, she asks direct questions. The truth spills out. Your parents are not angry at the money alone—they are hurt that secrecy turned the family into a lender and a detective at once. Your uncle stops answering; the relationship cools into polite distance.","choices":[]},"rel_n3_family_boundary":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rel_n3_family_boundary.mp3","text":"You repeat that love does not require hiding numbers. That night, with your parents at the table, your uncle hears a clear offer: a smaller, documented amount with a written timeline, or help finding a formal line of credit. He flushes—pride stung—but your father stays kind and firm. The room is awkward, yet when he leaves, your mother squeezes your hand. The boundary cost a minute of discomfort and bought years of trust.","choices":[]},"rel_n4_structured":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rel_n4_structured.mp3","text":"He agrees to meet at the bank branch. The manager explains eligibility; paperwork is boring but transparent. He may not qualify for the full amount immediately, yet he leaves with a plan instead of a favor owed in silence. You showed care without melting your own financial floor—and you modeled that family help can look like clarity, not just cash.","choices":[]}}}$b2_0g$,
  $b2_0p$Write down one sentence you will use the next time someone asks for a large loan on the spot (e.g. timing, documentation, who must be in the loop).$b2_0p$,
  $b2_0j$["When does helping family require transparency with other family members?", "How can you refuse a full loan while still showing care?"]$b2_0j$::jsonb,
  $b2_0n$DRAFT batch 2; upload segment audio to CDN paths in interactive_graph.$b2_0n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Interview Accent (en)',
  $b2_1$# The Interview Accent

**Public speaking / career** simulator about fear of 'bad English' before a job interview. Paths explore cramming jargon, plain-language preparation with real examples, and avoidance.

**Lesson:** Clarity and honest pacing outperform performed fluency; interviewers follow substance, not accent policing.$b2_1$,
  'Learn · Simulator · Life Skills',
  'Learn · Simulator · Life Skills',
  'en',
  22,
  'Candidate',
  118,
  2.0,
  $b2_1m$Fluency in interviews is the ability to explain your own work clearly—not to mimic a voice that is not yours.$b2_1m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_1g$ {"startSegmentId":"int_n1_fear","overlayStyle":"CARDS","segments":{"int_n1_fear":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/int_n1_fear.mp3","text":"Tomorrow is your first interview for a role you actually want. English is not your first language, and cousins have joked about your accent at weddings. At midnight you scroll videos of candidates using words you barely use in real life. Every imagined slip feels like proof you do not belong in that glass office.","choices":[{"id":"cram_jargon","label":"Memorize flashy phrases and complex vocabulary to sound 'fluent' overnight.","nextSegmentId":"int_n2_jargon","skillDeltas":{"wisdom":-15,"balance":-20}},{"id":"simple_prep","label":"Outline three real stories (problem, what you did, result) in plain English and practice aloud slowly.","nextSegmentId":"int_n2_clear","skillDeltas":{"wisdom":25,"balance":15}},{"id":"skip_interview","label":"Email that you are sick and try to postpone indefinitely.","nextSegmentId":"int_n2_avoid","skillDeltas":{"money":-5,"balance":-25}}]},"int_n2_jargon":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/int_n2_jargon.mp3","text":"In the interview you deploy a sentence you barely understand. The interviewer asks a simple follow-up and you freeze, guessing at synonyms. The panel notes confidence crumbling—not accent, but mismatch between words and thought. Later you replay it: they were not testing your English exam score; they were testing whether you could explain your own work.","choices":[]},"int_n2_clear":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/int_n2_clear.mp3","text":"You speak simply. Once you pause to search for a word and say, 'Let me rephrase that.' The interviewer nods. Your examples are concrete; your numbers are modest but yours. Walking out, you realize clarity carried more weight than any borrowed idiom. Fluency, you discover, includes honesty and pace—not accent erasure.","choices":[]},"int_n2_avoid":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/int_n2_avoid.mp3","text":"The HR lead politely accepts the excuse once, then moves on to the next candidate. The role fills. Months later the same fear whispers before other doors. Avoidance saved you one hour of awkwardness and cost a proof that you could show up anyway.","choices":[]}}}$b2_1g$,
  $b2_1p$Record yourself answering 'Tell me about a hard problem you solved' in two minutes using only words you use with friends—listen once, revise once.$b2_1p$,
  $b2_1j$["What is the difference between accent and clarity?", "When is it brave to admit you are thinking out loud in an interview?"]$b2_1j$::jsonb,
  $b2_1n$DRAFT batch 2; segment MP3s are placeholders.$b2_1n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Deepfake Call (en)',
  $b2_2$# The Deepfake Call

**Tech safety** simulator: a frantic voice call mimics a close friend in crisis and demands instant UPI payment. Branches cover paying immediately, verifying on a saved number, and in-call challenge questions.

**Lesson:** Voice can be cloned—verify through a second, trusted channel before any transfer.$b2_2$,
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  18,
  'Listener',
  112,
  2.0,
  $b2_2m$Urgency plus emotion is the scammer's toolkit; a ten-second callback on a known number breaks most traps.$b2_2m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_2g$ {"startSegmentId":"dfc_n1_call","overlayStyle":"CARDS","segments":{"dfc_n1_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dfc_n1_call.mp3","text":"Your phone rings. The voice is breathy, cracked, unmistakably like your friend Kiara—she says she is in an accident, the hospital will not treat her without an advance, and begs you to send twenty thousand rupees to a UPI number right now. 'Please don't tell my parents yet—they will panic.' Your chest locks. Kiara was fine at lunch.","choices":[{"id":"send_now","label":"Send the money immediately—what if it is really her?","nextSegmentId":"dfc_n2_scam","skillDeltas":{"money":-25,"DIGITAL_WISDOM":-25}},{"id":"call_saved","label":"Hang up and call Kiara on the number already saved in your phone—no new links.","nextSegmentId":"dfc_n2_verified","skillDeltas":{"DIGITAL_WISDOM":30,"balance":10}},{"id":"challenge_pin","label":"Stay on the line but ask a shared secret only Kiara would know before sending anything.","nextSegmentId":"dfc_n2_trap","skillDeltas":{"DIGITAL_WISDOM":20,"balance":5}}]},"dfc_n2_scam":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dfc_n2_scam.mp3","text":"The UPI goes through. Ten minutes later the real Kiara texts from class: her phone was cloned in a data leak FAQ you ignored. The cyber helpline says recovery is unlikely. The lesson lands cold: panic plus love is the exact recipe voice-AI scams cook with.","choices":[]},"dfc_n2_verified":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dfc_n2_verified.mp3","text":"Kiara answers confused from her couch, snack in hand. Together you report the number and screenshot the fake UPI ID. You post a one-line warning to your circle: verify voice on a second channel. Fear becomes fuel for a habit—never pay urgency without a known thread.","choices":[]},"dfc_n2_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dfc_n2_trap.mp3","text":"The caller stumbles over the nickname only you two use, then hangs up. Kiara confirms she never called. You realize emotion wanted you to skip the test; verification took ten seconds and saved your savings.","choices":[]}}}$b2_2g$,
  $b2_2p$Add one trusted contact shortcut labeled 'Verify voice scams' and practice the habit: pause, call back, then pay (if ever).$b2_2p$,
  $b2_2j$["What question only your real friend could answer?", "Why do scammers say 'don't tell anyone'?"]$b2_2j$::jsonb,
  $b2_2n$DRAFT batch 2.$b2_2n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Discount Trap (en)',
  $b2_3$# The Discount Trap

**Business / unit economics** simulator for shopkeepers: a flashy 'Buy 1 Get 2 Free' promotion on low-margin goods. Compare launching blind, modeling margins first, or redesigning the offer with floors.

**Lesson:** Promotions without per-unit math can turn foot traffic into losses.$b2_3$,
  'Learn · Simulator · Business',
  'Learn · Simulator · Business',
  'en',
  35,
  'Proprietor',
  105,
  2.0,
  $b2_3m$Every free unit has a cost; sustainable generosity in retail needs a margin floor.$b2_3m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_3g$ {"startSegmentId":"dsc_n1_promo","overlayStyle":"CARDS","segments":{"dsc_n1_promo":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dsc_n1_promo.mp3","text":"Your kirana is slow mid-week. A distributor suggests a bright red banner: Buy One Pack, Get Two Free on a snack that costs you one hundred rupees to stock and usually sells for two hundred and forty. Customers love free. You imagine queues—but you have not modeled rent, spoilage, or GST on the promo price.","choices":[{"id":"run_blind","label":"Launch the banner today—foot traffic first, math later.","nextSegmentId":"dsc_n2_bleed","skillDeltas":{"money":-25,"wisdom":-15}},{"id":"spreadsheet_first","label":"Build a one-day spreadsheet: units given away, net margin per customer, break-even footfall.","nextSegmentId":"dsc_n2_margin_truth","skillDeltas":{"wisdom":30,"fiscal_muscle":15}},{"id":"bundle_floor","label":"Switch to 'Buy two, get the third at half' with a minimum bill so free goods are not pure loss.","nextSegmentId":"dsc_n2_controlled","skillDeltas":{"wisdom":20,"money":5}}]},"dsc_n2_bleed":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dsc_n2_bleed.mp3","text":"The weekend is loud with shoppers grabbing three packs for the price of one. By Sunday evening your fast-moving shelf is empty and your margin report is red. You bought attention by giving away profit. The lesson stings: 'free' without unit economics is a loan you never collect.","choices":[]},"dsc_n2_margin_truth":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dsc_n2_margin_truth.mp3","text":"The sheet shows each 'free' pair costs you two hundred rupees in goods for two hundred forty collected—before overhead. You shrink the offer or pair it with a higher-margin item. Promotion becomes a lever, not a leak. Customers still come, but you sleep knowing the shop earns.","choices":[]},"dsc_n2_controlled":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dsc_n2_controlled.mp3","text":"The rule nudges bigger baskets. Some grumble, but shrinkage drops. You learn that generosity in retail needs a floor—otherwise love for the crowd bankrupts the owner.","choices":[]}}}$b2_3g$,
  $b2_3p$Pick one product you sell and write cost, price, and gross margin per unit on a sticky note for a week.$b2_3p$,
  $b2_3j$["When does a crowded shop still lose money?", "How can a promotion pull other, higher-margin items?"]$b2_3j$::jsonb,
  $b2_3n$DRAFT batch 2.$b2_3n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Exam Pressure (en)',
  $b2_4$# The Exam Pressure

**Mental health / resilience** simulator after a poor mock exam score amid family expectations. Paths include punitive cramming, asking a teacher for a focused plan, and hiding results.

**Lesson:** Resilience is repair and pacing—not self-punishment or secrecy.$b2_4$,
  'Learn · Simulator · Mental Health',
  'Learn · Simulator · Mental Health',
  'en',
  16,
  'Student',
  110,
  2.0,
  $b2_4m$One score is data; sleep, help-seeking, and honest conversation turn it into progress.$b2_4m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_4g$ {"startSegmentId":"exm_n1_mock","overlayStyle":"CARDS","segments":{"exm_n1_mock":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/exm_n1_mock.mp3","text":"The mock test PDF opens: forty-two percent. Your mother had said, softly, that neighbors' children are 'all above ninety.' Your throat closes. Boards are ten weeks away. The voice in your head jumps between shame and fatalism.","choices":[{"id":"all_nighter_spiral","label":"Punish yourself with an all-nighter cramming every chapter at once.","nextSegmentId":"exm_n2_burnout","skillDeltas":{"balance":-25,"wisdom":-10}},{"id":"help_plan","label":"Message the math teacher, show the breakdown, and ask for a two-week micro-plan on weakest topics only.","nextSegmentId":"exm_n2_resilience","skillDeltas":{"wisdom":25,"balance":20}},{"id":"hide_score","label":"Delete the screenshot and pretend the mock never happened.","nextSegmentId":"exm_n2_anxiety","skillDeltas":{"balance":-20,"social_capital":-10}}]},"exm_n2_burnout":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/exm_n2_burnout.mp3","text":"You sleep through the morning alarm, head pounding. The next practice set is worse because fear, not understanding, drove the night. Resilience is not volume without sleep—it is steady repair.","choices":[]},"exm_n2_resilience":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/exm_n2_resilience.mp3","text":"The teacher marks three chapters, not twelve. Small wins stack. Your mother sees effort, not just percent. The mock stops being a verdict and becomes data. Resilience, you learn, is asking for a map when pride wants to hide the terrain.","choices":[]},"exm_n2_anxiety":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/exm_n2_anxiety.mp3","text":"At parent-teacher meeting the truth surfaces anyway—now with extra disappointment that you carried it alone. The score was never the enemy; isolation was.","choices":[]}}}$b2_4g$,
  $b2_4p$List three topics you will tackle this week and one adult you will tell your real score to—before the night cram.$b2_4p$,
  $b2_4j$["How is resilience different from toughness?", "Who is safe to ask for a study plan when shame is loud?"]$b2_4j$::jsonb,
  $b2_4n$DRAFT batch 2.$b2_4n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The WhatsApp Forward (en)',
  $b2_5$# The WhatsApp Forward

**Tech ethics** simulator: a viral, politically charged message pressures you to forward without checking. Compare instant spread, fact-checking first, and gentle group correction.

**Lesson:** Research before amplification; chain letters weaponize your reputation.$b2_5$,
  'Learn · Simulator · Ethics',
  'Learn · Simulator · Ethics',
  'en',
  20,
  'Group member',
  100,
  2.0,
  $b2_5m$Sharing is a moral act when the claim is explosive—pause, verify, then speak.$b2_5m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_5g$ {"startSegmentId":"wa_n1_viral","overlayStyle":"CARDS","segments":{"wa_n1_viral":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wa_n1_viral.mp3","text":"A WhatsApp group lights up: a voice note plus screenshots claiming a shocking order about a festival was 'signed secretly.' The forward count is pasted—'already fifty thousand shares.' An uncle tags you: 'Youth should spread awareness.'","choices":[{"id":"forward_fast","label":"Forward to five groups immediately—better safe than sorry if it might be true.","nextSegmentId":"wa_n2_harm","skillDeltas":{"DIGITAL_WISDOM":-25,"social_capital":-15}},{"id":"fact_check","label":"Search the official press information bureau and two reputable outlets before sharing anything.","nextSegmentId":"wa_n2_informed","skillDeltas":{"wisdom":30,"DIGITAL_WISDOM":20}},{"id":"reply_correction","label":"Reply in the group with a fact-check link and a short note: pause before panic.","nextSegmentId":"wa_n2_leader","skillDeltas":{"social_capital":20,"wisdom":15}}]},"wa_n2_harm":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wa_n2_harm.mp3","text":"By evening elders argue at tea; someone repeats your forward as gospel. A journalist you follow debunks the audio as clipped from an old drama. You delete messages, but screenshots circulate without your name—and with your conscience. Research would have cost three minutes.","choices":[]},"wa_n2_informed":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wa_n2_informed.mp3","text":"The claim is false—mixed dates, no gazette entry. You post one clarifying link. A cousin admits relief; she almost forwarded too. Ethics here was boring: read, then speak.","choices":[]},"wa_n2_leader":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wa_n2_leader.mp3","text":"Two people mute the group; one thanks you privately. The rumor slows in your circle. Leadership in chat is not volume—it is slowing the chain.","choices":[]}}}$b2_5g$,
  $b2_5p$Find one reputable fact-check site bookmarked on your phone and use it once this week before forwarding any 'urgent' screenshot.$b2_5p$,
  $b2_5j$["Why does 'better safe than sorry' backfire with fake news?", "How can you correct misinformation without shaming relatives?"]$b2_5j$::jsonb,
  $b2_5n$DRAFT batch 2.$b2_5n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Society Meeting (en)',
  $b2_6$# The Society Meeting

**Leadership / negotiation** simulator at a housing society AGM: parking conflict escalates. Choices: pick a side loudly, propose written rules and rotation, or disengage.

**Lesson:** Neutral process beats tribal loyalty for lasting fixes.$b2_6$,
  'Learn · Simulator · Leadership',
  'Learn · Simulator · Leadership',
  'en',
  42,
  'Resident',
  102,
  2.0,
  $b2_6m$Negotiation in communities needs visible rules more than winning today's shout match.$b2_6m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_6g$ {"startSegmentId":"soc_n1_fight","overlayStyle":"CARDS","segments":{"soc_n1_fight":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/soc_n1_fight.mp3","text":"At the society AGM, Resident A blocks Resident B's car on camera. Voices rise over 'visitor parking' versus 'I was only five minutes.' The secretary bangs a water bottle for order. Half the room has taken sides before facts are clear.","choices":[{"id":"pick_side","label":"Stand up and loudly defend the neighbor you like—truth can sort itself later.","nextSegmentId":"soc_n2_split","skillDeltas":{"social_capital":-20,"wisdom":-10}},{"id":"facilitate_rule","label":"Propose a written visitor pass, time-capped slots, and a rotating marshal role—vote next month on numbers.","nextSegmentId":"soc_n2_deal","skillDeltas":{"social_capital":25,"wisdom":20}},{"id":"walk_out","label":"Leave the meeting—parking fights are beneath you.","nextSegmentId":"soc_n2_stale","skillDeltas":{"social_capital":-10,"balance":-5}}]},"soc_n2_split":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/soc_n2_split.mp3","text":"Whatsapp groups splinter into Team A and Team B. The gate log never gets updated; the next fight is worse because trust burned first. Negotiation needs neutrality more than loyalty.","choices":[]},"soc_n2_deal":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/soc_n2_deal.mp3","text":"Groans meet your motion, then practical questions. By month end, stickers and a simple register exist. Incidents drop—not because people became saints, but because rules replaced grudges in the dark. You practiced leadership as process, not personality.","choices":[]},"soc_n2_stale":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/soc_n2_stale.mp3","text":"The shouting continues without a minute-taker. Next week your own guest is towed because no policy passed. Avoidance did not protect peace—it postponed structure.","choices":[]}}}$b2_6g$,
  $b2_6p$Sketch a one-page parking or visitor rule you would propose at your own building—time cap, guest pass, enforcement buddy.$b2_6p$,
  $b2_6j$["When does defending a friend harm the whole community?", "What makes a housing rule fair enough to stick?"]$b2_6j$::jsonb,
  $b2_6n$DRAFT batch 2.$b2_6n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The First Credit Card (en)',
  $b2_7$# The First Credit Card

**Finance** simulator for young earners: a high limit arrives; peers push maxing out and minimum payments. Contrast debt spiral, disciplined utilization with full payoff, and unused-card thin file.

**Lesson:** Credit utilization and on-time full payment shape trust with lenders more than flashy spending.$b2_7$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  23,
  'Cardholder',
  108,
  2.0,
  $b2_7m$Treat the limit as a safety ceiling, not a spending target; pay in full whenever you can.$b2_7m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_7g$ {"startSegmentId":"cc_n1_limit","overlayStyle":"CARDS","segments":{"cc_n1_limit":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cc_n1_limit.mp3","text":"Your first card arrives: three lakh limit, metal shine in the envelope. Your roommate grins—'Festive season, bro—max it, pay minimum, build score.' The app shows 'recommended' EMI on a phone you do not need.","choices":[{"id":"max_fun","label":"Swipe for gadgets and trips—minimum due feels tiny this month.","nextSegmentId":"cc_n2_debt","skillDeltas":{"money":-25,"fiscal_muscle":-20}},{"id":"utilization_discipline","label":"Set autopay for full balance, cap personal spend under thirty percent of limit, track in a simple sheet.","nextSegmentId":"cc_n2_healthy","skillDeltas":{"fiscal_muscle":30,"wisdom":15}},{"id":"freeze_unused","label":"Lock the card in a drawer unused—no risk means no history.","nextSegmentId":"cc_n2_thin","skillDeltas":{"wisdom":-5,"money":0}}]},"cc_n2_debt":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cc_n2_debt.mp3","text":"Interest compounds while your statement story grows. The score you imagined bends the wrong way. Credit was never free money—it was timed trust. High utilization whispers 'distress' to algorithms louder than your selfie with the new phone.","choices":[]},"cc_n2_healthy":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cc_n2_healthy.mp3","text":"Six months later a pre-approved limit bump arrives—not because you chased rewards, but because you paid in full like a bill, not a loan. Credit utilization becomes a habit: borrow briefly, repay completely, sleep.","choices":[]},"cc_n2_thin":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cc_n2_thin.mp3","text":"Later, a car loan officer shrugs at a thin file. You learn that safe can mean invisible—small recurring bills on autopay might have built the same discipline without drama.","choices":[]}}}$b2_7g$,
  $b2_7p$Set a calendar reminder two days before your statement date and confirm autopay for the full statement balance.$b2_7p$,
  $b2_7j$["What is credit utilization in one sentence?", "Why might never using a card hurt you later?"]$b2_7j$::jsonb,
  $b2_7n$DRAFT batch 2.$b2_7n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Customer Is Wrong (en)',
  $b2_8$# The Customer Is Wrong

**Business / professionalism** simulator: a rude customer lies about product damage while others film. Compare shouting back, calm policy explanation with options, and freebies to escape noise.

**Lesson:** Professional tone and documented policy protect staff, brand, and truth under pressure.$b2_8$,
  'Learn · Simulator · Business',
  'Learn · Simulator · Business',
  'en',
  30,
  'Manager',
  104,
  2.0,
  $b2_8m$The customer is not always right—but your conduct can still be.$b2_8m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_8g$ {"startSegmentId":"cst_n1_rude","overlayStyle":"CARDS","segments":{"cst_n1_rude":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cst_n1_rude.mp3","text":"A customer slams a phone on the counter—speaker crackling, case scuffed. He demands a full refund, claiming 'factory defect,' while three people film. Your technician's note says liquid sensor tripped—likely washroom drop. He calls your staff thieves.","choices":[{"id":"yell_back","label":"Raise your voice and tell him to leave before you call security—enough is enough.","nextSegmentId":"cst_n2_viral","skillDeltas":{"social_capital":-25,"wisdom":-15}},{"id":"calm_policy","label":"Lower your tone, cite warranty terms on a printed sheet, offer a certified repair at cost or trade-in value.","nextSegmentId":"cst_n2_pro","skillDeltas":{"wisdom":25,"social_capital":15}},{"id":"free_replacement","label":"Hand a new phone to make him disappear—reputation first.","nextSegmentId":"cst_n2_pattern","skillDeltas":{"money":-20,"wisdom":-20}}]},"cst_n2_viral":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cst_n2_viral.mp3","text":"A clipped video hits local groups: 'shop abuses customers.' Context dies in comments. Professionalism was the only asset left in a loud room—and volume traded it away.","choices":[]},"cst_n2_pro":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cst_n2_pro.mp3","text":"Two onlookers nod; one says aloud, 'They are being fair.' The customer leaves muttering, but without a lawsuit meme. Your staff straighten shoulders. Professionalism is not winning every argument—it is keeping the truth legible.","choices":[]},"cst_n2_pattern":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cst_n2_pattern.mp3","text":"He returns next month with a scratched laptop. Word spreads that your store 'gives freebies if you shout.' You bought quiet once and sold a pattern.","choices":[]}}}$b2_8g$,
  $b2_8p$Role-play with a friend: one angry customer, one staff member—practice low voice, slow breath, repeat policy once, offer two options.$b2_8p$,
  $b2_8j$["When does giving in teach the wrong lesson to other customers?", "How do you protect staff dignity during public conflict?"]$b2_8j$::jsonb,
  $b2_8n$DRAFT batch 2.$b2_8n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The AI Career Fear (en)',
  $b2_9$# The AI Career Fear

**Leadership / adaptation** simulator for mid-career professionals hearing AI hype. Paths: passive worry, learning one concrete tool and piloting it, or denying change.

**Lesson:** Skill adaptation pairs human judgment with automation—avoidance and denial both carry risk.$b2_9$,
  'Learn · Simulator · Leadership',
  'Learn · Simulator · Leadership',
  'en',
  38,
  'Professional',
  115,
  2.0,
  $b2_9m$The durable career skill is learning to steer tools—not pretending they do not exist.$b2_9m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_9g$ {"startSegmentId":"aic_n1_news","overlayStyle":"CARDS","segments":{"aic_n1_news":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/aic_n1_news.mp3","text":"Leadership forwards an article: AI drafting client emails in seconds. Colleagues joke that analysts are 'optional.' You have fifteen years of pattern recognition in your domain—but tonight you scroll layoff threads until 2 a.m.","choices":[{"id":"doom_scroll","label":"Assume the worst and wait—maybe the company will clarify eventually.","nextSegmentId":"aic_n2_passive","skillDeltas":{"balance":-25,"wisdom":-10}},{"id":"learn_tool","label":"Pick one AI workflow (summarize long PDFs, draft first-pass charts) and present a pilot to your lead next sprint.","nextSegmentId":"aic_n2_adapt","skillDeltas":{"wisdom":30,"fiscal_muscle":10}},{"id":"deny_change","label":"Tell peers real expertise cannot be automated—ignore the tools as hype.","nextSegmentId":"aic_n2_blind","skillDeltas":{"wisdom":-20,"DIGITAL_WISDOM":-15}}]},"aic_n2_passive":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/aic_n2_passive.mp3","text":"Quarters pass. Juniors who paired judgment with tools ship faster reviews. Your caution reads as stagnation. Adaptation delayed is not neutrality—it is drift backward.","choices":[]},"aic_n2_adapt":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/aic_n2_adapt.mp3","text":"Your pilot saves four hours a week; you still validate every number. Leadership asks you to mentor others. Fear becomes fluency: the job was never the software—it was the decision layer on top.","choices":[]},"aic_n2_blind":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/aic_n2_blind.mp3","text":"A client asks why competitors deliver drafts overnight. Your team starts routing work around the bottleneck. Denial did not protect craft—it isolated it.","choices":[]}}}$b2_9g$,
  $b2_9p$List one repetitive task you did this week and search for a single reputable tutorial on using AI to draft a first pass—then edit critically by hand.$b2_9p$,
  $b2_9j$["What parts of your job are judgment versus repetition?", "Who at work models healthy experimentation with new tools?"]$b2_9j$::jsonb,
  $b2_9n$DRAFT batch 2.$b2_9n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Wedding Budget (en)',
  $b2_10$# The Wedding Budget

**Finance / prioritization** simulator: pressure to fund a sibling's wedding with loans, jewelry rentals, and status optics versus transparent family budgeting.

**Lesson:** Social display debt often outlasts the album; honest tradeoffs protect relationships and savings.$b2_10$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  28,
  'Sibling',
  106,
  2.0,
  $b2_10m$Meaningful rituals need not equal maximum spend; clarity beats silent sacrifice.$b2_10m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_10g$ {"startSegmentId":"wed_n1_pressure","overlayStyle":"CARDS","segments":{"wed_n1_pressure":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wed_n1_pressure.mp3","text":"Your sister's wedding list grows in the family group: premium venue, imported flowers, jewelry that 'signals respect.' Your parents look tired. You have savings for your own course abroad. Relatives hint that eldest children 'owe the family's face.'","choices":[{"id":"loan_face","label":"Take a personal loan and empty savings so the wedding matches the cousin's Instagram reel.","nextSegmentId":"wed_n2_debt","skillDeltas":{"money":-25,"balance":-20}},{"id":"honest_budget","label":"Call a family meeting with real numbers: what cash exists, what can wait, what rituals matter most without the five-star tax.","nextSegmentId":"wed_n2_priorities","skillDeltas":{"wisdom":25,"social_capital":15}},{"id":"status_borrow_jewelry","label":"Rent heavy jewelry and upgrade the venue on credit—photos matter more than the balance sheet.","nextSegmentId":"wed_n2_hangover","skillDeltas":{"money":-20,"wisdom":-15}}]},"wed_n2_debt":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wed_n2_debt.mp3","text":"The hall sparkles; emojis pour in. A year later EMIs eat your course fund. Your sister says she would have chosen smaller if someone had spoken first. Status borrowed from tomorrow arrives with interest.","choices":[]},"wed_n2_priorities":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wed_n2_priorities.mp3","text":"Some aunts sulk; others quietly admire the transparent spreadsheet. The wedding is warm, not loud. Your sister thanks you for giving her permission to be modest without shame. Prioritization hurt a few expectations and protected love longer than any centerpiece.","choices":[]},"wed_n2_hangover":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wed_n2_hangover.mp3","text":"The album is gorgeous; the bank calls are not. Family tension mixes with EMI reminders. Signal without savings is a costume that pinches after midnight.","choices":[]}}}$b2_10g$,
  $b2_10p$Draw two columns: non-negotiable wedding costs versus nice-to-have—discuss one line with family this month.$b2_10p$,
  $b2_10j$["How do you separate love from display?", "Who gets heard when money decisions are rushed?"]$b2_10j$::jsonb,
  $b2_10n$DRAFT batch 2.$b2_10n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Neighborhood Bully (en)',
  $b2_11$# The Neighborhood Bully

**Leadership / assertiveness** simulator for kids: a friend is targeted at school. Choices: fight, silent withdrawal, or firm voice plus seeking an adult.

**Lesson:** Assertiveness can interrupt harm without becoming harm.$b2_11$,
  'Learn · Simulator · Leadership',
  'Learn · Simulator · Leadership',
  'en',
  11,
  'Student',
  98,
  2.0,
  $b2_11m$Courage can sound like a clear sentence and a walk toward help—not a fist.$b2_11m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_11g$ {"startSegmentId":"blk_n1_lunch","overlayStyle":"CARDS","segments":{"blk_n1_lunch":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/blk_n1_lunch.mp3","text":"At the recess wall, Karan shoves your friend Aman and grabs his new water bottle—'tax for weak kids.' Aman looks at his shoes. A teacher is far across the field. Karan is taller than both of you.","choices":[{"id":"punch","label":"Throw the first punch—someone has to stop this now.","nextSegmentId":"blk_n2_trouble","skillDeltas":{"balance":-20,"wisdom":-15}},{"id":"silent_walk","label":"Pull Aman away quietly and say nothing—avoid trouble.","nextSegmentId":"blk_n2_shame","skillDeltas":{"social_capital":-20,"balance":-15}},{"id":"assert_adult","label":"Speak loudly and clearly: 'Give it back now—that is theft.' Walk toward the nearest duty teacher together.","nextSegmentId":"blk_n2_stop","skillDeltas":{"social_capital":25,"wisdom":20}}]},"blk_n2_trouble":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/blk_n2_trouble.mp3","text":"Detentions, parent calls, and a video that cuts off context. You defended pride, not strategy—and Aman still flinches at the gate.","choices":[]},"blk_n2_shame":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/blk_n2_shame.mp3","text":"Karan smirks; Aman stops eating lunch near you. Silence felt safe in the moment and cost trust after. Assertiveness sometimes looks like noise toward help, not fists toward harm.","choices":[]},"blk_n2_stop":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/blk_n2_stop.mp3","text":"The duty teacher arrives; Karan drops the bottle. Later, the counselor sets a check-in. You learn that steady voice plus adult witness beats both flight and brawl. Standing up meant naming the act, not becoming the violence.","choices":[]}}}$b2_11g$,
  $b2_11p$Practice one sentence with a parent: 'Stop—that is not okay'—and name which adult you would find on the playground.$b2_11p$,
  $b2_11j$["How is telling a teacher different from tattling?", "What does a good friend do when someone is scared?"]$b2_11j$::jsonb,
  $b2_11n$DRAFT batch 2.$b2_11n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Health Insurance Fine Print (en)',
  $b2_12$# The Health Insurance Fine Print

**Finance / literacy** simulator at claim time: room rent sub-limits surprise a family after surgery. Compare paying quietly, reading the policy and escalating properly, or venting at front-desk staff.

**Lesson:** Fine print is boring until it is expensive—read caps before admission when possible.$b2_12$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  45,
  'Family',
  112,
  2.0,
  $b2_12m$Insurance peace comes from understanding sub-limits, not from the agent's slogan alone.$b2_12m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_12g$ {"startSegmentId":"ins_n1_claim","overlayStyle":"CARDS","segments":{"ins_n1_claim":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/ins_n1_claim.mp3","text":"Mother's surgery goes well until the discharge desk: the insurer says room rent exceeds the policy cap, so a chunk of the bill is 'co-pay surprise.' The agent once said 'full coverage.' The PDF is forty pages.","choices":[{"id":"pay_silent","label":"Swipe the card and deal with insurance later—you cannot fight a hospital corridor.","nextSegmentId":"ins_n2_hit","skillDeltas":{"money":-20,"wisdom":-10}},{"id":"read_escalate","label":"Request the claim manager's email, highlight the room-rent clause, attach bills, and note IRDAI grievance timelines.","nextSegmentId":"ins_n2_informed","skillDeltas":{"wisdom":30,"fiscal_muscle":15}},{"id":"rage_staff","label":"Shout at the billing clerk until they waive something today.","nextSegmentId":"ins_n2_block","skillDeltas":{"social_capital":-25,"wisdom":-15}}]},"ins_n2_hit":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/ins_n2_hit.mp3","text":"Months later, appeals time out. The lesson lands on savings you did not plan to spend—fine print ignored becomes fine print enforced.","choices":[]},"ins_n2_informed":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/ins_n2_informed.mp3","text":"Partial relief arrives; more importantly, at renewal you choose a room category that matches reality. Reading the boring pages becomes a family ritual. Insurance literacy is love translated into clauses.","choices":[]},"ins_n2_block":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/ins_n2_block.mp3","text":"Security is called; notes mention 'aggressive behavior.' The bill stands. Anger vented at the wrong desk closed doors the policy office might have opened.","choices":[]}}}$b2_12g$,
  $b2_12p$Open your policy PDF and highlight room rent, co-pay, and waiting periods—one highlighter color each.$b2_12p$,
  $b2_12j$["What questions should you ask before buying family floater?", "Why is the billing clerk not the enemy in a claim dispute?"]$b2_12j$::jsonb,
  $b2_12n$DRAFT batch 2.$b2_12n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Lost Receipt (en)',
  $b2_13$# The Lost Receipt

**Ethics** simulator: a defective product but no proof of purchase. Choices include forging a receipt, honest alternate proof with a manager, or swallowing the loss.

**Lesson:** Honesty often unlocks goodwill; fraud burns trust fast when systems log truth.$b2_13$,
  'Learn · Simulator · Ethics',
  'Learn · Simulator · Ethics',
  'en',
  17,
  'Shopper',
  95,
  2.0,
  $b2_13m$Integrity is doing the right thing when the policy sign is against you—and accepting grace or no gracefully.$b2_13m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_13g$ {"startSegmentId":"rcpt_n1_return","overlayStyle":"CARDS","segments":{"rcpt_n1_return":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rcpt_n1_return.mp3","text":"The earbuds hiss on day four. The box is gone; the receipt was in a pocket that went through the wash. The store policy sign says 'receipt mandatory.' A forum post suggests 'just make one in Photoshop.'","choices":[{"id":"fake_receipt","label":"Create a fake receipt image—big chains will not check hard.","nextSegmentId":"rcpt_n2_caught","skillDeltas":{"wisdom":-30,"social_capital":-20}},{"id":"honest_proof","label":"Visit the store with bank SMS, serial photo, and ask for a manager exception politely.","nextSegmentId":"rcpt_n2_store_help","skillDeltas":{"wisdom":25,"social_capital":10}},{"id":"keep_broken","label":"Eat the loss and say nothing—honesty is expensive.","nextSegmentId":"rcpt_n2_bitter","skillDeltas":{"balance":-15,"money":-5}}]},"rcpt_n2_caught":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rcpt_n2_caught.mp3","text":"Their system shows a different transaction ID. Security keeps the chat. The embarrassment lasts longer than the refund would have. Shortcuts in ethics rarely stay secret.","choices":[]},"rcpt_n2_store_help":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rcpt_n2_store_help.mp3","text":"The manager matches your card last-four to their POS log. A store credit appears—not policy, but grace meeting honesty halfway. You leave proud of the boring truth.","choices":[]},"rcpt_n2_bitter":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rcpt_n2_bitter.mp3","text":"You stare at the dead buds and resent the brand. Integrity untested still erodes self-respect in small bites.","choices":[]}}}$b2_13g$,
  $b2_13p$Find a warranty or return policy for something you own and read the 'proof of purchase' line aloud with a parent.$b2_13p$,
  $b2_13j$["When does a small lie feel harmless but grow heavy?", "What honest proof might a store accept besides a receipt?"]$b2_13j$::jsonb,
  $b2_13n$DRAFT batch 2.$b2_13n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Grandparent''s Wallet — UPI (en)',
  $b2_14$# The Grandparent's Wallet — UPI

**Digital safety / finance hygiene** simulator: a senior's first UPI setup faces a phishing link disguised as a 'security update.' Paths: enter PIN on a fake page, use only the bank app with verification, or complete first payment at a branch with help.

**Lesson:** PIN and OTP stay inside official apps; when in doubt, slow down and involve trusted help.$b2_14$,
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  72,
  'Family',
  120,
  2.0,
  $b2_14m$Digital hygiene for UPI is the same as locking the front door—verify the app, verify the name, never rush the PIN.$b2_14m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v2',
  $b2_14g$ {"startSegmentId":"upi_n1_dadi","overlayStyle":"CARDS","segments":{"upi_n1_dadi":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/upi_n1_dadi.mp3","text":"Dadi finally agreed to try UPI for the milk vendor. A 'helpful' neighbor messages a link: 'Official RBI security update—enter PIN once to activate safe mode.' Her grandson is stuck in traffic. She trusts printed words that look official.","choices":[{"id":"neighbor_link","label":"Open the link and enter her UPI PIN as shown—she wants to finish before sunset.","nextSegmentId":"upi_n2_drained","skillDeltas":{"money":-30,"DIGITAL_WISDOM":-25}},{"id":"bank_app_only","label":"Close the message, open only her bank's official app, verify the milkman's UPI name character by character, send a tiny test rupee first.","nextSegmentId":"upi_n2_safe","skillDeltas":{"DIGITAL_WISDOM":30,"wisdom":15}},{"id":"branch_trip","label":"Walk to the bank branch with her passbook and ask staff to supervise the first real payment.","nextSegmentId":"upi_n2_guided","skillDeltas":{"DIGITAL_WISDOM":25,"balance":15}}]},"upi_n2_drained":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/upi_n2_drained.mp3","text":"The account empties before the grandson arrives. Recovery is uncertain; shame mixes with anger at official-looking fraud. Digital hygiene starts with refusing PINs outside the real app—no matter the hurry or the neighbor's confidence.","choices":[]},"upi_n2_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/upi_n2_safe.mp3","text":"The milkman laughs when one rupee pings; Dadi laughs too. She repeats the rule: PIN only inside the bank's green icon, names checked like spices on a jar. Pride replaces fear—small steps, loud verification.","choices":[]},"upi_n2_guided":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/upi_n2_guided.mp3","text":"The teller applauds her patience. The first full payment happens under fluorescent lights, slow and sure. Digital hygiene can include human scaffolding until confidence grows.","choices":[]}}}$b2_14g$,
  $b2_14p$Sit with a senior and label their real bank app icon together; delete suspicious 'helper' APKs if any.$b2_14p$,
  $b2_14j$["Why should grandchildren normalize saying 'show me the app icon first'?", "What is a safe first payment size when learning UPI?"]$b2_14j$::jsonb,
  $b2_14n$DRAFT batch 2.$b2_14n$
);
