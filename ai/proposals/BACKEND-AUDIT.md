# Backend audit: ZPet backend and cross-app use by ZBattle

- **Status:** read-only audit plus a **proposal**. Nothing here has been deployed, applied or created.
- **Decision reference:** D-BACKEND (Q12), D-OFFLINE-TRUST (Q13), D-CONTRACT-ACCEPT (Q3) in `ZBattle/ai/integration/DECISIONS.md`.
- **Written:** 2026-10-10 (America/Toronto) by Claude (ZBattle session).
- **Method:** repository evidence only. No network calls, no Supabase or MCP tools, no database queries, no writes to either repository. The live (deployed) state of the ZPet project was **not** inspected. Where this document says "deployed", it is quoting ZPet's own notes, not something verified here.
- **Secrets:** none are reproduced. Configuration is referred to by name only (for example `BackendConfig.PUBLISHABLE_KEY`, `SUPABASE_SERVICE_ROLE_KEY`). The project reference and API URL are named as `BackendConfig.PROJECT_REF` / `BackendConfig.API_URL` and not copied.

---

## 1. Scope and evidence

### 1.1 Commits read

| Repository | Path | Commit | Access |
|---|---|---|---|
| ZPet | `/home/user/zpet` | `1adcedb` ("Build signed ZPet APK after Explore cleanup phases 1–4"). This is the only commit touching `backend/` in the local history. | read-only clone |
| ZBattle | `/home/user/ZBattle` | local HEAD `9d09592` (contract v0.2 cites main `d2f938d`) | read-only |

### 1.2 ZPet backend files (`/home/user/zpet/backend/`)

| File | Lines | What it contains |
|---|---|---|
| `schema.sql` | 315 | Base schema: 12 `public.zpet_*` tables, the `zpet_private` schema (tester role tables), RLS/grant loops, and the original `public.zpet_dispatch(actor uuid, operation text, body jsonb)` RPC with 25 operations. Header comment: "Applied only to the user's ZPet project. All mutations are service-only RPC." |
| `extensions.sql` | 23 | `zpet_competitions`, `zpet_members`, `zpet_disputes`, `zpet_coop.result` column and their RLS/grants. **Byte-identical to `schema.sql` lines 75–97**, so applying both files in sequence would fail on duplicate objects. |
| `expansion.sql` | 128 | "Phase 16–18 additions": widens partner/attacker/defender checks to 0–11; adds `zpet_ranked`, `zpet_rank_matches`, `zpet_rank_archive`; adds `public.zpet_rank_round(int,int,int,int)`; renames the original RPC to `public.zpet_dispatch_core` and creates a new wrapper `public.zpet_dispatch` with six extra operations. |
| `ranked-hardening.sql` | 94 | "Apply only to an existing expansion deployment." `create or replace` of `zpet_rank_round` and the `zpet_dispatch` wrapper. Its bodies are **identical** to the corresponding bodies in `expansion.sql` (diff shows only `create` vs `create or replace`). ZPet notes say this patch was **not** deployed (see 1.4). |
| `checks.sql` | 52 | Transactional SQL fixture (ends in `rollback`) run as `service_role`: save revision CAS, cross-user save isolation, tester escalation rejection, friend/rival consent, battle ownership/replay, duo replay, competitions, block cleanup, sticky step quarantine. |
| `ranked-checks.sql` | 40 | Transactional ranked fixture: invalid/partial team rejection, `request_id` replay, pair cooldown, exhaustive 1,296-case round symmetry, season archive, and a check that `authenticated`/`anon` cannot execute `zpet_dispatch` / `zpet_rank_round`. |
| `index.ts` | 18 | The Edge Function source (Deno). The client calls it at path `/functions/v1/zpet-api`, so its deployed slug is `zpet-api`. |

Not found in repo: `supabase/config.toml`, a `supabase/functions/` tree, migration history, the Edge Function's JWT-verification setting, storage bucket definitions or policies, Auth provider settings, and any CORS handling.

### 1.3 ZPet Android client files

| File | What it contains (backend-relevant) |
|---|---|
| `app/src/main/java/com/zeus97x/zpet/BackendConfig.java` | Constants `PROJECT_REF`, `PROJECT_NAME`, `API_URL`, `PUBLISHABLE_KEY`. Comment: "Public endpoint and publishable key only. Privileged keys stay on the server." No service key is present. |
| `app/src/main/java/com/zeus97x/zpet/CloudClient.java` | HTTPS client (`HttpURLConnection`, 15 s connect / 20 s read, 2.5 MB response cap). Auth calls: `/auth/v1/token?grant_type=password`, `/auth/v1/signup`, `/auth/v1/recover`, `/auth/v1/verify` (`type=recovery`), `PUT /auth/v1/user`, `/auth/v1/token?grant_type=refresh_token`, `/auth/v1/logout?scope=local`. Game calls: `call(operation, body)` → `POST /functions/v1/zpet-api` with `{"operation":…, "body":…}` and `Authorization: Bearer <access token>` + `apikey: <publishable key>`. The session JSON is encrypted with AES-GCM using an Android Keystore key under alias `zpet.session.v1`, stored in SharedPreferences `zpet.cloud.v1`. The token is refreshed when it has less than 60 s left. |
| `app/src/main/java/com/zeus97x/zpet/MainActivity.java` | All `cloud.call(...)` / `cloudAction(...)` sites. Revision key `cloud.revision.<userId>.<normal|test>` (line 871). `save_put` / `save_get` (874–888). `steps_put` loop over `steps.competitionDays()` for the last 7 UTC days (1009–1019). `rank_match` with a persisted `request_id` UUID under key `rank.request.<userId>` (1072–1088). `battle_play` / `coop_play` with saved move drafts (1093–1099). |
| `app/src/main/java/com/zeus97x/zpet/RankedRules.java` | `SERVER_VALIDATION_READY=false`. Ranked team edits and matchmaking are blocked in the phone UI (MainActivity 1033, 1051, 1073). |
| `app/src/main/AndroidManifest.xml` | `INTERNET`, `ACTIVITY_RECOGNITION`, `health.READ_STEPS`, notification and foreground-service permissions. |

