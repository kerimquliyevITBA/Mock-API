# MBM_Nails — Booking Backend

Spring Boot 3 (Java 21) API for the MBM_Nails booking studio.

- **Database**: Supabase Postgres, isolated in a dedicated `gulnar` schema so it never mixes with anything else running on the same instance.
- **Runtime**: Railway (nixpacks + Java), JVM pinned to `Asia/Baku` at boot so "past date/time" server checks match the customer's wall clock.
- **Frontend**: `frontend/index.html` — single-file HTML page that consumes this API. Host it anywhere (Netlify, GitHub Pages, Railway static, or open locally).

The rest of this repo (root `pom.xml`, `src/`, `auth.sql`, etc.) belongs to another project and is **untouched**.

---

## 1. Supabase setup

1. In your Supabase project, open **SQL Editor** and create the schema once (Flyway will create tables on first boot):

   ```sql
   CREATE SCHEMA IF NOT EXISTS gulnar;
   ```

2. Get the **Session pooler** connection string (Supabase → Project Settings → Database → Connection pooling):

   ```
   Host:     aws-0-<region>.pooler.supabase.com
   Port:     5432
   Database: postgres
   User:     postgres.<project-ref>
   Password: <your db password>
   ```

3. Build the JDBC URL:

   ```
   jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres
   ```

   The app pins the `search_path` to `gulnar, public` on every connection.

## 2. Railway deploy

1. Create a new Railway service, connect this repo, and set the **root directory** to `gulnar-nail`.
2. Environment variables (copy from `.env.example`):

   | Name              | Value                                                                 |
   |-------------------|-----------------------------------------------------------------------|
   | `DATABASE_URL`    | `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres`  |
   | `DB_USERNAME`     | `postgres.<project-ref>`                                              |
   | `DB_PASSWORD`     | your Supabase DB password                                             |
   | `DB_SCHEMA`       | `gulnar`                                                              |
   | `JWT_SECRET`      | any 32+ char random string                                            |
   | `JWT_TTL_HOURS`   | `8`                                                                   |
   | `ADMIN_USERNAME`  | `admin`                                                               |
   | `ADMIN_PASSWORD`  | strong password (used only on first boot to create the admin user)    |
   | `ADMIN_NAME`      | `Administrator`                                                       |
   | `CORS_ORIGINS`    | `*` for dev, or `https://your-frontend.example`                       |
   | `SEED_DEMO`       | `true` on first boot to get demo services + open days, then `false`   |

3. Railway sets `PORT` automatically — the app respects it.
4. Build: nixpacks picks up `nixpacks.toml` → `./mvnw clean package` → `java -jar target/app.jar`.

## 3. Frontend

Open `frontend/index.html` in a browser. `window.API_BASE` is hardcoded to
the deployed Railway URL, so a double-click on the file is enough. The URL
can still be overridden with `?api=https://…` (persists in `localStorage`)
if you want to point at a staging backend.

Deploy: any static host works (Netlify, Vercel, Cloudflare Pages, GitHub
Pages, Railway static). No build step needed.

---

## 4. API surface

Base: `/api/v1`

### Public
- `GET  /health`
- `GET  /services` → active services (name, price, `durationMin`)
- `GET  /availability?from=YYYY-MM-DD&to=YYYY-MM-DD[&durationMin=N]` →
  days + working intervals + generated 20-minute-step slot start times
  filtered by the requested service duration. Pass the selected service's
  `durationMin` to only see start times that actually fit.
- `POST /reservations` — `{name, prefix, phone, serviceId, date, time}`.
  Duration is snapshotted from the service.
- `POST /reservations/status` — `{code, prefix, phone}`
- `POST /reservations/cancel` — `{code, prefix, phone}`

### Auth
- `POST /auth/login` — `{username, password}` → `{token, username, fullName, roles}`
- `GET  /auth/me` — echoes the DB record for the logged-in user
- `PUT  /auth/me` — `{fullName, username?}`; a new username is checked for
  uniqueness and the response carries a fresh JWT
- `POST /auth/change-password` — `{currentPassword, newPassword}`

### Admin (Bearer JWT, `ROLE_ADMIN`)
- `GET/POST/PUT/DELETE /admin/services[/{id}]` — delete is a soft-delete
  (`active=false`); soft-deleted rows are hidden from the admin list
- `GET  /admin/availability?from=&to=[&durationMin=N]`
- `POST /admin/availability/days` — `{date, closed?, note?}`; refuses to
  create a NEW day whose date is already in the past
- `DELETE /admin/availability/days/{date}`
- `POST /admin/availability/days/{date}/intervals` — `{start, end}` (both HH:mm)
- `DELETE /admin/availability/days/{date}/intervals/{HH:mm}`
- `GET  /admin/reservations?from=&to=`
- `POST /admin/reservations/{id}/cancel`
- `GET  /admin/stats`

## 5. Booking model
- Admin defines **working intervals** per day (e.g. 09:00–12:00 and 13:00–18:00).
- Each service carries `durationMin`, forced to a multiple of 20 (20, 40, 60,
  80, 100, 120, 140, 160, 180).
- The server expands each interval into candidate start times at 20-minute
  boundaries, then hides any candidate that would overlap an existing ACTIVE
  reservation. Overlap check uses `[start, start+duration)` on both sides, so
  an 11:00 60-min booking correctly blocks 11:20, 11:40 candidates and also
  refuses 10:20 for a 60-min service that would overrun 11:00.

## 6. Validation (server-side — never trust the client)
- Name: 2–80 chars
- Phone: `+994 <prefix> <7 digits>`; prefix ∈ {050, 051, 055, 070, 077, 099, 010};
  stored as `+994 <prefix without leading 0> <7 digits>`
- Service must be active, its `durationMin` must be a multiple of 20
- Reservation start time must be a multiple of 20 minutes
- Date/time cannot be in the past (JVM is pinned to Asia/Baku so the check
  matches the customer's wall clock)
- `[start, start+duration)` must fit fully inside a working interval and must
  not overlap an existing ACTIVE reservation on that date
- Same phone cannot hold two active reservations at the same date+time
- Cannot close a day or delete an interval that has an active reservation
  inside it

## 7. Cron job — expired reservations
`ExpiredReservationJob` runs every 5 minutes and sets `ACTIVE → COMPLETED` for anything whose date/time is in the past.

## 8. Local development

```bash
cd gulnar-nail
cp .env.example .env      # then edit
# export the vars (or use direnv / an IDE plugin)
./mvnw spring-boot:run
```

Health: `curl http://localhost:8080/api/v1/health`.
