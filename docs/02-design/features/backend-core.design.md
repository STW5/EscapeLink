# EscapeLink Backend — Design Document

## Status
- Phase: Design → Do (in progress)
- Owner: single-session build (Spring Boot backend)
- Target deployment: single Raspberry Pi 5 (ARM64) node

## 1. Summary

EscapeLink is a mobile web service used at offline events: teams join via a shared QR code,
solve quizzes / escape-room missions from multiple phones simultaneously, and see each
other's progress sync in real time. Admins monitor all teams live, review image submissions,
and can force-override game state on site (force-complete a quiz, reset a team, force a team
into the final stage). A public leaderboard screen shows read-only standings.

## 2. Stack

| Concern | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot (existing project: 4.1.1) |
| DB | PostgreSQL |
| ORM | Spring Data JPA |
| Migration | Flyway |
| AuthN/Z | Spring Security (participants: opaque cookie session backed by DB; admin: server session login) |
| Realtime | Spring WebSocket + STOMP |
| API | REST, Jakarta Bean Validation |
| Docs | springdoc-openapi |
| Build | Gradle |
| Deploy | Docker Compose (nginx + backend + postgres) on Raspberry Pi 5 ARM64 |
| Image storage | Local filesystem on SSD attached to the Pi |
| Explicitly out of scope for now | Redis, Kafka/RabbitMQ, Kubernetes, microservices |

PostgreSQL is the single source of truth for all game state. WebSocket never carries
authoritative state — it only notifies clients that a DB-committed change happened; clients
still fetch a snapshot (`GET /api/team-state`) to recover from missed events.

## 3. Package layout (existing base package: `com.stw.escapelink`)

```
com.stw.escapelink
├── EscapeLinkApplication
├── global/ (config, security, exception, response, websocket)
├── game/       (controller, service, repository, domain, dto)
├── team/       (controller, service, repository, domain, dto)   // Team + TeamSession
├── quiz/       (controller, service, repository, domain, dto)   // Quiz, QuizSecret, QuizProgress
├── submission/ (controller, service, repository, domain, dto)   // ImageSubmission
├── admin/      (controller, service, domain — Admin, AdminActionLog)
└── leaderboard/
```
Controller → Service → Repository. No business logic in controllers. Entities are never
returned directly from APIs — DTOs only.

## 4. Core domain model

- **Game**: id, title, status(READY/RUNNING/FINISHED), startAt, endAt.
- **Team**: id, gameId, name, inviteToken (random, not the DB id — never expose Team.id in the QR), currentRunNo.
- **TeamSession**: one per participant device. id, teamId, sessionToken, deviceId, createdAt, lastAccessAt, expiresAt, revoked. Many sessions → one team.
- **Quiz**: id, gameId, title, content, type(TEXT/IMAGE), orderNo, hint, hintDelaySeconds. Never carries the answer.
- **QuizSecret**: quizId, answer. Server-only table, never joined into participant-facing DTOs.
- **QuizProgress**: id, teamId, quizId, runNo, status(UNSOLVED/PENDING/COMPLETED), firstEnteredAt, solvedAt, lastWrongAnswerAt, version (optimistic lock). Unique on (teamId, quizId, runNo).
- **ImageSubmission**: id, teamId, quizId, runNo, submissionVersion, filePath, status(PENDING/APPROVED/REJECTED), rejectReason, createdAt, reviewedAt, reviewedBy.
- **Admin**: id, username, passwordHash, role(ADMIN). Fully separate auth from participants.
- **AdminActionLog**: actor, action(FORCE_COMPLETE/RESET_TEAM/FORCE_FINAL_STAGE/APPROVE_IMAGE/REJECT_IMAGE), targetTeam, timestamp, detail.
- **TeamRun**: finalStageEnteredAt, clearedAt (first-completion-wins, never overwritten once set).

## 5. QR join & session auth

`POST /api/team-sessions/join { inviteToken }` → validate token → check Game is joinable →
create TeamSession → set `HttpOnly Secure` cookie (session token) → return team info.
No participant auth token is ever placed in LocalStorage. Every subsequent request resolves
the acting Team **only** from the authenticated TeamSession — a client-supplied `teamId` in
the URL or body is never trusted.

## 6. TEXT quiz answer flow

`POST /api/quizzes/{quizId}/answer { requestId, answer, runNo }`

Validate: session → team → game RUNNING → runNo matches Team.currentRunNo → quiz exists and
is TEXT → not already COMPLETED → 10s cooldown not active → normalize (trim, case-fold) →
compare against QuizSecret → on match: transactionally flip QuizProgress to COMPLETED, set
solvedAt only if unset → commit → publish `QUIZ_COMPLETED` to `/topic/teams/{teamId}`.

`requestId` dedupes retries/offline replays: the same requestId returns the previously
computed result rather than reprocessing.

## 7. Cooldown

On a wrong answer, `lastWrongAnswerAt` is set (server clock). The same team cannot resubmit
to the same quiz for 10 seconds, regardless of which device submits. Response on a blocked
attempt: `{ code: "QUIZ_COOLDOWN", retryAfterSeconds }`. Not IP-based (event venues share a
single IP).

## 8. Concurrency

Two devices on the same team submitting simultaneously must yield exactly one COMPLETED
transition and one solvedAt. Enforced via `@Transactional` + a DB-level uniqueness/locking
strategy (optimistic `version` column on QuizProgress, retried on conflict, or a pessimistic
row lock on the QuizProgress row for the update path) — not by application-level mutexes.
Same discipline applies to: dual admin review of one image, reject-after-force-complete,
resubmission racing an approval, duplicate final-clear requests, duplicate requestIds.

