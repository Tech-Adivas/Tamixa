#!/bin/bash
# Tail pipeline logs. Use when Approve or Run pipeline is triggered from admin.
#
# Log file: usually backend/logs/tamixa.log when running from repo root.
# Logs also go to CONSOLE — watch the terminal where backend is running.
# Override: ./backend/scripts/check-pipeline-trigger.sh /path/to/tamixa.log
#
# Usage:
#   Terminal 1: ./backend/scripts/check-pipeline-trigger.sh
#   Terminal 2: Trigger Approve (Story for review) or Run pipeline (Story library)

# Try common locations (relative to repo root)
for candidate in "backend/logs/tamixa.log" "logs/tamixa.log" "./backend/logs/tamixa.log" "./logs/tamixa.log"; do
  if [[ -f "$candidate" ]]; then
    LOG="$candidate"
    break
  fi
done
LOG="${1:-${LOG:-backend/logs/tamixa.log}}"

if [[ ! -f "$LOG" ]]; then
  echo "Log file not found at $LOG"
  echo "Logs go to CONSOLE — watch the terminal where you ran: ./gradlew :backend:bootRun"
  echo "Look for: PIPELINE >>> RECEIVED retry storyId=X"
  echo ""
  echo "To use a file, set LOG_PATH before starting backend, e.g.:"
  echo "  LOG_PATH=\$(pwd)/logs ./gradlew :backend:bootRun -Ptamixa.backendOnly=true"
  echo "Then: tail -f logs/tamixa.log | grep 'PIPELINE >>>'"
  exit 1
fi

echo "Watching $LOG for PIPELINE >>> lines..."
echo "Run pipeline is in Story library only (Play icon). Trigger it, then look for:"
echo "  - PIPELINE >>> RECEIVED retry storyId=X"
echo "  - PIPELINE >>> START masterStoryId=X"
echo "  - PIPELINE >>> masterStoryId=X lang=Y TRANSLATE_START/DONE, REWRITE_START/DONE, TTS_START"
echo "  - PIPELINE >>> masterStoryId=X lang=Y DONE Zms"
echo ""
echo "If no output: ensure backend logs to file: LOG_PATH=\$(pwd)/backend/logs ./gradlew :backend:bootRun -Ptamixa.backendOnly=true"
echo ""
tail -f "$LOG" 2>/dev/null | grep --line-buffered "PIPELINE >>>"
