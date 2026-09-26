-- Adulting & Survival interactive library (English). DRAFT.
-- story_owner seed:survival-adulting-v1

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] The Hospital Nightmare — Room Rent (en)',
  $sv_0$# The Hospital Nightmare

**Adulting & Survival:** gallbladder surgery, a five-lakh policy, and the hidden logic of **room rent capping** and **proportionate deduction**.

**Lesson:** A higher room class can shrink the insurer’s share of the whole bill—not just the bed.$sv_0$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  48,
  'Family',
  130,
  2.5,
  $sv_0m$Always match room category to your policy’s rent limit before admission; ‘cashless’ is not ‘unlimited’.$sv_0m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_0g$ {"startSegmentId":"hnm_n1_crisis","overlayStyle":"CARDS","segments":{"hnm_n1_crisis":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/hnm_n1_crisis.mp3","text":"Dad needs gallbladder surgery tonight. The hospital quotes about one lakh fifty thousand rupees. Your family floater shows five lakh sum insured—relief washes over you. The coordinator smiles: a deluxe suite is available, and ‘insurance will handle it.’ Your cousin whispers that a private room looks more dignified in front of relatives.","choices":[{"id":"deluxe_suite","label":"Book the deluxe suite—five lakhs cover should absorb everything.","nextSegmentId":"hnm_n2_shock","skillDeltas":{"money":-18,"wisdom":-10}},{"id":"check_cap","label":"Stop—open the policy PDF and find the room rent limit before signing the bed category.","nextSegmentId":"hnm_n3_planned","skillDeltas":{"wisdom":25,"fiscal_muscle":18}}]},"hnm_n2_shock":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/hnm_n2_shock.mp3","text":"The discharge bill lands heavy. Insurer pays roughly one lakh ten thousand—not one fifty. The letter cites **proportionate deduction**: your suite crossed the **room rent cap**, so surgeon fees, OT, and consumables were scaled down as if you had chosen an ‘unauthorized’ tariff. The family argues at the desk; dignity in the lobby cost forty thousand rupees nobody budgeted. Generic medicines versus brands becomes a bitter footnote.","choices":[]},"hnm_n3_planned":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/hnm_n3_planned.mp3","text":"You read the clause: one percent of sum insured per day, or a named category. You pick the twin-sharing room that fits. The billing desk grumbles but aligns. At discharge, the E-card clears predictably; you compare surgeon lines on the estimate with the policy wording. Dad recovers; the savings account breathes.","choices":[]}}}$sv_0g$,
  $sv_0p$Open your health policy now: highlight room rent/sub-limit, co-pay, and ‘proportionate deduction’ in three colors.$sv_0p$,
  $sv_0j$["Ask your family: Do we know the room rent limit of our current health policy?", "Would you rather comfort in the lobby or clarity on the bill?"]$sv_0j$::jsonb,
  $sv_0n$DRAFT; flagship Adulting & Survival pilot. Audio paths under library/sim/survival-adulting/v1/en/.$sv_0n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] The Secret Loan — Spouse & Relative (en)',
  $sv_1$# The Secret Loan

**Marriage / finance:** a spouse discovers the other pledged income for a relative’s emergency without a shared conversation.

