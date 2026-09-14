# Project Review — ADRIAN-Mock (Disaster Response & Relief Network)

Reviewed: 2026-09-14
Scope: full repo (`backend/`, `frontend/`, `docs/`, `infrastructure/`, CI, README) — read-only review, no fixes applied except the two DTO constructor conflicts already fixed separately (`VerifyOtpRequest`, `ResetPasswordRequest`).

Legend: 🔴 High &nbsp; 🟠 Medium &nbsp; ⚪ Low

---

## 1. Fix first (breaks builds / onboarding / CI)

| # | Issue | File | Sev |
|---|---|---|---|
| 1 | CI has no root `package.json` but runs `npm ci`/`lint`/`build` at repo root with no `working-directory: frontend` — **every CI run fails** | `.github/workflows/ci-cd.yml` | 🔴 |
| 2 | No backend job in CI at all — the 3 existing JUnit tests never run | `.github/workflows/ci-cd.yml` | 🔴 |
| 3 | `docker-compose.yml` references `frontend/Dockerfile`, which doesn't exist — `docker-compose up --build` fails immediately | `infrastructure/docker/docker-compose.yml` | 🔴 |
| 4 | `docker-compose.yml` sets `NEXT_PUBLIC_API_URL=http://backend:8080/api/v1` but no controller uses a `/v1` prefix — API calls would 404 even if the image built | `infrastructure/docker/docker-compose.yml` | 🔴 |
| 5 | README "Quick Start" uses `cd project-nova/backend` etc. — this repo's root is `ADRIAN-Mock`, not `project-nova`; paths don't exist | `README.md` | 🟠 |
| 6 | README demo credentials (`officer@nova.org` / `Password123!`) don't match seeded accounts (`*@nova.lk` / `Demo1234`) — first thing a new dev tries fails | `README.md`, `DataSeederService.java` | 🔴 |
| 7 | `docs/api.md` documents base URL `http://localhost:8080/api/v1`; real controllers are mapped under `/api/...` with no `/v1` — every example path in the doc is wrong | `docs/api.md` | 🔴 |
| 8 | `docs/websocket-events.md` documents a full STOMP/SockJS WebSocket protocol; the codebase has no WebSocket config at all — real-time is via `SseController` (SSE). Doc is entirely stale/aspirational | `docs/websocket-events.md` | 🔴 |

## 2. Backend (Spring Boot)

**Duplication risk — Lombok `@Data` + hand-written getters/setters/constructors** (same class of bug already fixed in `VerifyOtpRequest`/`ResetPasswordRequest` — still present elsewhere; two sources of truth that can silently drift):
- `dto/ApiResponse.java` (+ nested `Meta`) — dead `@AllArgsConstructor`/`@NoArgsConstructor` imports never applied as annotations, plus fully duplicated manual accessors. 🟠
- `dto/AuthResponse.java` (+ nested `UserDto`) — `@Data @NoArgsConstructor @AllArgsConstructor` plus duplicated manual getters/setters. 🟠
- `dto/GoogleRegisterRequest.java`, `LoginRequest.java`, `OAuthExchangeRequest.java`, `OtpRequestDto.java`, `RegisterRequest.java` — redundant manual accessors/constructors alongside `@Data`. ⚪🟠

**Missing request validation** — only `AuthController` uses `@Valid`. Every other controller accepting a body has none: `AdminController`, `AIController`, `FoodSourceController`, `HospitalController`, `IncidentController`, `NotificationController`, `ReliefMissionController`, `ReliefRequestController`, `RescueTeamController`, `ResourceController`. 🔴
- `IncidentController.java` (lines 102, 112, 120, 132, 148, 161, 169, 180, 191, 202) takes raw `Map<String,Object>`/`Map<String,String>` instead of typed DTOs — no schema surface at all, relies on manual null checks. 🔴

**Security config**
- `SecurityConfig.java:56` — `GET /api/incidents/**` is `permitAll()`, exposing all incident data (locations, descriptions, possibly reporter info) with no auth. Verify this is intentional for the public map view; otherwise scope it down. 🟠🔴
- `SecurityConfig.java:51` — `/api/auth/oauth2/**` permitAll is redundant with the broader `/api/auth/**` rule on line 50. ⚪

**Secrets**
- `backend/.env` (untracked, confirmed via `git ls-files`) holds live-looking MongoDB Atlas creds, a real Gmail app password, and a Google OAuth client secret in plaintext — rotate these regardless of git-tracking status if they've ever been shared/pasted anywhere. 🟠
- `backend/.env` sets `tlsInsecure=true` on the Mongo URI — weakens cert validation. 🟠
- `infrastructure/docker/docker-compose.yml` hardcodes a real-looking `JWT_SECRET` in a committed file. 🟠

**Incomplete features**
- `ai/BedrockAIService.java:136` — `// TODO: Implement when AWS SDK is configured`. Bedrock is documented as a selectable `ADRN_AI_PROVIDER` in `.env.example`/README but not actually implemented. 🟠

**Test coverage** — only `AuthControllerTest`, `AuthServiceTest`, `IncidentFlowServiceTest` exist. Zero tests for: `AdminController`, `AIController`, `AlertController`, `AnalyticsController`, `FoodSourceController`, `HospitalController`, `NotificationController`, `ReliefMissionController`, `ReliefRequestController`, `RescueTeamController`, `ResourceController`, `RiskPredictionController`, `SseController`, and services `DataSeederService`, `EmailService`, `GoogleOAuthService`, `GridFsStorageService`, `ReliefScheduler`, `ReliefService`, `RescueTeamService`, `ResourceService`. The entire relief-logistics domain (a headline feature) is untested. 🔴

