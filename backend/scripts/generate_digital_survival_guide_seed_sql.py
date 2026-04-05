#!/usr/bin/env python3
"""
Generates:
- V86__digital_survival_guide_script_seed.sql — Ep01 (6 locales) + Ep02–15 English outlines
- V87__digital_survival_guide_ep02_kyc_graph.sql — UPDATE Ep02 English row
- V88__digital_survival_guide_ep03_ep05_graphs.sql — UPDATE Ep03–Ep05 English pilot rows
- V89__digital_survival_guide_ep06_ep08_graphs.sql — UPDATE Ep06–Ep08 English pilot rows
- V90__digital_survival_guide_ep09_ep15_graphs.sql — UPDATE Ep09–Ep15 English pilot rows

Run from repo root: python3 backend/scripts/generate_digital_survival_guide_seed_sql.py
"""
from __future__ import annotations

import json
from pathlib import Path

THEME = "Learn · Simulator · Digital Safety"
CATEGORY = "Learn · Simulator · Digital Safety"
STORY_OWNER = "seed:digital-survival-guide-v1"
STATUS = "DRAFT"
AGE = 10
CHILD_NAME = "Family"


def graph_for_locale(lang: str, labels: dict[str, str]) -> dict:
    base = f"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep01/{lang}/"
    return {
        "startSegmentId": "ep01_hook",
        "overlayStyle": "CARDS",
        "segments": {
            "ep01_hook": {
                "audioUrl": base + "hook_to_node1.mp3",
                "choices": [
                    {
                        "id": "call_sms_number",
                        "label": labels["n1a"],
                        "nextSegmentId": "ep01_mid_panic_call",
                        "skillDeltas": {"DIGITAL_WISDOM": -2, "balance": -1},
                    },
                    {
                        "id": "open_official_app",
                        "label": labels["n1b"],
                        "nextSegmentId": "ep01_mid_verify_app",
                        "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
                    },
                    {
                        "id": "ask_family_review_sms",
                        "label": labels["n1c"],
                        "nextSegmentId": "ep01_mid_collaborate",
                        "skillDeltas": {"DIGITAL_WISDOM": 1, "balance": 1},
                    },
                ],
            },
            "ep01_mid_panic_call": {
                "audioUrl": base + "branch_call_plus_fee_trap.mp3",
                "choices": [
                    {
                        "id": "pay_ten_fee",
                        "label": labels["n2a"],
                        "nextSegmentId": "ep01_outcome_pin_trap",
                        "skillDeltas": {"DIGITAL_WISDOM": -3},
                    },
                    {
                        "id": "refuse_upi_on_link",
                        "label": labels["n2b"],
                        "nextSegmentId": "ep01_outcome_safe",
                        "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
                    },
                    {
                        "id": "report_and_block",
                        "label": labels["n2c"],
                        "nextSegmentId": "ep01_outcome_report",
                        "skillDeltas": {"DIGITAL_WISDOM": 3, "balance": 1},
                    },
                ],
            },
            "ep01_mid_verify_app": {
                "audioUrl": base + "branch_app_plus_fee_trap.mp3",
                "choices": [
                    {
                        "id": "pay_ten_fee",
                        "label": labels["n2a"],
                        "nextSegmentId": "ep01_outcome_pin_trap",
                        "skillDeltas": {"DIGITAL_WISDOM": -3},
                    },
                    {
                        "id": "refuse_upi_on_link",
                        "label": labels["n2b"],
                        "nextSegmentId": "ep01_outcome_safe",
                        "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
                    },
                    {
                        "id": "report_and_block",
                        "label": labels["n2c"],
                        "nextSegmentId": "ep01_outcome_report",
                        "skillDeltas": {"DIGITAL_WISDOM": 3, "balance": 1},
                    },
                ],
            },
            "ep01_mid_collaborate": {
                "audioUrl": base + "branch_collab_plus_fee_trap.mp3",
                "choices": [
                    {
                        "id": "pay_ten_fee",
                        "label": labels["n2a"],
                        "nextSegmentId": "ep01_outcome_pin_trap",
                        "skillDeltas": {"DIGITAL_WISDOM": -3},
                    },
                    {
                        "id": "refuse_upi_on_link",
                        "label": labels["n2b"],
                        "nextSegmentId": "ep01_outcome_safe",
                        "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
                    },
                    {
                        "id": "report_and_block",
                        "label": labels["n2c"],
                        "nextSegmentId": "ep01_outcome_report",
                        "skillDeltas": {"DIGITAL_WISDOM": 3, "balance": 1},
                    },
                ],
            },
            "ep01_outcome_pin_trap": {
                "audioUrl": base + "outcome_what_if_pin.mp3",
                "choices": [],
            },
            "ep01_outcome_safe": {
                "audioUrl": base + "outcome_safe_family.mp3",
                "choices": [],
            },
            "ep01_outcome_report": {
                "audioUrl": base + "outcome_reported.mp3",
                "choices": [],
            },
        },
    }


def graph_ep02_for_locale(lang: str, labels: dict[str, str]) -> dict:
    """KYC urgency scam — same 7-segment shape as Ep01 (hook → 3 mids → 3 terminals)."""
    base = f"https://cdn.tamixa.app/library/sim/digital-survival-guide/ep02/{lang}/"
    mid_choices = [
        {
            "id": "pay_two_gateway",
            "label": labels["n2a"],
            "nextSegmentId": "ep02_outcome_harvest",
            "skillDeltas": {"DIGITAL_WISDOM": -3},
        },
        {
            "id": "refuse_sms_payment",
            "label": labels["n2b"],
            "nextSegmentId": "ep02_outcome_safe",
            "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
        },
        {
            "id": "call_official_line",
            "label": labels["n2c"],
            "nextSegmentId": "ep02_outcome_report",
            "skillDeltas": {"DIGITAL_WISDOM": 3, "balance": 1},
        },
    ]
    return {
        "startSegmentId": "ep02_hook",
        "overlayStyle": "CARDS",
        "segments": {
            "ep02_hook": {
                "audioUrl": base + "hook_to_node1.mp3",
                "choices": [
                    {
                        "id": "tap_kyc_link",
                        "label": labels["n1a"],
                        "nextSegmentId": "ep02_mid_tap_link",
                        "skillDeltas": {"DIGITAL_WISDOM": -2, "balance": -1},
                    },
                    {
                        "id": "open_bank_app",
                        "label": labels["n1b"],
                        "nextSegmentId": "ep02_mid_bank_app",
                        "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
                    },
                    {
                        "id": "call_card_helpline",
                        "label": labels["n1c"],
                        "nextSegmentId": "ep02_mid_card_call",
                        "skillDeltas": {"DIGITAL_WISDOM": 1, "balance": 1},
                    },
                ],
            },
            "ep02_mid_tap_link": {
                "audioUrl": base + "branch_tap_link_plus_fee_trap.mp3",
                "choices": mid_choices,
            },
            "ep02_mid_bank_app": {
                "audioUrl": base + "branch_bank_app_plus_fee_trap.mp3",
                "choices": mid_choices,
            },
            "ep02_mid_card_call": {
                "audioUrl": base + "branch_card_call_plus_fee_trap.mp3",
                "choices": mid_choices,
            },
            "ep02_outcome_harvest": {
                "audioUrl": base + "outcome_otp_harvest.mp3",
                "choices": [],
            },
            "ep02_outcome_safe": {
                "audioUrl": base + "outcome_safe_family.mp3",
                "choices": [],
            },
            "ep02_outcome_report": {
                "audioUrl": base + "outcome_reported.mp3",
                "choices": [],
            },
        },
    }