**Lesson:** Shared money needs shared decisions—secrecy erodes trust faster than the EMI.$sv_1$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  36,
  'Spouse',
  115,
  2.0,
  $sv_1m$Financial infidelity is still infidelity—agree on thresholds for lending, gifts, and guarantees.$sv_1m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_1g$ {"startSegmentId":"slt_n1_statement","overlayStyle":"CARDS","segments":{"slt_n1_statement":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/slt_n1_statement.mp3","text":"The bank SMS arrives on your phone: EMI debited for a personal loan you never discussed. Your partner looks away—‘Bhaiya needed it; you would have said no.’ The amount is two years of your child’s school fund.","choices":[{"id":"silent_rage","label":"Freeze communication and move money into an account they cannot see.","nextSegmentId":"slt_n2_wedge","skillDeltas":{"harmony":-30,"integrity":-15}},{"id":"table_talk","label":"Call a calm sit-down: full numbers, written rules for extended family help.","nextSegmentId":"slt_n3_repair","skillDeltas":{"wisdom":28,"social_capital":15}}]},"slt_n2_wedge":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/slt_n2_wedge.mp3","text":"Accounts split; lawyers get whispered. The loan still runs; now it runs through resentment. The cousin pays irregularly; the marriage pays daily.","choices":[]},"slt_n3_repair":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/slt_n3_repair.mp3","text":"You draft a one-page family finance charter: emergency caps, no new debt without both signatures, documented gifts. Awkward weeks follow—but future shocks hit together, not sideways.","choices":[]}}}$sv_1g$,
  $sv_1p$Write your household rule: above what rupee amount must both partners agree before lending?$sv_1p$,
  $sv_1j$["Where is the line between kindness to relatives and loyalty to your nuclear family?", "How do you rebuild trust after hidden debt?"]$sv_1j$::jsonb,
  $sv_1n$DRAFT batch Adulting & Survival.$sv_1n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] The Pink Slip — Voluntary Resignation (en)',
  $sv_2$# The Pink Slip

**Jobs / labor awareness:** HR asks you to resign ‘voluntarily’ during a restructuring.

**Lesson:** Notice pay, retrenchment rules, and documentation matter—do not sign under hallway pressure.$sv_2$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  32,
  'Employee',
  120,
  2.0,
  $sv_2m$Voluntary resignation can waive rights you would have under lawful termination—read, pause, consult.$sv_2m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_2g$ {"startSegmentId":"psk_n1_hr","overlayStyle":"CARDS","segments":{"psk_n1_hr":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/psk_n1_hr.mp3","text":"HR slides a letter in a quiet meeting: ‘We need your resignation today for a smooth reference.’ Your manager avoids eye contact. Your heart pounds—visa, rent, parents.","choices":[{"id":"sign_now","label":"Sign immediately so the reference stays warm.","nextSegmentId":"psk_n2_waived","skillDeltas":{"money":-15,"wisdom":-20}},{"id":"review_rights","label":"Ask for the letter overnight, read appointment terms, speak to a union peer or lawyer helpline.","nextSegmentId":"psk_n3_shielded","skillDeltas":{"wisdom":30,"fiscal_muscle":10}}]},"psk_n2_waived":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/psk_n2_waived.mp3","text":"You learn later that notice pay and statutory timelines vanished with your signature. The ‘smooth reference’ was cheaper for payroll than honesty.","choices":[]},"psk_n3_shielded":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/psk_n3_shielded.mp3","text":"You return with calm questions; some colleagues organize; you secure written clarity on dues and dates. The exit still hurts—but it is bounded, not blank.","choices":[]}}}$sv_2g$,
  $sv_2p$Save one reputable legal-aid or labour-rights FAQ link applicable to your state.$sv_2p$,
  $sv_2j$["Why do companies prefer ‘voluntary’ exits?", "Who can you call before signing under stress?"]$sv_2j$::jsonb,
  $sv_2n$DRAFT; not legal advice—prompts only.$sv_2n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] The Vanishing Minimum — Fees & Auto-Sweep (en)',
  $sv_3$# The Vanishing Minimum

**Banking:** ten thousand rupees in savings shrinks to eight—SMS packs, ATM charges, small debits.

