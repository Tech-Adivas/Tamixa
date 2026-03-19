# TTS Sample Files (Tamixa voice)

Use **Tamixa’s voice** (one MP3 for all narration) by setting:

1. `NARRATION_TTS_PROVIDER=tamixa` (or `sample`)
2. `NARRATION_TAMIXA_TTS_DEFAULT_FILE=classpath:tts-samples/tamixa.mp3`

Put your `tamixa.mp3` in this directory (`backend/src/main/resources/tts-samples/`), then restart the backend.

---

Alternatively, place one MP3 per language. The same file is used for all stories in that language.

## Required files

| Language | Filename |
|----------|----------|
| Tamil    | ta.mp3   |
| Hindi    | hi.mp3   |
| English  | en.mp3   |
| Telugu   | te.mp3   |
| Kannada  | kn.mp3   |
| Malayalam| ml.mp3   |
| Bengali  | bn.mp3   |

## Usage

1. Add your MP3 files to this directory (or set `NARRATION_TAMIXA_TTS_DIR=file:/path/to/your/samples`)
2. Set `.env`: `NARRATION_TTS_PROVIDER=sample`
3. Restart the backend

If a language file is missing, the fallback (`en.mp3` by default, or `NARRATION_TAMIXA_TTS_FALLBACK`) is used. If that too is missing, a minimal placeholder is used.

**Single default sample:** To use one MP3 for all languages (e.g. `tamixa.mp3`), set `NARRATION_TAMIXA_TTS_DEFAULT_FILE=file:/path/to/tamixa.mp3` or `NARRATION_TAMIXA_TTS_DEFAULT_FILE=classpath:tts-samples/tamixa.mp3`. That file is then used for every story regardless of language.

## Note

Every story in a given language will play the same sample. Use for demos or when cloud APIs are unavailable.
