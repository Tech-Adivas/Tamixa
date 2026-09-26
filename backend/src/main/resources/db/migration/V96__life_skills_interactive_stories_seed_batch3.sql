-- Life skills interactive simulators batch 3 (English). DRAFT.
-- story_owner seed:life-skills-interactive-pack-v3

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Gold Loan Dilemma (en)',
  $b3_0$# The Gold Loan Dilemma

**Finance** simulator for adults and seniors: emergency surgery money is needed, and your brother-in-law asks you to pledge family gold at a local pawnbroker versus routing through a formal bank gold loan with documented rates and tenure.

**Lesson:** Collateral is not abstract—compare annualized interest, receipts, and recovery rights before you sign over heirlooms.$b3_0$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  52,
  'Family',
  118,
  2.0,
  $b3_0m$Pawnbrokers can charge punishing monthly rates; banks offer paper trails and predictable foreclosure rules—speed is not the same as safety.$b3_0m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_0g$ {"startSegmentId":"gld_n1_hook","overlayStyle":"CARDS","segments":{"gld_n1_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/gld_n1_hook.mp3","text":"Your brother-in-law calls at dusk. His father needs emergency surgery; the hospital wants a deposit tonight. He needs two lakh rupees until insurance reimburses. Your wife's eyes fill—those bangles were her grandmother's. He whispers that a local pawnbroker will 'just hold' the gold for three months, no questions, no bank paperwork. Your chest tightens: family love, fear for an elder, and the weight of melting something sacred for speed.","choices":[{"id":"pawn_shop","label":"Agree and go to the local pawnbroker tonight—surgery cannot wait.","nextSegmentId":"gld_n2_pawn","skillDeltas":{"money":-12,"wisdom":-10,"balance":-18}},{"id":"bank_gold_loan","label":"Insist on a scheduled bank gold loan tomorrow morning; offer to drive him and carry the documents.","nextSegmentId":"gld_n3_bank","skillDeltas":{"wisdom":20,"fiscal_muscle":15}}]},"gld_n2_pawn":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/gld_n2_pawn.mp3","text":"The broker weighs the ornaments under a bare bulb and slides a chit: three percent per month, compounded if you slip—thirty-six percent a year if math is honest. Your brother-in-law pays the first month from borrowed cash, then misses the second when the pharmacy bill spikes. The broker's tone hardens: melt date, auction talk, 'rules are rules.' You learn that handshakes do not pause compound interest—and that fear moved faster than reading the fine print.","choices":[]},"gld_n3_bank":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/gld_n3_bank.mp3","text":"The branch opens; the gold loan desk stamps photos, KYC, and a fixed EMI. The rate is lower; the tenure is printed; partial prepayment is allowed. It costs one anxious morning, not zero stress—but when the hospital receipt is filed, the chain of custody is clear. Your wife exhales: dignity lived in procedure, not panic.","choices":[]}}}$b3_0g$,
  $b3_0p$Look up one bank's published gold-loan rate sheet and write the EMI formula you would use for two lakh over twelve months.$b3_0p$,
  $b3_0j$["Ask your elders: Why was Sunaar (the goldsmith) the only bank in the old days, and why is it dangerous now?", "When is speed worth more than a paper trail—and when is it not?"]$b3_0j$::jsonb,
  $b3_0n$DRAFT batch 3; segment MP3s are placeholders.$b3_0n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Deepfake Accident — Grandson Call (en)',
  $b3_1$# The Deepfake "Accident"

**Tech safety** simulator for families and seniors: a voice that sounds exactly like a grandson begs for instant UPI payment for a fake police fine.

**Lesson:** Voice can be cloned—verify on a second channel you already trust before any transfer.$b3_1$,
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  68,
  'Dadu',
  105,
  2.0,
  $b3_1m$Panic plus love is the scammer's recipe; a callback to a saved number breaks most traps.$b3_1m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_1g$ {"startSegmentId":"dfa_n1_call","overlayStyle":"CARDS","segments":{"dfa_n1_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dfa_n1_call.mp3","text":"The phone vibrates during tea. A young voice cracks—Aryan, your grandson—crying that there was a minor accident, police took his phone, and he must pay twenty thousand rupees G-Pay now or they will jail him. 'Dadu, please, don't tell Mummy yet.' Your hands shake; the UPI ID glows on screen.","choices":[{"id":"send_now","label":"Send the money immediately—what if it is really him?","nextSegmentId":"dfa_n2_scam","skillDeltas":{"money":-25,"DIGITAL_WISDOM":-25}},{"id":"verify_saved","label":"Hang up and call Aryan's mother or his number saved in your phone.","nextSegmentId":"dfa_n3_safe","skillDeltas":{"wisdom":30,"DIGITAL_WISDOM":20}}]},"dfa_n2_scam":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dfa_n2_scam.mp3","text":"The payment confirms; silence follows. When your daughter calls back, Aryan is confused—he has been in the college library all day. The cyber helpline says cloned voice fraud is rising. The money is likely gone. Shame mixes with anger at how perfectly fear erased doubt.","choices":[]},"dfa_n3_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/dfa_n3_safe.mp3","text":"Your daughter answers; she hands the phone to Aryan—chewing chips, annoyed at the interruption, perfectly fine. Together you screenshot the fake UPI ID and file a report. You practice one sentence aloud: 'I only pay people I have verified on a second call.'","choices":[]}}}$b3_1g$,
  $b3_1p$Add a home rule: no UPI above a small limit without a video or voice callback on a known number.$b3_1p$,
  $b3_1j$["What secret question could only the real grandchild answer?", "Why do scammers beg you not to tell parents?"]$b3_1j$::jsonb,
  $b3_1n$DRAFT batch 3.$b3_1n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Society Secretary (en)',
  $b3_2$# The Society Secretary

**Leadership** simulator: as new housing society secretary, you face a maintenance boycott over elevator noise and angry neighbors demanding harsh action.

**Lesson:** Coercion escalates feuds; facilitated listening plus expertise repairs trust faster than cutoffs.$b3_2$,
  'Learn · Simulator · Leadership',
  'Learn · Simulator · Leadership',
  'en',
  44,
  'Secretary',
  112,
  2.0,
  $b3_2m$Authority used as punishment breeds lawsuits and factions; process and listening turn noise complaints into fixable engineering tasks.$b3_2m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_2g$ {"startSegmentId":"sec_n1_khanna","overlayStyle":"CARDS","segments":{"sec_n1_khanna":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/sec_n1_khanna.mp3","text":"The WhatsApp group erupts. Mr. Khanna has not paid maintenance for two months, claiming the new elevator 'vibrates like a drill' into his bedroom. Others post bills they paid on time; someone demands his water be cut. You are new in the chair; the managing committee wants a 'strong message.'","choices":[{"id":"legal_water_cut","label":"Issue a legal notice and authorize cutting his water until he pays.","nextSegmentId":"sec_n2_coercion","skillDeltas":{"authority":10,"harmony":-30}},{"id":"chai_meeting","label":"Schedule a chai meeting with Mr. Khanna, two neutral residents, and the elevator AMC technician.","nextSegmentId":"sec_n3_facilitate","skillDeltas":{"social_capital":25,"harmony":20}}]},"sec_n2_coercion":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/sec_n2_coercion.mp3","text":"Lawyers exchange letters; Khanna counters with a harassment claim. The gate log shows shouting matches; families pick sides. The elevator noise is still unmeasured; cash flow for repairs worsens because bitterness replaced diagnosis.","choices":[]},"sec_n3_facilitate":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/sec_n3_facilitate.mp3","text":"In the clubhouse, the technician clips a decibel app, tightens a rail bracket, and schedules a damping pad. Khanna admits he felt unheard. You draft a one-page 'noise complaint protocol.' He pays arrears in installments with a signed plan. The group learns that leadership sometimes tastes like tea, not thunder.","choices":[]}}}$b3_2g$,
  $b3_2p$Write a three-step process your building could use for maintenance disputes before any cutoff threat.$b3_2p$,
  $b3_2j$["When does a secretary protect the collective without humiliating one flat?", "How do you bring expertise into the room without taking sides?"]$b3_2j$::jsonb,
  $b3_2n$DRAFT batch 3.$b3_2n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Stock Tip WhatsApp Group (en)',
  $b3_3$# The "Stock Tip" WhatsApp Group

**Finance / tech** simulator: you are pulled into a hype group pushing penny-stock screenshots and FOMO.

**Lesson:** Pump-and-dump schemes dress as community; verify on SEBI-registered channels before risking emergency savings.$b3_3$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  29,
  'Investor',
  108,
  2.0,
  $b3_3m$If everyone in chat is winning, the exit liquidity is probably you—research beats screenshots.$b3_3m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_3g$ {"startSegmentId":"stkt_n1_group","overlayStyle":"CARDS","segments":{"stkt_n1_group":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/stkt_n1_group.mp3","text":"Your cousin adds you to 'India Bull Run 500%.' Pinned messages celebrate a penny stock; an 'expert' posts voice notes; dozens paste broker screenshots of overnight doubles. Your emergency fund sits fifty thousand liquid—tempting, because rent is covered this month.","choices":[{"id":"yolo_emergency","label":"Deploy your full emergency fund now—double it before the window closes.","nextSegmentId":"stkt_n2_dump","skillDeltas":{"money":-22,"wisdom":-20}},{"id":"sebi_research","label":"Search the company on SEBI-scraper sites, exchange circulars, and independent news before a rupee moves.","nextSegmentId":"stkt_n3_sober","skillDeltas":{"wisdom":20,"integrity":10}}]},"stkt_n2_dump":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/stkt_n2_dump.mp3","text":"You buy high on volume hype; three sessions later, upper circuits reverse into freeze. The group admin mutes complaints; numbers go dead. The cousin apologizes—he was paid to add members. Your emergency cushion is a lesson etched in red.","choices":[]},"stkt_n3_sober":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/stkt_n3_sober.mp3","text":"Filings show promoter pledges and sketchy related-party deals. You stay out; you warn one friend privately. The stock still spikes for others—then collapses weeks later. You learn that boring homework outperforms adrenaline in finance.","choices":[]}}}$b3_3g$,
  $b3_3p$Bookmark one SEBI investor education page and read it once this week.$b3_3p$,
  $b3_3j$["Why are screenshots not evidence of profit?", "What is an emergency fund for—if not for emergencies?"]$b3_3j$::jsonb,
  $b3_3n$DRAFT batch 3.$b3_3n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Interview Jitters (en)',
  $b3_4$# The Interview Jitters

**Communication** simulator for teens and young adults: a fast English question triggers panic, but you know the answer in your mother tongue.

**Lesson:** Confidence often comes from asking for clarity—not from performing vocabulary you do not own.$b3_4$,
  'Learn · Simulator · Communication',
  'Learn · Simulator · Communication',
  'en',
  21,
  'Candidate',
  110,
  2.0,
  $b3_4m$Panels respect clear thinking in simple words more than fluent-sounding confusion.$b3_4m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_4g$ {"startSegmentId":"intj_n1_question","overlayStyle":"CARDS","segments":{"intj_n1_question":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/intj_n1_question.mp3","text":"The interviewer fires a long question about system design trade-offs, words blurring together. Your heart hammers; you understand the concept in Gujarati in your head, but English feels like a locked door. Silence stretches; you feel judged already.","choices":[{"id":"big_words","label":"String together impressive English words even if you are stuttering.","nextSegmentId":"intj_n2_muddle","skillDeltas":{"confidence":-15,"clarity":-20}},{"id":"ask_simple","label":"Say politely: \"Can I explain this simply? I want my logic to be clear.\"","nextSegmentId":"intj_n3_clear","skillDeltas":{"confidence":20,"clarity":25}}]},"intj_n2_muddle":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/intj_n2_muddle.mp3","text":"You deploy jargon half-understood; a follow-up exposes the gap. The panel notes kindness but confusion. Walking out, you wish you had named your uncertainty instead of decorating it.","choices":[]},"intj_n3_clear":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/intj_n3_clear.mp3","text":"You draw boxes on the whiteboard, label them slowly, and check in: 'Is this the constraint you meant?' The interviewer leans in. You leave knowing you were seen for thinking, not for accent theatre.","choices":[]}}}$b3_4g$,
  $b3_4p$Practice one technical answer in two speeds: 30 seconds plain, then 90 seconds detailed—in your own words.$b3_4p$,
  $b3_4j$["When is asking for clarification a strength?", "How is clarity different from fluency?"]$b3_4j$::jsonb,
  $b3_4n$DRAFT batch 3.$b3_4n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Secret Credit Card (en)',
  $b3_5$# The Secret Credit Card