**Lesson:** Read statements; use **auto-sweep** (or equivalent) so idle cash earns closer to FD rates while staying liquid.$sv_3$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  29,
  'Saver',
  118,
  2.0,
  $sv_3m$Minimum balance is a promise to yourself—track nickle-and-dime fees and make idle money work.$sv_3m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_3g$ {"startSegmentId":"vmn_n1_alert","overlayStyle":"CARDS","segments":{"vmn_n1_alert":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/vmn_n1_alert.mp3","text":"Your salary account looked healthy last month. Today the app shows eight thousand. SMS alerts, cross-ATM withdrawals, and a ‘value pack’ you tapped once stack quietly.","choices":[{"id":"ignore","label":"Top up and forget—it's only small money.","nextSegmentId":"vmn_n2_bleed","skillDeltas":{"money":-12,"wisdom":-15}},{"id":"fix_sweep","label":"Download six months of statements, disable junk packs, enable auto-sweep above a threshold.","nextSegmentId":"vmn_n3_hack","skillDeltas":{"fiscal_muscle":30,"wisdom":18}}]},"vmn_n2_bleed":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/vmn_n2_bleed.mp3","text":"Year on year, friction fees eat a month of groceries. Compounding works against you when leaks are invisible.","choices":[]},"vmn_n3_hack":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/vmn_n3_hack.mp3","text":"One toggle moves surplus into linked FD chunks nightly; seven percent on slices beats three on idle dust. You set SMS alerts only for debits above five hundred. The statement becomes boring—in a good way.","choices":[]}}}$sv_3g$,
  $sv_3p$In your banking app, find ‘auto-sweep’ or ‘flexi deposit’ and note the threshold you would set.$sv_3p$,
  $sv_3j$["What three lines on a bank statement confuse most people?", "When is convenience worth a monthly fee?"]$sv_3j$::jsonb,
  $sv_3n$DRAFT; rates illustrative—verify with your bank.$sv_3n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] The Surgery Denied — Proposal Truth (en)',
  $sv_4$# The Surgery Denied

**Insurance:** a two-year-old policy refuses a knee surgery as ‘pre-existing’ because the proposal hid old pain.

**Lesson:** Disclose completely on the form—underwriting beats claim-time detective work.$sv_4$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  52,
  'Policyholder',
  112,
  2.0,
  $sv_4m$Honest proposals may cost a loading or waiting period; dishonest ones cost the whole claim.$sv_4m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_4g$ {"startSegmentId":"sdx_n1_form","overlayStyle":"CARDS","segments":{"sdx_n1_form":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/sdx_n1_form.mp3","text":"The agent says, ‘Don’t mention the old sports injury—premiums jump.’ Your spouse hesitates; the form glows on screen.","choices":[{"id":"omit","label":"Skip the old injury—save premium now.","nextSegmentId":"sdx_n2_denied","skillDeltas":{"integrity":-35,"money":-25}},{"id":"disclose","label":"Declare everything; accept loading or a named exclusion with eyes open.","nextSegmentId":"sdx_n3_covered","skillDeltas":{"integrity":35,"wisdom":22}}]},"sdx_n2_denied":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/sdx_n2_denied.mp3","text":"Investigation finds physiotherapy bills. The denial letter cites non-disclosure. The wait-period trap becomes a courtroom tone you cannot afford.","choices":[]},"sdx_n3_covered":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/sdx_n3_covered.mp3","text":"Premium is higher; maybe a waiting period applies—but when surgery comes, the claim path is straight. Sleep before illness is the real rider.","choices":[]}}}$sv_4g$,
  $sv_4p$Re-read your last proposal: is every chronic issue listed truthfully?$sv_4p$,
  $sv_4j$["Why do agents push ‘clean’ forms?", "What is a waiting period versus a lie?"]$sv_4j$::jsonb,
  $sv_4n$DRAFT.$sv_4n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] The Tax Notice — Nil GST Return (en)',
  $sv_5$# The Tax Notice

**Business compliance:** a small vendor skipped filing when sales were zero.

**Lesson:** Nil returns still have a deadline; non-compliance penalties can exceed the tax.$sv_5$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  41,
  'Vendor',
  105,
  2.0,
  $sv_5m$A nil return is five minutes; a notice is five weeks of stress—calendar the cycle.$sv_5m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_5g$ {"startSegmentId":"gst_n1_busy","overlayStyle":"CARDS","segments":{"gst_n1_busy":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/gst_n1_busy.mp3","text":"No sales this quarter—why bother logging in? A brown envelope arrives: late fee and interest for missed filings.","choices":[{"id":"ignore_notice","label":"Ignore the letter until business picks up.","nextSegmentId":"gst_n2_spiral","skillDeltas":{"money":-20,"business_health":-25}},{"id":"file_nil","label":"File nil returns immediately and set phone reminders for every cycle.","nextSegmentId":"gst_n3_clean","skillDeltas":{"wisdom":28,"fiscal_muscle":20}}]},"gst_n2_spiral":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/gst_n2_spiral.mp3","text":"Penalties compound; bank limits wobble when compliance flags rise. The ‘saved’ hour costs reputation with buyers who need your GSTIN clean.","choices":[]},"gst_n3_clean":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/gst_n3_clean.mp3","text":"Dashboard green feels dull until you compare it to a neighbor’s raid story. Compliance is the rent for playing in the formal economy.","choices":[]}}}$sv_5g$,
  $sv_5p$Add quarterly GST / filing dates to your family calendar with two reminders.$sv_5p$,
  $sv_5j$["When is a zero-sales quarter still a filing quarter?", "Who helps micro-businesses with free compliance clinics?"]$sv_5j$::jsonb,
  $sv_5n$DRAFT; not tax advice.$sv_5n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] Term vs Money-Back — Twenty Years (en)',
  $sv_6$# Term vs Money-Back

**Money hack simulation:** compare a costly money-back plan with low life cover versus pure term over twenty years.

**Lesson:** Protection first—savings belong in instruments built for growth, not mixed insurance.$sv_6$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  35,
  'Parent',
  125,
  2.5,
  $sv_6m$Huge cover at low premium (term) plus disciplined investing usually beats bundled ‘returns’.$sv_6m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_6g$ {"startSegmentId":"tvt_n1_agent","overlayStyle":"CARDS","segments":{"tvt_n1_agent":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/tvt_n1_agent.mp3","text":"The agent projects a glossy chart: money-back after fifteen years, ‘guaranteed’ lakhs, tiny premiums. Term insurance sounds like ‘money gone if nothing happens.’","choices":[{"id":"money_back","label":"Buy the money-back plan for peace and cashback.","nextSegmentId":"tvt_n2_thin_cover","skillDeltas":{"money":-15,"wisdom":-18}},{"id":"term_plus_sip","label":"Buy high-cover term only; route the difference into a simple index SIP.","nextSegmentId":"tvt_n3_stack","skillDeltas":{"fiscal_muscle":32,"wisdom":22}}]},"tvt_n2_thin_cover":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/tvt_n2_thin_cover.mp3","text":"A tragedy years later exposes cover that cannot clear the home loan. Returns lag inflation; opportunity cost whispers in every statement.","choices":[]},"tvt_n3_stack":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/tvt_n3_stack.mp3","text":"The spreadsheet is blunt: term premium is a sliver; the SIP curve crosses the ‘guaranteed’ line with room to breathe. You teach the kids the difference between insurance and investment.","choices":[]}}}$sv_6g$,
  $sv_6p$List premium, sum assured, and exclusions for any policy you own—one sticky note each.$sv_6p$,
  $sv_6j$["Why do money-back plans feel safer than term?", "What is the job of life insurance in your family?"]$sv_6j$::jsonb,
  $sv_6n$DRAFT; illustrations only.$sv_6n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] The SIP Leak Hunt (en)',
  $sv_7$# The SIP Leak Hunt

**Money hack:** find five hundred rupees of monthly leaks—subscriptions, chai breaks, impulse top-ups—to fund a starter SIP.

**Lesson:** Small leaks veto big goals until you name them.$sv_7$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  27,
  'Earner',
  100,
  2.0,
  $sv_7m$Visibility beats willpower—track first, cut second, automate third.$sv_7m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_7g$ {"startSegmentId":"sip_n1_goal","overlayStyle":"CARDS","segments":{"sip_n1_goal":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/sip_n1_goal.mp3","text":"You want a five-thousand SIP but cash feels tight. A friend says find leaks first.","choices":[{"id":"guess_cut","label":"Cancel one big subscription without checking the rest.","nextSegmentId":"sip_n2_bounce","skillDeltas":{"balance":-12,"money":-8}},{"id":"audit","label":"Export UPI/bank tags for 60 days; circle recurring and impulse buckets.","nextSegmentId":"sip_n3_funded","skillDeltas":{"fiscal_muscle":28,"wisdom":15}}]},"sip_n2_bounce":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/sip_n2_bounce.mp3","text":"The SIP bounces once; bank fees eat optimism. Random cuts don’t stick without a map.","choices":[]},"sip_n3_funded":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/sip_n3_funded.mp3","text":"Forgotten OTT, auto-tips, and smoke money add up to seven hundred. You schedule SIP day-two after salary; leaks fund the first three months consciously.","choices":[]}}}$sv_7g$,
  $sv_7p$Highlight every auto-debit in your last two statements—decide keep vs cancel for each.$sv_7p$,
  $sv_7j$["What is one ‘small’ expense that became a habit?", "How small can a first SIP be and still count?"]$sv_7j$::jsonb,
  $sv_7n$DRAFT.$sv_7n$
);

INSERT INTO library_stories (
  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,
  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note
) VALUES (
  '[Survival] Survival 101 — Three Drills (en)',
  $sv_8$# Survival 101 — Three Drills

**Free foundation track:** pick one drill—reading a bank statement, checking a health policy, or the first sixty minutes after cyber theft.

**Lesson:** Panic shrinks when you have a sequence memorized.$sv_8$,
  'Learn · Simulator · Adulting & Survival',
  'Learn · Simulator · Adulting & Survival',
  'en',
  18,
  'You',
  110,
  2.0,
  $sv_8m$Rehearse boring steps before the emergency—buttons matter more than headlines.$sv_8m$,
  'DRAFT',
  'seed:survival-adulting-v1',
  $sv_8g$ {"startSegmentId":"s101_n1_pick","overlayStyle":"CARDS","segments":{"s101_n1_pick":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/s101_n1_pick.mp3","text":"You have twenty quiet minutes. Which survival skill will you practice today?","choices":[{"id":"bank_stmt","label":"Drill: read a bank statement line by line (debits, balances, fees).","nextSegmentId":"s101_n2_bank","skillDeltas":{"fiscal_muscle":18,"wisdom":12}},{"id":"policy","label":"Drill: open your health policy—find room rent, co-pay, exclusions.","nextSegmentId":"s101_n3_policy","skillDeltas":{"wisdom":22,"fiscal_muscle":12}},{"id":"cyber60","label":"Drill: rehearse the first 60 minutes after UPI fraud (1930, bank, freeze).","nextSegmentId":"s101_n4_cyber","skillDeltas":{"DIGITAL_WISDOM":28,"wisdom":15}}]},"s101_n2_bank":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/s101_n2_bank.mp3","text":"You circle opening balance, credits, every debit category, and closing balance. You flag unknown merchant codes for one verification call. Boring becomes power.","choices":[]},"s101_n3_policy":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/s101_n3_policy.mp3","text":"You screenshot the room rent clause and WhatsApp it to your spouse with one sentence: ‘We stay below this bed class.’ The TPA number goes into contacts as ‘Health claim’.","choices":[]},"s101_n4_cyber":{"audioUrl":"https://cdn.tamixa.app/library/sim/survival-adulting/v1/en/s101_n4_cyber.mp3","text":"You write a card: minute zero—call 1930; minute ten—bank fraud desk; minute twenty—screenshot and save UPI IDs; minute forty—password resets on email; no shame, fast moves. The golden hour is real.","choices":[]}}}$sv_8g$,
  $sv_8p$Teach one household member the drill you chose—voice beats solo scrolling.$sv_8p$,
  $sv_8j$["Which number would you dial first if money left your account today?", "What document do you wish you had photographed before an emergency?"]$sv_8j$::jsonb,
  $sv_8n$DRAFT; Survival 101 free-track pilot.$sv_8n$
);
