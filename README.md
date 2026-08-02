# Nova Invest — Spring Boot Backend

A direct Spring Boot port of the original FastAPI backend. Same MongoDB collections,
same JSON field names (snake_case, e.g. `amount_usd`, `tx_hash`), same JWT cookie auth
flow — your existing React frontend (`api.js`) should work against this without changes,
as long as it's still pointed at `/api/...` on this server.

## Stack

- Java 17, Spring Boot 3.3
- Spring Web, Spring Security (stateless JWT), Spring Data MongoDB
- `io.jsonwebtoken` (jjwt) for JWT signing/parsing
- Lombok for boilerplate

## Project layout

```
src/main/java/com/novainvest/
  NovaInvestApplication.java     entry point
  model/                         MongoDB documents (User, Plan, Deposit, Withdrawal, Investment, Transaction)
  repository/                    Spring Data Mongo repositories
  dto/                           request/response bodies
  security/                      JwtService, JwtAuthFilter, CookieUtil
  config/                        SecurityConfig, GlobalExceptionHandler
  controller/                    REST controllers (one per resource, mirrors the FastAPI routers)
  bootstrap/                     DataSeeder — seeds the 4 investment plans + admin user on startup
```

## 1. Configure environment variables

`src/main/resources/application.yml` reads everything from env vars (with safe defaults for local dev).
Set these before running — same names as your old FastAPI `.env`:

| Variable         | Example                                   | Notes                              |
|-------------------|--------------------------------------------|-------------------------------------|
| `MONGO_URL`        | `mongodb://localhost:27017`                 | same Mongo instance you already use |
| `DB_NAME`           | `test_database`                              | keep the same DB name to keep your existing data |
| `JWT_SECRET`         | (your existing secret)                        | reuse the same one and old tokens keep working |
| `FRONTEND_URL`        | `http://localhost:3000`                        | used for CORS `Access-Control-Allow-Origin` |
| `ADMIN_EMAIL`          | `admin@nova.invest`                             | seeded admin account |
| `ADMIN_PASSWORD`        | `Admin@12345`                                    | seeded admin account |
| `BTC_WALLET` / `ETH_WALLET` / `USDT_WALLET` | wallet addresses | shown on the deposit page |



**Or**, for simplicity while developing locally, just hardcode the defaults directly in
`application.yml` instead of using env vars (fine for local dev, don't commit real secrets).

You'll need a MongoDB instance running — either local (`mongod`) or Atlas. If you don't
have Mongo installed locally, the easiest option is a free Atlas cluster and point
`MONGO_URL` at its connection string.

## 2. Run it

```bash
mvn spring-boot:run
```

or build a jar and run it:

```bash
mvn clean package
java -jar target/nova-invest-backend-1.0.0.jar
```

The API comes up on **http://localhost:8000** (same port your FastAPI backend used, based
on the frontend's expected base URL — adjust `server.port` in `application.yml` if needed).

On startup, `DataSeeder` will:
- Upsert the 4 default investment plans (starter/silver/gold/platinum)
- Create the admin user from `ADMIN_EMAIL`/`ADMIN_PASSWORD` if it doesn't exist yet

## 3. Point the frontend at it

In your React app's `.env`, make sure the API base URL points to this server, e.g.:
```
REACT_APP_BACKEND_URL=http://localhost:8000
```
(check `frontend/src/lib` or wherever `api.js` builds its base URL — it should already be
reading from an env var like this since that's how the FastAPI version worked too).

## Endpoints (unchanged from FastAPI)

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
GET  /api/plans
GET  /api/deposits/wallets
POST /api/deposits
GET  /api/deposits
POST /api/admin/deposits/{id}      (admin only)
POST /api/withdrawals
GET  /api/withdrawals
POST /api/investments
GET  /api/investments
GET  /api/transactions
GET  /api/portfolio/summary
GET  /api/portfolio/chart
GET  /api/market/ticker
```

## Notes / things worth knowing

- **Auth**: JWT stored in an httpOnly cookie (`access_token`, `refresh_token`), same as
  before. A `Bearer` token in the `Authorization` header also works as a fallback (used by
  the register/login JSON response's `access_token` field, in case the frontend needs it
  for non-cookie contexts).
- **Passwords**: hashed with BCrypt (Spring Security's `BCryptPasswordEncoder`) — compatible
  with the bcrypt hashes your FastAPI backend already created, so **existing user passwords
  in Mongo will still work** without needing a reset.
- **JSON casing**: `application.yml` sets `spring.jackson.property-naming-strategy: SNAKE_CASE`
  globally so Java's `amountUsd` becomes `amount_usd` in JSON, matching what your frontend
  already expects from the Pydantic models. If you add new fields later, keep this in mind —
  it applies automatically to any new POJO fields, but **not** to `Map<String,Object>`
  responses, where you need to write the snake_case key literally.
- **Market ticker**: same pseudo-random deterministic simulation, refreshing every 30 seconds
  server-side (not live prices, same as your original backend — it was mocked there too).
- **Refresh tokens**: created and set as a cookie on login/register but there's no
  `/api/auth/refresh` endpoint yet — same as the original FastAPI code, which also
  issued a refresh token but never consumed it. Worth adding if you want silent
  re-auth after the 12h access token expires.
