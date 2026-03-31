#!/usr/bin/env python3
"""Merge CI context into pr-governance.json for webhook / warehouse export."""
from __future__ import annotations

import json
import os
import sys
from pathlib import Path


def main() -> int:
    src = Path(os.environ.get("GOVERNANCE_JSON", "pr-governance.json"))
    if not src.is_file():
        print(f"enrich: missing {src}", file=sys.stderr)
        return 1
    data = json.loads(src.read_text(encoding="utf-8"))

    optional = {
        "merged_at": os.environ.get("MERGED_AT") or None,
        "merge_sha": os.environ.get("MERGE_SHA") or None,
        "base_ref": os.environ.get("BASE_REF") or None,
        "head_ref": os.environ.get("HEAD_REF") or None,
        "pr_title": os.environ.get("PR_TITLE") or None,
        "pr_url": os.environ.get("PR_URL") or None,
        "merged_by": os.environ.get("MERGED_BY") or None,
        "workflow_run_url": os.environ.get("WORKFLOW_RUN_URL") or None,
    }
    data["export"] = {k: v for k, v in optional.items() if v is not None}

    out = Path(os.environ.get("METRICS_EXPORT_JSON", "pr-metrics-export.json"))
    out.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
