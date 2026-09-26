#!/usr/bin/env python3
"""
Generates Flyway seed: V96__life_skills_interactive_stories_seed_batch3.sql

Run: python3 backend/scripts/generate_life_skills_sim_batch3_seed_sql.py
"""
from __future__ import annotations

import json
import textwrap
from pathlib import Path

CDN = "https://cdn.tamixa.app/library/sim/life-skills-pack/v1/en"
STORY_OWNER = "seed:life-skills-interactive-pack-v3"
STATUS = "DRAFT"


def audio(seg_id: str) -> str:
    return f"{CDN}/{seg_id}.mp3"


def N(seg_id: str, text: str, choices: list) -> tuple[str, dict]:
    return seg_id, {"audioUrl": audio(seg_id), "text": text, "choices": choices}


def graph(start: str, segments: list[tuple[str, dict]]) -> dict:
    return {"startSegmentId": start, "overlayStyle": "CARDS", "segments": dict(segments)}


def row(**kwargs) -> dict:
    kwargs["content"] = textwrap.dedent(kwargs["content"]).strip()
    return kwargs


def emit_sql(rows: list[dict]) -> str:
    lines = [
        "-- Life skills interactive simulators batch 3 (English). DRAFT.",
        f"-- story_owner {STORY_OWNER}",
        "",
    ]
    for i, r in enumerate(rows):
        gj = json.dumps(r["graph"], ensure_ascii=False, separators=(",", ":"))
        pj = json.dumps(r["parent_discussion_prompts"], ensure_ascii=False)
        t = f"b3_{i}"
        def dq(tag: str, s: str) -> str:
            return f"${tag}$" + s + f"${tag}$"

        title = r["title"].replace("'", "''")
        cn = r["child_name"].replace("'", "''")
        lines.extend(
            [
                "INSERT INTO library_stories (",
                "  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,",
                "  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note",
                ") VALUES (",
                f"  '{title}',",
                f"  {dq(t, r['content'])},",
                f"  '{r['theme']}',",
                f"  '{r['category']}',",
                f"  '{r['language']}',",
                f"  {r['age']},",
                f"  '{cn}',",
                f"  {r['word_count']},",
                f"  {r['reading_time_minutes']},",
                f"  {dq(t + 'm', r['moral'])},",
                f"  '{STATUS}',",
                f"  '{STORY_OWNER}',",
                f"  {dq(t + 'g', gj)},",
                f"  {dq(t + 'p', r['post_story_mission'])},",
                f"  {dq(t + 'j', pj)}::jsonb,",
                f"  {dq(t + 'n', r['parent_content_note'])}",
                ");",
                "",
            ]
        )
    return "\n".join(lines)


