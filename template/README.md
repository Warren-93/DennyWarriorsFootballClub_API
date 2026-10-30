# Club website template

A reusable full-stack framework for football and other sports club websites,
extracted from the Denny Warriors FC site. It has two halves, one per repository:

| Part | Lives in | Stack |
|---|---|---|
| **club-api** | `DennyWarriorsFootballClub_API/template/club-api` | Java 17, Spring Boot 3.5, MongoDB |
| **club-web** | `DennyWarriorsFootballClub/template/club-web` | React 18, Vite (Vercel-ready) |

Each folder is self-contained and is meant to become the root of a new
repository for a new club. Each has its own README, tests and CI workflow.

```
 Static host (Vercel, Netlify...)          Container host (Render, Fly...)        MongoDB Atlas
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
cp -r DennyWarriorsFootballClub/template/club-web                my-club-web

# 2. Backend: rename the package, configure, run
cd my-club-api && ./scripts/init-club.sh org.myclub.api my-club-api
cp .env.example .env    # JWT_SECRET, ADMIN_PASSWORD, CLUB_NAME
docker compose up --build

# 3. Frontend: brand it, run
cd ../my-club-web && npm install
#    edit src/config/club.config.js and replace public/badge.svg
npm run dev            # http://localhost:3000
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
  `admin/resources.js`. That gives you a dashboard tab, and one hook call
  (`useResourceEditor`) adds in-place editing on the public page. Security
  works by path convention, so no security change is needed.
- **New league data source:** implement `LeagueDataProvider` (two methods).
  Scheduling, retries, upserts and the admin sync screen already exist.
- **Other sports:** positions, column labels, player stats and league
  points are all configuration.

## What changed from the Denny Warriors code, and why

The template keeps the original's architecture and its strongest pieces
(hand-rolled JWT with refresh tokens, login rate limiting, the
`LeagueApiClient` seam, config-driven admin CRUD, the sync service with
retries and logs). These issues were fixed on the way:

**Frontend** (`DennyWarriorsFootballClub`)

The template keeps this site's design (squad cards, history timeline, media
page with a live player, article pages) and its in-place editing for staff,
and fixes these:

- The login page accepted any pasted JWT. It now takes a username and
  password only.
- The refresh token returned at login was thrown away, so admins were
  silently logged out once the 24h token expired: the admin route only
  checked that *a* token existed. The client now refreshes on 401, sharing
  one refresh across concurrent requests, and signs out cleanly if that
  fails.
- Errors showed the API's raw JSON body (`{"timestamp":…,"message":…}`).
  Errors now show the API's `message`, and validation errors name the field.
- The role used to show or hide edit controls was decoded from the token on
  each render: it ignored expiry and didn't update on login or logout. The
  session is now a small store that updates every component, across tabs.
- Three hand-written form modals (player, article, history) and repeated
  position lists became one form built from `admin/resources.js`, shared by
  the public pages and the dashboard. The form is lazy-loaded, so visitors
  never download it.
- Seasons were hard-coded and disagreed with each other (navbar "2026/27",
  league table "2025/26"). They now come from the API.
- `ordinal()` produced "11st", "21th", and "nullth" when there was no
  position. Fixed and tested.
- Unknown URLs silently redirected home; there is now a "Page not found"
  page.
- The Vite config still carried Create React App bridges (`process.env`
  defines, `.js` files parsed as JSX). It is now a plain Vite setup.
- Club name, colours, YouTube channel and sport-specific wording are now
  configuration instead of being spread through the components.

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