## 9. WebSocket

STOMP, per-team topic `/topic/teams/{teamId}`. Clients authenticate the WebSocket handshake
using the same TeamSession cookie; a client may only subscribe to its own team's topic.
Events: QUIZ_COMPLETED, IMAGE_PENDING, IMAGE_APPROVED, IMAGE_REJECTED, FINAL_STAGE_ENABLED,
FORCE_FINAL_STAGE, TEAM_RESET. Event delivery is best-effort; reconnecting clients always
re-fetch `GET /api/team-state` rather than trusting replayed events.

## 10. Image submission & review

Client pre-compresses (≤1080px long edge, JPEG/WebP, ≤2MB, configurable). Server re-validates
MIME type, real image content, size, extension, that the quiz belongs to the team and is
IMAGE type, and that runNo matches. Filenames are UUID-generated, never user input, stored
under `/srv/escapelink/uploads/game-{id}/team-{id}/quiz-{id}/run-{n}/`. Submission moves
QuizProgress UNSOLVED→PENDING; admin approve→COMPLETED, reject→UNSOLVED (with reason, one of
a small preset list) and fires `IMAGE_REJECTED`.

## 11. Hint

Hint availability is per-team, anchored to the first team member's entry into the quiz
(`firstEnteredAt`, set once). Available when `now >= firstEnteredAt + hintDelaySeconds`.
The hint body itself is withheld from the response until then — only an `available`/
`availableAt` flag is returned early.

## 12. Final stage & reset

Final stage unlocks when all quizzes are COMPLETED, or via admin force
(`POST /api/admin/teams/{teamId}/force-final-stage`, logged, does not silently mark quizzes
complete — forced vs. earned entry stays distinguishable and survives reconnects via a
server-stored flag, not just a WebSocket event). Clear time (`TeamRun.clearedAt`) is set once
only, on the first successful completion.

Team reset increments `Team.currentRunNo` rather than deleting rows. Requests carrying a
stale `runNo` are rejected (`INVALID_RUN`) — this is what makes late-arriving offline
requests from before a reset harmless. Historical QuizProgress/ImageSubmission rows are kept
for audit.

## 13. Admin

`/api/admin/**` requires ADMIN role via Spring Security and is unreachable with a participant
TeamSession cookie. Passwords hashed with BCrypt. Every override
(force-complete, reset, force-final-stage, approve/reject image) writes an AdminActionLog row.

## 14. Leaderboard

`GET /api/leaderboard` is read-only and public-safe: team name, completed/total quiz counts,
finalStageReached, clearTime. Never exposes: answers, inviteToken, admin data, raw submitted
images, or session data. Broadcastable over WebSocket for a live display screen.

## 15. Errors

`@RestControllerAdvice` global handler, uniform envelope:
```json
{ "success": false, "code": "QUIZ_COOLDOWN", "message": "...", "data": { "retryAfterSeconds": 6 } }
```
Codes: TEAM_NOT_FOUND, INVALID_INVITE_TOKEN, SESSION_EXPIRED, GAME_NOT_RUNNING, QUIZ_NOT_FOUND,
QUIZ_ALREADY_COMPLETED, QUIZ_COOLDOWN, WRONG_ANSWER, INVALID_QUIZ_TYPE, INVALID_RUN,
IMAGE_TOO_LARGE, INVALID_IMAGE, SUBMISSION_ALREADY_REVIEWED, ACCESS_DENIED.

## 16. Deployment

Docker Compose: nginx (reverse proxy/TLS termination) + backend + postgres, all ARM64-capable
images. Volumes: `/srv/escapelink/postgres`, `/srv/escapelink/uploads`. `ddl-auto` is never
`create` outside local dev — schema changes go through Flyway (`V1__init.sql`, ...).

## 17. Build order (this design is implemented incrementally)

1. Project foundation (Gradle deps, Docker Compose for local Postgres, Flyway baseline, global
   exception handling, health check).
2. Core entities + repositories (Game, Team, TeamSession, Quiz, QuizSecret, QuizProgress).
3. QR join + TeamSession auth + team-state snapshot endpoint.
4. TEXT quiz list/answer/cooldown/concurrency.
5. WebSocket team-channel broadcast of QUIZ_COMPLETED.
   → First vertical slice: two phones on one team, phone A answers, phone B sees it live.
6. Image upload. 7. Admin review. 8. Admin manual override. 9. Final stage. 10. Leaderboard.
11. Offline/reconnect hardening. 12. Raspberry Pi Docker deployment.

Steps 1–8 are implemented (backend + a Next.js static-export frontend in `frontend/`,
covering QR join, TEXT answer submission, IMAGE submission with client-side
resize/compression, and an `/admin` console with session-based admin login,
image approve/reject review, and team control: force-complete a quiz, reset a
team to a fresh run, and force-open the final stage). 9–12 are designed above
but not yet built. Note: there is still no API to create games/teams/quizzes —
all content so far was seeded via the dev-only seeder or direct SQL.

## 18. Test requirements carried into implementation

Concurrent same-quiz double-submit → single COMPLETED; cooldown enforcement across devices;
stale-runNo rejection after reset; duplicate requestId → no duplicate state change; admin-only
routes reject participant sessions (403); duplicate final-clear → first timestamp wins.
Prefer Testcontainers-backed Postgres integration tests where feasible.
