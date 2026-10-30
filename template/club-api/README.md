# club-api: sports club website API (template)

A reusable Spring Boot + MongoDB backend for a football (or any sports) club
website: fixtures and results, league table, squad, news, club history,
image uploads, YouTube videos, and a role-based admin API.

It was extracted from the Denny Warriors FC API, with everything
club-specific moved into configuration. Its frontend counterpart is
**club-web** (React), which lives in the website repository's
`template/club-web` folder and is built against the API contract below.

- Java 17, Spring Boot 3.5, Spring Security, Spring Data MongoDB
- Stateless JWT auth (access + refresh tokens), BCrypt, login rate limiting
- Pluggable league data: enter fixtures by hand, or sync them from a league API
- No MongoDB needed to run the test suite

---

## Quick start

```bash
# 1. Make it yours: renames the Java package and Maven coordinates
./scripts/init-club.sh org.myclub.api my-club-api

# 2. Configure
cp .env.example .env        # set JWT_SECRET, ADMIN_PASSWORD, CLUB_NAME

# 3a. Run with Docker (starts MongoDB too)
docker compose up --build

# 3b. ...or against your own MongoDB
export $(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run
```

Log in with `ADMIN_USERNAME` (default `admin`) and `ADMIN_PASSWORD`:

```bash
curl -s -X POST localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD>"}'
```

Run the tests: `./mvnw test`

---

## How it fits together

```
 Browser (club-web)
   │  GET /api/v1/...            public, no token
   │  /api/v1/admin/...          Bearer access token
   ▼
 SecurityConfig ── JwtAuthFilter ── path-convention access rules
   ▼
 Feature controllers (one package each)
   auth/  news/  squad/  history/  media/  matches/  sync/
   ▼
 MongoDB  ◄──── SyncService ◄──── LeagueDataProvider
                (schedule,          ├─ ManualLeagueDataProvider (default)
                 retries, logs)     └─ CometLeagueDataProvider
                                       (or your own: FA Full-Time, CSV...)
```

**Package by feature.** Each folder under `com.clubsite.api` is a complete
feature: document model, repository, request/response records and a
controller holding both the public and the `/admin` endpoints. `common/`
holds the shared pieces (`ApiException`, `ApiListResponse`, the global error
handler) and `config/` holds `ClubProperties` and security.

**Public reads never call a third party.** League data is synced into MongoDB
by `SyncService`, so a league API outage can't take the site down.

**Fixtures are stored neutrally** ("home vs away"). `ClubPerspective` derives
the club's view (home/away, opponent, scoreFor/Against, W/D/L) from
`CLUB_NAME`, so the fixture list, the stats and the league table agree.

---

## Configuration

Everything club-specific is in `ClubProperties` (`club.*`), mapped onto
environment variables in `application.properties`. See `.env.example` for
the full list.

| Variable | Default | Purpose |
|---|---|---|
| `MONGODB_URI` | **required** | Connection string including the database name |
| `JWT_SECRET` | **required** | 32+ chars. `openssl rand -base64 48` |
| `ADMIN_PASSWORD` | required on first boot | Creates the first SUPER_ADMIN (12+ chars) |
| `CLUB_NAME` | `Example FC` | Must match the league data's spelling (case-insensitive) |
| `CLUB_TIMEZONE` | `Europe/London` | Kick-off times are shown in this zone |
| `POINTS_FOR_WIN` / `_DRAW` / `_LOSS` | `3` / `1` / `0` | Season stats; change for other sports |
| `ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated frontend origins (CORS) |
| `LEAGUE_PROVIDER` | `manual` | `manual` or `comet` |
| `COMET_FIXTURES_URL` / `COMET_STANDINGS_URL` | | Only for `comet`; include the API key |
| `YOUTUBE_CHANNEL_ID` | blank (off) | Channel id starting `UC` |
| `SPRING_PROFILES_ACTIVE` | | `prod` for JSON logs |

---

## API contract

All list endpoints return `{ "data": [...], "page", "size", "totalElements" }`.
All errors return `{ "timestamp", "status", "error", "message", "path", "fieldErrors"? }`.

### Public (no token)

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/fixtures` | `?status=upcoming\|past\|<raw>&season=&competition=&limit=`. `past` is newest-first |
| GET | `/api/v1/fixtures/next` | 204 when nothing is scheduled |
| GET | `/api/v1/fixtures/{id}` | |
| GET | `/api/v1/fixtures/seasons` | Newest first, e.g. `["2025/26", "2024/25"]` |
| GET | `/api/v1/fixtures/competitions` | |
| GET | `/api/v1/standings` | Each row has `isClub` |
| GET | `/api/v1/stats` | `?season=` (defaults to latest) |
| GET | `/api/v1/squad` | `?position=` |
| GET | `/api/v1/squad/{id}` | |
| GET | `/api/v1/news` | Published only. `?tag=&limit=` |
| GET | `/api/v1/news/{slug}` | Drafts return 404 |
| GET | `/api/v1/history` | `?type=TIMELINE\|HONOUR` |
| GET | `/api/v1/media/{id}` | Uploaded image bytes |
| GET | `/api/v1/media/videos` | Latest YouTube uploads |