def seven_segment_simulator_graph(
    prefix: str,
    lang: str,
    labels: dict[str, str],
    n1: tuple[tuple[str, str], tuple[str, str], tuple[str, str]],
    mid_audio: tuple[str, str, str],
    n2_ids: tuple[str, str, str],
    bad_outcome_audio: str,
) -> dict:
    """Reusable 2-node simulator: hook → 3 mids (same node-2 choices) → bad / safe / report terminals."""
    base = f"https://cdn.tamixa.app/library/sim/digital-survival-guide/{prefix}/{lang}/"
    mid_ids = (f"{prefix}_mid_1", f"{prefix}_mid_2", f"{prefix}_mid_3")
    obad = f"{prefix}_outcome_bad"
    osafe = f"{prefix}_outcome_safe"
    orep = f"{prefix}_outcome_report"
    mid_choices = [
        {
            "id": n2_ids[0],
            "label": labels["n2a"],
            "nextSegmentId": obad,
            "skillDeltas": {"DIGITAL_WISDOM": -3},
        },
        {
            "id": n2_ids[1],
            "label": labels["n2b"],
            "nextSegmentId": osafe,
            "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
        },
        {
            "id": n2_ids[2],
            "label": labels["n2c"],
            "nextSegmentId": orep,
            "skillDeltas": {"DIGITAL_WISDOM": 3, "balance": 1},
        },
    ]
    segs: dict[str, dict] = {
        f"{prefix}_hook": {
            "audioUrl": base + "hook_to_node1.mp3",
            "choices": [
                {
                    "id": n1[0][0],
                    "label": labels[n1[0][1]],
                    "nextSegmentId": mid_ids[0],
                    "skillDeltas": {"DIGITAL_WISDOM": -2, "balance": -1},
                },
                {
                    "id": n1[1][0],
                    "label": labels[n1[1][1]],
                    "nextSegmentId": mid_ids[1],
                    "skillDeltas": {"DIGITAL_WISDOM": 2, "balance": 1},
                },
                {
                    "id": n1[2][0],
                    "label": labels[n1[2][1]],
                    "nextSegmentId": mid_ids[2],
                    "skillDeltas": {"DIGITAL_WISDOM": 1, "balance": 1},
                },
            ],
        },
        mid_ids[0]: {"audioUrl": base + mid_audio[0], "choices": mid_choices},
        mid_ids[1]: {"audioUrl": base + mid_audio[1], "choices": mid_choices},
        mid_ids[2]: {"audioUrl": base + mid_audio[2], "choices": mid_choices},
        obad: {"audioUrl": base + bad_outcome_audio, "choices": []},
        osafe: {"audioUrl": base + "outcome_safe_family.mp3", "choices": []},
        orep: {"audioUrl": base + "outcome_reported.mp3", "choices": []},
    }
    return {"startSegmentId": f"{prefix}_hook", "overlayStyle": "CARDS", "segments": segs}


def graph_ep03_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep03",
        lang,
        labels,
        n1=(
            ("pay_49_fee", "n1a"),
            ("check_official_tracking", "n1b"),
            ("ask_family_parcel", "n1c"),
        ),
        mid_audio=(
            "branch_pay49_plus_apk_trap.mp3",
            "branch_track_plus_apk_trap.mp3",
            "branch_family_plus_apk_trap.mp3",
        ),
        n2_ids=("install_tracking_apk", "refuse_apk_from_sms", "call_courier_official_site"),
        bad_outcome_audio="outcome_apk_malware.mp3",
    )


def graph_ep04_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep04",
        lang,
        labels,
        n1=(
            ("send_upi_now", "n1a"),
            ("hangup_call_school_landline", "n1b"),
            ("call_spouse_verify", "n1c"),
        ),
        mid_audio=(
            "branch_send_plus_secrecy_trap.mp3",
            "branch_landline_plus_secrecy_trap.mp3",
            "branch_spouse_plus_secrecy_trap.mp3",
        ),
        n2_ids=("pay_secrecy_fee", "refuse_secrecy_red_flag", "conference_teacher_known"),
        bad_outcome_audio="outcome_upi_sent.mp3",
    )


def graph_ep05_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep05",
        lang,
        labels,
        n1=(
            ("pay_registration_499", "n1a"),
            ("check_official_careers_only", "n1b"),
            ("ask_family_it_elder", "n1c"),
        ),
        mid_audio=(
            "branch_pay499_plus_fasttrack_trap.mp3",
            "branch_careers_plus_fasttrack_trap.mp3",
            "branch_elder_plus_fasttrack_trap.mp3",
        ),
        n2_ids=("pay_fasttrack_99", "stop_fake_job_fees", "report_scam_number"),
        bad_outcome_audio="outcome_registration_lost.mp3",
    )


def graph_ep06_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep06",
        lang,
        labels,
        n1=(
            ("pay_one_rupee_verify", "n1a"),
            ("delete_never_entered", "n1b"),
            ("search_scam_with_family", "n1c"),
        ),
        mid_audio=(
            "branch_pay_one_plus_telegram_trap.mp3",
            "branch_delete_plus_telegram_trap.mp3",
            "branch_search_plus_telegram_trap.mp3",
        ),
        n2_ids=("add_telegram_admin_otp", "refuse_otp_is_account_key", "block_mark_spam"),
        bad_outcome_audio="outcome_telegram_otp_harvest.mp3",
    )


def graph_ep07_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep07",
        lang,
        labels,
        n1=(
            ("register_subsidy_link", "n1a"),
            ("check_mygov_official_only", "n1b"),
            ("ask_gas_agency_next_visit", "n1c"),
        ),
        mid_audio=(
            "branch_register_plus_aadhaar_trap.mp3",
            "branch_mygov_plus_aadhaar_trap.mp3",
            "branch_local_plus_aadhaar_trap.mp3",
        ),
        n2_ids=("upload_aadhaar_fake_form", "refuse_aadhaar_random_site", "use_official_uidai_only"),
        bad_outcome_audio="outcome_aadhaar_harvest.mp3",
    )


def graph_ep08_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep08",
        lang,
        labels,
        n1=(
            ("pay_five_hundred_link", "n1a"),
            ("open_real_policy_papers", "n1b"),
            ("plan_branch_visit", "n1c"),
        ),
        mid_audio=(
            "branch_pay500_plus_screen_share_trap.mp3",
            "branch_policy_doc_plus_screen_share_trap.mp3",
            "branch_branch_plan_plus_screen_share_trap.mp3",
        ),
        n2_ids=("share_screen_anydesk", "refuse_insurer_remote_tool", "hangup_call_policy_helpline"),
        bad_outcome_audio="outcome_screen_share_takeover.mp3",
    )


def graph_ep09_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep09",
        lang,
        labels,
        n1=(
            ("panic_open_pan_sms_link", "n1a"),
            ("type_incometax_gov_url_only", "n1b"),
            ("ask_ca_or_family_reader", "n1c"),
        ),
        mid_audio=(
            "branch_panic_link_plus_100_fee_trap.mp3",
            "branch_typed_itr_plus_100_fee_trap.mp3",
            "branch_ca_family_plus_100_fee_trap.mp3",
        ),
        n2_ids=("pay_100_processing_upi", "refuse_random_gov_sms_fee", "verify_deadline_official_site_only"),
        bad_outcome_audio="outcome_fake_itr_harvest.mp3",
    )


def graph_ep10_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep10",
        lang,
        labels,
        n1=(
            ("return_5000_to_stranger_upi", "n1a"),
            ("check_upi_actually_credited", "n1b"),
            ("ignore_accidental_transfer_script", "n1c"),
        ),
        mid_audio=(
            "branch_return_now_plus_test1_trap.mp3",
            "branch_check_credit_plus_test1_trap.mp3",
            "branch_ignore_script_plus_test1_trap.mp3",
        ),
        n2_ids=("send_one_rupee_test_payment", "refuse_test_trains_trust", "use_bank_dispute_not_stranger"),
        bad_outcome_audio="outcome_upi_refund_trap.mp3",
    )


