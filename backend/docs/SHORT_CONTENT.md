# Short-Form Content (Riddles, Thought for the Day, etc.)

Backend support for kid-friendly short content: riddles, thought for the day, proverbs, tongue twisters, and more.

## Implemented types

| Type | Description |
|------|-------------|
| `RIDDLE` | Riddle text + optional `answer` |
| `THOUGHT_FOR_THE_DAY` | Daily quote/thought; use `display_date` for “one per day” |
| `PROVERB` | Traditional proverb or saying |
| `TONGUE_TWISTER` | Phrase for pronunciation practice |
| `JOKE` | Kid-safe joke; `answer` can be punchline |
| `FUN_FACT` | Short interesting fact |
| `WORD_OF_THE_DAY` | Word + brief meaning/example |
| `BRAIN_TEASER` | Puzzle or logic question; `answer` for solution |
| `AFFIRMATION` | Positive affirmation for kids |
| `QUOTE` | Inspirational or famous quote |
| `DID_YOU_KNOW` | “Did you know?” style fact |
| `RHYME` | Short rhyme or verse |
| `TRIVIA` | Trivia Q&A; `answer` for the answer |

## API (parent-facing)

- **List by type**  
  `GET /api/v1/short-content?type=RIDDLE&language=ta&page=0&size=20`  
  Returns published items of that type and language (paginated).

- **Daily item**  
  `GET /api/v1/short-content/daily?type=THOUGHT_FOR_THE_DAY&language=ta&date=2025-03-17`  
  Returns the single published item for the given type, language, and `display_date` (default today).

- **Types**  
  `GET /api/v1/short-content/types`  
  Returns the list of supported type codes for tabs/filters.

All require `PARENT` role (JWT).

## Data model

- **short_content** table: `id`, `type`, `content`, `answer`, `language`, `age_min`, `age_max`, `display_date`, `audio_url`, `status`, `created_at`, `updated_at`.
- **status**: `DRAFT` | `PUBLISHED`. Only `PUBLISHED` is returned by the app APIs.
- **display_date**: Optional. Used for “daily” content (e.g. one thought per day); can be null for evergreen list content.

## Adding content

- Insert/update via SQL or a future admin API.
- Sample seed data: `backend/src/main/resources/db/data/sample_short_content.sql` (run manually for dev).

## More ideas (future)

- **POEM** – Short poem or verse  
- **MANTRA** – Calming phrase to repeat  
- **GREETING** – “Good morning” / “Good night” by language  
- **CHALLENGE** – Daily small challenge (e.g. “Say thank you to 3 people”)  
- **JOKE_OF_THE_DAY** – One joke per day using `display_date`  
- **RIDDLE_OF_THE_DAY** – One riddle per day  

To add a new type: extend `ShortContentType` in the backend and run a migration if you add DB constraints.
