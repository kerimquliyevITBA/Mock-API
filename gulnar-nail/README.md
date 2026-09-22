# Gülnar Nail — Booking Backend

Spring Boot 3 (Java 21) API for the Gülnar Nail booking studio.

- **Database**: Supabase Postgres, isolated in a dedicated `gulnar` schema so it never mixes with anything else running on the same instance.
- **Runtime**: Railway (nixpacks + Java).
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

3. Build the JDBC URL. Recommended (adds statement caching for JPA):

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
4. Build: nixpacks picks up `nixpacks.toml` → `mvn clean package` → `java -jar target/app.jar`.

## 3. Frontend

Open `frontend/index.html` in a browser. Configure the API base one of three ways:

- URL param once: `?api=https://your-railway-app.up.railway.app` (persists in `localStorage`)
- Inject before the main script: `<script>window.API_BASE="https://…"</script>`
- Or edit the `window.API_BASE` fallback in the script.

The active API URL is shown in the small corner badge.

Deploy: any static host works (Netlify, Vercel, Cloudflare Pages, GitHub Pages, Railway static). No build step needed.

---

## 4. API surface

Base: `/api/v1`

### Public
- `GET  /health`
- `GET  /services` → active services
- `GET  /availability?from=YYYY-MM-DD&to=YYYY-MM-DD` → days + open slots (past hidden)
- `POST /reservations` — `{name, prefix, phone, serviceId, date, time}`
- `POST /reservations/status` — `{code, prefix, phone}`
- `POST /reservations/cancel` — `{code, prefix, phone}`

### Auth
- `POST /auth/login` — `{username, password}` → `{token, username, roles, ...}`
- `GET  /auth/me` (Bearer)

### Admin (Bearer JWT with `ADMIN` role)
- `GET/POST/PUT/DELETE /admin/services[/{id}]`
- `GET  /admin/availability?from=&to=` (includes past + taken flags)
- `POST /admin/availability/days` — `{date, closed?, note?}`
- `DELETE /admin/availability/days/{date}`
- `POST /admin/availability/days/{date}/slots` — `{time: "HH:mm"}`
- `DELETE /admin/availability/days/{date}/slots/{HH:mm}`
- `GET  /admin/reservations?from=&to=`
- `POST /admin/reservations/{id}/cancel`
- `GET  /admin/stats`

## 5. Validation (server-side — never trust the client)
- Name: 2–80 chars
- Phone: `+994 <prefix> <7 digits>`; prefix ∈ {050, 051, 055, 070, 077, 099, 010}
- Service must be active
- Date/time cannot be in the past
- Slot must be open (day exists, `is_closed=false`, slot present)
- Cannot double-book the same slot (unique partial index)
- Same phone cannot hold two active reservations at the same date+time
- Cannot close a day or delete a slot that has active reservations

## 6. Cron job — expired reservations
`ExpiredReservationJob` runs every 5 minutes and sets `ACTIVE → COMPLETED` for anything whose date/time is in the past.

## 7. Local development

```bash
cd gulnar-nail
cp .env.example .env      # then edit
# export the vars (or use direnv / an IDE plugin)
./mvnw spring-boot:run
```

Health: `curl http://localhost:8080/api/v1/health`.
