# Club website template

A reusable full-stack framework for football and other sports club websites,
extracted from the Denny Warriors FC site. It has two halves, one per repository:

| Part | Lives in | Stack |
|---|---|---|
| **club-api** | `DennyWarriorsFootballClub_API/template/club-api` | Java 17, Spring Boot 3.5, MongoDB |
| **club-web** | `DennyWarriorsFootballClub_Website-pages/template/club-web` | React 18, Vite |

Each folder is self-contained and is meant to become the root of a new
repository for a new club. Each has its own README, tests and CI workflow.

```
 Static host (Netlify, Pages...)          Container host (Render, Fly...)        MongoDB Atlas
 ┌──────────────────────────┐   HTTPS    ┌────────────────────────────┐        ┌───────────┐
 │ club-web                 │ ─────────► │ club-api  /api/v1/...      │ ─────► │ fixtures  │
 │  club.config.js = brand  │  JSON+JWT  │  CLUB_NAME, LEAGUE_PROVIDER│        │ news ...  │
 └──────────────────────────┘            └─────────────┬──────────────┘        └───────────┘
                                                       │ scheduled sync (optional)
                                                       ▼
                                          League API (Comet, or your own provider)
```

## Starting a new club site

```bash
# 1. Copy each half into its own new repository
cp -r DennyWarriorsFootballClub_API/template/club-api            my-club-api
cp -r DennyWarriorsFootballClub_Website-pages/template/club-web  my-club-web

# 2. Backend: rename the package, configure, run
cd my-club-api && ./scripts/init-club.sh org.myclub.api my-club-api
cp .env.example .env    # JWT_SECRET, ADMIN_PASSWORD, CLUB_NAME
docker compose up --build

# 3. Frontend: brand it, run
cd ../my-club-web && npm install
#    edit src/config/club.config.js and replace public/badge.svg
npm run dev
```

What changes per club is **configuration, not code**:

| What | Where |
|---|---|
| Name, colours, badge, positions, table labels, sections on/off | `club-web/src/config/club.config.js` |
| Club name for matching league data, timezone, points per win/draw | API environment variables |
| Where fixtures come from: entered by hand, or synced | `LEAGUE_PROVIDER` (`manual` / `comet` / your own) |

## Extension points

- **New content type** (sponsors, staff, documents): copy the API's
  `history/` package, then add one entry to the web app's
  `admin/resources.js`. Security works by path convention, so no security
  change is needed.
- **New league data source:** implement `LeagueDataProvider` (two methods).
  Scheduling, retries, upserts and the admin sync screen already exist.
- **Other sports:** positions, column labels, player stats and league
  points are all configuration.

## What changed from the Denny Warriors code, and why

The template keeps the original's architecture and its strongest pieces
(hand-rolled JWT with refresh tokens, login rate limiting, the
`LeagueApiClient` seam, config-driven admin CRUD, the sync service with
retries and logs). These issues were fixed on the way:

**Frontend and API contract**
- The website and API had drifted apart. The site called
  `/api/dennywarriors/results`, `/league/table` and admin writes such as
  `POST /fixtures` that the API doesn't have, and it read list responses
  as arrays although the API wraps them in `{ data, ... }`. The template
  ships both halves against one documented contract, with tests on each side.
- The login page showed a hard-coded dev password, accepted any pasted JWT,
  and had a localhost-only login bypass (`warriors123`). All removed.
- Tokens were never refreshed: the 24h token simply expired. The client now
  refreshes on 401, sharing one refresh across concurrent requests.
- "Read more" buttons did nothing. Articles now have their own page,
  rendered as text (no HTML injection).
- `ordinal()` produced "11st", "21th", and "nullth" when there was no
  position. Fixed and tested.
- Season ("2024/25") and competition filters were hard-coded; they now
  come from the API.
- Create React App (deprecated) was replaced by Vite, and the admin panel is
  code-split.

**API**
- `GET /news/{slug}` returned **unpublished drafts** to anyone. It now
  returns 404 for drafts.
- Login accepted **plaintext stored passwords** as a fallback, and unknown
  usernames returned faster than wrong passwords (username enumeration).
  The template uses BCrypt only, with constant-work rejection.
- Unauthenticated requests got 403, so a client couldn't tell "token
  expired" from "not allowed". Missing or expired tokens now get 401.
- Admin endpoints bound request bodies straight onto the database entities,
  so clients could set server-managed fields. They now take validated
  request records.
- `SyncService` read MongoDB in `@PostConstruct`, so the app (and the test
  suite) couldn't start without a database. Nothing touches the database at
  startup now, and the tests need no MongoDB.
- A manual sync could overlap the scheduled one. Runs are now serialised.
  Standings are inserted before the old rows are deleted, so a failure
  can't leave an empty table.
- List endpoints loaded whole collections and filtered in memory. They now
  query MongoDB with filters, sorting and limits.
- Five near-identical exception classes became one `ApiException`, and the
  `isBlank`/`normalise` helpers copied across four services became `Text`.
- Hard-coded `Europe/London`, 3/1/0 points and the Denny Warriors YouTube
  channel became configuration.
- Users had no way to change their own password. `PUT /account/password`
  adds one.
- Fixtures, results and the league table could only come from Comet. A
  `manual` provider and admin endpoints now let clubs outside Comet use the
  site.
- Spring Boot 3.2 (out of OSS support) was upgraded to 3.5, and the unused
  mail and WebFlux starters were removed (`RestClient` replaces `WebClient`).
