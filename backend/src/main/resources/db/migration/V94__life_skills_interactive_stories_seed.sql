-- Life skills interactive simulators (English). DRAFT; story_owner seed:life-skills-interactive-pack-v1.
-- Narration: upload audio to paths referenced in interactive_graph (cdn.tamixa.app/.../life-skills-pack/...).

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Phantom OTP (en)',
  $ls0$# The Phantom OTP

Interactive life simulator for **seniors and adults**: a cold caller impersonates cyber police, invents a "Digital Arrest," and pressures Ramesh Ji toward a fake verification link. Co-listen with family; compare calm verification habits with panic reactions.

**Focus:** Social engineering, impersonation, and why real agencies do not demand net banking over the phone.$ls0$,
  'Learn · Simulator · Digital Safety',
  'Learn · Simulator · Digital Safety',
  'en',
  70,
  'Family',
  120,
  2.0,
  $ls0m$No legitimate agency issues a 'Digital Arrest' by phone or asks for net banking on a link. Verify through official channels and trusted family.$ls0m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v1',
  $ls0g$ {"startSegmentId":"phantom_n1_hook","overlayStyle":"CARDS","segments":{"phantom_n1_hook":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/phantom_n1_hook.mp3","text":"Ramesh Ji is drinking tea when his phone rings. It is an unknown number, but the voice sounds official, cold, and professional. \"Hello, I am Inspector Chauhan from the Cyber Cell. Your Aadhaar number has been linked to a money laundering case in Mumbai. A warrant is being issued for your 'Digital Arrest' in 30 minutes. Do not hang up.\" Ramesh's heart races. He has not done anything wrong, but the voice is so terrifying.","choices":[{"id":"stay_ask_innocence","label":"Stay on the line and ask how to prove innocence.","nextSegmentId":"phantom_n2_pressure","skillDeltas":{"wisdom":-10,"balance":-20}},{"id":"hangup_daughter","label":"Hang up and call his daughter who works in a bank.","nextSegmentId":"phantom_n3_safe","skillDeltas":{"wisdom":20,"social_capital":10}}]},"phantom_n2_pressure":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/phantom_n2_pressure.mp3","text":"The 'Inspector' says, \"To avoid immediate arrest, we need to verify your bank assets. I am sending a secure verification link. Click it and enter your net banking ID. We are monitoring your screen for your safety.\" Ramesh is trembling. He sees the link arrive. It looks like a government portal.","choices":[{"id":"click_link","label":"Click the link to clear his name quickly.","nextSegmentId":"phantom_n4_scammed","skillDeltas":{"money":-25,"DIGITAL_WISDOM":-20}},{"id":"go_police_station","label":"Tell the caller he will come to the local police station instead.","nextSegmentId":"phantom_n5_verified_safe","skillDeltas":{"wisdom":30,"social_capital":10}}]},"phantom_n3_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/phantom_n3_safe.mp3","text":"His daughter picks up. \"Papa, hang up NOW! There is no such thing as a 'Digital Arrest' over a phone call. It is a scam trending on the news.\" Ramesh breathes a sigh of relief. He realizes the 'Inspector' was just a voice on a screen.","choices":[]},"phantom_n4_scammed":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/phantom_n4_scammed.mp3","text":"Ramesh enters his banking details on the fake page. Within hours, a large unauthorized transfer empties much of his savings. The real cyber cell confirms it was impersonation. Recovery is uncertain.","choices":[]},"phantom_n5_verified_safe":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/phantom_n5_verified_safe.mp3","text":"Ramesh hangs up and walks to the police station with his daughter. Officers reassure him: no legitimate agency demands net banking over the phone. They help him report the number and protect his accounts.","choices":[]}}}$ls0g$,
  $ls0p$Together, find one official government or bank helpline number saved in your phone and practice reading it aloud.$ls0p$,
  $ls0j$["Ask the senior in your house: Have you ever received a call that made you feel scared for no reason?", "What would you do before clicking any 'verification' link from a stranger?"]$ls0j$::jsonb,
  $ls0n$DRAFT seed: attach narration audio at cdn paths under life-skills-pack/v1/en/.$ls0n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Zero-Interest Illusion (en)',
  $ls1$# The Zero-Interest Illusion

Branching simulator for **young adults** tempted by no-cost EMI on a luxury watch. Hidden fees, insurance add-ons, and GST appear on the statement; a second choice tests whether to stack more debt or reset.