def graph_ep11_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep11",
        lang,
        labels,
        n1=(
            ("download_teamviewer_refund", "n1a"),
            ("check_orders_in_retail_app_only", "n1b"),
            ("ask_teen_verify_order_id", "n1c"),
        ),
        mid_audio=(
            "branch_teamviewer_plus_remote_type_trap.mp3",
            "branch_inapp_orders_plus_remote_type_trap.mp3",
            "branch_teen_verify_plus_remote_type_trap.mp3",
        ),
        n2_ids=("type_refund_in_remote_box", "refuse_amounts_for_refund", "use_in_app_help_only"),
        bad_outcome_audio="outcome_remote_desktop_harvest.mp3",
    )


def graph_ep12_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep12",
        lang,
        labels,
        n1=(
            ("pay_police_badge_fee", "n1a"),
            ("read_official_matrimonial_faq", "n1b"),
            ("discuss_match_offline_with_family", "n1c"),
        ),
        mid_audio=(
            "branch_pay_badge_plus_apk_kyc_trap.mp3",
            "branch_faq_rules_plus_apk_kyc_trap.mp3",
            "branch_family_discuss_plus_apk_kyc_trap.mp3",
        ),
        n2_ids=("install_video_kyc_apk", "refuse_apk_outside_play_store", "meet_via_family_channel_only"),
        bad_outcome_audio="outcome_matrimonial_apk_harvest.mp3",
    )


def graph_ep13_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep13",
        lang,
        labels,
        n1=(
            ("pay_fifty_join_vip_group", "n1a"),
            ("remember_no_guaranteed_returns", "n1b"),
            ("talk_parent_sebi_channels_only", "n1c"),
        ),
        mid_audio=(
            "branch_join_fee_plus_margin_trap.mp3",
            "branch_no_sure_returns_plus_margin_trap.mp3",
            "branch_parent_sebi_plus_margin_trap.mp3",
        ),
        n2_ids=("deposit_five_hundred_margin_upi", "refuse_pyramid_margin_pattern", "exit_group_report_upi"),
        bad_outcome_audio="outcome_tipster_margin_loss.mp3",
    )


def graph_ep14_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep14",
        lang,
        labels,
        n1=(
            ("pay_maintenance_portal_link", "n1a"),
            ("check_notice_or_known_secretary", "n1b"),
            ("ask_neighbor_on_saved_phone", "n1c"),
        ),
        mid_audio=(
            "branch_portal_pay_plus_late_window_trap.mp3",
            "branch_notice_board_plus_late_window_trap.mp3",
            "branch_neighbor_known_plus_late_window_trap.mp3",
        ),
        n2_ids=("pay_twenty_late_ten_minutes", "refuse_minute_level_threats", "confirm_with_known_society_admins"),
        bad_outcome_audio="outcome_society_phishing_loss.mp3",
    )


def graph_ep15_for_locale(lang: str, labels: dict[str, str]) -> dict:
    return seven_segment_simulator_graph(
        "ep15",
        lang,
        labels,
        n1=(
            ("transfer_hospital_deposit_upi_now", "n1a"),
            ("call_relative_on_saved_number", "n1b"),
            ("call_hospital_main_from_maps_or_bill", "n1c"),
        ),
        mid_audio=(
            "branch_transfer_now_plus_secrecy_trap.mp3",
            "branch_saved_relative_plus_secrecy_trap.mp3",
            "branch_hospital_main_plus_secrecy_trap.mp3",
        ),
        n2_ids=("obey_secrecy_do_not_worry_family", "refuse_secrecy_urgency_signature", "conference_family_known_number"),
        bad_outcome_audio="outcome_hospital_wire_scam.mp3",
    )


EP03_TITLE = "[DSG Pilot] Ep03 — The ₹49 Courier"
EP04_TITLE = "[DSG Pilot] Ep04 — The School Panic Call"
EP05_TITLE = "[DSG Pilot] Ep05 — The Job Registration Fee"

EP03_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Pay the ₹49 customs digitization fee on the link now",
        "n1b": "Check parcel tracking on the courier official website with the AWB",
        "n1c": "Ask my family if anyone ordered a delivery",
        "n2a": "Install the tracking APK from this SMS",
        "n2b": "Never install APK files from SMS links",
        "n2c": "Call the courier using only the number from their official website",
    },
}

EP04_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Send UPI money to the number they gave — it is an emergency",
        "n1b": "Hang up and call our school landline from the diary",
        "n1c": "Call my spouse first and check the story together",
        "n2a": "Pay the ₹100 secrecy fee so teachers are not upset",
        "n2b": "Stop — secrecy plus urgency is how scammers pressure people",
        "n2c": "Conference our child teacher on a number we already trust",
    },
}

EP05_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Pay ₹499 registration now for the sure walk-in seat",
        "n1b": "Open only the company official careers website to verify",
        "n1c": "Ask a family member who works in IT before paying anything",
        "n2a": "Pay ₹99 fast-track fee to lock the interview slot",
        "n2b": "Stop — real employers do not charge job registration fees",
        "n2c": "Report this WhatsApp number for fraud",
    },
}

EP03_MISSION_EN = (
    "Compare a real courier SMS sender ID with a random 10-digit mobile pretending to be BlueDart."
)
EP04_MISSION_EN = (
    "Pick one family codeword for real emergencies so you can verify panic calls without shame."
)
EP05_MISSION_EN = (
    "Name three fake-job red flags: money before interview, personal Gmail only, extreme urgency."
)

EP03_PROMPTS_JSON = (
    '["Would BlueDart ask for customs fees over a random SMS link?",'
    '"Why would tracking need a new APK from a text message?"]'
)
EP04_PROMPTS_JSON = (
    '["Would a real school forbid you from telling other teachers?",'
    '"What is the one school phone number you would call back first?"]'
)
EP05_PROMPTS_JSON = (
    '["Have you ever paid to apply for a job at a listed company?",'
    '"Where is the official careers page URL saved or bookmarked?"]'
)

EP06_TITLE = "[DSG Pilot] Ep06 — The Lottery You Never Entered"
EP07_TITLE = "[DSG Pilot] Ep07 — The Subsidy Portal"
EP08_TITLE = "[DSG Pilot] Ep08 — Insurance Lapse Tonight"

EP06_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Pay ₹1 verification fee now — it is only one rupee",
        "n1b": "Delete the message — I never entered any lottery",
        "n1c": "Search with family for this kind of prize scam",
        "n2a": "Add the Telegram admin and send the prize OTP they ask for",
        "n2b": "Stop — OTP is the key to my bank accounts, never share it",
        "n2c": "Block the number and mark the chat as spam",
    },
}

EP07_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Register on this subsidy portal link before midnight",
        "n1b": "Check MyGov or trusted official news only",
        "n1c": "Wait and ask our gas agency or local office next visit",
        "n2a": "Upload Aadhaar front and back photos on this form",
        "n2b": "Never upload Aadhaar to random websites from SMS",
        "n2c": "Use only official UIDAI or government channels if needed",
    },
}

EP08_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Pay ₹500 on this link now to stop the policy lapse",
        "n1b": "Open my real policy papers and trusted agent number",
        "n1c": "Plan to visit the insurance branch on the next working day",
        "n2a": "Share my screen so they can fill the revival form for me",
        "n2b": "Stop — real insurers do not use remote desktop for forms",
        "n2c": "Hang up and dial the helpline printed on my policy",
    },
}

EP06_MISSION_EN = (
    "At dinner, ask: what is one thing you would never do for so-called free prize money?"
)
EP07_MISSION_EN = (
    "Point out one fake .com site versus a real .gov.in or .nic.in domain together."
)
EP08_MISSION_EN = (
    "Write your real policy number and toll-free helpline on paper and keep one copy in a drawer."
)

