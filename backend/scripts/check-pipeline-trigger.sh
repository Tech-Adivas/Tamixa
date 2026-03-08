#!/bin/bash
# Quick script to verify pipeline triggers reach the backend.
# Run this while tailing logs, then trigger Update & republish from admin.
#
# Usage:
#   Terminal 1: ./backend/scripts/check-pipeline-trigger.sh
#   Terminal 2: (or browser) trigger Update & republish
#   You should see: REQUEST, REPUBLISH, TASK STARTED within a few seconds

LOG="${1:-backend/logs/araro.log}"
echo "Watching $LOG for pipeline triggers..."
echo "Trigger Update & republish from admin, then look for:"
echo "  - POST /api/v1/admin/curated-stories/.../republish"
echo "  - Republish requested storyId="
echo "  - >>> PIPELINE TASK STARTED"
echo ""
tail -f "$LOG" 2>/dev/null | grep --line-buffered -E "POST.*republish|PUT.*curated-stories|Republish requested|PIPELINE TASK STARTED|PIPELINE TASK FAILED|invalidateAndReprocess"