**Focus:** Reading the full cost of credit, emergency savings, and fast-cash app risk.$ls1$,
  'Learn · Simulator · Money',
  'Learn · Simulator · Money',
  'en',
  22,
  'Arjun',
  110,
  2.0,
  $ls1m$No-cost EMI often hides fees and add-ons. Pausing purchases builds room for real emergencies without debt spirals.$ls1m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v1',
  $ls1g$ {"startSegmentId":"emi_n1_tempt","overlayStyle":"CARDS","segments":{"emi_n1_tempt":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/emi_n1_tempt.mp3","text":"Arjun wants the new Galaxy watch. It costs twenty-five thousand rupees—his entire month's savings. But the store clerk smiles and says, \"Sir, why pay now? We have zero percent interest, no-cost EMI. Just two thousand five hundred a month. It is like buying a pizza every month!\"","choices":[{"id":"take_emi","label":"Take the EMI and keep his cash for partying.","nextSegmentId":"emi_n2_hidden","skillDeltas":{"money":-8,"social_capital":10}},{"id":"wait_cash","label":"Wait for 3 months and buy it in cash.","nextSegmentId":"emi_n3_patient","skillDeltas":{"wisdom":15,"fiscal_muscle":10}}]},"emi_n2_hidden":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/emi_n2_hidden.mp3","text":"Two months later, Arjun looks at his statement. The EMI is two thousand five hundred, but there is a convenience fee of five hundred, an insurance premium of two hundred, and GST. His 'pizza' costs far more than he thought. Suddenly, he needs money for a bike repair.","choices":[{"id":"topup_loan","label":"Take a \"Top-up Loan\" from a fast-cash app.","nextSegmentId":"emi_n4_debt_spiral","skillDeltas":{"money":-12,"wisdom":-20}},{"id":"sell_watch","label":"Admit the mistake and sell the watch.","nextSegmentId":"emi_n5_reset","skillDeltas":{"wisdom":20,"money":5}}]},"emi_n3_patient":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/emi_n3_patient.mp3","text":"Arjun saves for three months, buys the watch outright, and feels no statement surprises. He keeps emergency cash for repairs and small joys without stacking hidden fees.","choices":[]},"emi_n4_debt_spiral":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/emi_n4_debt_spiral.mp3","text":"The app loan carries high interest. Payments collide; his credit score slips. The watch no longer feels like a win.","choices":[]},"emi_n5_reset":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/emi_n5_reset.mp3","text":"He sells the watch, pays off the EMI plan early where he can, and budgets honestly. The sting of the lesson lasts longer than the gadget would have.","choices":[]}}}$ls1g$,
  $ls1p$Open one recent statement and circle every fee that is not the headline EMI amount.$ls1p$,
  $ls1j$["Discuss with your teenager: If something is 'No-Cost', how is the company making money?", "When is waiting three months better than instant gratification?"]$ls1j$::jsonb,
  $ls1n$DRAFT seed; audio URLs are placeholders until narration is produced.$ls1n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Group Project Crisis (en)',
  $ls2$# The Group Project Crisis

Simulator for **kids and teens** leading a science project when one teammate disengages. Choices contrast rescue heroics, public shaming, and a private problem-solving conversation before the deadline.

**Focus:** Conflict resolution, assertiveness, and fair workload sharing.$ls2$,
  'Learn · Simulator · Leadership',
  'Learn · Simulator · Leadership',
  'en',
  12,
  'Sneha',
  105,
  2.0,
  $ls2m$Leadership often means clarity and curiosity before confrontation. Private check-ins can unlock help better than public blame.$ls2m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v1',
  $ls2g$ {"startSegmentId":"proj_n1_slacker","overlayStyle":"CARDS","segments":{"proj_n1_slacker":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/proj_n1_slacker.mp3","text":"Sneha is the leader of a science project. There are four members. One member, Rohan, has not done any work. He just plays games during meetings. The submission is in two days.","choices":[{"id":"do_work_herself","label":"Do Rohan's work herself to ensure a good grade.","nextSegmentId":"proj_n2_burnout","skillDeltas":{"social_capital":-10,"balance":-15}},{"id":"confront_public","label":"Confront Rohan in front of the teacher.","nextSegmentId":"proj_n3_public","skillDeltas":{"social_capital":10,"balance":-20}},{"id":"private_talk","label":"Have a private 1-on-1 talk with Rohan to find out why he isn't helping.","nextSegmentId":"proj_n4_private","skillDeltas":{"social_capital":20,"wisdom":10}}]},"proj_n2_burnout":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/proj_n2_burnout.mp3","text":"The project ships with a good grade, but Sneha is exhausted. Teammates assume she will always absorb slack; resentment simmers under the poster board smiles.","choices":[]},"proj_n3_public":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/proj_n3_public.mp3","text":"The teacher scolds Rohan; he shuts down. The group atmosphere turns icy, and Sneha must still finish the work amid awkward silence.","choices":[]},"proj_n4_private":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/proj_n4_private.mp3","text":"Rohan admits he felt lost on the topic. Sneha splits tasks with clear checkpoints; two classmates volunteer to pair with him. The submission reflects shared effort, not heroics.","choices":[]}}}$ls2g$,
  $ls2p$Before the next group task, write one sentence each: what you need and what you can offer.$ls2p$,
  $ls2j$["When is it better to talk privately than in front of a teacher?", "How can a leader ask for help without doing all the work alone?"]$ls2j$::jsonb,
  $ls2n$DRAFT seed; publish after audio/CDN.$ls2n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Kirana Start-up (en)',
  $ls3$# The Kirana Start-up

Branching story for **adults** bootstrapping a pickle delivery business with limited capital. Compare spending on premium packaging versus ingredients, then face the cash-flow crunch that follows.