EP06_PROMPTS_JSON = (
    '["Did you ever register for KBC or Google lottery with this phone?",'
    '"Why would a prize need your bank OTP on Telegram?"]'
)
EP07_PROMPTS_JSON = (
    '["Would a real subsidy portal come only as a midnight SMS link?",'
    '"Who should see a full Aadhaar image besides government eKYC you choose?"]'
)
EP08_PROMPTS_JSON = (
    '["Would LIC or your insurer ask for Anydesk to type a form?",'
    '"Where is the one policy helpline number you would call back?"]'
)

EP09_TITLE = "[DSG Pilot] Ep09 — PAN–Aadhaar Last Day"
EP10_TITLE = "[DSG Pilot] Ep10 — The Accidental UPI Transfer"
EP11_TITLE = "[DSG Pilot] Ep11 — The Refund Executive"
EP12_TITLE = "[DSG Pilot] Ep12 — The Rishta Verification"
EP13_TITLE = "[DSG Pilot] Ep13 — The Telegram Tipster"
EP14_TITLE = "[DSG Pilot] Ep14 — Society Maintenance Portal"
EP15_TITLE = "[DSG Pilot] Ep15 — The Hospital Bed Deposit"

EP09_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Panic and open the PAN link in this SMS right now",
        "n1b": "Open incometax.gov.in by typing the URL — not from the message",
        "n1c": "Ask our CA or a literate family member before I act",
        "n2a": "Pay ₹100 processing on this UPI ID they sent",
        "n2b": "Stop — real government sites do not collect random ₹100 from SMS",
        "n2c": "Verify any PAN–Aadhaar deadline only on the official Income Tax website",
    },
}

EP10_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Return ₹5000 to this UPI ID now — they said it was a mistake",
        "n1b": "Check my UPI app to see if money actually credited first",
        "n1c": "Ignore it — this accidental-transfer script is very common",
        "n2a": "Send ₹1 as the test payment they asked for",
        "n2b": "Stop — even ₹1 tests train me to trust a stranger's story",
        "n2c": "If money really arrived, use the bank dispute path — not their UPI",
    },
}

EP11_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Download TeamViewer so they can verify the refund",
        "n1b": "Open orders only inside the real shopping app — no refund pending",
        "n1c": "Ask a teen at home to verify the order ID in the app",
        "n2a": "Type the refund amount in the remote-control box they show",
        "n2b": "Never type amounts or PINs for someone claiming a refund",
        "n2c": "Use only in-app help or official chat inside the retailer app",
    },
}

EP12_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Pay ₹999 now for the police verification trust badge",
        "n1b": "Read the matrimonial site FAQ on verified profiles and badges",
        "n1c": "Discuss the match offline with family before paying anything",
        "n2a": "Install this APK for quick video KYC",
        "n2b": "APK installs outside the Play Store are high identity risk",
        "n2c": "Meet new people only through a family-arranged channel we trust",
    },
}

EP13_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Pay ₹50 admin fee to join the VIP tips group",
        "n1b": "Remember — no one can sell guaranteed stock returns",
        "n1c": "Talk to a parent; invest only via SEBI-registered channels",
        "n2a": "Deposit ₹500 margin on this UPI for today's sure trade",
        "n2b": "Stop — pay-to-join then margin is a pyramid pressure pattern",
        "n2c": "Leave the group and report the UPI number for fraud",
    },
}

EP14_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Pay maintenance now on this new portal link",
        "n1b": "Check the society notice board or the known secretary number",
        "n1c": "Ask a neighbor on a phone number we already trust",
        "n2a": "Pay ₹20 late fee within ten minutes or lose gate access",
        "n2b": "Real societies rarely threaten you minute-by-minute in chat",
        "n2c": "Confirm in the society WhatsApp with admins we recognize",
    },
}

EP15_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Transfer the hospital deposit immediately on this UPI",
        "n1b": "Call my relative on the number already saved in my phone",
        "n1c": "Call the hospital main line from Google Maps or an old bill",
        "n2a": "Do not tell family — they will only worry more",
        "n2b": "Stop — secrecy plus urgency is a classic scam signature",
        "n2c": "Start a conference call with family on a number we trust",
    },
}

EP09_MISSION_EN = (
    "Practice typing one government URL together without tapping any link from a message."
)
EP10_MISSION_EN = 'Read aloud together: "UPI received" versus "UPI promised."'
EP11_MISSION_EN = "Show one real in-app help or order path on a parent phone."
EP12_MISSION_EN = (
    "List two green flags (known introducer, slow pace) versus red flags (money before meeting)."
)
EP13_MISSION_EN = 'Say together: "If they need your money to teach you money, walk away."'
EP14_MISSION_EN = (
    "Save two trusted society contacts with real numbers, not just a Secretary label."
)
EP15_MISSION_EN = (
    "Agree at home: any real hospital bill can wait one verified phone call."
)

EP09_PROMPTS_JSON = (
    '["Would Income Tax send a PAN link only as an SMS graphic?",'
    '"Why would a real portal ask for ₹100 on random UPI?"]'
)
EP10_PROMPTS_JSON = (
    '["Did ₹5000 actually appear in your UPI received history?",'
    '"Why is even a ₹1 test risky with a stranger?"]'
)
EP11_PROMPTS_JSON = (
    '["Is there any refund pending inside the real shopping app?",'
    '"Would Amazon or Flipkart need TeamViewer for a refund?"]'
)
EP12_PROMPTS_JSON = (
    '["Does your matrimonial site sell police badges for cash?",'
    '"Why is video KYC safest only inside the official app?"]'
)
EP13_PROMPTS_JSON = (
    '["Who profits if they need your ₹50 to teach sure returns?",'
    '"What pattern is small fee to join then a margin deposit?"]'
)
EP14_PROMPTS_JSON = (
    '["Can you reach your real secretary from a saved contact?",'
    '"Do honest housing bills threaten you in ten minutes?"]'
)
EP15_PROMPTS_JSON = (
    '["Can you reach your relative on a number you already saved?",'
    '"Would a real hospital forbid you from telling family?"]'
)


EP02_TITLE = "[DSG Pilot] Ep02 — The KYC Countdown"

EP02_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Tap the KYC link in the SMS right now",
        "n1b": "Open only my bank's official app to check KYC status",
        "n1c": "Call the helpline number printed on my debit card",
        "n2a": "Pay ₹2 gateway fee on this link — unfreeze the account",
        "n2b": "Stop — never pay bank fees through an SMS link",
        "n2c": "Hang up and use the official toll-free from my passbook or card",
    },
}

EP02_MISSION_EN = (
    "Find your bank's one official customer-care number (card or statement) and save it as a contact."
)