**Dependencies** — `pom.xml` (Spring Boot 3.3.4, jjwt 0.12.6, Lombok 1.18.34) is current, no known-vulnerable pins found. No `spring-data-redis` or AWS SDK dependency despite both being referenced by docs/infra. ⚪

**Stray file**
- `backend/spring-error.txt` — committed stack trace from a stale local run under an old path (`C:\Users\User\Desktop\AI Emergency Response Network\project-nova\backend`), leaking a local Windows path; looks like a one-off classpath glitch, not a live bug. Delete it. ⚪

## 3. Config

- `backend/.env` is missing `CORS_ALLOWED_ORIGINS` (present in `.env.example`); `.env.example` is missing `GOOGLE_REDIRECT_URI`, which `application.properties` actually reads. Keep both files in sync. 🟠
- `frontend/.env.local.example` doesn't document `NEXT_PUBLIC_AI_URL`, which `lib/api/client.ts:9` reads (defaults to `http://localhost:8001`). 🟠

## 4. Frontend (Next.js/TypeScript)

- `lib/mock/data.ts` — a full mock dataset with zero references anywhere in `app/`, `components/`, `lib/`, `hooks/`. Dead code; either wire it in for a demo/offline mode or delete it. ⚪
- `lib/copilot/ollama-client.ts` — hardcodes `http://localhost:11434` with no hosted-LLM fallback. Copilot will silently fail in any deployed environment (e.g. the AWS setup in `docs/aws-deployment.md`) unless a self-hosted Ollama box is stood up and `OLLAMA_BASE_URL` set. 🟠
- 58 `any`/`as any` occurrences across 20 files, worst in `app/register/page.tsx` (7), `app/citizen/tracking/[id]/page.tsx` (6), `app/relief/page.tsx` (5), `lib/store/nova-store.ts` (4), `hooks/useAudioRecorder.ts` (4). Type-safety erosion, worth tightening incrementally. ⚪🟠
- No stray `console.log`/TODO found otherwise — frontend is comparatively clean.

## 5. Docs

- `docs/api.md` — wrong base URL (`/api/v1` vs actual `/api`); example login creds don't match seed data (`officer@nova.org`/`Password123!` vs `officer@nova.lk`/`Demo1234`). 🔴🟠
- `docs/websocket-events.md` — describes an unimplemented STOMP/SockJS layer; actual mechanism is SSE (`SseController`). Needs a rewrite or removal. 🔴
- `docs/architecture.md`, `docs/aws-deployment.md` — not fully verified line-by-line, but given the pattern above (Redis, WebSockets, `/v1` all mentioned but not real) treat as partially aspirational; re-validate before using for onboarding. 🟠

## 6. Infrastructure

- `docker-compose.yml` builds a nonexistent `frontend/Dockerfile` and points the frontend at a `/api/v1` URL the backend doesn't serve — compose is currently non-functional end-to-end. 🔴
- `docker-compose.yml` includes `redis` and `localstack` services that nothing in the Spring app actually uses (no Redis client, no AWS SDK dependency) — infra/code drift. 🟠
- `infrastructure/aws/iam-policy.json` is reasonable but supports the unimplemented Bedrock path — aspirational, not yet wired to anything runnable.
- None of `infrastructure/` is referenced by `.github/workflows/ci-cd.yml` — no image build/push, no deploy job.

## 7. README

- Badge says "Spring Boot 3.2.0"; actual is 3.3.4. ⚪
- Lists Redis as part of the stack; not an actual dependency. 🟠
- Quick Start paths (`project-nova/...`) don't match this repo's actual folder name (`ADRIAN-Mock`). 🟠
- Demo credentials wrong (see §5/§6 above; same fix covers both). 🔴
- Docker Compose instructions currently fail (see §6).

## 8. Git / CI

- Last commit (`9a7f055`, merged as `c5222d0`) is a legitimate, correctly-implemented fix for an OAuth open-redirect: `handleGoogleCallback` used to accept a client-supplied `redirect_uri` and forward it straight to Google's token exchange; now it always uses the server-configured redirect URI, and `isConfigured()` rejects placeholder client secrets. Covered by a new `AuthControllerTest` assertion. No action needed — noted for context.
- `.github/workflows/ci-cd.yml` is broken as committed (§1, items 1–2) — nothing in it currently passes.

---

## Suggested priority order

1. Fix CI (`working-directory: frontend` for the npm steps; add a `mvn test` backend job).
2. Fix README/docs base URL, demo credentials, and repo path (`project-nova` → `ADRIAN-Mock`) — cheap, high onboarding impact.
3. Either implement `frontend/Dockerfile` + align `NEXT_PUBLIC_API_URL`, or clearly mark Docker Compose as "not yet functional" in the README.
4. Rewrite/remove `docs/websocket-events.md` (SSE, not WebSockets) and correct `docs/api.md` base path.
5. Add `@Valid` + typed DTOs to the controllers currently taking raw `Map` bodies (`IncidentController` first).
6. Add tests for the relief-logistics domain (currently 0% covered) before extending it further.
7. Clean up the remaining Lombok-`@Data`-plus-manual-accessors DTOs (low risk, but same bug class as the constructor conflict already fixed).
8. Rotate/tighten secrets (`.env` real credentials, hardcoded `JWT_SECRET` in `docker-compose.yml`, `tlsInsecure=true`), and re-check whether public `GET /api/incidents/**` is intended.
9. Delete `backend/spring-error.txt`.