### 1.4 ZPet documentation consulted

| File | Backend-relevant content |
|---|---|
| `AGENTS.md` | "backend/: schema/function references and proposed ranked validation patch. Do not rerun full schema against the existing project." Never record secrets. |
| `HANDOFF.md` (lines 15, 36) | Same warning. Live ranked phone actions stay disabled until the reviewed patch is "specifically authorized, deployed and verified. No backend work is authorized by this handoff/build request." |
| `BLUEPRINT.md` line 11 | "All ZPet backend resources belong to the existing Supabase project ZPet … Do not create another backend project or use another app's project." |
| `BLUEPRINT.md` lines 68, 74–78 | Offline ledger, upload with unique IDs, "Backend validates rewards, evolution and competitive outcomes", two-user separation tests, guest/local saves need a defined merge (no silent overwrite). The entity list on line 74 is described as "a conceptual inventory, not a migrated SQL schema". |
| `BLUEPRINT.md` lines 259, 270–294 | Phase 13 integrity summary. "Trustworthy step attestation" is listed as **missing**. Twelve public tables have RLS enabled with no client grants. "Anonymous HTTP calls to the deployed function return 401. Authenticated Edge HTTP and app sign-in remain untested." Auth leaked-password protection is **disabled**. "No claim of production-ready competitive security." |
| `audit/CODING-REVIEW-2026-10-08.md` lines 20–36 | The ranked hardening patch is "awaiting approval": deployment was rejected by automatic review and no production ranked function changed. |
| `audit/NIGHT-REVIEW.md` lines 20–22, 53–54 | A ranked foundation was deployed earlier. The local ranked SQL was tightened after deployment, so "the current source is not proof of the deployed null-input behavior". |
| `audit/USER-TESTS-DEFERRED.md` lines 26–30 | "Approve the concrete ranked validation deployment in backend/ranked-hardening.sql … No deployment occurred." |
| `audit/UI-PHASES-1-3.md` line 74 | "Usernames did not work during user testing." Deferred; not reproduced. |

### 1.5 ZBattle context

| File | Relevant content |
|---|---|
| `ai/integration/CONTRACT-v0.2.md` | Envelope (`accountId`, `eventId`, `sourceApp`, `recordType`, `recordId`, `recordRevision`, …), acceptance rules R1–R12 and their codes, inbox states, `verification` = `unverified-client` / `server-settled`. Status: PROPOSED, not deployed. |
| `ai/integration/AUDIT-A1.md` | ZBattle has no network permission and no backend client. ZPet identity, steps and cloud-save findings (G1–G8). |
| `ai/integration/DECISIONS.md` | D-BACKEND, D-OFFLINE-TRUST, D-CONTRACT-ACCEPT, D-PARTICIPATION, D-WEEK-WINDOW. |
| `ai/CROSS_APP_ROADMAP.md` | Phase D (CLAUDE-007) needs "backend approval". "No trust guarantees from client IDs alone." |
| ZBattle manifests | `grep uses-permission` over ZBattle's `AndroidManifest.xml` files finds **no permissions at all**, so there is no `INTERNET`. |

---

## 2. Current architecture (as written in the repo)

### 2.1 Request path

```
ZPet phone (CloudClient)
  ├─ Supabase Auth REST (/auth/v1/*)  ← publishable key only
  └─ POST /functions/v1/zpet-api  {operation, body}   Bearer <user access token>
        Edge Function index.ts (Deno, server-side)
          ├─ createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)
          ├─ auth.getUser(token)  → require user.email_confirmed_at
          └─ rpc('zpet_dispatch', {actor: user.id, operation, body})
                public.zpet_dispatch (wrapper, SECURITY INVOKER, runs as service_role)
                  ├─ rank_status / rank_team / rank_match / rival_history /
                  │  moderation_list / moderation_review  (handled in wrapper)
                  └─ everything else → public.zpet_dispatch_core (original RPC)
```

### 2.2 Auth