LOCALE_LABELS: dict[str, dict[str, str]] = {
    "en": {
        "n1a": "Call the number in the SMS immediately",
        "n1b": "Open the official electricity app or website to check",
        "n1c": "Ask Priya to read the SMS carefully together",
        "n2a": "Pay ₹10 on the link — small amount, stay safe",
        "n2b": "Stop — never enter UPI PIN on a link from SMS",
        "n2c": "Report to the real helpline and block the number",
    },
    "hi": {
        "n1a": "SMS वाले नंबर पर तुरंत कॉल करूँ",
        "n1b": "आधिकारिक बिजली ऐप या वेबसाइट खोलकर चेक करूँ",
        "n1c": "प्रिया से SMS साथ मिलकर ध्यान से पढ़वाऊँ",
        "n2a": "दस रुपये छोटी रकम है, भर देता हूँ",
        "n2b": "रुकूँगा — SMS के लिंक पर UPI PIN कभी नहीं",
        "n2c": "असली हेल्पलाइन पर रिपोर्ट करूँगा और नंबर ब्लॉक करूँगा",
    },
    "ta": {
        "n1a": "SMS-ல இருக்க நம்பருக்கு உடனே கால்",
        "n1b": "TANGEDCO அதிகாரப்பூர்வ ஆப் அல்லது வலைத்தளம் திறந்து சரிபார்ப்பு",
        "n1c": "பிரியாவை கூப்பிட்டு SMS ஒண்ணா படித்து பார்க்க வைக்கிறேன்",
        "n2a": "பத்து ரூபாய் சின்னதுதான், கட்டிடுறேன்",
        "n2b": "நிறுத்து — SMS லிங்குல UPI PIN போட மாட்டேன்",
        "n2c": "உண்மையான ஹெல்ப்லைனுக்கு புகார்; எண்ணை ப்ளாக்",
    },
    "te": {
        "n1a": "SMS లోని నంబర్‌కు వెంటనే కాల్",
        "n1b": "డిస్కాం అధికారిక యాప్ లేదా వెబ్‌సైట్ తెరిచి చెక్",
        "n1c": "ప్రియాతో కలిసి SMS జాగ్రత్తగా చదవించు",
        "n2a": "పది రూపాయలు చిన్నదే, కట్టేస్తా",
        "n2b": "ఆగు — SMS లింక్‌లో UPI PIN ఇవ్వను",
        "n2c": "నిజమైన హెల్ప్‌లైన్‌కు రిపోర్ట్, నంబర్ బ్లాక్",
    },
    "kn": {
        "n1a": "SMS ನಲ್ಲಿನ ನಂಬರ್‌ಗೆ ತಕ್ಷಣ ಕರೆ",
        "n1b": "BESCOM / ಅಧಿಕೃತ ವಿದ್ಯುತ್ ಯಾಪ್ ಅಥವಾ ವೆಬ್‌ಸೈಟ್ ತೆರೆದು ಪರಿಶೀಲಿಸು",
        "n1c": "ಪ್ರಿಯಾಳ ಜೊತೆ SMS ಒಟ್ಟಿಗೆ ಓದಿ ನೋಡು",
        "n2a": "ಹತ್ತು ರೂಪಾಯಿ ಚಿಕ್ಕದು, ಪಾವತಿಸುತ್ತೇನೆ",
        "n2b": "ನಿಲ್ಲು — SMS ಲಿಂಕ್‌ನಲ್ಲಿ UPI PIN ಇಡುವುದಿಲ್ಲ",
        "n2c": "ನಿಜವಾದ ಹೆಲ್ಪ್‌ಲೈನ್‌ಗೆ ದೂರು, ನಂಬರ್ ಬ್ಲಾಕ್",
    },
    "ml": {
        "n1a": "SMS-ലെ നമ്പറിലേക്ക് ഉടൻ വിളിക്കും",
        "n1b": "KSEB ഔദ്യോഗിക ആപ്പ് അല്ലെങ്കിൽ വെബ്സൈറ്റ് തുറന്ന് പരിശോധിക്കും",
        "n1c": "പ്രിയയോടൊപ്പം SMS ഒന്നിച്ച് ശ്രദ്ധിച്ച് വായിക്കും",
        "n2a": "പത്തു രൂപ ചെറിയതാണ്, അടയ്ക്കാം",
        "n2b": "നിർത്തുക — SMS ലിങ്കിൽ UPI PIN ഇടില്ല",
        "n2c": "യഥാർത്ഥ ഹെൽപ്‌ലൈനിൽ റിപ്പോർട്ട്, നമ്പർ ബ്ലോക്ക്",
    },
}

EP01_CONTENT = {
    "en": """# The Midnight Blackout (Episode 1)

Co-listening simulator: senior + family; electricity bill urgency scam; two decision nodes (call vs verify vs collaborate; then ₹10 UPI link).

**Doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md — full multilingual VO in §5.

**Patterns:** urgency, unknown sender, small-amount lure, screen-share / fake payment page.
""",
    "hi": """# अर्धरात्रि बिजली गुल (एपिसोड 1)

सह-सुनने वाला सिम्युलेटर: बिजली बिल की झूठी आपातकालीन SMS; दो निर्णय बिंदु।

**पूरा स्क्रिप्ट:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.2

**पैटर्न:** जल्दबाज़ी, अज्ञात भेजने वाला, छोटी रकम का जाल, फर्जी लिंक।
""",
    "ta": """# அரையிரவு மின் துண்டிப்பு (அத்தியாயம் 1)

குடும்பத்துடன் கேட்கும் சிமுலேட்டர்: மின் பில் மோசடி SMS; இரண்டு தேர்வு முனைகள்.

**முழு வசனம்:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.3

**பேட்டர்ன்கள்:** அவசரம், அந்நிய அனுப்புநர், சிறு தொகை, போலி இணைப்பு.
""",
    "te": """# మధ్యరాత్రి కరెంట్ కట్ (ఎపిసోడ్ 1)

కుటుంబంతో కలిసి వినే సిమ్యులేటర్; విద్యుత్ బిల్ మోసం SMS; రెండు నిర్ణయాలు.

**పూర్తి స్క్రిప్ట్:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.4
""",
    "kn": """# ಮಧ್ಯರಾತ್ರಿ ವಿದ್ಯುತ್ ಕಡಿತ (ಎಪಿಸೋಡ್ 1)

ಕುಟುಂಬದೊಂದಿಗೆ ಕೇಳುವ ಸಿಮ್ಯುಲೇಟರ್; ವಿದ್ಯುತ್ ಬಿಲ್ ವಂಚನೆ; ಎರಡು ಆಯ್ಕೆ ನೋಡ್‌ಗಳು.

**ಪೂರ್ಣ ಲಿಪಿ:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.5
""",
    "ml": """# അർദ്ധരാത്രി വൈദ്യുതി മുറിക്കൽ (എപ്പിസോഡ് 1)

കുടുംബത്തോടൊപ്പം കേൾക്കുന്ന സിമുലേറ്റർ; KSEB തട്ടിപ്പ് SMS; രണ്ട് തീരുമാന നോഡുകൾ.

**പൂർണ്ണ സ്ക്രിപ്റ്റ്:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §5.6
""",
}

EP01_MISSION = {
    "en": "The Screen-Share Challenge: With a parent or grandparent, open one unknown “click link” SMS; name urgency, sender, and grammar; delete it together.",
    "hi": "स्क्रीन-शेयर चैलेंज: माता-पिता या दादा-दादी के साथ एक अनजान ‘लिंक’ SMS खोलें; जल्दबाज़ी, भेजने वाला, भाषा बताएँ; साथ में डिलीट करें।",
    "ta": "ஸ்கிரீன்-ஷேர் சவால்: பாட்டி/தாத்தாவுடன் ‘லிங்க் கிளிக்’ மெசேஜ் ஒன்றைத் திறந்து அவசரம், அனுப்புநர், மொழி சொல்லி ஒண்ணா டிலீட் பண்ணுங்க.",
    "te": "స్క్రీన్-షేర్ సవాల్: తాతయ్య/నానమ్మతో ‘లింక్ క్లిక్’ మెసేజ్ తెరవండి; తొందర, పంపినవారు, భాష చెప్పి కలిసి డిలీట్ చేయండి.",
    "kn": "ಸ್ಕ್ರೀನ್-ಶೇರ್ ಸವಾಲ್: ಅಜ್ಜಿ/ಅಜ್ಜನೊಂದಿಗೆ ‘ಲಿಂಕ್ ಕ್ಲಿಕ್’ ಮೆಸೇಜ್ ತೆರೆಯಿರಿ; ತ್ವರೆ, ಕಳುಹಿಸಿದವರು, ಭಾಷೆ ಹೇಳಿ ಒಟ್ಟಿಗೆ ಡಿಲೀಟ್ ಮಾಡಿ.",
    "ml": "സ്‌ക്രീൻ-ഷെയർ ചലഞ്ച്: മുത്തശ്ശി/മുത്തച്ഛനൊപ്പം ‘ലിങ്ക് ക്ലിക്ക്’ മെസേജ് തുറന്ന് അത്യാവശ്യം, അയച്ചവർ, ഭാഷ പറഞ്ഞ് ഒന്നിച്ച് ഡിലീറ്റ് ചെയ്യുക.",
}

