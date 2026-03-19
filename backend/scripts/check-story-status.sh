#!/bin/bash
# Check pipeline completion for a story: scan logs and show DB query.
#
# Usage:
#   ./backend/scripts/check-story-status.sh [storyId]
#   ./backend/scripts/check-story-status.sh 33
#
# Looks at backend/logs/tamixa.log for PIPELINE >>> lines for the story.
# Run after pipeline has had time to complete (or appear stuck).

STORY_ID="${1:-33}"
LOG="${2:-backend/logs/tamixa.log}"

echo "=== Pipeline status for story #$STORY_ID ==="
echo ""

if [[ -f "$LOG" ]]; then
  echo "From logs ($LOG):"
  echo "---"
  grep "masterStoryId=$STORY_ID\|storyId=$STORY_ID" "$LOG" 2>/dev/null | grep "PIPELINE >>>" | tail -50
  echo "---"
  echo ""
  echo "Summary by language (last log line per lang):"
  for lang in en hi kn te ml ta; do
    last=$(grep "masterStoryId=$STORY_ID" "$LOG" 2>/dev/null | grep "lang=$lang" | grep "PIPELINE >>>" | tail -1)
    if [[ -n "$last" ]]; then
      if echo "$last" | grep -qE "lang=$lang DONE [0-9]+ms"; then
        echo "  $lang: DONE"
      elif echo "$last" | grep -q "TIMEOUT\|FAILED"; then
        echo "  $lang: FAILED/TIMEOUT"
      elif echo "$last" | grep -q "REWRITE_DONE\|REWRITE_START\|TTS_PROCESSING\|TRANSLATE"; then
        echo "  $lang: in progress"
      else
        echo "  $lang: (check logs)"
      fi
    fi
  done
else
  echo "Log file not found: $LOG"
  echo "Set LOG_PATH before bootRun, or pass log path as 2nd arg."
fi

echo ""
echo "=== Database check (run if you have psql) ==="
echo ""
echo "SELECT id, language, status, last_error, retry_count"
echo "FROM story_translations"
echo "WHERE master_story_id = $STORY_ID;"
echo ""
echo "COMPLETED = done. TRANSLATING/REWRITING/TTS_PROCESSING = in progress or stuck."
echo ""