| Aspect | Evidence |
|---|---|
| Provider | Supabase Auth, email + password (`/auth/v1/signup`, `grant_type=password`). No OAuth, username or anonymous sign-in found in the client. |
| Email confirmation | Required by the Edge Function: `if(error||!user||!user.email_confirmed_at) … 401 'Verified sign-in required'`. |
| Identity used by the database | `actor` = `user.id` from `auth.getUser(token)`. The client never sends a user id for itself. |
| Session storage | AES-GCM, Android Keystore alias `zpet.session.v1`, prefs `zpet.cloud.v1`. |
| Password recovery | The client validates that the pasted link is `https://<PROJECT_REF>.supabase.co/auth/v1/verify?type=recovery`, then calls `verify` and `PUT /auth/v1/user`. Minimum password length is 8 (client-side check). |
| Tester role | `zpet_private.tester_users(user_id)`, seeded once in `schema.sql` from a single hard-coded, confirmed email (the owner's; not reproduced here). On every call, `zpet_dispatch_core` sets `zpet_profiles.tester = exists(select 1 from zpet_private.tester_users …)`. `zpet_private.tester_emails` is seeded but is not read by any function. |
| Leaked-password protection | Disabled, per `BLUEPRINT.md` line 294 (a project setting, not verifiable from source). |

### 2.3 Tables

All tables are in `public` unless noted. **RLS policy column:** RLS is **enabled on every table and no `create policy` statement exists anywhere in the repo.** Combined with revoked grants, `anon` and `authenticated` have no row access. `service_role` bypasses RLS (Supabase default role attribute; not set in this repo).

| Table | Columns (constraints) | Grants |
|---|---|---|
| `zpet_profiles` | `user_id uuid PK → auth.users on delete cascade`; `code text unique` (12 upper-hex chars from `gen_random_uuid()`); `name text` 1–24, default `'Traveller'`; `visibility` ∈ `private`/`bands`/`exact`; `partner int` 0–11; `form int` 0–5; `tester bool`; `created_at` | revoke all from `anon, authenticated`; grant all to `service_role` |
| `zpet_saves` | PK `(user_id, slot)`; `slot` ∈ `normal`/`test`; `revision bigint` default 1; `payload text not null`, length ≤ 2,000,000; `updated_at` | same |
| `zpet_links` | PK `(a,b)` with `a<b`; `requester uuid`; `status` ∈ `pending`/`accepted`/`blocked` | same |
| `zpet_rivals` | `user_id PK`, `rival_id unique`, `user_id<>rival_id` | same |
| `zpet_invitations` | `id uuid PK`; `sender`, `recipient`; `kind` ∈ `rival`/`coop`; `status` ∈ `pending`/`accepted`/`declined`; `created_at` | same |
| `zpet_steps` | PK `(user_id, day date)`; `steps bigint` 0–100,000; `source` ∈ `phone`/`health`; `flagged bool`; `updated_at` | same |
| `zpet_battles` | `id uuid PK`; `sender`, `recipient`; `attacker`, `defender` int 0–11 (widened in `expansion.sql`); `status` ∈ `pending`/`accepted`/`declined`/`complete`; `result jsonb`; `created_at` | same |
| `zpet_coop` | `id uuid PK`; `a`, `b`; `starts date`; `baseline bigint`; `goal bigint` default 10,000; `claimed bool`; `result jsonb` (added later) | same |
| `zpet_audit` | `id bigint identity PK`; `user_id`; `action text`; `created_at`. Index `(user_id, created_at)`. | same, plus `usage, select` on `zpet_audit_id_seq` to `service_role` |
| `zpet_competitions` | `id uuid PK`; `owner`; `title` 1–40; `starts`, `ends` (`ends>starts`) | same |
| `zpet_members` | PK `(competition, user_id)`; `status` ∈ `invited`/`accepted`/`declined`; `baseline bigint` | same |
| `zpet_disputes` | `id uuid PK`; `user_id`; `day`; `note` 5–500; `created_at`; `status` ∈ `pending`/`reviewed`; `unique(user_id, day)` | same |
| `zpet_ranked` (expansion) | `user_id PK`; `season date`; `rating int` 100–3000, default 1000; `team jsonb` and `stances jsonb` (array, length 3); `opted bool`; `wins`, `played` | revoke all from `public, anon, authenticated`; grant all to `service_role` |
| `zpet_rank_matches` (expansion) | `id uuid PK`; `a`, `b` (`a<>b`); `season`; **`request_id uuid not null`, `unique(a, request_id)`**; `result jsonb`; `created_at` | same |
| `zpet_rank_archive` (expansion) | PK `(user_id, season)`; `rating`, `wins`, `played` | same |
| `zpet_private.tester_emails` | `email text PK` | schema usage revoked from `public, anon, authenticated`; `usage` on the schema granted to `service_role`; no table grant; RLS on |
| `zpet_private.tester_users` | `user_id uuid PK → auth.users` | `select` to `service_role`; RLS on |

`BLUEPRINT.md` says "Twelve public tables have RLS enabled". The repo defines **15** public tables once `expansion.sql` is included, so that count predates the ranked tables.

### 2.4 Functions / RPCs

None of the functions is `SECURITY DEFINER`.

| Function | Signature | Security | Who can execute (per repo) |
|---|---|---|---|
| `public.zpet_dispatch` (wrapper, `expansion.sql` / `ranked-hardening.sql`) | `(actor uuid, operation text, body jsonb default '{}') returns jsonb`, plpgsql, `set search_path=''` | `security invoker` | revoke all from `public, anon, authenticated`; grant execute to `service_role` |
| `public.zpet_dispatch_core` (the original `schema.sql` function, renamed by `expansion.sql`) | same signature | `security invoker` | inherits the original ACL (revoked from `public, anon, authenticated`; execute to `service_role`). `ranked-checks.sql` does **not** check this function's grants. |
| `public.zpet_rank_round` | `(a int, sa int, b int, sb int) returns int`, `immutable`, `set search_path=''` | invoker (default) | revoke from `public, anon, authenticated`; execute to `service_role` |

**Common prologue of `zpet_dispatch_core`, run on every operation:**
1. `actor` must not be null.
2. `pg_advisory_xact_lock(hashtextextended(actor::text,0))` serialises all calls for one player.
3. A profile row is upserted, `tester` is refreshed from `zpet_private.tester_users`, and the row is locked `for update`.
4. **Rate guard:** more than 120 `zpet_audit` rows for this actor in the last minute raises 'Too many requests'.
5. One `zpet_audit(user_id, action)` row is inserted per call. No retention or cleanup was found.

The wrapper takes a global `pg_advisory_xact_lock(731840016)` for `rank_%` operations, then calls the core `profile` operation (which writes an audit row recorded as `profile`, not the ranked operation name).

**Operations:**

| Group | Operations (exact names) | Called by the ZPet phone? |
|---|---|---|
| Profile | `profile` | yes |
| Saves | `save_get`, `save_put` | yes |
| Friends | `friend_request`, `friend_accept`, `friend_remove`, `friend_block`, `dashboard` | yes |
| Invites | `rival_invite`, `coop_invite`, `battle_invite`, `invite_accept`, `invite_decline`, `rival_end` | yes |
| Friend battle | `battle_accept`, `battle_decline`, `battle_play` | yes |
| Steps | `steps_put`, `improvement`, `league`, `dispute` | yes |
| Competitions | `competition_create`, `competition_reply`, `competitions` | yes |
| Duo | `coop_play` (yes), `coop_claim` (**no** client call found) | partly |
| Ranked (wrapper) | `rank_status`, `rank_team`, `rank_match` | UI present but gated off by `RankedRules.SERVER_VALIDATION_READY=false` (`rank_status` is still callable) |
| Wrapper extras | `rival_history`, `moderation_list`, `moderation_review` | **no** client call found |

### 2.5 Edge Function `index.ts` (slug `zpet-api`)

1. Rejects anything other than `POST` (405).
2. Reads `Authorization`, strips `Bearer `, and returns 401 if it is missing.
3. Creates a server-side client from env `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` (no session persistence).
4. `client.auth.getUser(token)`: Auth validates the token. The function returns 401 unless a user exists **and** `email_confirmed_at` is set.
5. If the raw body is over 2,100,000 characters, returns 413.
6. Parses `{operation, body={}}`. `operation` must be a string and `body` a non-null, non-array object, or it throws and returns 400 "Request could not be processed".
7. `client.rpc('zpet_dispatch', {actor: user.id, operation, body})`. A database error message is returned verbatim as `{error: message}` with status 400.
8. Returns `result.data` as JSON.

It has no per-operation allow-list (unknown operations are rejected by the RPC's `else raise 'Unknown operation'`), no CORS headers, no app attestation, no request id and no logging beyond the database audit row.

### 2.6 How the ZPet client uses it

- `CloudClient.call(op, body)` runs on a single-thread executor. Only one `cloudAction` runs at a time (`cloudBusy`).
- **Saves:** `save_put{slot, payload=world().encode(), revision=<last known>}`. On `conflict` the client keeps the local copy and shows it for review; it does not merge. `save_get` restore asks for confirmation and keeps a local recovery copy.
- **Steps:** for each of the last 7 UTC days, `steps_put{day, steps, source = health|phone}`. Sandbox (test) mode is blocked client-side.
- **Ranked:** a `request_id` UUID is generated, persisted **before** sending, reused on retry and removed only after a response with an `id`.
- **Friend/duo battles:** move drafts are saved locally under `battle.draft.<user>.<id>` and submitted as a `moves` array.

---

## 3. Actual validation (server side)

Key:
- **Validated** means the server rejects the input or recomputes it from server-owned state.
- **Stored as claimed** means the server persists or uses the value without any means of checking it against reality.

Every row assumes the Edge Function step: the caller holds a valid, email-confirmed Supabase session, and `actor` is the verified user id. That proves *who* is calling. It does **not** prove that the device data is true.

| Entry point | Validated server-side (exact check) | Stored / used as claimed (not verifiable by the server) |
|---|---|---|
| Edge `zpet-api` | POST only; Bearer token valid via `auth.getUser`; `email_confirmed_at` present; body ≤ 2,100,000 chars; `operation` string; `body` a plain object | Which app or build sent the request. Any holder of the user's token can call it; there is no app attestation. |
| `profile` | `name` 1–24 after `trim` (table check); `visibility` ∈ `private/bands/exact` (table check); `partner` 0–11, `form` 0–5 (table checks; a non-integer cast raises) | `partner` (family) and `form` are **client-asserted**: there is no check that the player owns that companion or reached that form. A `visibility`-only update is silently ignored, because the `update` runs only if `name`, `partner` or `form` is present. |
| `save_get` | `slot` `test` requires the `tester` role; reads only `user_id = actor` | — |
| `save_put` | `slot` ∈ `normal/test`; `test` requires tester; payload length ≥ 8 (RPC) and ≤ 2,000,000 (table check); **revision compare-and-swap**: the stored revision must equal `body.revision`, otherwise `{conflict:true, revision}` | **The entire `payload` is opaque text.** Companions, forms, XP, inventory, captures and adventure progress inside it are not parsed or validated. A save is a backup, not a verified record. ZPet's own notes state that saves never feed leaderboards or battle stats (`BLUEPRINT.md` line 270). |
| `friend_request` | The code must match an existing profile and not be self; existing link returned (replay safe) | — |
| `friend_accept` / `friend_remove` / `friend_block` | Not self; accept requires a `pending` link where the actor is not the requester; remove/unblock is refused if the *other* user blocked; rivals cleaned up | — |
| `rival_invite` / `coop_invite` / `battle_invite` | Accepted friendship; no duplicate pending invite/challenge | `battle_invite` snapshots `attacker = me.partner` and `defender = other.partner`, which are **client-asserted** profile values. |
| `invite_accept` / `invite_decline` | The actor is the recipient; status `pending`; friendship still accepted; one rival bond at a time; one open duo per pair; the duo `baseline` is the server sum of today's unflagged steps | The baseline is built from client-reported steps. |
| `rival_end` | Deletes only the actor's own bond | — |
| `battle_accept` / `battle_decline` | The actor is the recipient; status `pending`; friendship accepted | — |
| **`battle_play`** | Only the sender plays; status `accepted`; `moves` is an array of 1–30 entries ∈ `strike/guard/skill`; `skill` only on turns 1, 4, 7, …; the plan must finish the fight (or reach 30 turns); **the server simulates the fight** (fixed HP 100/100, fixed enemy pattern, affinity from `attacker%3`/`defender%3`); once `complete`, the stored result is returned (replay safe) | The battle is **equalised and deterministic**: the outcome depends only on the move list and two client-asserted partner families. The recipient never plays and the enemy pattern is fixed, so the result is server-computed but not adversarial. No companion stats, level or ownership is involved. |
| **`steps_put`** | Testers rejected; `day` within `[UTC today − 6, UTC today]`; `0 ≤ steps ≤ 100000`; `source` ∈ `phone/health`; **heuristic flag**: `steps > 60000` or an increase greater than `max(3000, seconds since last update × 5)` over the stored value; the flag is sticky (`flagged = old OR new`) | **The step count is client-reported.** `source` is a client-declared label; the server cannot tell Health Connect from the phone counter. A plausible value under the thresholds is accepted unflagged. Values can also be lowered (overwritten). The response says `provisional: true`. ZPet lists "trustworthy step attestation" as missing (`BLUEPRINT.md` line 259). |
| `dispute` | A flagged `zpet_steps` row must exist for that day; `note` 5–500 (table check) | The note text |
| `competition_create` | Testers rejected; `friends` array of 1–10 accepted, non-tester friends; `title` 1–40 (table check); dates from server UTC; owner baseline from server sum | The scores are sums of client-reported steps. |
| `competition_reply` | Competition not ended; actor not a tester; `reply` ∈ `accepted/declined`; actor invited; friendship with owner | Baseline from client-reported steps |
| `competitions`, `league`, `improvement`, `dashboard`, `rival_history` | Read-only; scoped to the actor and accepted friends; respects `visibility` (exact, banded to 1,000, or hidden); excludes flagged days and testers | Every number shown is derived from client-reported steps. `league` returns `rewards_enabled: false`. |
| **`coop_play`** | Actor is a participant; friendship accepted; no testers; replay returns the stored result; **gate**: the server-summed unflagged steps since the baseline must be ≥ `goal`; moves validated (1–30, `skill` cooldown); server simulates the boss (HP 150 vs 160) | The step gate relies on client-reported steps. The reward is cosmetic only (`'Storm & Rune · duo badge'`). |
| `coop_claim` | Same participant/friend/tester checks; requires a stored `result` | — (no client call found) |
| `rank_team` (wrapper) | Testers rejected; `team` and `stances` must be arrays of length 3; each team element a JSON number matching `^(0|[1-9]|10|11)$`; each stance a number matching `^[0-2]$`; `opted` must be boolean if present; `zpet_rank_round` rejects nulls/out-of-range | The team choice is free by design ("Walking, rarity and save stats do not change ranked power"). **The deployed version may differ.** ZPet notes say the hardened checks shown here were not deployed. |
| **`rank_match`** (wrapper) | `request_id` required (UUID cast); **idempotent**: an existing `(a=actor, request_id)` returns the stored result; opt-in required; ≤ 5 matches per UTC day for both players; opponent opted in, non-tester, same season, rating within ±250, not blocked, no match in 24 h; server resolves three `zpet_rank_round` rounds and Elo (K = 24, clamp 100–3000) | Fully server-settled from server-held teams. The request id is client-generated, but it is used only for deduplication. Live use is gated off in the phone. |
| `rank_status` | Season roll-over and archive; reads the actor's own row | — |
| `moderation_list` / `moderation_review` | Tester role required (re-derived per call from `zpet_private.tester_users`) | `moderation_review` takes any dispute id. `moderation_list` shows all users' dispute notes (without user ids) to testers. |

### 3.1 Summary for Q13

| Data | Truth level today |
|---|---|
| Who is calling | **Verified** (Supabase Auth + confirmed email) |
| Step counts (`steps_put`) | **Client-reported.** The server only range-checks them and flags implausible ones. Nothing attests the counts. |
| Local adventure battles, captures, XP, evolution, inventory | **Not sent as records at all.** They exist only inside the opaque `zpet_saves.payload` (client state, CAS-protected backup). |
| Friend battle / duo boss outcomes | **Server-computed** from validated move lists, but the inputs (`partner`, and the step gate for duo) are client-asserted, and the combat is a simplified equalised model unrelated to ZPet's local stats. |
| Ranked match outcomes | **Server-settled** from server-held teams with idempotent `request_id`. Not in live use. |
| Anything from ZBattle | **No path exists.** ZBattle has no network permission or backend client. |

Conclusion for D-OFFLINE-TRUST: a ZPet or ZBattle phone report reaching this backend today would, at best, be **authenticated and stored**. It would not be **verified**. Under the contract, records derived from steps, local battles, captures or evolution must carry `verification: "unverified-client"` and stay pending.

---

## 4. Gaps for cross-app use

| # | Gap | Evidence | Impact on contract v0.2 |
|---|---|---|---|
| X1 | **ZBattle has no accounts or network.** | No `uses-permission` in ZBattle manifests; AUDIT-A1 §1 | `accountId` (R1) cannot be produced by ZBattle yet. |
| X2 | **No shared-record storage.** There are no inbox/outbox, event, record-revision or redemption tables. | Tables in §2.3 | R6, R7, R8, R10–R12 and the inbox states have nowhere to live. |
| X3 | **No generic idempotency.** Only `zpet_rank_matches.unique(a, request_id)`. Other operations are replay-safe through state (CAS revision, `status='complete'`, `result is not null`, duplicate-pending guards), not through an event id. | `expansion.sql` line 20 | R6 (`accountId + eventId`, canonical-payload compare) is not implemented anywhere. The `request_id` pattern is a usable precedent. |
| X4 | **No per-record revision.** Only `zpet_saves.revision` (whole-save CAS) exists. | `schema.sql` line 13 | R7, R8 and R10 need `(accountId, recordType, recordId) → authoritativeRevision`. |
| X5 | **No server catalogue.** Species, forms, areas and encounters live only in app code (`SpeciesCatalog.java`, ZBattle `Creatures.kt` / `Regions.kt`). | Not found in repo backend | R9 (`REJECT_CATALOGUE`) cannot be checked server-side without a catalogue table or embedded constants. |
| X6 | **Producer identity is not authenticable.** Both APKs would hold the same kind of user JWT and publishable key. The server cannot tell which app sent a request. | `index.ts` has no attestation | R4 (`sourceApp` may produce `recordType`) can only check the *declared* `sourceApp`. Integrity attestation is not present in either repo. |
| X7 | **Step and battle truth.** Steps are client-reported; local battles are not reported at all. | §3 | RewardEarned (step-derived) and BattleCompleted are `unverified-client` until an authority exists (D-OFFLINE-TRUST). |
| X8 | **Two-account isolation is tested only for saves.** `checks.sql` line 13 checks that B cannot read A's save. All reads are keyed by `actor`. There is no fixture for authenticated HTTP. | `BLUEPRINT.md` line 290: "Authenticated Edge HTTP and app sign-in remain untested" | New shared-record tables need their own two-account fixtures, including "the same `eventId` under two accounts". |
| X9 | **Guest/offline linking is undefined.** ZPet uses per-user prefs keys (`cloud.revision.<user>.<slot>`), but local game state is not bound to an account. `BLUEPRINT.md` line 76 requires a defined merge. | MainActivity 871 | A ZBattle guest progress → account flow must be designed (§5.4). |
| X10 | **Anonymous accounts are rejected.** `index.ts` requires `email_confirmed_at`. | `index.ts` line 10 | A "guest cloud account" via anonymous sign-in would not work with the existing gate. |
| X11 | **Service key exposure risk.** Today the service key is only an Edge env var (`SUPABASE_SERVICE_ROLE_KEY`). The ZPet APK contains only `BackendConfig.PUBLISHABLE_KEY`. Risks for cross-app use: (a) embedding any privileged key in ZBattle; (b) a separate project needing server-to-server calls, which puts a privileged key for project A into project B's runtime; (c) Edge error passthrough (`result.error.message`) reveals database exception text. | `index.ts`, `BackendConfig.java` | Keep the "service-only RPC behind a verified-Auth Edge Function" pattern. Never ship privileged keys. |
| X12 | **Unverifiable deployed state.** The deployed ranked functions predate the hardening. The repo files are references, not migrations ("Do not rerun full schema"). `extensions.sql` duplicates `schema.sql`. | AGENTS.md, HANDOFF.md, NIGHT-REVIEW.md | Any future change needs a read of the live schema (not done here) and a migration file, not a rerun of `schema.sql`. |
| X13 | **Rate/audit coupling.** A shared 120-calls/minute limit per actor counts every `zpet_audit` row. Cross-app calls routed through `zpet_dispatch` would share ZPet's budget. The audit table has no retention policy. | `schema.sql` line 119 | A separate dispatcher should have its own limit and audit stream. |
| X14 | **Auth hardening is incomplete.** Leaked-password protection is disabled; sign-in from a real device is untested; "usernames did not work" is user-reported. | `BLUEPRINT.md` lines 294, 300; `UI-PHASES-1-3.md` line 74 | ZBattle accounts would inherit these until they are fixed. |
| X15 | **The tester role is seeded once from a literal email.** New testers need a manual insert. The owner's email literal is committed in `schema.sql`. | `schema.sql` lines 58, 62 | A cross-app tester role would need the same service-only table. Moving the email literal out of source is advisable. |
| X16 | **Policy conflict.** ZPet `BLUEPRINT.md` line 11 says the ZPet project should not be used by "another app" (worded as a rule for ZPet). | `BLUEPRINT.md` line 11 | Option A (below) needs an explicit owner decision overriding or clarifying this line. |

---

## 5. Recommendation

> **Proposal — not deployed; needs Zeus97x approval (D-BACKEND).** No SQL in this section is to be applied. Table and function names are placeholders for discussion.

### 5.1 Options

| | (A) Reuse ZPet's Supabase project, with new isolated tables/RPCs for contract v0.2 records | (B) A separate Supabase project for ZBattle / shared records |
|---|---|---|
| Account identity | **One Auth user pool.** The same email account signs into both apps, and `accountId` = `auth.users.id` in both, so R1 works directly. | Two user pools. The same person has two UUIDs. Needs an account-link table plus a cross-project proof (for example, project A verifies a token from project B). |
| Cross-app records | Both apps write to one database. Inbox/outbox are plain tables, and redemption is one transaction (contract §5). | Records must cross projects through server-to-server calls. Atomic redemption across two databases is not possible; it needs sagas. |
| Secret exposure | One privileged key, kept in Edge env only (as today). | Each project's Edge runtime needs a privileged credential for the other, which doubles the exposure (X11b). |
| Blast radius | ZBattle bugs or load share ZPet's database. Mitigated by separate tables, a separate dispatcher, a separate Edge Function and separate rate limits. | Fully isolated. |
| Existing guidance | Conflicts with the wording of `BLUEPRINT.md` line 11 (X16); needs an owner override. | Consistent with line 11, but contradicts "one account across apps". |
| New resources | New tables, RPC and Edge Function in an existing project | A whole new project, plus everything in A |

### 5.2 Recommended: option (A), with strict isolation

Reasons:
1. Contract R1 and the dedup scope (`accountId + eventId`) assume a single account namespace. Option A gives that without federation.
2. Contract §5 requires redemption to be **one transaction**. That is only straightforward in one database.
3. It keeps the proven pattern: Edge verifies Auth, then a service-only, `security invoker` RPC with all client grants revoked and RLS on. No new kind of secret is introduced.
4. Isolation rules, to address X13, X16 and the blast radius:
   - a separate table prefix (for example `xapp_`);
   - a separate service-only dispatcher (for example `public.xapp_dispatch(actor, operation, body)`), **not** new branches inside `zpet_dispatch`;
   - a separate Edge Function slug (for example `xapp-api`);
   - its own rate limit and audit table.
   ZPet tables are not readable or writable from the cross-app RPC. The one exception is a narrowly reviewed read of `zpet_profiles.user_id` for existence.
5. The ZBattle APK would gain `INTERNET` and a client modelled on `CloudClient.java`: email/password, Keystore-encrypted session, publishable key only.

Preconditions before any deployment:
- Zeus97x approves D-BACKEND option A and clarifies `BLUEPRINT.md` line 11.
- A read-only inspection of the live schema confirms it matches the repo.
- ZPet Claude accepts contract v0.2 (D-CONTRACT-ACCEPT).
- Leaked-password protection is enabled.
- Authenticated-HTTP two-account fixtures exist.

### 5.3 Verification level per record (proposal)

Three levels, so that "the server received it" is never mistaken for "verified" (Q13):
- **client-claimed:** authenticated and stored; contract `verification: "unverified-client"`.
- **server-accepted:** passed R1–R12 (schema, producer, revision, idempotency). Still `unverified-client`.
- **server-settled:** the outcome is computed by the server from server-owned state, randomness or time; contract `verification: "server-settled"` with `settlementRef`.

| Record | Proposed level | Why |
|---|---|---|
| CompanionSnapshot | client-claimed → server-accepted | The origin app's local state (from the opaque save). The server can enforce monotonic form (R11) and bond revision (R8), but cannot prove progress. |
| BattleCompleted (ZBattle) | **unverified-client** | Battles run locally in ZBattle's Kotlin engine. The server can verify only if it re-simulates with server-issued seeds and inputs (not available). Expedition participation credit (D-PARTICIPATION) stays **pending** until that, or another approved authority, exists. |
| RewardEarned (ZPet) | unverified-client at the source; server-accepted for delivery | Usually derived from client-reported steps (§3). Deduplicated by `rewardId`. Visible in the inbox as pending, not spendable as "verified". |
| RewardRedeemed (ZBattle) | **server-settled** (proposed) | The redemption roll, inventory grant and "at most once" rule (R12) can run entirely in one server transaction with server randomness. The *value* redeemed is still only as trustworthy as the RewardEarned behind it. |
| BossState timers (D-WEEK-WINDOW) | server-settled for **time** only | The server clock can anchor the 168-hour window and 4-hour cooldown. The victory that starts it is client-claimed. |
| ExpeditionSnapshot, LineageSnapshot | client-claimed | Display only |
| Ranked (existing ZPet) | server-settled | Already true for `rank_match` (not in live use) |

Offline rule (Q13): both apps keep local progress offline. Cross-app effects stay in the local outbox as `pending`, and the consumer shows them as pending until the authority returns `ACCEPTED` (and `redeemed` where relevant).

### 5.4 Guest → account linking (proposal)

1. **Default is guest:** ZBattle works fully offline with no account (as today). Local records are written to a local outbox with `accountId = null`. No cross-app effect happens while the user is a guest.
2. **Sign-in:** this uses the same email/password Supabase account as ZPet. Anonymous sign-in is **not** proposed, because `zpet-api` rejects unconfirmed users (X10) and it would create orphan accounts.
3. **First link on a device:** the user explicitly confirms "Attach this device's progress to <email>". Local state is then bound to that `accountId` (stored per account, like ZPet's `cloud.revision.<user>.<slot>`). Pending outbox records are stamped with `accountId` and **re-signed with new `eventId`s only if they were never sent**. Already-sent records keep their ids.
4. **Different account later:** no merge. The other account's local profile is kept separately. Nothing is silently overwritten (`BLUEPRINT.md` line 76).
5. **Account already holds cross-app data:** the server's records are authoritative for cross-app state (inbox, redemptions). Local guest battle history uploads as `unverified-client` and never re-earns rewards already redeemed (R6, R12).
6. **No credential sharing between APKs.** Each app signs in on its own. There is no shared token store.

### 5.5 Minimal resource plan (PLAN only, no SQL, not to be applied)

> **Proposal — not deployed; needs Zeus97x approval (D-BACKEND).**

**Tables.** All service-only: RLS enabled, no policies, `revoke all from public, anon, authenticated`, grants to `service_role` only, following the existing pattern.

| Placeholder name | Purpose | Key / uniqueness |
|---|---|---|
| `xapp_events` | Every submitted envelope and its result code (R2–R12) | PK `(account_id, event_id)`; canonical payload hash for `DUPLICATE_IGNORED` vs `REJECT_REPLAY_MISMATCH` |
| `xapp_records` | Latest accepted payload and `authoritative_revision` per record | PK `(account_id, record_type, record_id)` |
| `xapp_inbox` | Reward delivery state machine `pending` / `accepted` / `rejected` / `redeemed` / `acknowledged`, with `failure_code`, `failure_retryable` | PK `(account_id, reward_id)` |
| `xapp_redemptions` | Atomic redemption outcome (one roll) | unique `(account_id, delivery_id)` and `(account_id, reward_id)` |
| `xapp_settlements` | Server-settled facts (`settlementRef`), for example boss window anchors | PK `settlement_id` |
| `xapp_devices` | Pseudonymous `sourceDeviceId` per account (no hardware ids) | PK `(account_id, source_device_id)` |
| `xapp_catalogue` (or constants inside the RPC) | Species/form/area/encounter ids for R9 | catalogue ids |
| `xapp_audit` | Separate audit and rate-limit stream | `(account_id, created_at)` index |

**Functions.**

| Placeholder | Security | Execute grant | Operations |
|---|---|---|---|
| `public.xapp_dispatch(actor uuid, operation text, body jsonb)` | `security invoker`, `search_path=''` | `service_role` only | `submit` (one envelope → R2…R12 code); `inbox_list`; `redeem` (one transaction: consume, roll, grant, store); `acknowledge`; `records_get` (own account only) |

**Edge Function.** Placeholder slug `xapp-api`, a copy of the `index.ts` pattern:
- POST only;
- verified, email-confirmed user;
- `actor = user.id`, so `accountId` in the envelope must equal the actor (R1);
- size limit;
- generic error mapping (no raw database messages) to close X11c.

**Fixtures before any deploy:**
- the contract `fixtures/manifest.json` sequences (S1–S12) run against `xapp_dispatch` in a rolled-back transaction;
- two-account isolation (the same `eventId` under two accounts);
- grant checks (`anon` / `authenticated` cannot execute);
- an authenticated HTTP test against a non-production branch.

**Explicitly out of scope:** changes to `zpet_*` tables or functions, the ranked hardening deploy, storage buckets, and any service key in either APK.

---

## 6. Open questions

| # | Question | For |
|---|---|---|
| OQ1 | Approve option A (reuse the ZPet project with isolated `xapp_*` resources) or B? If A, how should `BLUEPRINT.md` line 11 ("do not … use another app's project") be reworded? | Zeus97x (D-BACKEND) |
| OQ2 | Which authority validates steps-derived rewards and ZBattle battle outcomes for cross-app use, if any? Options: server re-simulation with server-issued seeds; Play Integrity/attestation; or accept `unverified-client` permanently with pending/limited effects. | Zeus97x (D-OFFLINE-TRUST) |
| OQ3 | May unverified ZBattle victories ever grant ZPet expedition credit, or only after a verification authority exists? | Zeus97x (D-PARTICIPATION) |
| OQ4 | Should RewardEarned derived from client-reported steps be redeemable in ZBattle at all before step attestation exists, or only displayed as pending? | Zeus97x |
| OQ5 | Is email/password the only sign-in for ZBattle, or are other providers wanted? (ZPet's "usernames did not work" report is still open.) | Zeus97x |
| OQ6 | What does the live ZPet project actually contain (tables, function bodies, Edge Function JWT setting)? This needs an approved **read-only** inspection; this audit did not touch it. | Zeus97x → ZPet Claude |
| OQ7 | Should the server-side catalogue for R9 be a table maintained by ZPet, or constants in the RPC, and who updates it when the art/catalogue changes? | ZPet Claude, Claude |
| OQ8 | Retention for `zpet_audit` and future `xapp_events` / `xapp_audit` (privacy/export/deletion per `BLUEPRINT.md` line 78)? | Zeus97x |
| OQ9 | Enable Auth leaked-password protection before ZBattle accounts launch? | Zeus97x |
| OQ10 | Should the owner email literal in `backend/schema.sql` (tester seed) be removed from source in a ZPet-owned change? | Zeus97x → ZPet Claude |
| OQ11 | Does ZPet Claude's contract adoption (Q3) need ZPet's own outbox before backend work, or can the backend come first? | ZPet Claude |

*End of audit. Proposal — not deployed; needs Zeus97x approval (D-BACKEND).*