A fixture response includes the neutral fields plus `home`, `opponent`,
`scoreFor`, `scoreAgainst`, `outcome` (`W`/`D`/`L`, null if unplayed) and
`localKickoffTime` (`HH:mm` in `CLUB_TIMEZONE`).

### Auth

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/api/v1/auth/login` | `{username, password}` | `{accessToken, refreshToken, username, role, expiresAt}` |
| POST | `/api/v1/auth/refresh` | `{refreshToken}` | `{accessToken, expiresAt}` |
| PUT | `/api/v1/account/password` | `{currentPassword, newPassword}` | 204. Any signed-in user |

Access tokens last 1 hour and refresh tokens 7 days. A missing or expired
token gets **401**, meaning "refresh and retry". The wrong role gets **403**.

### Admin (Bearer token)

| Path | GET | POST / PUT / DELETE |
|---|---|---|
| `/api/v1/admin/fixtures[/{id}]` | VIEWER+ | EDITOR+ |
| `/api/v1/admin/standings` | VIEWER+ | EDITOR+ (PUT replaces the whole table) |
| `/api/v1/admin/squad[/{id}]` | VIEWER+ | EDITOR+ |
| `/api/v1/admin/news[/{id}]` | VIEWER+ (includes drafts) | EDITOR+ |
| `/api/v1/admin/history[/{id}]` | VIEWER+ | EDITOR+ |
| `/api/v1/admin/media` | | EDITOR+ (multipart `file`, returns absolute `url`) |
| `/api/v1/admin/sync/logs`, `/settings` | VIEWER+ | EDITOR+ (`POST /trigger`, `PUT /settings {intervalDays}`) |
| `/api/v1/admin/users[/{id}]` | SUPER_ADMIN | SUPER_ADMIN |

---

## Extending

### Add a content type (sponsors, staff, documents...)

Copy `history/`: it is the smallest complete module (model, repository and
one controller with a request record). Put public reads under
`/api/v1/<feature>` and writes under `/api/v1/admin/<feature>`. Because
`SecurityConfig` works by path convention, the new endpoints are protected
without touching it. Then add a resource entry in club-web's
`src/admin/resources.js` to get an admin screen for it.

### Sync from a different league system

Implement `sync/LeagueDataProvider` and annotate it with
`@ConditionalOnProperty(name = "club.league.provider", havingValue = "<id>")`.
Map the league's data onto `ExternalFixture` / `ExternalStandingsRow` and
leave retries, upserts, logging and scheduling to `SyncService`. See
`sync/comet/` for a complete example, including a parser test built from real
data.

### Other sports

Positions, news categories and table column labels are frontend config.
League points are `POINTS_FOR_*`. `MatchEvent.type` and `StandingsRow`'s
`scoreFor`/`scoreAgainst` are sport-neutral.

---

## Deploying

Any platform that runs a Dockerfile works (Render, Railway, Fly.io...). Set
the required variables, point the health check at `/actuator/health`, and
use MongoDB Atlas (the free tier is plenty). Once the first login works, set
`ADMIN_BOOTSTRAP_ENABLED=false`.

`.github/workflows/` runs the tests and a gitleaks secret scan on every push
once this folder is the root of its own repository.

## Security notes

- No secrets have defaults. The app refuses to start without `JWT_SECRET`.
- Passwords are BCrypt only. Unknown usernames take the same time to reject
  as wrong passwords, so login timing doesn't reveal which accounts exist.
- Refresh re-checks the user, so deleting a user or changing their role
  takes effect within one access-token lifetime (1 hour).
- Request bodies are validated records: clients can't set `id`, `createdAt`
  and similar fields.
- The login rate limiter is in-memory and per instance: fine for one
  instance, but use a shared store before scaling out.