PILOT_OUTLINES: list[tuple[int, str, str]] = [
    (
        2,
        "The KYC Countdown",
        """# The Digital Survival Guide — Episode 2 (Pilot outline)

**Hook:** SMS: "HDFC/SBI — KYC incomplete, account blocked in 2 hours. Link to update."

**Node 1:** A) Tap link now · B) Open bank app only · C) Call number printed on debit card back.

**Fallout:** A → fake form + OTP harvest; B/C → no pending KYC in real app.

**Node 2:** "Pay ₹2 gateway charge to unfreeze." A) Pay · B) Never pay via SMS link · C) Call official 1800 from card / statement.

**Mission:** Find the one official customer-care number for your bank (statement/card) and save it as a contact.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 2.
""",
    ),
    (
        3,
        "The ₹49 Courier",
        """# Episode 3 — The ₹49 Courier

**Hook:** "BlueDart — your parcel on hold; ₹49 customs digitization fee."

**Node 1:** A) Pay ₹49 · B) Check tracking on brand site with AWB · C) Ask family if anyone ordered.

**Converge:** Second SMS: "Download tracking APK."

**Node 2:** A) Install APK · B) Never install APK from SMS · C) Call courier from website number only.

**Mission:** Compare sender ID on a real courier SMS vs a random 10-digit sender.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 3.
""",
    ),
    (
        4,
        "The School Panic Call",
        """# Episode 4 — The School Panic Call

**Hook:** Voice call: "I am from your child's school; accident; send money for ambulance UPI."

**Node 1:** A) Send immediately · B) Hang up, call school landline you already have · C) Call spouse first, conference.

**Node 2:** "Don't tell teachers — secrecy fee ₹100." A) Pay · B) Secrecy = red flag · C) Conference with class teacher on known number.

**Mission:** Agree a family codeword for real emergencies (no shame in verifying).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 4.
""",
    ),
    (
        5,
        "The Job Registration Fee",
        """# Episode 5 — The Job Registration Fee

**Hook:** WhatsApp: "TCS/Infosys walk-in confirmed; pay ₹499 registration."

**Node 1:** A) Pay for "sure seat" · B) Check careers.* official site only · C) Ask elder who works in IT.

**Node 2:** "Upgrade to fast-track ₹99." A) Pay · B) Real employers don't charge registration · C) Report number.

**Mission:** List three signs a job offer is fake (money upfront, personal Gmail, urgency).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 5.
""",
    ),
    (
        6,
        "The Lottery You Never Entered",
        """# Episode 6 — The Lottery You Never Entered

**Hook:** "Congratulations — KBC / Google winner ₹5 lakh; pay ₹1 verification."

**Node 1:** A) Pay ₹1 · B) Delete — you never entered · C) Search scam pattern with family.

**Node 2:** "Add admin on Telegram for prize OTP." A) Add · B) OTP = account key · C) Block and mark spam.

**Mission:** One dinner question: "What would you never do for 'free' money?"

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 6.
""",
    ),
    (
        7,
        "The Subsidy Portal",
        """# Episode 7 — The Subsidy Portal

**Hook:** SMS: "PM Ujjwala / subsidy — register on this portal before midnight."

**Node 1:** A) Register on link · B) Check mygov / official channel news · C) Ask panchayat / gas agency next visit.

**Node 2:** "Upload Aadhaar front-back on this form." A) Upload · B) Never upload Aadhaar to random sites · C) Use only official UIDAI flows if ever needed.

**Mission:** Point out one fake .com domain vs a known .gov.in / .nic.in pattern (age-appropriate).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 7.
""",
    ),
    (
        8,
        "Insurance Lapse Tonight",
        """# Episode 8 — Insurance Lapse Tonight

**Hook:** Call: "Your LIC policy lapsed; pay ₹500 now or lose ₹2 lakh benefit."

**Node 1:** A) Pay on link · B) Open policy document / agent number you chose yourself · C) Visit branch next working day.

**Node 2:** "Share screen so I can fill the form." A) Share · B) No insurer asks Anydesk · C) Hang up, call policy helpline.

**Mission:** Locate your real policy number and helpline on paper; put in one drawer.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 8.
""",
    ),
    (
        9,
        "PAN–Aadhaar Last Day",
        """# Episode 9 — PAN–Aadhaar Last Day

**Hook:** Urgent SMS with "income tax" logo image — link to link PAN "or fine ₹10,000."

**Node 1:** A) Panic-link · B) Open incometax.gov.in from typed URL · C) Ask CA / literate family member.

**Node 2:** "Pay ₹100 processing on UPI." A) Pay · B) Government portals don't collect random ₹100 on SMS · C) Verify deadline on official site only.

**Mission:** Practice typing one government URL together (no tap from message).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 9.
""",
    ),
    (
        10,
        "The Accidental UPI Transfer",
        """# Episode 10 — The Accidental UPI Transfer

**Hook:** "I sent ₹5000 to your number by mistake; please return to this UPI ID."

**Node 1:** A) Return immediately · B) Check if money actually credited in app · C) Ignore — common script.

**Node 2:** "Send ₹1 test first." A) Send · B) Test payments still train you to trust scammer · C) If real credit, use bank dispute channel, not stranger ID.

**Mission:** Read aloud: "UPI received vs promised."

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 10.
""",
    ),
    (
        11,
        "The Refund Executive",
        """# Episode 11 — The Refund Executive

**Hook:** "Amazon/Flipkart refund failed; download TeamViewer for verification."

**Node 1:** A) Download · B) Open app orders only — no refund pending · C) Ask teen to verify order ID.

**Node 2:** "Type refund amount in this remote box." A) Type · B) Never type amounts/PIN for a 'refund' · C) Use in-app help chat only.

**Mission:** Show one real in-app "help" path on a parent phone.

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 11.
""",
    ),
    (
        12,
        "The Rishta Verification",
        """# Episode 12 — The Rishta Verification

**Hook:** Matrimonial site DM: "Pay ₹999 for police verification badge."

**Node 1:** A) Pay for trust · B) Check site settings / verified rules on official FAQ · C) Discuss with family offline first.

**Node 2:** "Video KYC on this APK." A) Install · B) APK outside Play Store = high risk · C) Meet through arranged family channel only.

**Mission:** List two green flags (known introducer, slow pace) vs red flags (money before meeting).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 12.
""",
    ),
    (
        13,
        "The Telegram Tipster",
        """# Episode 13 — The Telegram Tipster

**Hook:** "Join VIP group — bank Nifty sure shot; pay ₹50 admin fee."

**Node 1:** A) Pay to join · B) Remember: no one sells sure returns · C) Talk to parent about investing only via SEBI-registered advisors.

**Node 2:** "Deposit ₹500 in this UPI for 'margin'." A) Deposit · B) Stop — pyramid pattern · C) Exit group, report.

**Mission:** One line: "If they need your money to teach you money, walk away."

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 13.
""",
    ),
    (
        14,
        "Society Maintenance Portal",
        """# Episode 14 — Society Maintenance Portal

**Hook:** WhatsApp from "Secretary" unknown number: "Pay maintenance on new portal link."

**Node 1:** A) Pay on link · B) Check society notice board / known secretary number · C) Ask neighbor on known phone.

**Node 2:** "Late fee ₹20 if not paid in 10 minutes." A) Pay · B) Real societies rarely minute-level threaten · C) Confirm in elevator group with known admins.

**Mission:** Save two trusted society contacts (not just "Secretary" label).

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 14.
""",
    ),
    (
        15,
        "The Hospital Bed Deposit",
        """# Episode 15 — The Hospital Bed Deposit (sensitive)

**Tone:** Calm VO; no graphic detail.

**Hook:** Call claiming hospital needs instant deposit for a named relative.

**Node 1:** A) Transfer now · B) Call relative on saved number · C) Call hospital main line from Google Maps / known bill.

**Node 2:** "Don't tell family — they'll worry." A) Obey secrecy · B) Secrecy + urgency = scam signature · C) Conference call with family.

**Mission:** Agree: "Any real hospital bill can wait one verified phone call."

**Authoring doc:** docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Episode 15.
""",
    ),
]


