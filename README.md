# retain-ai

Voice-based spaced repetition review app. Reads articles from Notion, generates
questions with an LLM, quizzes you out loud via the Gemini Live API, grades
your spoken answer against the source material, and schedules the next
review with FSRS.

## How this maps to the build plan

| Step | Package |
|---|---|
| Sync Notion into your own DB | `adapter.notion`, `repository` |
| Generate questions with an LLM | `service.QuestionGeneratorService` |
| Realtime voice loop | `voice.LiveVoiceSessionHandler`, `voice.GeminiLiveClient` |
| Grading logic (go deep here) | `service.GradingService` |
| Spaced repetition scheduler | `service.SchedulerService`, `domain.ReviewCard` |
| Close the loop | `adapter.ContentSource#writeBackStatus`, `controller.ReviewController` |

## Key design decision: content vs. review state

`Article` (content: title, url, tags, your notes) and `ReviewCard` (review
state: question, schedule, times reviewed) are deliberately separate
entities in our own Postgres DB. Notion is only ever a read source for
`Article` data, plus one lightweight status write-back — it is never
queried for "what's due," since that would hit API rate limits fast and
would need reinventing per note-taking app if you ever add another source.

Adding a second content source later (Obsidian, Readwise, plain markdown)
means implementing one more `ContentSource`, not touching anything in
`service` or `voice`.

## Build order

1. Implement `NotionClient` + `NotionContentSource`, verify `/api/sync` pulls
   real articles into Postgres.
2. Implement `QuestionGeneratorService` against a couple of real articles;
   inspect output by hand before wiring anything downstream.
3. Implement `GeminiLiveClient` + `LiveVoiceSessionHandler` against a single
   **hardcoded** question first — isolate audio/session issues early.
4. Implement `GradingService` last, and iterate on the prompt against a
   small fixed test set of known-good/bad/partial answers. This is the
   piece worth documenting in detail.
5. Wire in an FSRS library inside `SchedulerService`.

## Not built in v1 (by design)

- Multi-user / auth
- Two-way Notion sync
- Any content source other than Notion (the `ContentSource` interface is
  designed for it, but only implemented for Notion — mention this
  distinction explicitly if this project comes up in an interview).