**Focus:** Unit economics, quality versus perception, pricing, and borrowing trade-offs.$ls3$,
  'Learn · Simulator · Business',
  'Learn · Simulator · Business',
  'en',
  38,
  'Mrs. Rao',
  100,
  2.0,
  $ls3m$Customers remember taste before jars. Protect margin and reinvest when quality already earns word of mouth.$ls3m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v1',
  $ls3g$ {"startSegmentId":"kirana_n1_launch","overlayStyle":"CARDS","segments":{"kirana_n1_launch":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/kirana_n1_launch.mp3","text":"Mrs. Rao starts a home-delivery service for organic pickles. She has ten thousand rupees. She needs jars, labels, and ingredients.","choices":[{"id":"fancy_jars","label":"Spend seven thousand on fancy glass jars and professional branding to \"look premium.\"","nextSegmentId":"kirana_n2_premium_look","skillDeltas":{"money":3,"social_capital":15}},{"id":"simple_jars","label":"Use simple plastic jars for two thousand and spend five thousand on high-quality ingredients.","nextSegmentId":"kirana_n3_quality_first","skillDeltas":{"money":3,"wisdom":20}}]},"kirana_n2_premium_look":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/kirana_n2_premium_look.mp3","text":"The jars look beautiful, but three customers complain the pickles taste average. Mrs. Rao has no money left to buy better spices.","choices":[{"id":"lower_price","label":"Lower the price to get rid of stock.","nextSegmentId":"kirana_n4_race_bottom","skillDeltas":{"money":-10,"wisdom":-5}},{"id":"take_loan","label":"Take a loan to buy better ingredients.","nextSegmentId":"kirana_n5_leverage","skillDeltas":{"money":-15,"balance":-10}}]},"kirana_n3_quality_first":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/kirana_n3_quality_first.mp3","text":"Word spreads: the pickles taste exceptional. Repeat orders arrive; Mrs. Rao reinvests margin into labels gradually without starving quality.","choices":[]},"kirana_n4_race_bottom":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/kirana_n4_race_bottom.mp3","text":"Discounts train buyers to wait for sales. Margins vanish; the premium jars feel like empty theatre on the shelf.","choices":[]},"kirana_n5_leverage":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/kirana_n5_leverage.mp3","text":"Interest nibbles every month. One slow week and she juggles repayments while marketing stalls.","choices":[]}}}$ls3g$,
  $ls3p$List three fixed costs and three variable costs for a tiny food business you know.$ls3p$,
  $ls3j$["Would you rather impress with packaging first or flavor first? Why?", "When does a small loan help versus hurt a new seller?"]$ls3j$::jsonb,
  $ls3n$DRAFT seed; interactive_graph references placeholder MP3 paths.$ls3n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[LifeSim] The Plagiarism Shortcut (en)',
  $ls4$# The Plagiarism Shortcut

Short ethics simulator for **teens** facing a history deadline after a busy weekend. A friend pitches instant AI text; the honest path is shorter but defensible in conversation with a teacher.

**Focus:** Academic integrity, owning your thinking, and what happens when prose cannot be explained aloud.$ls4$,
  'Learn · Simulator · Ethics',
  'Learn · Simulator · Ethics',
  'en',
  15,
  'Kabir',
  95,
  2.0,
  $ls4m$Tools are tempting under fatigue, but your voice and understanding are the real assignment.$ls4m$,
  'DRAFT',
  'seed:life-skills-interactive-pack-v1',
  $ls4g$ {"startSegmentId":"eth_n1_deadline","overlayStyle":"CARDS","segments":{"eth_n1_deadline":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/eth_n1_deadline.mp3","text":"Kabir has a history essay due tomorrow. He spent the weekend at a cousin's wedding. He is tired. A friend says, \"Just use this AI tool. It will write the whole thing in ten seconds. The teacher will never know.\"","choices":[{"id":"use_ai","label":"Use the AI and go to sleep.","nextSegmentId":"eth_n2_ai","skillDeltas":{"wisdom":-25,"balance":-10}},{"id":"honest_essay","label":"Write a shorter, honest essay himself, even if it is not perfect.","nextSegmentId":"eth_n3_honest","skillDeltas":{"wisdom":30,"balance":20}}]},"eth_n2_ai":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/eth_n2_ai.mp3","text":"The teacher runs a quick interview; Kabir cannot explain his own arguments. An integrity conversation follows, and the draft is rejected.","choices":[]},"eth_n3_honest":{"audioUrl":"https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en/eth_n3_honest.mp3","text":"The essay is short, but Kabir can discuss every paragraph. The teacher notes rough edges yet praises clear thinking and honesty.","choices":[]}}}$ls4g$,
  $ls4p$Rewrite one paragraph of tonight's homework without tools, then read it aloud to check if it sounds like you.$ls4p$,
  $ls4j$["How would you explain your essay if the teacher asked for one idea per paragraph?", "When does using AI cross from help to misrepresentation?"]$ls4j$::jsonb,
  $ls4n$DRAFT seed; add segment audio when ready.$ls4n$
);