def sql_string_literal(s: str) -> str:
    return "'" + s.replace("'", "''") + "'"


def main() -> None:
    out = Path(__file__).resolve().parent.parent / "src/main/resources/db/migration/V86__digital_survival_guide_script_seed.sql"
    lines: list[str] = [
        "-- Digital Survival Guide: Episode 1 (6 locales, interactive_graph) + pilot outlines Ep2–Ep15.",
        "-- DRAFT rows; story_owner = seed:digital-survival-guide-v1. Publish from admin after audio/CDN.",
        "-- Source doc: docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md",
        "",
    ]

    def emit_insert(
        title: str,
        content: str,
        language: str,
        word_count: int,
        reading_min: float,
        interactive_graph: str | None,
        post_mission: str | None,
        parent_prompts: str | None,
        parent_note: str | None,
    ) -> None:
        ig = "NULL" if interactive_graph is None else sql_string_literal(interactive_graph)
        pm = "NULL" if post_mission is None else sql_string_literal(post_mission)
        pp = "NULL" if parent_prompts is None else f"'{parent_prompts}'::jsonb"
        pn = "NULL" if parent_note is None else sql_string_literal(parent_note)
        lines.append("INSERT INTO library_stories (")
        lines.append(
            "  title, content, theme, category, language, age, child_name, word_count, reading_time_minutes,"
        )
        lines.append(
            "  moral, status, story_owner, interactive_graph, post_story_mission, parent_discussion_prompts, parent_content_note"
        )
        lines.append(") VALUES (")
        lines.append(f"  {sql_string_literal(title)},")
        lines.append(f"  {sql_string_literal(content)},")
        lines.append(f"  {sql_string_literal(THEME)},")
        lines.append(f"  {sql_string_literal(CATEGORY)},")
        lines.append(f"  {sql_string_literal(language)},")
        lines.append(f"  {AGE},")
        lines.append(f"  {sql_string_literal(CHILD_NAME)},")
        lines.append(f"  {word_count},")
        lines.append(f"  {reading_min},")
        lines.append(
            f"  {sql_string_literal('Pattern recognition: urgency, unknown channel, small rupee lure, trusted app first.')},"
        )
        lines.append(f"  {sql_string_literal(STATUS)},")
        lines.append(f"  {sql_string_literal(STORY_OWNER)},")
        lines.append(f"  {ig},")
        lines.append(f"  {pm},")
        lines.append(f"  {pp},")
        lines.append(f"  {pn}")
        lines.append(");")
        lines.append("")

    for lang in ("en", "hi", "ta", "te", "kn", "ml"):
        g = json.dumps(graph_for_locale(lang, LOCALE_LABELS[lang]), ensure_ascii=False, separators=(",", ":"))
        content = EP01_CONTENT[lang]
        wc = max(50, len(content.split()))
        emit_insert(
            title=f"[DSG] Ep01 — The Midnight Blackout ({lang})",
            content=content,
            language=lang,
            word_count=wc,
            reading_min=round(wc / 200.0, 2),
            interactive_graph=g,
            post_mission=EP01_MISSION[lang],
            parent_prompts=r'["Who sent this message — a short code or a random mobile?","Would a real utility ask for your UPI PIN on an SMS link?"]',
            parent_note="EduStory simulator pilot; full scripts in repo docs §5.",
        )

    for num, ep_title, body in PILOT_OUTLINES:
        wc = max(40, len(body.split()))
        emit_insert(
            title=f"[DSG Pilot] Ep{num:02d} — {ep_title}",
            content=body,
            language="en",
            word_count=wc,
            reading_min=round(wc / 200.0, 2),
            interactive_graph=None,
            post_mission=None,
            parent_prompts=r'["What is one red flag in an urgent payment message?"]',
            parent_note=f"English pilot; interactive_graph from Flyway V87–V90 (Ep02–Ep15). See DIGITAL_SURVIVAL_GUIDE doc §3 Episode {num}.",
        )

    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {out}")

    # V87: attach graph to Ep02 English row created in V86
    v87 = Path(__file__).resolve().parent.parent / "src/main/resources/db/migration/V87__digital_survival_guide_ep02_kyc_graph.sql"
    ep02_graph_compact = json.dumps(
        graph_ep02_for_locale("en", EP02_LABELS["en"]),
        ensure_ascii=False,
        separators=(",", ":"),
    )
    ep02_prompts_json = (
        '["Where do you save your bank helpline number?",'
        '"Would RBI or your bank ask for KYC through a random SMS link?"]'
    )
    # Flyway treats "${" as a placeholder at $tag$ boundaries. Insert an extra "$" so the file
    # contains $tag$$... — Flyway collapses "$$" to "$" and the JSON body still starts with "{".
    ep02_interactive_sql = "$ep02$" + "$" + ep02_graph_compact + "$ep02$"
    v87_body = f"""-- Digital Survival Guide Ep02 (KYC Countdown): attach interactive_graph to English pilot row from V86.
-- Requires V86 applied first. Audio URLs are placeholders until CDN upload.

UPDATE library_stories
SET
  interactive_graph = {ep02_interactive_sql},
  post_story_mission = {sql_string_literal(EP02_MISSION_EN)},
  parent_discussion_prompts = $dsgep2prm${ep02_prompts_json}$dsgep2prm$::jsonb,
  parent_content_note = {sql_string_literal("EduStory simulator; full outline docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3 Ep2. Regenerate: backend/scripts/generate_digital_survival_guide_seed_sql.py")},
  updated_at = NOW()
WHERE story_owner = {sql_string_literal(STORY_OWNER)}
  AND language = 'en'
  AND title = {sql_string_literal(EP02_TITLE)};
"""
    v87.write_text(v87_body, encoding="utf-8")
    print(f"Wrote {v87}")

    v88 = Path(__file__).resolve().parent.parent / "src/main/resources/db/migration/V88__digital_survival_guide_ep03_ep05_graphs.sql"
    regen_note = (
        "EduStory simulator; outline docs/admin/DIGITAL_SURVIVAL_GUIDE_EP01_GRAPH_AND_PILOT_SCRIPTS.md §3. "
        "Regenerate: backend/scripts/generate_digital_survival_guide_seed_sql.py"
    )

    def pilot_update_sql(
        graph_tag: str,
        prm_tag: str,
        title: str,
        graph_dict: dict,
        mission: str,
        prompts_json: str,
        ep_suffix: str,
    ) -> str:
        gc = json.dumps(graph_dict, ensure_ascii=False, separators=(",", ":"))
        # See V87: extra "$" before JSON so Flyway does not parse $tag${"..." as a placeholder.
        ig_sql = f"${graph_tag}$" + "$" + gc + f"${graph_tag}$"
        return f"""UPDATE library_stories
SET
  interactive_graph = {ig_sql},
  post_story_mission = {sql_string_literal(mission)},
  parent_discussion_prompts = ${prm_tag}${prompts_json}${prm_tag}$::jsonb,
  parent_content_note = {sql_string_literal(regen_note + " " + ep_suffix)},
  updated_at = NOW()
WHERE story_owner = {sql_string_literal(STORY_OWNER)}
  AND language = 'en'
  AND title = {sql_string_literal(title)};"""

    v88_body = "\n\n".join(
        [
            "-- Digital Survival Guide Ep03–Ep05: attach interactive_graph to English pilot rows from V86.",
            "-- Requires V86 (and recommended after V87). Segment ids: ep03_* / ep04_* / ep05_* (shared mid_1/2/3 pattern).",
            pilot_update_sql(
                "ep03g",
                "ep03p",
                EP03_TITLE,
                graph_ep03_for_locale("en", EP03_LABELS["en"]),
                EP03_MISSION_EN,
                EP03_PROMPTS_JSON,
                "Ep3.",
            ),
            pilot_update_sql(
                "ep04g",
                "ep04p",
                EP04_TITLE,
                graph_ep04_for_locale("en", EP04_LABELS["en"]),
                EP04_MISSION_EN,
                EP04_PROMPTS_JSON,
                "Ep4.",
            ),
            pilot_update_sql(
                "ep05g",
                "ep05p",
                EP05_TITLE,
                graph_ep05_for_locale("en", EP05_LABELS["en"]),
                EP05_MISSION_EN,
                EP05_PROMPTS_JSON,
                "Ep5.",
            ),
        ]
    )
    v88.write_text(v88_body, encoding="utf-8")
    print(f"Wrote {v88}")

    v89 = Path(__file__).resolve().parent.parent / "src/main/resources/db/migration/V89__digital_survival_guide_ep06_ep08_graphs.sql"
    v89_body = "\n\n".join(
        [
            "-- Digital Survival Guide Ep06–Ep08: attach interactive_graph to English pilot rows from V86.",
            "-- Requires V86. Lottery / subsidy Aadhaar / insurance screen-share patterns.",
            pilot_update_sql(
                "ep06g",
                "ep06p",
                EP06_TITLE,
                graph_ep06_for_locale("en", EP06_LABELS["en"]),
                EP06_MISSION_EN,
                EP06_PROMPTS_JSON,
                "Ep6.",
            ),
            pilot_update_sql(
                "ep07g",
                "ep07p",
                EP07_TITLE,
                graph_ep07_for_locale("en", EP07_LABELS["en"]),
                EP07_MISSION_EN,
                EP07_PROMPTS_JSON,
                "Ep7.",
            ),
            pilot_update_sql(
                "ep08g",
                "ep08p",
                EP08_TITLE,
                graph_ep08_for_locale("en", EP08_LABELS["en"]),
                EP08_MISSION_EN,
                EP08_PROMPTS_JSON,
                "Ep8.",
            ),
        ]
    )
    v89.write_text(v89_body, encoding="utf-8")
    print(f"Wrote {v89}")

    v90 = Path(__file__).resolve().parent.parent / "src/main/resources/db/migration/V90__digital_survival_guide_ep09_ep15_graphs.sql"
    v90_body = "\n\n".join(
        [
            "-- Digital Survival Guide Ep09–Ep15: attach interactive_graph to English pilot rows from V86.",
            "-- Requires V86. PAN/UPI, refund remote desktop, matrimonial APK, trading tipster, society portal, hospital urgency.",
            pilot_update_sql(
                "ep09g",
                "ep09p",
                EP09_TITLE,
                graph_ep09_for_locale("en", EP09_LABELS["en"]),
                EP09_MISSION_EN,
                EP09_PROMPTS_JSON,
                "Ep9.",
            ),
            pilot_update_sql(
                "ep10g",
                "ep10p",
                EP10_TITLE,
                graph_ep10_for_locale("en", EP10_LABELS["en"]),
                EP10_MISSION_EN,
                EP10_PROMPTS_JSON,
                "Ep10.",
            ),
            pilot_update_sql(
                "ep11g",
                "ep11p",
                EP11_TITLE,
                graph_ep11_for_locale("en", EP11_LABELS["en"]),
                EP11_MISSION_EN,
                EP11_PROMPTS_JSON,
                "Ep11.",
            ),
            pilot_update_sql(
                "ep12g",
                "ep12p",
                EP12_TITLE,
                graph_ep12_for_locale("en", EP12_LABELS["en"]),
                EP12_MISSION_EN,
                EP12_PROMPTS_JSON,
                "Ep12.",
            ),
            pilot_update_sql(
                "ep13g",
                "ep13p",
                EP13_TITLE,
                graph_ep13_for_locale("en", EP13_LABELS["en"]),
                EP13_MISSION_EN,
                EP13_PROMPTS_JSON,
                "Ep13.",
            ),
            pilot_update_sql(
                "ep14g",
                "ep14p",
                EP14_TITLE,
                graph_ep14_for_locale("en", EP14_LABELS["en"]),
                EP14_MISSION_EN,
                EP14_PROMPTS_JSON,
                "Ep14.",
            ),
            pilot_update_sql(
                "ep15g",
                "ep15p",
                EP15_TITLE,
                graph_ep15_for_locale("en", EP15_LABELS["en"]),
                EP15_MISSION_EN,
                EP15_PROMPTS_JSON,
                "Ep15.",
            ),
        ]
    )
    v90.write_text(v90_body, encoding="utf-8")
    print(f"Wrote {v90}")

    # Test fixtures (English graphs) for backend validation tests.
    fixture_dir = Path(__file__).resolve().parent.parent / "src/test/resources/edu"
    fixture_dir.mkdir(parents=True, exist_ok=True)
    fixture_path = fixture_dir / "digital_survival_guide_ep01_en.graph.json"
    en_graph = json.dumps(graph_for_locale("en", LOCALE_LABELS["en"]), ensure_ascii=False, indent=2) + "\n"
    fixture_path.write_text(en_graph, encoding="utf-8")
    print(f"Wrote {fixture_path}")

    ep02_fixture = fixture_dir / "digital_survival_guide_ep02_en.graph.json"
    ep02_pretty = json.dumps(graph_ep02_for_locale("en", EP02_LABELS["en"]), ensure_ascii=False, indent=2) + "\n"
    ep02_fixture.write_text(ep02_pretty, encoding="utf-8")
    print(f"Wrote {ep02_fixture}")

    for ep_num, labels, builder in (
        (3, EP03_LABELS["en"], graph_ep03_for_locale),
        (4, EP04_LABELS["en"], graph_ep04_for_locale),
        (5, EP05_LABELS["en"], graph_ep05_for_locale),
        (6, EP06_LABELS["en"], graph_ep06_for_locale),
        (7, EP07_LABELS["en"], graph_ep07_for_locale),
        (8, EP08_LABELS["en"], graph_ep08_for_locale),
        (9, EP09_LABELS["en"], graph_ep09_for_locale),
        (10, EP10_LABELS["en"], graph_ep10_for_locale),
        (11, EP11_LABELS["en"], graph_ep11_for_locale),
        (12, EP12_LABELS["en"], graph_ep12_for_locale),
        (13, EP13_LABELS["en"], graph_ep13_for_locale),
        (14, EP14_LABELS["en"], graph_ep14_for_locale),
        (15, EP15_LABELS["en"], graph_ep15_for_locale),
    ):
        fp = fixture_dir / f"digital_survival_guide_ep{ep_num:02d}_en.graph.json"
        txt = json.dumps(builder("en", labels), ensure_ascii=False, indent=2) + "\n"
        fp.write_text(txt, encoding="utf-8")
        print(f"Wrote {fp}")


if __name__ == "__main__":
    main()
