#!/usr/bin/env python3
"""
Extract optional PR template metrics for Actions step summary and JSON artifact.
Reads PR body from stdin; writes summary to GITHUB_STEP_SUMMARY if set.
"""
from __future__ import annotations

import json
import os
import re
import sys


def parse_bool_line(body: str, label: str) -> bool | None:
    # Matches "- [x] ai-assisted: yes" or "ai-assisted: yes" line
    pat = re.compile(
        rf"(?im)^(?:-\s*\[[ xX]\]\s*)?{re.escape(label)}\s*:\s*(yes|no|true|false|n/a)\s*$"
    )
    m = pat.search(body)
    if not m:
        # Loose: label anywhere on line
        loose = re.compile(rf"(?im)^{re.escape(label)}\s*:\s*(yes|no|true|false|n/a)\b")
        m = loose.search(body)
    if not m:
        return None
    v = m.group(1).lower()
    if v == "n/a":
        return None
    return v in ("yes", "true")


def parse_reprompts(body: str) -> int | None:
    m = re.search(r"(?im)^(?:-\s*\[[ xX]\]\s*)?re-prompts:\s*(\d+)\s*$", body)
    if not m:
        m = re.search(r"(?im)re-prompts:\s*(\d+)", body)
    if not m:
        return None
    return int(m.group(1))


def main() -> int:
    body = sys.stdin.read() or ""
    pr_number = os.environ.get("PR_NUMBER", "")
    repo = os.environ.get("GITHUB_REPOSITORY", "")

    data = {
        "pull_request": int(pr_number) if pr_number.isdigit() else None,
        "repository": repo or None,
        "ai_assisted": parse_bool_line(body, "ai-assisted"),
        "eval_pass": parse_bool_line(body, "eval-pass"),
        "re_prompts": parse_reprompts(body),
    }

    out_path = os.environ.get("GOVERNANCE_JSON", "pr-governance.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
        f.write("\n")

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        lines = [
            "## PR governance (AI SDLC metrics)",
            "",
            "| Field | Value |",
            "|-------|-------|",
            f"| ai-assisted | {data['ai_assisted']} |",
            f"| eval-pass | {data['eval_pass']} |",
            f"| re-prompts | {data['re_prompts']} |",
            "",
            "_Parsed from PR description (optional checkboxes/lines). Use for dashboards or quarterly rollups._",
            "",
            f"Artifact: `{out_path}` (upload in workflow).",
        ]
        with open(summary_path, "a", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