ROWS: list[dict] = [
    row(
        title="[LifeSim] The Gold Loan Dilemma (en)",
        content="""
        # The Gold Loan Dilemma

        **Finance** simulator for adults and seniors: emergency surgery money is needed, and your brother-in-law asks you to pledge family gold at a local pawnbroker versus routing through a formal bank gold loan with documented rates and tenure.

        **Lesson:** Collateral is not abstract—compare annualized interest, receipts, and recovery rights before you sign over heirlooms.
        """,
        theme="Learn · Simulator · Money",
        category="Learn · Simulator · Money",
        language="en",
        age=52,
        child_name="Family",
        word_count=118,
        reading_time_minutes=2.0,
        moral="Pawnbrokers can charge punishing monthly rates; banks offer paper trails and predictable foreclosure rules—speed is not the same as safety.",
        graph=graph(
            "gld_n1_hook",
            [
                N(
                    "gld_n1_hook",
                    "Your brother-in-law calls at dusk. His father needs emergency surgery; the hospital wants a deposit tonight. He needs two lakh rupees until insurance reimburses. "
                    "Your wife's eyes fill—those bangles were her grandmother's. He whispers that a local pawnbroker will 'just hold' the gold for three months, no questions, no bank paperwork. "
                    "Your chest tightens: family love, fear for an elder, and the weight of melting something sacred for speed.",
                    [
                        {
                            "id": "pawn_shop",
                            "label": "Agree and go to the local pawnbroker tonight—surgery cannot wait.",
                            "nextSegmentId": "gld_n2_pawn",
                            "skillDeltas": {"money": -12, "wisdom": -10, "balance": -18},
                        },
                        {
                            "id": "bank_gold_loan",
                            "label": "Insist on a scheduled bank gold loan tomorrow morning; offer to drive him and carry the documents.",
                            "nextSegmentId": "gld_n3_bank",
                            "skillDeltas": {"wisdom": 20, "fiscal_muscle": 15},
                        },
                    ],
                ),
                N(
                    "gld_n2_pawn",
                    "The broker weighs the ornaments under a bare bulb and slides a chit: three percent per month, compounded if you slip—thirty-six percent a year if math is honest. "
                    "Your brother-in-law pays the first month from borrowed cash, then misses the second when the pharmacy bill spikes. The broker's tone hardens: melt date, auction talk, 'rules are rules.' "
                    "You learn that handshakes do not pause compound interest—and that fear moved faster than reading the fine print.",
                    [],
                ),
                N(
                    "gld_n3_bank",
                    "The branch opens; the gold loan desk stamps photos, KYC, and a fixed EMI. The rate is lower; the tenure is printed; partial prepayment is allowed. "
                    "It costs one anxious morning, not zero stress—but when the hospital receipt is filed, the chain of custody is clear. Your wife exhales: dignity lived in procedure, not panic.",
                    [],
                ),
            ],
        ),
        post_story_mission="Look up one bank's published gold-loan rate sheet and write the EMI formula you would use for two lakh over twelve months.",
        parent_discussion_prompts=[
            "Ask your elders: Why was Sunaar (the goldsmith) the only bank in the old days, and why is it dangerous now?",
            "When is speed worth more than a paper trail—and when is it not?",
        ],
        parent_content_note="DRAFT batch 3; segment MP3s are placeholders.",
    ),
    row(
        title="[LifeSim] The Deepfake Accident — Grandson Call (en)",
        content="""
        # The Deepfake "Accident"

        **Tech safety** simulator for families and seniors: a voice that sounds exactly like a grandson begs for instant UPI payment for a fake police fine.

        **Lesson:** Voice can be cloned—verify on a second channel you already trust before any transfer.
        """,
        theme="Learn · Simulator · Digital Safety",
        category="Learn · Simulator · Digital Safety",
        language="en",
        age=68,
        child_name="Dadu",
        word_count=105,
        reading_time_minutes=2.0,
        moral="Panic plus love is the scammer's recipe; a callback to a saved number breaks most traps.",
        graph=graph(
            "dfa_n1_call",
            [
                N(
                    "dfa_n1_call",
                    "The phone vibrates during tea. A young voice cracks—Aryan, your grandson—crying that there was a minor accident, police took his phone, and he must pay twenty thousand rupees G-Pay now or they will jail him. "
                    "'Dadu, please, don't tell Mummy yet.' Your hands shake; the UPI ID glows on screen.",
                    [
                        {
                            "id": "send_now",
                            "label": "Send the money immediately—what if it is really him?",
                            "nextSegmentId": "dfa_n2_scam",
                            "skillDeltas": {"money": -25, "DIGITAL_WISDOM": -25},
                        },
                        {
                            "id": "verify_saved",
                            "label": "Hang up and call Aryan's mother or his number saved in your phone.",
                            "nextSegmentId": "dfa_n3_safe",
                            "skillDeltas": {"wisdom": 30, "DIGITAL_WISDOM": 20},
                        },
                    ],
                ),
                N(
                    "dfa_n2_scam",
                    "The payment confirms; silence follows. When your daughter calls back, Aryan is confused—he has been in the college library all day. "
                    "The cyber helpline says cloned voice fraud is rising. The money is likely gone. Shame mixes with anger at how perfectly fear erased doubt.",
                    [],
                ),
                N(
                    "dfa_n3_safe",
                    "Your daughter answers; she hands the phone to Aryan—chewing chips, annoyed at the interruption, perfectly fine. "
                    "Together you screenshot the fake UPI ID and file a report. You practice one sentence aloud: 'I only pay people I have verified on a second call.'",
                    [],
                ),
            ],
        ),
        post_story_mission="Add a home rule: no UPI above a small limit without a video or voice callback on a known number.",
        parent_discussion_prompts=[
            "What secret question could only the real grandchild answer?",
            "Why do scammers beg you not to tell parents?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Society Secretary (en)",
        content="""
        # The Society Secretary

        **Leadership** simulator: as new housing society secretary, you face a maintenance boycott over elevator noise and angry neighbors demanding harsh action.

        **Lesson:** Coercion escalates feuds; facilitated listening plus expertise repairs trust faster than cutoffs.
        """,
        theme="Learn · Simulator · Leadership",
        category="Learn · Simulator · Leadership",
        language="en",
        age=44,
        child_name="Secretary",
        word_count=112,
        reading_time_minutes=2.0,
        moral="Authority used as punishment breeds lawsuits and factions; process and listening turn noise complaints into fixable engineering tasks.",
        graph=graph(
            "sec_n1_khanna",
            [
                N(
                    "sec_n1_khanna",
                    "The WhatsApp group erupts. Mr. Khanna has not paid maintenance for two months, claiming the new elevator 'vibrates like a drill' into his bedroom. Others post bills they paid on time; someone demands his water be cut. "
                    "You are new in the chair; the managing committee wants a 'strong message.'",
                    [
                        {
                            "id": "legal_water_cut",
                            "label": "Issue a legal notice and authorize cutting his water until he pays.",
                            "nextSegmentId": "sec_n2_coercion",
                            "skillDeltas": {"authority": 10, "harmony": -30},
                        },
                        {
                            "id": "chai_meeting",
                            "label": "Schedule a chai meeting with Mr. Khanna, two neutral residents, and the elevator AMC technician.",
                            "nextSegmentId": "sec_n3_facilitate",
                            "skillDeltas": {"social_capital": 25, "harmony": 20},
                        },
                    ],
                ),
                N(
                    "sec_n2_coercion",
                    "Lawyers exchange letters; Khanna counters with a harassment claim. The gate log shows shouting matches; families pick sides. "
                    "The elevator noise is still unmeasured; cash flow for repairs worsens because bitterness replaced diagnosis.",
                    [],
                ),
                N(
                    "sec_n3_facilitate",
                    "In the clubhouse, the technician clips a decibel app, tightens a rail bracket, and schedules a damping pad. Khanna admits he felt unheard. "
                    "You draft a one-page 'noise complaint protocol.' He pays arrears in installments with a signed plan. The group learns that leadership sometimes tastes like tea, not thunder.",
                    [],
                ),
            ],
        ),
        post_story_mission="Write a three-step process your building could use for maintenance disputes before any cutoff threat.",
        parent_discussion_prompts=[
            "When does a secretary protect the collective without humiliating one flat?",
            "How do you bring expertise into the room without taking sides?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Stock Tip WhatsApp Group (en)",
        content="""
        # The "Stock Tip" WhatsApp Group

        **Finance / tech** simulator: you are pulled into a hype group pushing penny-stock screenshots and FOMO.

        **Lesson:** Pump-and-dump schemes dress as community; verify on SEBI-registered channels before risking emergency savings.
        """,
        theme="Learn · Simulator · Money",
        category="Learn · Simulator · Money",
        language="en",
        age=29,
        child_name="Investor",
        word_count=108,
        reading_time_minutes=2.0,
        moral="If everyone in chat is winning, the exit liquidity is probably you—research beats screenshots.",
        graph=graph(
            "stkt_n1_group",
            [
                N(
                    "stkt_n1_group",
                    "Your cousin adds you to 'India Bull Run 500%.' Pinned messages celebrate a penny stock; an 'expert' posts voice notes; dozens paste broker screenshots of overnight doubles. "
                    "Your emergency fund sits fifty thousand liquid—tempting, because rent is covered this month.",
                    [
                        {
                            "id": "yolo_emergency",
                            "label": "Deploy your full emergency fund now—double it before the window closes.",
                            "nextSegmentId": "stkt_n2_dump",
                            "skillDeltas": {"money": -22, "wisdom": -20},
                        },
                        {
                            "id": "sebi_research",
                            "label": "Search the company on SEBI-scraper sites, exchange circulars, and independent news before a rupee moves.",
                            "nextSegmentId": "stkt_n3_sober",
                            "skillDeltas": {"wisdom": 20, "integrity": 10},
                        },
                    ],
                ),
                N(
                    "stkt_n2_dump",
                    "You buy high on volume hype; three sessions later, upper circuits reverse into freeze. The group admin mutes complaints; numbers go dead. "
                    "The cousin apologizes—he was paid to add members. Your emergency cushion is a lesson etched in red.",
                    [],
                ),
                N(
                    "stkt_n3_sober",
                    "Filings show promoter pledges and sketchy related-party deals. You stay out; you warn one friend privately. "
                    "The stock still spikes for others—then collapses weeks later. You learn that boring homework outperforms adrenaline in finance.",
                    [],
                ),
            ],
        ),
        post_story_mission="Bookmark one SEBI investor education page and read it once this week.",
        parent_discussion_prompts=[
            "Why are screenshots not evidence of profit?",
            "What is an emergency fund for—if not for emergencies?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Interview Jitters (en)",
        content="""
        # The Interview Jitters

        **Communication** simulator for teens and young adults: a fast English question triggers panic, but you know the answer in your mother tongue.

        **Lesson:** Confidence often comes from asking for clarity—not from performing vocabulary you do not own.
        """,
        theme="Learn · Simulator · Communication",
        category="Learn · Simulator · Communication",
        language="en",
        age=21,
        child_name="Candidate",
        word_count=110,
        reading_time_minutes=2.0,
        moral="Panels respect clear thinking in simple words more than fluent-sounding confusion.",
        graph=graph(
            "intj_n1_question",
            [
                N(
                    "intj_n1_question",
                    "The interviewer fires a long question about system design trade-offs, words blurring together. Your heart hammers; you understand the concept in Gujarati in your head, but English feels like a locked door. "
                    "Silence stretches; you feel judged already.",
                    [
                        {
                            "id": "big_words",
                            "label": "String together impressive English words even if you are stuttering.",
                            "nextSegmentId": "intj_n2_muddle",
                            "skillDeltas": {"confidence": -15, "clarity": -20},
                        },
                        {
                            "id": "ask_simple",
                            "label": 'Say politely: "Can I explain this simply? I want my logic to be clear."',
                            "nextSegmentId": "intj_n3_clear",
                            "skillDeltas": {"confidence": 20, "clarity": 25},
                        },
                    ],
                ),
                N(
                    "intj_n2_muddle",
                    "You deploy jargon half-understood; a follow-up exposes the gap. The panel notes kindness but confusion. Walking out, you wish you had named your uncertainty instead of decorating it.",
                    [],
                ),
                N(
                    "intj_n3_clear",
                    "You draw boxes on the whiteboard, label them slowly, and check in: 'Is this the constraint you meant?' The interviewer leans in. "
                    "You leave knowing you were seen for thinking, not for accent theatre.",
                    [],
                ),
            ],
        ),
        post_story_mission="Practice one technical answer in two speeds: 30 seconds plain, then 90 seconds detailed—in your own words.",
        parent_discussion_prompts=[
            "When is asking for clarification a strength?",
            "How is clarity different from fluency?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Secret Credit Card (en)",
        content="""
        # The Secret Credit Card

        **Finance** simulator: first card with a one-lakh limit meets a ninety-thousand-rupee gaming laptop during a sale.

        **Lesson:** High utilization and minimum payments erode scores and sleep; cash-budget purchases keep margin for real life.
        """,
        theme="Learn · Simulator · Money",
        category="Learn · Simulator · Money",
        language="en",
        age=22,
        child_name="Cardholder",
        word_count=102,
        reading_time_minutes=2.0,
        moral="A limit is not income; match big buys to money you already have, not to minimum-due math.",
        graph=graph(
            "scc_n1_sale",
            [
                N(
                    "scc_n1_sale",
                    "Amazon lights up: Republic Day sale, zero-cost EMI banners, a gaming laptop at ninety thousand—ten below your entire credit line. Friends say swipe now, think later; 'minimum due is pocket change.' "
                    "Your savings account holds thirty thousand for rent buffer.",
                    [
                        {
                            "id": "swipe_max",
                            "label": "Buy the laptop on the card; figure out minimum dues later.",
                            "nextSegmentId": "scc_n2_trap",
                            "skillDeltas": {"money": -22, "balance": -18},
                        },
                        {
                            "id": "cash_fit",
                            "label": "Buy a capable machine within your thirty thousand cash budget—or wait one more sale cycle.",
                            "nextSegmentId": "scc_n3_discipline",
                            "skillDeltas": {"fiscal_muscle": 30, "wisdom": 10},
                        },
                    ],
                ),
                N(
                    "scc_n2_trap",
                    "Interest and processing fees creep; utilization stays above eighty percent; a pre-approved loan offer arrives like mockery. "
                    "The laptop thrills for a month; the statement anxiety lasts longer than any game session.",
                    [],
                ),
                N(
                    "scc_n3_discipline",
                    "You research a refurbished unit within cash. Credit utilization stays low; autopay stays boring. "
                    "You game happily enough—and when a real emergency hits, the card is still a safety net, not a shovel.",
                    [],
                ),
            ],
        ),
        post_story_mission="Calculate thirty percent of your credit limit and write that number on a sticky note as a spending ceiling.",
        parent_discussion_prompts=[
            "What is the difference between a credit limit and savings?",
            "Why do minimum dues grow the hole?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Plagiarism Dilemma (en)",
        content="""
        # The Plagiarism Dilemma

        **Ethics** simulator for teens: a paid 'unique' thesis offer versus an honest incomplete submission.

        **Lesson:** Short-term grades bought with fraud corrode skill and reputation; vulnerability with a teacher can open real help.
        """,
        theme="Learn · Simulator · Ethics",
        category="Learn · Simulator · Ethics",
        language="en",
        age=17,
        child_name="Student",
        word_count=100,
        reading_time_minutes=2.0,
        moral="Integrity is built when deadlines bite—choose the awkward truth over a polished lie.",
        graph=graph(
            "plag_n1_offer",
            [
                N(
                    "plag_n1_offer",
                    "The final project is due tomorrow; the doc is blank. A site promises a unique thesis for five hundred rupees, untraceable, 'AI-checked.' Your friend says everyone does it. "
                    "Your stomach twists—you actually care about the subject.",
                    [
                        {
                            "id": "pay_plagiarism",
                            "label": "Pay five hundred and submit the file tonight.",
                            "nextSegmentId": "plag_n2_exposed",
                            "skillDeltas": {"integrity": -35, "wisdom": -12},
                        },
                        {
                            "id": "honest_partial",
                            "label": "Work through the night, submit incomplete work, and email the teacher the truth in the morning.",
                            "nextSegmentId": "plag_n3_grace",
                            "skillDeltas": {"integrity": 40, "wisdom": 20},
                        },
                    ],
                ),
                N(
                    "plag_n2_exposed",
                    "The viva asks for a derivation you never read; the tool flags similarity anyway. An integrity committee email lands. "
                    "The grade zero hurts less than the mirror—you traded your voice for a PDF.",
                    [],
                ),
                N(
                    "plag_n3_grace",
                    "You hand in rough sections with sticky notes marking gaps. The teacher grants a guarded extension and a tutor slot. "
                    "The final grade is modest, but the skills are yours—and sleep returns without dread.",
                    [],
                ),
            ],
        ),
        post_story_mission="Write one paragraph in your own words on the hardest topic—run it through your school's policy checklist.",
        parent_discussion_prompts=[
            "When does 'help' cross into misrepresentation?",
            "Who benefits when students buy essays?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Customer Is Rude — Cafe (en)",
        content="""
        # The Customer Is Rude

        **Business** simulator: a small cafe, a shouting customer, and the room watching.

        **Lesson:** Calm replacement plus clear explanation protects brand, staff dignity, and other guests better than volume for volume.
        """,
        theme="Learn · Simulator · Business",
        category="Learn · Simulator · Business",
        language="en",
        age=26,
        child_name="Owner",
        word_count=104,
        reading_time_minutes=2.0,
        moral="Professionalism is low voice, fast fix, visible fairness—not matching insult for insult.",
        graph=graph(
            "cusr_n1_coffee",
            [
                N(
                    "cusr_n1_coffee",
                    "A customer slams a cup on the counter—coffee 'not hot enough,' voice rising, phone camera half-lifted. Regulars shift uncomfortably; your barista freezes.",
                    [
                        {
                            "id": "shout_back",
                            "label": "Shout back and tell them to leave if they cannot behave.",
                            "nextSegmentId": "cusr_n2_scene",
                            "skillDeltas": {"status": -12, "business_health": -22},
                        },
                        {
                            "id": "replace_calm",
                            "label": "Replace the drink quietly, offer a small cookie, explain peak-hour wait times without blaming them.",
                            "nextSegmentId": "cusr_n3_pro",
                            "skillDeltas": {"status": 20, "business_health": 15},
                        },
                    ],
                ),
                N(
                    "cusr_n2_scene",
                    "A clipped video circulates—context lost. One-star reviews mention 'rude owner.' Staff morale dips; you replay the moment you chose ego over de-escalation.",
                    [],
                ),
                N(
                    "cusr_n3_pro",
                    "The steam wand hisses; a fresh cup goes out. The line relaxes; someone mouths thank you. The rude customer leaves still muttering—but without a viral moment. "
                    "Your team copies your tone the next week. That is culture.",
                    [],
                ),
            ],
        ),
        post_story_mission="Role-play: friend plays angry customer—you practice square breathing before speaking.",
        parent_discussion_prompts=[
            "How do you protect staff while serving a harsh customer?",
            "When is refusing service better than a free replacement?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Senior's First UPI — Stranger Help (en)",
        content="""
        # The Senior's First UPI

        **Tech safety** simulator at a grocery QR: a stranger offers to 'handle' the phone and PIN.

        **Lesson:** PIN never leaves your hand; polite refusal is digital hygiene.
        """,
        theme="Learn · Simulator · Digital Safety",
        category="Learn · Simulator · Digital Safety",
        language="en",
        age=71,
        child_name="Dada",
        word_count=98,
        reading_time_minutes=2.0,
        moral="Help that needs your PIN is not help—use the shopkeeper or family, not a stranger's fingers.",
        graph=graph(
            "upi2_n1_qr",
            [
                N(
                    "upi2_n1_qr",
                    "At the kirana, Dada wants to try the QR taped to the glass. A helpful stranger steps in: 'Uncle, give me the phone—I will scan and type the PIN fast; the queue is long.' "
                    "The shopkeeper is busy weighing dal.",
                    [
                        {
                            "id": "hand_phone",
                            "label": "Hand over the phone—he seems confident.",
                            "nextSegmentId": "upi2_n2_drained",
                            "skillDeltas": {"money": -12, "wisdom": -18},
                        },
                        {
                            "id": "self_try",
                            "label": "Ask the shopkeeper to wait; you will try yourself step by step.",
                            "nextSegmentId": "upi2_n3_safe",
                            "skillDeltas": {"wisdom": 20, "DIGITAL_WISDOM": 25},
                        },
                    ],
                ),
                N(
                    "upi2_n2_drained",
                    "Two quick transfers later—groceries unpaid, savings lighter—the stranger melts into the street. "
                    "The shopkeeper sighs: 'Uncle, PIN is like your house key.' Shame and lesson arrive together.",
                    [],
                ),
                N(
                    "upi2_n3_safe",
                    "You shield the PIN; the first payment is slow and correct. The queue waits without jeering. Walking home, you teach your grandchild one rule: 'No PIN on anyone else's screen.'",
                    [],
                ),
            ],
        ),
        post_story_mission="Practice covering the PIN pad with your palm in front of a mirror once.",
        parent_discussion_prompts=[
            "How can kids help grandparents without touching the PIN?",
            "What phrase can seniors memorize to refuse 'quick help'?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Wedding Budget War (en)",
        content="""
        # The Wedding Budget War

        **Finance / social** simulator: five-star venue pressure versus transparent family math on loans and retirement.

        **Lesson:** Social debt often outlasts the album; shared numbers build consent.
        """,
        theme="Learn · Simulator · Money",
        category="Learn · Simulator · Money",
        language="en",
        age=48,
        child_name="Parent",
        word_count=108,
        reading_time_minutes=2.0,
        moral="Love for a child does not require bankrupting the next decade—clarity beats keeping up appearances.",
        graph=graph(
            "wbw_n1_venue",
            [
                N(
                    "wbw_n1_venue",
                    "Six months to the wedding. Your wife forwards a five-star ballroom quote—double what the spreadsheet allows. 'What will people say if we use the community hall?' "
                    "Your daughter looks torn between joy and guilt.",
                    [
                        {
                            "id": "personal_loan_face",
                            "label": "Take a personal loan so the venue matches expectations—figure out EMIs later.",
                            "nextSegmentId": "wbw_n2_debt",
                            "skillDeltas": {"money": -25, "social_capital": 18},
                        },
                        {
                            "id": "show_five_year",
                            "label": "Sit everyone down with a five-year cash-flow sketch: loan EMI vs retirement and emergencies.",
                            "nextSegmentId": "wbw_n3_align",
                            "skillDeltas": {"fiscal_muscle": 40, "balance": 12},
                        },
                    ],
                ),
                N(
                    "wbw_n2_debt",
                    "The sangeet sparkles; emojis pour in. Years later the EMI competes with medical bills. Your daughter says she would have chosen smaller if the numbers had been spoken aloud at the start. "
                    "Status borrowed from tomorrow charges interest on family peace.",
                    [],
                ),
                N(
                    "wbw_n3_align",
                    "Tears and laughter mix around the spreadsheet. You choose a warm hall, splurge on food people remember, skip the chandelier tax. "
                    "Guests still dance; your pension breathes. The lesson lands: dignity is not a star rating.",
                    [],
                ),
            ],
        ),
        post_story_mission="List three wedding costs you would cut first if budget halved—and ask one family member their top non-negotiable.",
        parent_discussion_prompts=[
            "How do you separate love from display?",
            "Who is absent from the room when loans are decided in silence?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
]

# --- stories 16–25 appended below in second block to keep file maintainable ---

ROWS += [
    row(
        title="[LifeSim] The Viral Rumor — Plastic Salt (en)",
        content="""
        # The Viral Rumor

        **Ethics / tech** simulator: a WhatsApp video claims plastic in salt and begs mass forwarding.

        **Lesson:** Chain messages exploit care; verify with official or fact-check sources before you amplify fear.
        """,
        theme="Learn · Simulator · Ethics",
        category="Learn · Simulator · Ethics",
        language="en",
        age=55,
        child_name="Family",
        word_count=102,
        reading_time_minutes=2.0,
        moral="Forwarding without checking trades trust for panic—research is the real kindness.",
        graph=graph(
            "viru_n1_salt",
            [
                N(
                    "viru_n1_salt",
                    "A video shows crystals and a stern voice: plastic in a famous salt brand—'forward to ten people to save lives.' Your aunt already shared it; red ticks multiply.",
                    [
                        {
                            "id": "forward_all",
                            "label": "Forward immediately to every family group—better safe than sorry.",
                            "nextSegmentId": "viru_n2_panic",
                            "skillDeltas": {"integrity": -20, "wisdom": -12},
                        },
                        {
                            "id": "fact_check",
                            "label": "Check a fact-check site and the FSSAI or brand statement before sharing.",
                            "nextSegmentId": "viru_n3_clarity",
                            "skillDeltas": {"wisdom": 30, "integrity": 10},
                        },
                    ],
                ),
                N(
                    "viru_n2_panic",
                    "Small shops pull bags off shelves; neighbors argue at the ration shop. A lab later shows the clip was recycled from another country. "
                    "You feel small knowing fear made you a broadcaster, not a reader.",
                    [],
                ),
                N(
                    "viru_n3_clarity",
                    "Official tests confirm salt is fine; you post one link with a calm sentence. A cousin deletes her forward. "
                    "Boring verification beats heroic rumor.",
                    [],
                ),
            ],
        ),
        post_story_mission="Save one trusted fact-check handle and use it once before your next 'urgent' share.",
        parent_discussion_prompts=[
            "Why does 'share to save lives' bypass our skepticism?",
            "How do you correct elders without shaming them?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Exam Stress Escape (en)",
        content="""
        # The Exam Stress Escape

        **Mental health** simulator after failing a math mock: hide and quit versus asking for a new learning path.

        **Lesson:** Resilience includes showing the paper and requesting help—not disappearing from class.
        """,
        theme="Learn · Simulator · Mental Health",
        category="Learn · Simulator · Mental Health",
        language="en",
        age=14,
        child_name="Student",
        word_count=100,
        reading_time_minutes=2.0,
        moral="Shame grows in secrecy; data plus support turns a bad mark into a map.",
        graph=graph(
            "exm2_n1_fail",
            [
                N(
                    "exm2_n1_fail",
                    "The math mock comes back red. Friends compare marks in the corridor; you want to vanish. Tuition flyers feel like judgment. "
                    "The urge to crumple the paper is loud.",
                    [
                        {
                            "id": "hide_quit",
                            "label": "Hide the marks from parents and stop attending classes for a while.",
                            "nextSegmentId": "exm2_n2_spiral",
                            "skillDeltas": {"balance": -28, "integrity": -18},
                        },
                        {
                            "id": "show_ask_help",
                            "label": "Show your parents the paper and ask to try a different tutor or method.",
                            "nextSegmentId": "exm2_n3_repair",
                            "skillDeltas": {"balance": 22, "wisdom": 15},
                        },
                    ],
                ),
                N(
                    "exm2_n2_spiral",
                    "Absences stack; the gap widens; parents find out from the school portal anyway—now with anger at the hiding. "
                    "You learn too late that silence made the math harder, not easier.",
                    [],
                ),
                N(
                    "exm2_n3_repair",
                    "Your mother winces at the percent, then schedules a diagnostic test with a kind teacher. Topics shrink to bite size. "
                    "The next mock is not perfect, but it moves. You practice saying, 'I need help' as a skill, not a confession.",
                    [],
                ),
            ],
        ),
        post_story_mission="Circle three wrong questions on any old test and redo them with a timer—no grade, just pattern.",
        parent_discussion_prompts=[
            "How is asking for a new method different from giving up?",
            "What does your body feel when you hide a bad mark?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Neighborhood Bully — Swing Timer (en)",
        content="""
        # The Neighborhood Bully

        **Leadership** simulator for kids: a bigger child monopolizes the park swing.

        **Lesson:** Fair rules and calm voice can claim space without fists.
        """,
        theme="Learn · Simulator · Leadership",
        category="Learn · Simulator · Leadership",
        language="en",
        age=10,
        child_name="Kid",
        word_count=95,
        reading_time_minutes=2.0,
        moral="Assertiveness can sound like a fair timer, not a punch.",
        graph=graph(
            "nbl2_n1_swing",
            [
                N(
                    "nbl2_n1_swing",
                    "The swing chain squeaks. A bigger kid blocks the seat—'This is my spot.' Your friends watch from the slide; your stomach flips.",
                    [
                        {
                            "id": "fight",
                            "label": "Push him off to prove you are strong.",
                            "nextSegmentId": "nbl2_n2_trouble",
                            "skillDeltas": {"harmony": -28, "wisdom": -12},
                        },
                        {
                            "id": "timer_fair",
                            "label": 'Say loudly: "We all get five minutes—let\'s use a phone timer."',
                            "nextSegmentId": "nbl2_n3_fair",
                            "skillDeltas": {"leadership": 30, "harmony": 20},
                        },
                    ],
                ),
                N(
                    "nbl2_n2_trouble",
                    "Dust flies; a supervisor runs over; both of you sit out. Your friend whispers that swinging mattered less than watching you become scary too.",
                    [],
                ),
                N(
                    "nbl2_n3_fair",
                    "A parent nods; the timer beeps; turns rotate. The bully tests once, then shrugs and waits his round. "
                    "You learn that leadership can be a rule everyone sees, not a fight nobody wins.",
                    [],
                ),
            ],
        ),
        post_story_mission="Practice one fair rule for a game at home with siblings.",
        parent_discussion_prompts=[
            "When is speaking up different from starting a fight?",
            "Who can kids ask if a bully breaks the timer rule?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Health Insurance Fine Print — Room Rent (en)",
        content="""
        # The Health Insurance Fine Print

        **Finance** simulator at purchase time: agent says 'everything covered' while a room-rent cap hides in the PDF.

        **Lesson:** Ask rupee questions before you sign—especially room category and co-pay.
        """,
        theme="Learn · Simulator · Money",
        category="Learn · Simulator · Money",
        language="en",
        age=40,
        child_name="Buyer",
        word_count=110,
        reading_time_minutes=2.0,
        moral="Trust but verify clauses that cap room rent—otherwise a private room becomes out-of-pocket shock.",
        graph=graph(
            "hins2_n1_agent",
            [
                N(
                    "hins2_n1_agent",
                    "The agent slides brochures: 'Full cashless, sir, everything covered.' Your eye catches tiny text—room rent capping at one percent of sum insured per day. "
                    "He waves it off as 'standard.'",
                    [
                        {
                            "id": "trust_sign",
                            "label": "Trust him and sign today to lock the 'discount.'",
                            "nextSegmentId": "hins2_n2_bill",
                            "skillDeltas": {"money": -22, "wisdom": -12},
                        },
                        {
                            "id": "ask_numbers",
                            "label": "Ask exactly what you pay for a private room in your preferred hospital under this cap.",
                            "nextSegmentId": "hins2_n3_informed",
                            "skillDeltas": {"wisdom": 30, "fiscal_muscle": 15},
                        },
                    ],
                ),
                N(
                    "hins2_n2_bill",
                    "Later, admission happens; the insurer pays a slice; a fifty-thousand-rupee gap appears for room upgrade. "
                    "The agent's voice is busy. Fine print ignored becomes fine print enforced.",
                    [],
                ),
                N(
                    "hins2_n3_informed",
                    "You model two scenarios on paper, compare a higher variant, or accept the cap with eyes open. "
                    "The policy you buy matches the hospitals you actually use. Sleep before illness is cheaper than shock after.",
                    [],
                ),
            ],
        ),
        post_story_mission="Highlight room rent, co-pay, and waiting periods in your policy PDF in three colors.",
        parent_discussion_prompts=[
            "What three questions should every buyer ask before paying premium?",
            "Why do agents minimize caps?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The AI Artist — Freelance Logo (en)",
        content="""
        # The AI Artist

        **Business / ethics** simulator for freelancers: AI-generated logo passed off as hand-drawn versus transparent hybrid workflow.

        **Lesson:** Client trust lives in honest process disclosure; passing off erodes craft and contracts.
        """,
        theme="Learn · Simulator · Ethics",
        category="Learn · Simulator · Ethics",
        language="en",
        age=24,
        child_name="Designer",
        word_count=112,
        reading_time_minutes=2.0,
        moral="Tools are fine; lying about authorship is not—name the pipeline, charge for judgment and refinement.",
        graph=graph(
            "aia_n1_deadline",
            [
                N(
                    "aia_n1_deadline",
                    "A logo deadline looms; you are exhausted. Midjourney can draft ten concepts in minutes; the client praised your 'hand-drawn soul' in the brief. "
                    "No one will zoom pixels, right?",
                    [
                        {
                            "id": "ai_lie_fee",
                            "label": "Generate, lightly tweak, deliver as fully hand-crafted for full fee.",
                            "nextSegmentId": "aia_n2_caught",
                            "skillDeltas": {"integrity": -30, "skill": -12},
                        },
                        {
                            "id": "ai_disclose_redraw",
                            "label": "Use AI for rough ideation, redraw vectors yourself, tell the client honestly.",
                            "nextSegmentId": "aia_n3_trust",
                            "skillDeltas": {"integrity": 30, "skill": 20},
                        },
                    ],
                ),
                N(
                    "aia_n2_caught",
                    "The client's designer friend recognizes telltale glitches; the contract wobbles; your portfolio comment section asks questions you cannot answer. "
                    "One shortcut costs repeat work and self-respect.",
                    [],
                ),
                N(
                    "aia_n3_trust",
                    "You send process slides: prompts, sketches, manual cleanup. The client pays for expertise, not mythology. "
                    "Referrals mention integrity. Your craft sharpens because you still move the pen.",
                    [],
                ),
            ],
        ),
        post_story_mission="Write a one-paragraph 'how I work' blurb listing AI and human steps.",
        parent_discussion_prompts=[
            "When is using AI fair to a paying client?",
            "What part of design cannot be outsourced to a generator?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Lost Wallet — Playground (en)",
        content="""
        # The Lost Wallet

        **Ethics** simulator for kids/teens: found cash, no ID, friends want ice cream.

        **Lesson:** Found money is not found permission—turn it in and let adults trace the owner.
        """,
        theme="Learn · Simulator · Ethics",
        category="Learn · Simulator · Ethics",
        language="en",
        age=12,
        child_name="Student",
        word_count=92,
        reading_time_minutes=2.0,
        moral="Integrity is choosing the principal's desk over the ice-cream queue.",
        graph=graph(
            "lwal_n1_find",
            [
                N(
                    "lwal_n1_find",
                    "A worn wallet lies near the swings—five hundred rupees, no ID, no bus pass. Friends circle: 'Split it—cone for everyone!' Laughter feels like permission.",
                    [
                        {
                            "id": "ice_cream",
                            "label": "Buy treats for the group—finders keepers.",
                            "nextSegmentId": "lwal_n2_guilt",
                            "skillDeltas": {"social_capital": 10, "integrity": -38},
                        },
                        {
                            "id": "principal",
                            "label": "Give the wallet to the school office immediately.",
                            "nextSegmentId": "lwal_n3_right",
                            "skillDeltas": {"integrity": 45, "wisdom": 12},
                        },
                    ],
                ),
                N(
                    "lwal_n2_guilt",
                    "A younger kid sobs later that lunch money vanished; descriptions match. You stare at your sticky fingers. "
                    "Cheap sugar tastes sour when you realize whose week you stole.",
                    [],
                ),
                N(
                    "lwal_n3_right",
                    "The office logs it; a parent claims it the next day with tears. The head mentions honesty in assembly without naming you—but you feel tall walking past the canteen.",
                    [],
                ),
            ],
        ),
        post_story_mission="Role-play finding money: what three steps would you take?",
        parent_discussion_prompts=[
            "Why does peer pressure make found money feel okay?",
            "What would you want someone to do if you lost your wallet?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Bonus Splurge (en)",
        content="""
        # The Bonus Splurge

        **Finance** simulator: Diwali bonus versus upcoming car insurance and a shiny TV.

        **Lesson:** Opportunity cost is invisible until the due date—younger you spends older you's peace.
        """,
        theme="Learn · Simulator · Money",
        category="Learn · Simulator · Money",
        language="en",
        age=34,
        child_name="Earner",
        word_count=100,
        reading_time_minutes=2.0,
        moral="Fund obligations and buffers before lifestyle upgrades—bonuses feel like free money until they are not.",
        graph=graph(
            "bns_n1_bonus",
            [
                N(
                    "bns_n1_bonus",
                    "Fifty thousand lands as a Diwali bonus. The 4K TV deal ends Sunday; car insurance is due in eight weeks—roughly the same size as the TV if you squint. "
                    "The living room feels small; the calendar feels far.",
                    [
                        {
                            "id": "tv_first",
                            "label": "Buy the TV now; next salary can handle insurance somehow.",
                            "nextSegmentId": "bns_n2_scramble",
                            "skillDeltas": {"social_capital": 18, "balance": -22},
                        },
                        {
                            "id": "insurance_reserve",
                            "label": "Park the insurance amount in a separate sub-account; buy a smaller TV or wait for sale.",
                            "nextSegmentId": "bns_n3_smooth",
                            "skillDeltas": {"fiscal_muscle": 30, "balance": 12},
                        },
                    ],
                ),
                N(
                    "bns_n2_scramble",
                    "The TV glows gorgeous; the insurance reminder arrives during a tight month; you borrow on a card. "
                    "Interest nibbles the 'free' bonus. You learn opportunity cost has a sound—EMI alerts at midnight.",
                    [],
                ),
                N(
                    "bns_n3_smooth",
                    "Movie night waits two months; the car papers renew without drama. The older TV still works; peace of mind is its own OLED.",
                    [],
                ),
            ],
        ),
        post_story_mission="List two upcoming mandatory expenses in the next quarter and assign them envelopes today.",
        parent_discussion_prompts=[
            "Why do windfalls feel easier to spend than salary?",
            "How do you decide want vs need with bonus money?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Remote Work Trust (en)",
        content="""
        # The Remote Work Trust

        **Leadership / integrity** simulator: empty calendar after quick tasks—movie versus upskilling or helping a teammate.

        **Lesson:** Professional integrity compounds invisibly; slack today is a loan from tomorrow's reputation.
        """,
        theme="Learn · Simulator · Leadership",
        category="Learn · Simulator · Leadership",
        language="en",
        age=31,
        child_name="Professional",
        word_count=108,
        reading_time_minutes=2.0,
        moral="Trust remote work gives is repaid with predictable output and growth—not with empty hours hidden.",
        graph=graph(
            "rwt_n1_home",
            [
                N(
                    "rwt_n1_home",
                    "You finish core tickets by eleven; the standup praised your speed. Your boss is offline at a summit. A matinee ticket pings—no one would know.",
                    [
                        {
                            "id": "movie",
                            "label": "Log off and catch the film—you earned the break.",
                            "nextSegmentId": "rwt_n2_slip",
                            "skillDeltas": {"integrity": -20},
                        },
                        {
                            "id": "upskill_help",
                            "label": "Use the window to document a runbook and unblock a junior stuck on your dependency.",
                            "nextSegmentId": "rwt_n3_growth",
                            "skillDeltas": {"integrity": 20, "skill": 28},
                        },
                    ],
                ),
                N(
                    "rwt_n2_slip",
                    "It becomes a pattern; standup estimates drift; your manager notices velocity variance without explanation. "
                    "Remote trust is a glass desk—scratches show.",
                    [],
                ),
                N(
                    "rwt_n3_growth",
                    "The junior ships; your doc becomes team canon; lead mentions you in retro. Integrity did not feel cinematic—it felt like typing when no one clapped.",
                    [],
                ),
            ],
        ),
        post_story_mission="Block one calendar hour weekly labeled 'proactive'—documentation, learning, or pairing.",
        parent_discussion_prompts=[
            "How do adults define 'enough' work in a day at home?",
            "When is a break earned versus stolen?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The Fake Parcel Call — FedEx Customs (en)",
        content="""
        # The Fake "Parcel" Call

        **Tech safety** simulator: 'FedEx' plus fake customs plus police bridge—classic authority scam.

        **Lesson:** Hang up; verify on official courier tools and real police numbers—never stay on their line.
        """,
        theme="Learn · Simulator · Digital Safety",
        category="Learn · Simulator · Digital Safety",
        language="en",
        age=52,
        child_name="Recipient",
        word_count=105,
        reading_time_minutes=2.0,
        moral="Legitimate couriers do not threaten arrest over the phone or demand instant transfers to 'clear' fake parcels.",
        graph=graph(
            "fpc_n1_call",
            [
                N(
                    "fpc_n1_call",
                    "A crisp voice says FedEx; a parcel in your name allegedly holds illegal items; Mumbai customs has flagged it; they will connect you to 'Inspector' now. "
                    "Your pulse spikes— you did order supplements online once.",
                    [
                        {
                            "id": "stay_on_line",
                            "label": "Stay on the line and follow instructions to clear your name.",
                            "nextSegmentId": "fpc_n2_bled",
                            "skillDeltas": {"money": -30, "DIGITAL_WISDOM": -30},
                        },
                        {
                            "id": "hangup_verify",
                            "label": "Hang up; open the courier's official site or app; call the police helpline from a government page if worried.",
                            "nextSegmentId": "fpc_n3_safe",
                            "skillDeltas": {"wisdom": 35, "DIGITAL_WISDOM": 28},
                        },
                    ],
                ),
                N(
                    "fpc_n2_bled",
                    "'Verification fees' and 'court deposits' drain lakhs; the line goes dead. The real FedEx chatbot says no such hold exists. "
                    "Authority theater stole your calm first, then your account.",
                    [],
                ),
                N(
                    "fpc_n3_safe",
                    "Official tracking shows no mystery parcel; the cyber cell confirms the script. You teach your building group the pattern. "
                    "Verification beats performance anxiety.",
                    [],
                ),
            ],
        ),
        post_story_mission="Save three official helpline numbers from .gov or bank sites—never from random SMS.",
        parent_discussion_prompts=[
            "Why do scammers impersonate both courier and police?",
            "What is a safe way to verify a scary call?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
    row(
        title="[LifeSim] The First Business Pivot — Tailoring (en)",
        content="""
        # The First Business Pivot

        **Business** simulator: tailoring slows as ready-made e-commerce booms—race to the bottom versus niche pivot.

        **Lesson:** Adaptation beats suicidal pricing; sell what fast fashion cannot—fit, alteration, styling.
        """,
        theme="Learn · Simulator · Business",
        category="Learn · Simulator · Business",
        language="en",
        age=46,
        child_name="Tailor",
        word_count=110,
        reading_time_minutes=2.0,
        moral="Competing only on price against factories is exhaustion; reposition into alterations and premium fit.",
        graph=graph(
            "pvt_n1_slow",
            [
                N(
                    "pvt_n1_slow",
                    "Stitching orders thin; customers show phone screenshots of cheaper kurtas online. Rent is due; you consider slashing rates again.",
                    [
                        {
                            "id": "lower_more",
                            "label": "Cut prices further to match online—volume will return.",
                            "nextSegmentId": "pvt_n2_bleed",
                            "skillDeltas": {"business_health": -28, "money": -10},
                        },
                        {
                            "id": "pivot_alterations",
                            "label": "Pivot to express alterations, restyling online-bought clothes, and premium measurement sessions.",
                            "nextSegmentId": "pvt_n3_niche",
                            "skillDeltas": {"business_health": 38, "wisdom": 20},
                        },
                    ],
                ),
                N(
                    "pvt_n2_bleed",
                    "Your fingers blur; margins vanish; a shoulder injury whispers. You learn factories do not tire—humans cannot price-match robots forever.",
                    [],
                ),
                N(
                    "pvt_n3_niche",
                    "Instagram before/afters of hem fixes go small-viral locally. Boutiques send overflow work. You charge for skill and speed, not thread alone. "
                    "The sewing machine hums with a new story: you finish what algorithms ship wrong.",
                    [],
                ),
            ],
        ),
        post_story_mission="List three services only a human tailor can sell in your neighborhood this month.",
        parent_discussion_prompts=[
            "When is competing on price a trap?",
            "What did your grandparents' shop sell that apps cannot?",
        ],
        parent_content_note="DRAFT batch 3.",
    ),
]


def main() -> None:
    out = (
        Path(__file__).resolve().parents[1]
        / "src/main/resources/db/migration/V96__life_skills_interactive_stories_seed_batch3.sql"
    )
    out.write_text(emit_sql(ROWS), encoding="utf-8")
    print(f"Wrote {out}", flush=True)


if __name__ == "__main__":
    main()