**Finance** simulator: first card with a one-lakh limit meets a ninety-thousand-rupee gaming laptop during a sale.

**Lesson:** High utilization and minimum payments erode scores and sleep; cash-budget purchases keep margin for real life.$b3_5$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  22,
  'Cardholder',
  102,
  2.0,
  $b3_5m$A limit is not income; match big buys to money you already have, not to minimum-due math.$b3_5m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_5g$ {"startSegmentId":"scc_n1_sale","overlayStyle":"CARDS","segments":{"scc_n1_sale":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/scc_n1_sale.mp3","text":"Amazon lights up: Republic Day sale, zero-cost EMI banners, a gaming laptop at ninety thousand—ten below your entire credit line. Friends say swipe now, think later; 'minimum due is pocket change.' Your savings account holds thirty thousand for rent buffer.","choices":[{"id":"swipe_max","label":"Buy the laptop on the card; figure out minimum dues later.","nextSegmentId":"scc_n2_trap","skillDeltas":{"money":-22,"balance":-18}},{"id":"cash_fit","label":"Buy a capable machine within your thirty thousand cash budget—or wait one more sale cycle.","nextSegmentId":"scc_n3_discipline","skillDeltas":{"fiscal_muscle":30,"wisdom":10}}]},"scc_n2_trap":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/scc_n2_trap.mp3","text":"Interest and processing fees creep; utilization stays above eighty percent; a pre-approved loan offer arrives like mockery. The laptop thrills for a month; the statement anxiety lasts longer than any game session.","choices":[]},"scc_n3_discipline":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/scc_n3_discipline.mp3","text":"You research a refurbished unit within cash. Credit utilization stays low; autopay stays boring. You game happily enough—and when a real emergency hits, the card is still a safety net, not a shovel.","choices":[]}}}$b3_5g$,
  $b3_5p$Calculate thirty percent of your credit limit and write that number on a sticky note as a spending ceiling.$b3_5p$,
  $b3_5j$["What is the difference between a credit limit and savings?", "Why do minimum dues grow the hole?"]$b3_5j$::jsonb,
  $b3_5n$DRAFT batch 3.$b3_5n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Plagiarism Dilemma (en)',
  $b3_6$# The Plagiarism Dilemma

**Ethics** simulator for teens: a paid 'unique' thesis offer versus an honest incomplete submission.

**Lesson:** Short-term grades bought with fraud corrode skill and reputation; vulnerability with a teacher can open real help.$b3_6$,
  'Learn · Simulator · Ethics',
  'Learn · Simulator · Ethics',
  'en',
  17,
  'Student',
  100,
  2.0,
  $b3_6m$Integrity is built when deadlines bite—choose the awkward truth over a polished lie.$b3_6m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_6g$ {"startSegmentId":"plag_n1_offer","overlayStyle":"CARDS","segments":{"plag_n1_offer":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/plag_n1_offer.mp3","text":"The final project is due tomorrow; the doc is blank. A site promises a unique thesis for five hundred rupees, untraceable, 'AI-checked.' Your friend says everyone does it. Your stomach twists—you actually care about the subject.","choices":[{"id":"pay_plagiarism","label":"Pay five hundred and submit the file tonight.","nextSegmentId":"plag_n2_exposed","skillDeltas":{"integrity":-35,"wisdom":-12}},{"id":"honest_partial","label":"Work through the night, submit incomplete work, and email the teacher the truth in the morning.","nextSegmentId":"plag_n3_grace","skillDeltas":{"integrity":40,"wisdom":20}}]},"plag_n2_exposed":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/plag_n2_exposed.mp3","text":"The viva asks for a derivation you never read; the tool flags similarity anyway. An integrity committee email lands. The grade zero hurts less than the mirror—you traded your voice for a PDF.","choices":[]},"plag_n3_grace":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/plag_n3_grace.mp3","text":"You hand in rough sections with sticky notes marking gaps. The teacher grants a guarded extension and a tutor slot. The final grade is modest, but the skills are yours—and sleep returns without dread.","choices":[]}}}$b3_6g$,
  $b3_6p$Write one paragraph in your own words on the hardest topic—run it through your school's policy checklist.$b3_6p$,
  $b3_6j$["When does 'help' cross into misrepresentation?", "Who benefits when students buy essays?"]$b3_6j$::jsonb,
  $b3_6n$DRAFT batch 3.$b3_6n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Customer Is Rude — Cafe (en)',
  $b3_7$# The Customer Is Rude

**Business** simulator: a small cafe, a shouting customer, and the room watching.

**Lesson:** Calm replacement plus clear explanation protects brand, staff dignity, and other guests better than volume for volume.$b3_7$,
  'Learn · Simulator · Business',
  'Learn · Simulator · Business',
  'en',
  26,
  'Owner',
  104,
  2.0,
  $b3_7m$Professionalism is low voice, fast fix, visible fairness—not matching insult for insult.$b3_7m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_7g$ {"startSegmentId":"cusr_n1_coffee","overlayStyle":"CARDS","segments":{"cusr_n1_coffee":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cusr_n1_coffee.mp3","text":"A customer slams a cup on the counter—coffee 'not hot enough,' voice rising, phone camera half-lifted. Regulars shift uncomfortably; your barista freezes.","choices":[{"id":"shout_back","label":"Shout back and tell them to leave if they cannot behave.","nextSegmentId":"cusr_n2_scene","skillDeltas":{"status":-12,"business_health":-22}},{"id":"replace_calm","label":"Replace the drink quietly, offer a small cookie, explain peak-hour wait times without blaming them.","nextSegmentId":"cusr_n3_pro","skillDeltas":{"status":20,"business_health":15}}]},"cusr_n2_scene":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cusr_n2_scene.mp3","text":"A clipped video circulates—context lost. One-star reviews mention 'rude owner.' Staff morale dips; you replay the moment you chose ego over de-escalation.","choices":[]},"cusr_n3_pro":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/cusr_n3_pro.mp3","text":"The steam wand hisses; a fresh cup goes out. The line relaxes; someone mouths thank you. The rude customer leaves still muttering—but without a viral moment. Your team copies your tone the next week. That is culture.","choices":[]}}}$b3_7g$,
  $b3_7p$Role-play: friend plays angry customer—you practice square breathing before speaking.$b3_7p$,
  $b3_7j$["How do you protect staff while serving a harsh customer?", "When is refusing service better than a free replacement?"]$b3_7j$::jsonb,
  $b3_7n$DRAFT batch 3.$b3_7n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Senior''s First UPI — Stranger Help (en)',
  $b3_8$# The Senior's First UPI

**Tech safety** simulator at a grocery QR: a stranger offers to 'handle' the phone and PIN.

**Lesson:** PIN never leaves your hand; polite refusal is digital hygiene.$b3_8$,
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  71,
  'Dada',
  98,
  2.0,
  $b3_8m$Help that needs your PIN is not help—use the shopkeeper or family, not a stranger's fingers.$b3_8m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_8g$ {"startSegmentId":"upi2_n1_qr","overlayStyle":"CARDS","segments":{"upi2_n1_qr":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/upi2_n1_qr.mp3","text":"At the kirana, Dada wants to try the QR taped to the glass. A helpful stranger steps in: 'Uncle, give me the phone—I will scan and type the PIN fast; the queue is long.' The shopkeeper is busy weighing dal.","choices":[{"id":"hand_phone","label":"Hand over the phone—he seems confident.","nextSegmentId":"upi2_n2_drained","skillDeltas":{"money":-12,"wisdom":-18}},{"id":"self_try","label":"Ask the shopkeeper to wait; you will try yourself step by step.","nextSegmentId":"upi2_n3_safe","skillDeltas":{"wisdom":20,"DIGITAL_WISDOM":25}}]},"upi2_n2_drained":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/upi2_n2_drained.mp3","text":"Two quick transfers later—groceries unpaid, savings lighter—the stranger melts into the street. The shopkeeper sighs: 'Uncle, PIN is like your house key.' Shame and lesson arrive together.","choices":[]},"upi2_n3_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/upi2_n3_safe.mp3","text":"You shield the PIN; the first payment is slow and correct. The queue waits without jeering. Walking home, you teach your grandchild one rule: 'No PIN on anyone else's screen.'","choices":[]}}}$b3_8g$,
  $b3_8p$Practice covering the PIN pad with your palm in front of a mirror once.$b3_8p$,
  $b3_8j$["How can kids help grandparents without touching the PIN?", "What phrase can seniors memorize to refuse 'quick help'?"]$b3_8j$::jsonb,
  $b3_8n$DRAFT batch 3.$b3_8n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Wedding Budget War (en)',
  $b3_9$# The Wedding Budget War

**Finance / social** simulator: five-star venue pressure versus transparent family math on loans and retirement.

**Lesson:** Social debt often outlasts the album; shared numbers build consent.$b3_9$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  48,
  'Parent',
  108,
  2.0,
  $b3_9m$Love for a child does not require bankrupting the next decade—clarity beats keeping up appearances.$b3_9m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_9g$ {"startSegmentId":"wbw_n1_venue","overlayStyle":"CARDS","segments":{"wbw_n1_venue":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wbw_n1_venue.mp3","text":"Six months to the wedding. Your wife forwards a five-star ballroom quote—double what the spreadsheet allows. 'What will people say if we use the community hall?' Your daughter looks torn between joy and guilt.","choices":[{"id":"personal_loan_face","label":"Take a personal loan so the venue matches expectations—figure out EMIs later.","nextSegmentId":"wbw_n2_debt","skillDeltas":{"money":-25,"social_capital":18}},{"id":"show_five_year","label":"Sit everyone down with a five-year cash-flow sketch: loan EMI vs retirement and emergencies.","nextSegmentId":"wbw_n3_align","skillDeltas":{"fiscal_muscle":40,"balance":12}}]},"wbw_n2_debt":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wbw_n2_debt.mp3","text":"The sangeet sparkles; emojis pour in. Years later the EMI competes with medical bills. Your daughter says she would have chosen smaller if the numbers had been spoken aloud at the start. Status borrowed from tomorrow charges interest on family peace.","choices":[]},"wbw_n3_align":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/wbw_n3_align.mp3","text":"Tears and laughter mix around the spreadsheet. You choose a warm hall, splurge on food people remember, skip the chandelier tax. Guests still dance; your pension breathes. The lesson lands: dignity is not a star rating.","choices":[]}}}$b3_9g$,
  $b3_9p$List three wedding costs you would cut first if budget halved—and ask one family member their top non-negotiable.$b3_9p$,
  $b3_9j$["How do you separate love from display?", "Who is absent from the room when loans are decided in silence?"]$b3_9j$::jsonb,
  $b3_9n$DRAFT batch 3.$b3_9n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Viral Rumor — Plastic Salt (en)',
  $b3_10$# The Viral Rumor

**Ethics / tech** simulator: a WhatsApp video claims plastic in salt and begs mass forwarding.

**Lesson:** Chain messages exploit care; verify with official or fact-check sources before you amplify fear.$b3_10$,
  'Learn · Simulator · Ethics',
  'Learn · Simulator · Ethics',
  'en',
  55,
  'Family',
  102,
  2.0,
  $b3_10m$Forwarding without checking trades trust for panic—research is the real kindness.$b3_10m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_10g$ {"startSegmentId":"viru_n1_salt","overlayStyle":"CARDS","segments":{"viru_n1_salt":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/viru_n1_salt.mp3","text":"A video shows crystals and a stern voice: plastic in a famous salt brand—'forward to ten people to save lives.' Your aunt already shared it; red ticks multiply.","choices":[{"id":"forward_all","label":"Forward immediately to every family group—better safe than sorry.","nextSegmentId":"viru_n2_panic","skillDeltas":{"integrity":-20,"wisdom":-12}},{"id":"fact_check","label":"Check a fact-check site and the FSSAI or brand statement before sharing.","nextSegmentId":"viru_n3_clarity","skillDeltas":{"wisdom":30,"integrity":10}}]},"viru_n2_panic":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/viru_n2_panic.mp3","text":"Small shops pull bags off shelves; neighbors argue at the ration shop. A lab later shows the clip was recycled from another country. You feel small knowing fear made you a broadcaster, not a reader.","choices":[]},"viru_n3_clarity":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/viru_n3_clarity.mp3","text":"Official tests confirm salt is fine; you post one link with a calm sentence. A cousin deletes her forward. Boring verification beats heroic rumor.","choices":[]}}}$b3_10g$,
  $b3_10p$Save one trusted fact-check handle and use it once before your next 'urgent' share.$b3_10p$,
  $b3_10j$["Why does 'share to save lives' bypass our skepticism?", "How do you correct elders without shaming them?"]$b3_10j$::jsonb,
  $b3_10n$DRAFT batch 3.$b3_10n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Exam Stress Escape (en)',
  $b3_11$# The Exam Stress Escape

**Mental health** simulator after failing a math mock: hide and quit versus asking for a new learning path.

**Lesson:** Resilience includes showing the paper and requesting help—not disappearing from class.$b3_11$,
  'Learn · Simulator · Mental Health',
  'Learn · Simulator · Mental Health',
  'en',
  14,
  'Student',
  100,
  2.0,
  $b3_11m$Shame grows in secrecy; data plus support turns a bad mark into a map.$b3_11m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_11g$ {"startSegmentId":"exm2_n1_fail","overlayStyle":"CARDS","segments":{"exm2_n1_fail":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/exm2_n1_fail.mp3","text":"The math mock comes back red. Friends compare marks in the corridor; you want to vanish. Tuition flyers feel like judgment. The urge to crumple the paper is loud.","choices":[{"id":"hide_quit","label":"Hide the marks from parents and stop attending classes for a while.","nextSegmentId":"exm2_n2_spiral","skillDeltas":{"balance":-28,"integrity":-18}},{"id":"show_ask_help","label":"Show your parents the paper and ask to try a different tutor or method.","nextSegmentId":"exm2_n3_repair","skillDeltas":{"balance":22,"wisdom":15}}]},"exm2_n2_spiral":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/exm2_n2_spiral.mp3","text":"Absences stack; the gap widens; parents find out from the school portal anyway—now with anger at the hiding. You learn too late that silence made the math harder, not easier.","choices":[]},"exm2_n3_repair":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/exm2_n3_repair.mp3","text":"Your mother winces at the percent, then schedules a diagnostic test with a kind teacher. Topics shrink to bite size. The next mock is not perfect, but it moves. You practice saying, 'I need help' as a skill, not a confession.","choices":[]}}}$b3_11g$,
  $b3_11p$Circle three wrong questions on any old test and redo them with a timer—no grade, just pattern.$b3_11p$,
  $b3_11j$["How is asking for a new method different from giving up?", "What does your body feel when you hide a bad mark?"]$b3_11j$::jsonb,
  $b3_11n$DRAFT batch 3.$b3_11n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Neighborhood Bully — Swing Timer (en)',
  $b3_12$# The Neighborhood Bully

**Leadership** simulator for kids: a bigger child monopolizes the park swing.

**Lesson:** Fair rules and calm voice can claim space without fists.$b3_12$,
  'Learn · Simulator · Leadership',
  'Learn · Simulator · Leadership',
  'en',
  10,
  'Kid',
  95,
  2.0,
  $b3_12m$Assertiveness can sound like a fair timer, not a punch.$b3_12m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_12g$ {"startSegmentId":"nbl2_n1_swing","overlayStyle":"CARDS","segments":{"nbl2_n1_swing":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/nbl2_n1_swing.mp3","text":"The swing chain squeaks. A bigger kid blocks the seat—'This is my spot.' Your friends watch from the slide; your stomach flips.","choices":[{"id":"fight","label":"Push him off to prove you are strong.","nextSegmentId":"nbl2_n2_trouble","skillDeltas":{"harmony":-28,"wisdom":-12}},{"id":"timer_fair","label":"Say loudly: \"We all get five minutes—let's use a phone timer.\"","nextSegmentId":"nbl2_n3_fair","skillDeltas":{"leadership":30,"harmony":20}}]},"nbl2_n2_trouble":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/nbl2_n2_trouble.mp3","text":"Dust flies; a supervisor runs over; both of you sit out. Your friend whispers that swinging mattered less than watching you become scary too.","choices":[]},"nbl2_n3_fair":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/nbl2_n3_fair.mp3","text":"A parent nods; the timer beeps; turns rotate. The bully tests once, then shrugs and waits his round. You learn that leadership can be a rule everyone sees, not a fight nobody wins.","choices":[]}}}$b3_12g$,
  $b3_12p$Practice one fair rule for a game at home with siblings.$b3_12p$,
  $b3_12j$["When is speaking up different from starting a fight?", "Who can kids ask if a bully breaks the timer rule?"]$b3_12j$::jsonb,
  $b3_12n$DRAFT batch 3.$b3_12n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Health Insurance Fine Print — Room Rent (en)',
  $b3_13$# The Health Insurance Fine Print

**Finance** simulator at purchase time: agent says 'everything covered' while a room-rent cap hides in the PDF.

**Lesson:** Ask rupee questions before you sign—especially room category and co-pay.$b3_13$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  40,
  'Buyer',
  110,
  2.0,
  $b3_13m$Trust but verify clauses that cap room rent—otherwise a private room becomes out-of-pocket shock.$b3_13m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_13g$ {"startSegmentId":"hins2_n1_agent","overlayStyle":"CARDS","segments":{"hins2_n1_agent":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/hins2_n1_agent.mp3","text":"The agent slides brochures: 'Full cashless, sir, everything covered.' Your eye catches tiny text—room rent capping at one percent of sum insured per day. He waves it off as 'standard.'","choices":[{"id":"trust_sign","label":"Trust him and sign today to lock the 'discount.'","nextSegmentId":"hins2_n2_bill","skillDeltas":{"money":-22,"wisdom":-12}},{"id":"ask_numbers","label":"Ask exactly what you pay for a private room in your preferred hospital under this cap.","nextSegmentId":"hins2_n3_informed","skillDeltas":{"wisdom":30,"fiscal_muscle":15}}]},"hins2_n2_bill":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/hins2_n2_bill.mp3","text":"Later, admission happens; the insurer pays a slice; a fifty-thousand-rupee gap appears for room upgrade. The agent's voice is busy. Fine print ignored becomes fine print enforced.","choices":[]},"hins2_n3_informed":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/hins2_n3_informed.mp3","text":"You model two scenarios on paper, compare a higher variant, or accept the cap with eyes open. The policy you buy matches the hospitals you actually use. Sleep before illness is cheaper than shock after.","choices":[]}}}$b3_13g$,
  $b3_13p$Highlight room rent, co-pay, and waiting periods in your policy PDF in three colors.$b3_13p$,
  $b3_13j$["What three questions should every buyer ask before paying premium?", "Why do agents minimize caps?"]$b3_13j$::jsonb,
  $b3_13n$DRAFT batch 3.$b3_13n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The AI Artist — Freelance Logo (en)',
  $b3_14$# The AI Artist

**Business / ethics** simulator for freelancers: AI-generated logo passed off as hand-drawn versus transparent hybrid workflow.

**Lesson:** Client trust lives in honest process disclosure; passing off erodes craft and contracts.$b3_14$,
  'Learn · Simulator · Ethics',
  'Learn · Simulator · Ethics',
  'en',
  24,
  'Designer',
  112,
  2.0,
  $b3_14m$Tools are fine; lying about authorship is not—name the pipeline, charge for judgment and refinement.$b3_14m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_14g$ {"startSegmentId":"aia_n1_deadline","overlayStyle":"CARDS","segments":{"aia_n1_deadline":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/aia_n1_deadline.mp3","text":"A logo deadline looms; you are exhausted. Midjourney can draft ten concepts in minutes; the client praised your 'hand-drawn soul' in the brief. No one will zoom pixels, right?","choices":[{"id":"ai_lie_fee","label":"Generate, lightly tweak, deliver as fully hand-crafted for full fee.","nextSegmentId":"aia_n2_caught","skillDeltas":{"integrity":-30,"skill":-12}},{"id":"ai_disclose_redraw","label":"Use AI for rough ideation, redraw vectors yourself, tell the client honestly.","nextSegmentId":"aia_n3_trust","skillDeltas":{"integrity":30,"skill":20}}]},"aia_n2_caught":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/aia_n2_caught.mp3","text":"The client's designer friend recognizes telltale glitches; the contract wobbles; your portfolio comment section asks questions you cannot answer. One shortcut costs repeat work and self-respect.","choices":[]},"aia_n3_trust":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/aia_n3_trust.mp3","text":"You send process slides: prompts, sketches, manual cleanup. The client pays for expertise, not mythology. Referrals mention integrity. Your craft sharpens because you still move the pen.","choices":[]}}}$b3_14g$,
  $b3_14p$Write a one-paragraph 'how I work' blurb listing AI and human steps.$b3_14p$,
  $b3_14j$["When is using AI fair to a paying client?", "What part of design cannot be outsourced to a generator?"]$b3_14j$::jsonb,
  $b3_14n$DRAFT batch 3.$b3_14n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Lost Wallet — Playground (en)',
  $b3_15$# The Lost Wallet

**Ethics** simulator for kids/teens: found cash, no ID, friends want ice cream.

**Lesson:** Found money is not found permission—turn it in and let adults trace the owner.$b3_15$,
  'Learn · Simulator · Ethics',
  'Learn · Simulator · Ethics',
  'en',
  12,
  'Student',
  92,
  2.0,
  $b3_15m$Integrity is choosing the principal's desk over the ice-cream queue.$b3_15m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_15g$ {"startSegmentId":"lwal_n1_find","overlayStyle":"CARDS","segments":{"lwal_n1_find":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/lwal_n1_find.mp3","text":"A worn wallet lies near the swings—five hundred rupees, no ID, no bus pass. Friends circle: 'Split it—cone for everyone!' Laughter feels like permission.","choices":[{"id":"ice_cream","label":"Buy treats for the group—finders keepers.","nextSegmentId":"lwal_n2_guilt","skillDeltas":{"social_capital":10,"integrity":-38}},{"id":"principal","label":"Give the wallet to the school office immediately.","nextSegmentId":"lwal_n3_right","skillDeltas":{"integrity":45,"wisdom":12}}]},"lwal_n2_guilt":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/lwal_n2_guilt.mp3","text":"A younger kid sobs later that lunch money vanished; descriptions match. You stare at your sticky fingers. Cheap sugar tastes sour when you realize whose week you stole.","choices":[]},"lwal_n3_right":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/lwal_n3_right.mp3","text":"The office logs it; a parent claims it the next day with tears. The head mentions honesty in assembly without naming you—but you feel tall walking past the canteen.","choices":[]}}}$b3_15g$,
  $b3_15p$Role-play finding money: what three steps would you take?$b3_15p$,
  $b3_15j$["Why does peer pressure make found money feel okay?", "What would you want someone to do if you lost your wallet?"]$b3_15j$::jsonb,
  $b3_15n$DRAFT batch 3.$b3_15n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Bonus Splurge (en)',
  $b3_16$# The Bonus Splurge

**Finance** simulator: Diwali bonus versus upcoming car insurance and a shiny TV.

**Lesson:** Opportunity cost is invisible until the due date—younger you spends older you's peace.$b3_16$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  34,
  'Earner',
  100,
  2.0,
  $b3_16m$Fund obligations and buffers before lifestyle upgrades—bonuses feel like free money until they are not.$b3_16m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_16g$ {"startSegmentId":"bns_n1_bonus","overlayStyle":"CARDS","segments":{"bns_n1_bonus":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/bns_n1_bonus.mp3","text":"Fifty thousand lands as a Diwali bonus. The 4K TV deal ends Sunday; car insurance is due in eight weeks—roughly the same size as the TV if you squint. The living room feels small; the calendar feels far.","choices":[{"id":"tv_first","label":"Buy the TV now; next salary can handle insurance somehow.","nextSegmentId":"bns_n2_scramble","skillDeltas":{"social_capital":18,"balance":-22}},{"id":"insurance_reserve","label":"Park the insurance amount in a separate sub-account; buy a smaller TV or wait for sale.","nextSegmentId":"bns_n3_smooth","skillDeltas":{"fiscal_muscle":30,"balance":12}}]},"bns_n2_scramble":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/bns_n2_scramble.mp3","text":"The TV glows gorgeous; the insurance reminder arrives during a tight month; you borrow on a card. Interest nibbles the 'free' bonus. You learn opportunity cost has a sound—EMI alerts at midnight.","choices":[]},"bns_n3_smooth":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/bns_n3_smooth.mp3","text":"Movie night waits two months; the car papers renew without drama. The older TV still works; peace of mind is its own OLED.","choices":[]}}}$b3_16g$,
  $b3_16p$List two upcoming mandatory expenses in the next quarter and assign them envelopes today.$b3_16p$,
  $b3_16j$["Why do windfalls feel easier to spend than salary?", "How do you decide want vs need with bonus money?"]$b3_16j$::jsonb,
  $b3_16n$DRAFT batch 3.$b3_16n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Remote Work Trust (en)',
  $b3_17$# The Remote Work Trust

**Leadership / integrity** simulator: empty calendar after quick tasks—movie versus upskilling or helping a teammate.

**Lesson:** Professional integrity compounds invisibly; slack today is a loan from tomorrow's reputation.$b3_17$,
  'Learn · Simulator · Leadership',
  'Learn · Simulator · Leadership',
  'en',
  31,
  'Professional',
  108,
  2.0,
  $b3_17m$Trust remote work gives is repaid with predictable output and growth—not with empty hours hidden.$b3_17m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_17g$ {"startSegmentId":"rwt_n1_home","overlayStyle":"CARDS","segments":{"rwt_n1_home":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rwt_n1_home.mp3","text":"You finish core tickets by eleven; the standup praised your speed. Your boss is offline at a summit. A matinee ticket pings—no one would know.","choices":[{"id":"movie","label":"Log off and catch the film—you earned the break.","nextSegmentId":"rwt_n2_slip","skillDeltas":{"integrity":-20}},{"id":"upskill_help","label":"Use the window to document a runbook and unblock a junior stuck on your dependency.","nextSegmentId":"rwt_n3_growth","skillDeltas":{"integrity":20,"skill":28}}]},"rwt_n2_slip":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rwt_n2_slip.mp3","text":"It becomes a pattern; standup estimates drift; your manager notices velocity variance without explanation. Remote trust is a glass desk—scratches show.","choices":[]},"rwt_n3_growth":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/rwt_n3_growth.mp3","text":"The junior ships; your doc becomes team canon; lead mentions you in retro. Integrity did not feel cinematic—it felt like typing when no one clapped.","choices":[]}}}$b3_17g$,
  $b3_17p$Block one calendar hour weekly labeled 'proactive'—documentation, learning, or pairing.$b3_17p$,
  $b3_17j$["How do adults define 'enough' work in a day at home?", "When is a break earned versus stolen?"]$b3_17j$::jsonb,
  $b3_17n$DRAFT batch 3.$b3_17n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Fake Parcel Call — FedEx Customs (en)',
  $b3_18$# The Fake "Parcel" Call

**Tech safety** simulator: 'FedEx' plus fake customs plus police bridge—classic authority scam.

**Lesson:** Hang up; verify on official courier tools and real police numbers—never stay on their line.$b3_18$,
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  52,
  'Recipient',
  105,
  2.0,
  $b3_18m$Legitimate couriers do not threaten arrest over the phone or demand instant transfers to 'clear' fake parcels.$b3_18m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_18g$ {"startSegmentId":"fpc_n1_call","overlayStyle":"CARDS","segments":{"fpc_n1_call":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/fpc_n1_call.mp3","text":"A crisp voice says FedEx; a parcel in your name allegedly holds illegal items; Mumbai customs has flagged it; they will connect you to 'Inspector' now. Your pulse spikes— you did order supplements online once.","choices":[{"id":"stay_on_line","label":"Stay on the line and follow instructions to clear your name.","nextSegmentId":"fpc_n2_bled","skillDeltas":{"money":-30,"DIGITAL_WISDOM":-30}},{"id":"hangup_verify","label":"Hang up; open the courier's official site or app; call the police helpline from a government page if worried.","nextSegmentId":"fpc_n3_safe","skillDeltas":{"wisdom":35,"DIGITAL_WISDOM":28}}]},"fpc_n2_bled":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/fpc_n2_bled.mp3","text":"'Verification fees' and 'court deposits' drain lakhs; the line goes dead. The real FedEx chatbot says no such hold exists. Authority theater stole your calm first, then your account.","choices":[]},"fpc_n3_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/fpc_n3_safe.mp3","text":"Official tracking shows no mystery parcel; the cyber cell confirms the script. You teach your building group the pattern. Verification beats performance anxiety.","choices":[]}}}$b3_18g$,
  $b3_18p$Save three official helpline numbers from .gov or bank sites—never from random SMS.$b3_18p$,
  $b3_18j$["Why do scammers impersonate both courier and police?", "What is a safe way to verify a scary call?"]$b3_18j$::jsonb,
  $b3_18n$DRAFT batch 3.$b3_18n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The First Business Pivot — Tailoring (en)',
  $b3_19$# The First Business Pivot

**Business** simulator: tailoring slows as ready-made e-commerce booms—race to the bottom versus niche pivot.

**Lesson:** Adaptation beats suicidal pricing; sell what fast fashion cannot—fit, alteration, styling.$b3_19$,
  'Learn · Simulator · Business',
  'Learn · Simulator · Business',
  'en',
  46,
  'Tailor',
  110,
  2.0,
  $b3_19m$Competing only on price against factories is exhaustion; reposition into alterations and premium fit.$b3_19m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v3',
  $b3_19g$ {"startSegmentId":"pvt_n1_slow","overlayStyle":"CARDS","segments":{"pvt_n1_slow":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/pvt_n1_slow.mp3","text":"Stitching orders thin; customers show phone screenshots of cheaper kurtas online. Rent is due; you consider slashing rates again.","choices":[{"id":"lower_more","label":"Cut prices further to match online—volume will return.","nextSegmentId":"pvt_n2_bleed","skillDeltas":{"business_health":-28,"money":-10}},{"id":"pivot_alterations","label":"Pivot to express alterations, restyling online-bought clothes, and premium measurement sessions.","nextSegmentId":"pvt_n3_niche","skillDeltas":{"business_health":38,"wisdom":20}}]},"pvt_n2_bleed":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/pvt_n2_bleed.mp3","text":"Your fingers blur; margins vanish; a shoulder injury whispers. You learn factories do not tire—humans cannot price-match robots forever.","choices":[]},"pvt_n3_niche":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/pvt_n3_niche.mp3","text":"Instagram before/afters of hem fixes go small-viral locally. Boutiques send overflow work. You charge for skill and speed, not thread alone. The sewing machine hums with a new story: you finish what algorithms ship wrong.","choices":[]}}}$b3_19g$,
  $b3_19p$List three services only a human tailor can sell in your neighborhood this month.$b3_19p$,
  $b3_19j$["When is competing on price a trap?", "What did your grandparents' shop sell that apps cannot?"]$b3_19j$::jsonb,
  $b3_19n$DRAFT batch 3.$b3_19n$
);
