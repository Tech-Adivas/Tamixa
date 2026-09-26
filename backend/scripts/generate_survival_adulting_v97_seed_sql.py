#!/usr/bin/env python3
"""Generates V97__survival_adulting_stories_seed.sql — Adulting & Survival simulator track."""
from __future__ import annotations

import json
import textwrap
from pathlib import Path

CDN = "https://cdn.tamixa.app/library/sim/survival-adulting/v1/en"
OWNER = "seed:survival-adulting-v1"
THEME = "Learn · Simulator · Adulting & Survival"


def audio(sid: str) -> str:
    return f"{CDN}/{sid}.mp3"


def N(sid: str, text: str, choices: list) -> tuple[str, dict]:
    return sid, {"audioUrl": audio(sid), "text": text, "choices": choices}


def graph(start: str, segments: list) -> dict:
    return {"startSegmentId": start, "overlayStyle": "CARDS", "segments": dict(segments)}


def row(**k):
    k["content"] = textwrap.dedent(k["content"]).strip()
    return k


def emit(rows: list[dict]) -> str:
    lines = ["-- Adulting & Survival interactive library (English). DRAFT.", f"-- story_owner {OWNER}", ""]
    for i, r in enumerate(rows):
        gj = json.dumps(r["graph"], ensure_ascii=False, separators=(",", ":"))
        pj = json.dumps(r["parent_discussion_prompts"], ensure_ascii=False)
        t = f"sv_{i}"

        def dq(tag, s):
            return f"${tag}$" + s + f"${tag}$"

        title = r["title"].replace("'", "''")
        cn = r["child_name"].replace("'", "''")
        lines += [
            "INSERT INTO library_stories (",
            "  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,",
            "  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note",
            ") VALUES (",
            f"  '{title}',",
            f"  {dq(t, r['content'])},",
            f"  '{THEME}',",
            f"  '{THEME}',",
            f"  'en',",
            f"  {r['age']},",
            f"  '{cn}',",
            f"  {r['word_count']},",
            f"  {r['reading_time_minutes']},",
            f"  {dq(t + 'm', r['moral'])},",
            "  'DRAFT',",
            f"  '{OWNER}',",
            f"  {dq(t + 'g', gj)},",
            f"  {dq(t + 'p', r['post_story_mission'])},",
            f"  {dq(t + 'j', pj)}::jsonb,",
            f"  {dq(t + 'n', r['parent_content_note'])}",
            ");",
            "",
        ]
    return "\n".join(lines)


ROWS = [
    row(
        title="[Survival] The Hospital Nightmare — Room Rent (en)",
        content="""
        # The Hospital Nightmare

        **Adulting & Survival:** gallbladder surgery, a five-lakh policy, and the hidden logic of **room rent capping** and **proportionate deduction**.

        **Lesson:** A higher room class can shrink the insurer’s share of the whole bill—not just the bed.
        """,
        age=48,
        child_name="Family",
        word_count=130,
        reading_time_minutes=2.5,
        moral="Always match room category to your policy’s rent limit before admission; ‘cashless’ is not ‘unlimited’.",
        graph=graph(
            "hnm_n1_crisis",
            [
                N(
                    "hnm_n1_crisis",
                    "Dad needs gallbladder surgery tonight. The hospital quotes about one lakh fifty thousand rupees. Your family floater shows five lakh sum insured—relief washes over you. "
                    "The coordinator smiles: a deluxe suite is available, and ‘insurance will handle it.’ Your cousin whispers that a private room looks more dignified in front of relatives.",
                    [
                        {
                            "id": "deluxe_suite",
                            "label": 'Book the deluxe suite—five lakhs cover should absorb everything.',
                            "nextSegmentId": "hnm_n2_shock",
                            "skillDeltas": {"money": -18, "wisdom": -10},
                        },
                        {
                            "id": "check_cap",
                            "label": "Stop—open the policy PDF and find the room rent limit before signing the bed category.",
                            "nextSegmentId": "hnm_n3_planned",
                            "skillDeltas": {"wisdom": 25, "fiscal_muscle": 18},
                        },
                    ],
                ),
                N(
                    "hnm_n2_shock",
                    "The discharge bill lands heavy. Insurer pays roughly one lakh ten thousand—not one fifty. The letter cites **proportionate deduction**: your suite crossed the **room rent cap**, so surgeon fees, OT, and consumables were scaled down as if you had chosen an ‘unauthorized’ tariff. "
                    "The family argues at the desk; dignity in the lobby cost forty thousand rupees nobody budgeted. Generic medicines versus brands becomes a bitter footnote.",
                    [],
                ),
                N(
                    "hnm_n3_planned",
                    "You read the clause: one percent of sum insured per day, or a named category. You pick the twin-sharing room that fits. The billing desk grumbles but aligns. "
                    "At discharge, the E-card clears predictably; you compare surgeon lines on the estimate with the policy wording. Dad recovers; the savings account breathes.",
                    [],
                ),
            ],
        ),
        post_story_mission="Open your health policy now: highlight room rent/sub-limit, co-pay, and ‘proportionate deduction’ in three colors.",
        parent_discussion_prompts=[
            "Ask your family: Do we know the room rent limit of our current health policy?",
            "Would you rather comfort in the lobby or clarity on the bill?",
        ],
        parent_content_note="DRAFT; flagship Adulting & Survival pilot. Audio paths under library/sim/survival-adulting/v1/en/.",
    ),
    row(
        title="[Survival] The Secret Loan — Spouse & Relative (en)",
        content="""
        # The Secret Loan

        **Marriage / finance:** a spouse discovers the other pledged income for a relative’s emergency without a shared conversation.

        **Lesson:** Shared money needs shared decisions—secrecy erodes trust faster than the EMI.
        """,
        age=36,
        child_name="Spouse",
        word_count=115,
        reading_time_minutes=2.0,
        moral="Financial infidelity is still infidelity—agree on thresholds for lending, gifts, and guarantees.",
        graph=graph(
            "slt_n1_statement",
            [
                N(
                    "slt_n1_statement",
                    "The bank SMS arrives on your phone: EMI debited for a personal loan you never discussed. Your partner looks away—‘Bhaiya needed it; you would have said no.’ "
                    "The amount is two years of your child’s school fund.",
                    [
                        {
                            "id": "silent_rage",
                            "label": "Freeze communication and move money into an account they cannot see.",
                            "nextSegmentId": "slt_n2_wedge",
                            "skillDeltas": {"harmony": -30, "integrity": -15},
                        },
                        {
                            "id": "table_talk",
                            "label": "Call a calm sit-down: full numbers, written rules for extended family help.",
                            "nextSegmentId": "slt_n3_repair",
                            "skillDeltas": {"wisdom": 28, "social_capital": 15},
                        },
                    ],
                ),
                N(
                    "slt_n2_wedge",
                    "Accounts split; lawyers get whispered. The loan still runs; now it runs through resentment. The cousin pays irregularly; the marriage pays daily.",
                    [],
                ),
                N(
                    "slt_n3_repair",
                    "You draft a one-page family finance charter: emergency caps, no new debt without both signatures, documented gifts. Awkward weeks follow—but future shocks hit together, not sideways.",
                    [],
                ),
            ],
        ),
        post_story_mission="Write your household rule: above what rupee amount must both partners agree before lending?",
        parent_discussion_prompts=[
            "Where is the line between kindness to relatives and loyalty to your nuclear family?",
            "How do you rebuild trust after hidden debt?",
        ],
        parent_content_note="DRAFT batch Adulting & Survival.",
    ),
    row(
        title="[Survival] The Pink Slip — Voluntary Resignation (en)",
        content="""
        # The Pink Slip

        **Jobs / labor awareness:** HR asks you to resign ‘voluntarily’ during a restructuring.

        **Lesson:** Notice pay, retrenchment rules, and documentation matter—do not sign under hallway pressure.
        """,
        age=32,
        child_name="Employee",
        word_count=120,
        reading_time_minutes=2.0,
        moral="Voluntary resignation can waive rights you would have under lawful termination—read, pause, consult.",
        graph=graph(
            "psk_n1_hr",
            [
                N(
                    "psk_n1_hr",
                    "HR slides a letter in a quiet meeting: ‘We need your resignation today for a smooth reference.’ Your manager avoids eye contact. Your heart pounds—visa, rent, parents.",
                    [
                        {
                            "id": "sign_now",
                            "label": "Sign immediately so the reference stays warm.",
                            "nextSegmentId": "psk_n2_waived",
                            "skillDeltas": {"money": -15, "wisdom": -20},
                        },
                        {
                            "id": "review_rights",
                            "label": "Ask for the letter overnight, read appointment terms, speak to a union peer or lawyer helpline.",
                            "nextSegmentId": "psk_n3_shielded",
                            "skillDeltas": {"wisdom": 30, "fiscal_muscle": 10},
                        },
                    ],
                ),
                N(
                    "psk_n2_waived",
                    "You learn later that notice pay and statutory timelines vanished with your signature. The ‘smooth reference’ was cheaper for payroll than honesty.",
                    [],
                ),
                N(
                    "psk_n3_shielded",
                    "You return with calm questions; some colleagues organize; you secure written clarity on dues and dates. The exit still hurts—but it is bounded, not blank.",
                    [],
                ),
            ],
        ),
        post_story_mission="Save one reputable legal-aid or labour-rights FAQ link applicable to your state.",
        parent_discussion_prompts=[
            "Why do companies prefer ‘voluntary’ exits?",
            "Who can you call before signing under stress?",
        ],
        parent_content_note="DRAFT; not legal advice—prompts only.",
    ),
    row(
        title="[Survival] The Vanishing Minimum — Fees & Auto-Sweep (en)",
        content="""
        # The Vanishing Minimum

        **Banking:** ten thousand rupees in savings shrinks to eight—SMS packs, ATM charges, small debits.

        **Lesson:** Read statements; use **auto-sweep** (or equivalent) so idle cash earns closer to FD rates while staying liquid.
        """,
        age=29,
        child_name="Saver",
        word_count=118,
        reading_time_minutes=2.0,
        moral="Minimum balance is a promise to yourself—track nickle-and-dime fees and make idle money work.",
        graph=graph(
            "vmn_n1_alert",
            [
                N(
                    "vmn_n1_alert",
                    "Your salary account looked healthy last month. Today the app shows eight thousand. SMS alerts, cross-ATM withdrawals, and a ‘value pack’ you tapped once stack quietly.",
                    [
                        {
                            "id": "ignore",
                            "label": "Top up and forget—it's only small money.",
                            "nextSegmentId": "vmn_n2_bleed",
                            "skillDeltas": {"money": -12, "wisdom": -15},
                        },
                        {
                            "id": "fix_sweep",
                            "label": "Download six months of statements, disable junk packs, enable auto-sweep above a threshold.",
                            "nextSegmentId": "vmn_n3_hack",
                            "skillDeltas": {"fiscal_muscle": 30, "wisdom": 18},
                        },
                    ],
                ),
                N(
                    "vmn_n2_bleed",
                    "Year on year, friction fees eat a month of groceries. Compounding works against you when leaks are invisible.",
                    [],
                ),
                N(
                    "vmn_n3_hack",
                    "One toggle moves surplus into linked FD chunks nightly; seven percent on slices beats three on idle dust. You set SMS alerts only for debits above five hundred. The statement becomes boring—in a good way.",
                    [],
                ),
            ],
        ),
        post_story_mission="In your banking app, find ‘auto-sweep’ or ‘flexi deposit’ and note the threshold you would set.",
        parent_discussion_prompts=[
            "What three lines on a bank statement confuse most people?",
            "When is convenience worth a monthly fee?",
        ],
        parent_content_note="DRAFT; rates illustrative—verify with your bank.",
    ),
    row(
        title="[Survival] The Surgery Denied — Proposal Truth (en)",
        content="""
        # The Surgery Denied

        **Insurance:** a two-year-old policy refuses a knee surgery as ‘pre-existing’ because the proposal hid old pain.

        **Lesson:** Disclose completely on the form—underwriting beats claim-time detective work.
        """,
        age=52,
        child_name="Policyholder",
        word_count=112,
        reading_time_minutes=2.0,
        moral="Honest proposals may cost a loading or waiting period; dishonest ones cost the whole claim.",
        graph=graph(
            "sdx_n1_form",
            [
                N(
                    "sdx_n1_form",
                    "The agent says, ‘Don’t mention the old sports injury—premiums jump.’ Your spouse hesitates; the form glows on screen.",
                    [
                        {
                            "id": "omit",
                            "label": "Skip the old injury—save premium now.",
                            "nextSegmentId": "sdx_n2_denied",
                            "skillDeltas": {"integrity": -35, "money": -25},
                        },
                        {
                            "id": "disclose",
                            "label": "Declare everything; accept loading or a named exclusion with eyes open.",
                            "nextSegmentId": "sdx_n3_covered",
                            "skillDeltas": {"integrity": 35, "wisdom": 22},
                        },
                    ],
                ),
                N(
                    "sdx_n2_denied",
                    "Investigation finds physiotherapy bills. The denial letter cites non-disclosure. The wait-period trap becomes a courtroom tone you cannot afford.",
                    [],
                ),
                N(
                    "sdx_n3_covered",
                    "Premium is higher; maybe a waiting period applies—but when surgery comes, the claim path is straight. Sleep before illness is the real rider.",
                    [],
                ),
            ],
        ),
        post_story_mission="Re-read your last proposal: is every chronic issue listed truthfully?",
        parent_discussion_prompts=[
            "Why do agents push ‘clean’ forms?",
            "What is a waiting period versus a lie?",
        ],
        parent_content_note="DRAFT.",
    ),
    row(
        title="[Survival] The Tax Notice — Nil GST Return (en)",
        content="""
        # The Tax Notice

        **Business compliance:** a small vendor skipped filing when sales were zero.

        **Lesson:** Nil returns still have a deadline; non-compliance penalties can exceed the tax.
        """,
        age=41,
        child_name="Vendor",
        word_count=105,
        reading_time_minutes=2.0,
        moral="A nil return is five minutes; a notice is five weeks of stress—calendar the cycle.",
        graph=graph(
            "gst_n1_busy",
            [
                N(
                    "gst_n1_busy",
                    "No sales this quarter—why bother logging in? A brown envelope arrives: late fee and interest for missed filings.",
                    [
                        {
                            "id": "ignore_notice",
                            "label": "Ignore the letter until business picks up.",
                            "nextSegmentId": "gst_n2_spiral",
                            "skillDeltas": {"money": -20, "business_health": -25},
                        },
                        {
                            "id": "file_nil",
                            "label": "File nil returns immediately and set phone reminders for every cycle.",
                            "nextSegmentId": "gst_n3_clean",
                            "skillDeltas": {"wisdom": 28, "fiscal_muscle": 20},
                        },
                    ],
                ),
                N(
                    "gst_n2_spiral",
                    "Penalties compound; bank limits wobble when compliance flags rise. The ‘saved’ hour costs reputation with buyers who need your GSTIN clean.",
                    [],
                ),
                N(
                    "gst_n3_clean",
                    "Dashboard green feels dull until you compare it to a neighbor’s raid story. Compliance is the rent for playing in the formal economy.",
                    [],
                ),
            ],
        ),
        post_story_mission="Add quarterly GST / filing dates to your family calendar with two reminders.",
        parent_discussion_prompts=[
            "When is a zero-sales quarter still a filing quarter?",
            "Who helps micro-businesses with free compliance clinics?",
        ],
        parent_content_note="DRAFT; not tax advice.",
    ),
    row(
        title="[Survival] Term vs Money-Back — Twenty Years (en)",
        content="""
        # Term vs Money-Back

        **Money hack simulation:** compare a costly money-back plan with low life cover versus pure term over twenty years.

        **Lesson:** Protection first—savings belong in instruments built for growth, not mixed insurance.
        """,
        age=35,
        child_name="Parent",
        word_count=125,
        reading_time_minutes=2.5,
        moral="Huge cover at low premium (term) plus disciplined investing usually beats bundled ‘returns’.",
        graph=graph(
            "tvt_n1_agent",
            [
                N(
                    "tvt_n1_agent",
                    "The agent projects a glossy chart: money-back after fifteen years, ‘guaranteed’ lakhs, tiny premiums. Term insurance sounds like ‘money gone if nothing happens.’",
                    [
                        {
                            "id": "money_back",
                            "label": "Buy the money-back plan for peace and cashback.",
                            "nextSegmentId": "tvt_n2_thin_cover",
                            "skillDeltas": {"money": -15, "wisdom": -18},
                        },
                        {
                            "id": "term_plus_sip",
                            "label": "Buy high-cover term only; route the difference into a simple index SIP.",
                            "nextSegmentId": "tvt_n3_stack",
                            "skillDeltas": {"fiscal_muscle": 32, "wisdom": 22},
                        },
                    ],
                ),
                N(
                    "tvt_n2_thin_cover",
                    "A tragedy years later exposes cover that cannot clear the home loan. Returns lag inflation; opportunity cost whispers in every statement.",
                    [],
                ),
                N(
                    "tvt_n3_stack",
                    "The spreadsheet is blunt: term premium is a sliver; the SIP curve crosses the ‘guaranteed’ line with room to breathe. You teach the kids the difference between insurance and investment.",
                    [],
                ),
            ],
        ),
        post_story_mission="List premium, sum assured, and exclusions for any policy you own—one sticky note each.",
        parent_discussion_prompts=[
            "Why do money-back plans feel safer than term?",
            "What is the job of life insurance in your family?",
        ],
        parent_content_note="DRAFT; illustrations only.",
    ),
    row(
        title="[Survival] The SIP Leak Hunt (en)",
        content="""
        # The SIP Leak Hunt

        **Money hack:** find five hundred rupees of monthly leaks—subscriptions, chai breaks, impulse top-ups—to fund a starter SIP.

        **Lesson:** Small leaks veto big goals until you name them.
        """,
        age=27,
        child_name="Earner",
        word_count=100,
        reading_time_minutes=2.0,
        moral="Visibility beats willpower—track first, cut second, automate third.",
        graph=graph(
            "sip_n1_goal",
            [
                N(
                    "sip_n1_goal",
                    "You want a five-thousand SIP but cash feels tight. A friend says find leaks first.",
                    [
                        {
                            "id": "guess_cut",
                            "label": "Cancel one big subscription without checking the rest.",
                            "nextSegmentId": "sip_n2_bounce",
                            "skillDeltas": {"balance": -12, "money": -8},
                        },
                        {
                            "id": "audit",
                            "label": "Export UPI/bank tags for 60 days; circle recurring and impulse buckets.",
                            "nextSegmentId": "sip_n3_funded",
                            "skillDeltas": {"fiscal_muscle": 28, "wisdom": 15},
                        },
                    ],
                ),
                N(
                    "sip_n2_bounce",
                    "The SIP bounces once; bank fees eat optimism. Random cuts don’t stick without a map.",
                    [],
                ),
                N(
                    "sip_n3_funded",
                    "Forgotten OTT, auto-tips, and smoke money add up to seven hundred. You schedule SIP day-two after salary; leaks fund the first three months consciously.",
                    [],
                ),
            ],
        ),
        post_story_mission="Highlight every auto-debit in your last two statements—decide keep vs cancel for each.",
        parent_discussion_prompts=[
            "What is one ‘small’ expense that became a habit?",
            "How small can a first SIP be and still count?",
        ],
        parent_content_note="DRAFT.",
    ),
    row(
        title="[Survival] Survival 101 — Three Drills (en)",
        content="""
        # Survival 101 — Three Drills

        **Free foundation track:** pick one drill—reading a bank statement, checking a health policy, or the first sixty minutes after cyber theft.

        **Lesson:** Panic shrinks when you have a sequence memorized.
        """,
        age=18,
        child_name="You",
        word_count=110,
        reading_time_minutes=2.0,
        moral="Rehearse boring steps before the emergency—buttons matter more than headlines.",
        graph=graph(
            "s101_n1_pick",
            [
                N(
                    "s101_n1_pick",
                    "You have twenty quiet minutes. Which survival skill will you practice today?",
                    [
                        {
                            "id": "bank_stmt",
                            "label": "Drill: read a bank statement line by line (debits, balances, fees).",
                            "nextSegmentId": "s101_n2_bank",
                            "skillDeltas": {"fiscal_muscle": 18, "wisdom": 12},
                        },
                        {
                            "id": "policy",
                            "label": "Drill: open your health policy—find room rent, co-pay, exclusions.",
                            "nextSegmentId": "s101_n3_policy",
                            "skillDeltas": {"wisdom": 22, "fiscal_muscle": 12},
                        },
                        {
                            "id": "cyber60",
                            "label": "Drill: rehearse the first 60 minutes after UPI fraud (1930, bank, freeze).",
                            "nextSegmentId": "s101_n4_cyber",
                            "skillDeltas": {"DIGITAL_WISDOM": 28, "wisdom": 15},
                        },
                    ],
                ),
                N(
                    "s101_n2_bank",
                    "You circle opening balance, credits, every debit category, and closing balance. You flag unknown merchant codes for one verification call. Boring becomes power.",
                    [],
                ),
                N(
                    "s101_n3_policy",
                    "You screenshot the room rent clause and WhatsApp it to your spouse with one sentence: ‘We stay below this bed class.’ The TPA number goes into contacts as ‘Health claim’.",
                    [],
                ),
                N(
                    "s101_n4_cyber",
                    "You write a card: minute zero—call 1930; minute ten—bank fraud desk; minute twenty—screenshot and save UPI IDs; minute forty—password resets on email; no shame, fast moves. The golden hour is real.",
                    [],
                ),
            ],
        ),
        post_story_mission="Teach one household member the drill you chose—voice beats solo scrolling.",
        parent_discussion_prompts=[
            "Which number would you dial first if money left your account today?",
            "What document do you wish you had photographed before an emergency?",
        ],
        parent_content_note="DRAFT; Survival 101 free-track pilot.",
    ),
]


def main():
    out = Path(__file__).resolve().parents[1] / "src/main/resources/db/migration/V97__survival_adulting_stories_seed.sql"
    out.write_text(emit(ROWS), encoding="utf-8")
    print("Wrote", out)


if __name__ == "__main__":
    main()
