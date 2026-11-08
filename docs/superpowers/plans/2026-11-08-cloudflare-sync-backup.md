# Cloudflare Sync Backup Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans, inline execution, followed by one fresh whole-branch reviewer. Direct user approval and the implementation handoff authorize execution without another design gate.

**Goal:** Independently restore shared Safari observations and safe coordination during a primary outage.

**Architecture:** A standby TypeScript Worker delegates bounded authentication, routing and party rooms to one SQLite Durable Object. The Kotlin client retains one gameplay transport, visits and provenance, polls routing asynchronously and reconciles irreversible evidence before enabling automatic actions.

**Tech Stack:** Kotlin/JDK 25, existing Gson/Java HTTP client; TypeScript, Cloudflare Workers/Durable Objects, Wrangler, Vitest Workers pool (pinned in lockfile).

**Spec:** `docs/superpowers/specs/2026-11-08-cloudflare-sync-backup-design.md` (approved directly, despite historical proposed status).

## Global constraints

- Primary remains Serkosea, no live mirroring, one gameplay socket.
- 30-second continuous unusability; 30-second routing checks with jitter; backup sticky within visit; 10-minute abandoned routing expiry; preserve 7-second initial fallback and 75-second transport liveness.
- Exact roster, normalized lobby, random visit nonces, serialized route generation; no stale roster rollback or ambiguous visit import.
- Fixed migration cohort: positive retirement or completed bootstrap required; lease expiry cannot release automatic-action barrier.
- Retain provenance, sequence, locks, catches and instruction suppression; never transfer peer inventory as current or invent rewards/history.
- Existing A v2/v3 sizes 38/39, 4096-byte frames, 3072-character payload, 40 server messages/10 seconds, 36 client budget.
- No deployment credentials in source, UUID claim is not ownership proof, bounded admission and privacy-safe diagnostics.
- Local only: no push, merge, release, deployment or instance changes. Never read `.devAuth`. All three game targets must build.

## Review focus

1. Reused lobby with new nonce while prior peers remain: pending, no gameplay import (task 1).
2. Missing cohort member expires: objective sharing works, automatic actions remain blocked (task 2).
3. Stale found arrives before another bootstrap catch: no premature atomic snapshot (task 2).
4. Same-provider run changes remain rejected; migration keeps locks and prevents duplicate instructions (tasks 3–4).
5. Routing failure, brief token renewal and old callbacks never interrupt healthy primary or authorize entry (task 4).

### Task 1: Independent auth and routing

**Files:** `backend/cloudflare-sync/{package.json,wrangler.jsonc,tsconfig.json,vitest.config.ts,src/protocol.ts,src/routing.ts,src/auth.ts,src/index.ts,test/routing.test.ts,test/runtime.test.ts}`.

**Interfaces:** `route(state: RouteRoom | undefined, request: RouteRequest, now: number): RouteResult`; strict `identity(lobby,self,roster)`; origin-scoped `issueToken/verifyToken`; Durable Object `/v1/route`, `/v1/auth/uuid/session` and `/v1/sync`.

- [ ] Write routing tests: at 29,999ms no switch; at 30,000ms choose backup once; healthy peer adopts same generation; new nonce pending; all-positive-departure/new visit resets primary; expired whole room resets; stale roster subset rejected; confirmed expansion preserves generation; no cross-roster relay.
- [ ] Run tests and observe missing implementation failure.
- [ ] Implement pure bounded routing, strict protocol validation, independent signed auth and SQLite persistence with bounded admission.
- [ ] Run unit and real Worker HTTP auth/routing tests, including wrong-token, expired-token, foreign self and kill switch.
- [ ] Commit local task.

### Task 2: Hibernating relay, reconciliation and hotspot contract

**Files:** `backend/cloudflare-sync/src/{room.ts,allocator.ts,index.ts}`, `test/{room.test.ts,allocator.test.ts,runtime.test.ts}`.

**Interfaces:** `Room` stores active-source attachment snapshots; `Allocation` stores immutable locks, fairness, deadlines and acknowledgments; bootstrap-v1 staged per reporter, route generation/nonce bound. Atomic Sparkling union precedes replay only after fixed cohort reconciles.

- [ ] Add failing tests for exact numeric bounds, malformed frames, catch-up-only precedence, original-source bootstrap facts without inventory, missing cohort lease, caught-before-found, late bootstrap, contradictory locks/fairness and imported-win suppression.
- [ ] Implement relay and bounded staged transaction; retain negative union across connected peers and hibernation; exact Java signed UUID hash, 2s/5s initial deadlines, 250ms late batching and 2s display grace; leader-only notices/ack after submission; positive retirement distinct from socket loss.
- [ ] Exercise real local DO WebSockets, replay/hibernate reconstruction, admission/rate bounds, room isolation, timing/alarms and ack persistence. Record measured traffic/storage budget.
- [ ] Commit local task.

### Task 3: Kotlin provenance and explicit authority transition

**Files:** existing `sync/PartySyncProtocol.kt`, new `sync/PartySyncBootstrap.kt`, tests `sync/PartySyncBootstrapTest.kt` and existing protocol tests.

**Interfaces:** `PartySyncState.transitionAuthority(provider: String, generation: String)` changes authority namespace only after validated route; `bootstrap()` emits bounded immutable evidence with source UUID, never live peer holdings. Retain primary fairness run/revision and instructed player/biome targets.

- [ ] Write failing tests preserving locks/sequence across validated migration, rejecting same-provider changed run, clearing live freshness, retaining original reporters and negative masks; duplicate notice target cannot be submitted twice.
- [ ] Implement retained evidence/fairness plus explicit transition and staged bootstrap serialization, and lowercase canonical lobby.
- [ ] Run targeted Kotlin tests and commit local task.

### Task 4: Asynchronous client provider lifecycle

**Files:** new `sync/PartySyncRouting.kt`, existing `sync/{PartySyncTransport,SafariPartySync,PartyRoomDiscoveries}.kt`, new routing tests and existing transport tests.

**Interfaces:** `PartySyncRouting` owns visit nonce, route generation, per-provider availability and asynchronous check delivery; transport gains generation-bound endpoint selection without sending leave on migration; adapter publishes bootstrap before positives and gates current inventory/entry on exact validated snapshot.

- [ ] Write failing deterministic tests for continuous 30s failure, brief renewal reset, healthy-primary routing failure, partial outage convergence, sticky backup outage, new visit reset, pending generation and replaced callbacks.
- [ ] Implement optional backup-origin configuration (unset until deployed), async authenticated routing, provider-separated tokens/backoff, explicit departure routing and debug provider status/switch notice.
- [ ] Integrate staged bootstrap, automatic-action barrier, snapshot republish and leader instruction target suppression. Preserve ordinary primary behavior when no backup origin exists.
- [ ] Run real localhost transport lifecycle tests plus focused sync regression suite and commit locally.

### Task 5: Delivery, documentation and independent verification

**Files:** `backend/cloudflare-sync/README.md`, `docs/PARTY_SYNC.md`, local validation receipt outside repository.

- [ ] Document Free-account setup, secret creation outside repo, kill switch, workers.dev deploy/rollback, authenticated TLS smoke and measured quota worksheet. Clearly distinguish local verification from missing deployment/live tests.
- [ ] Run full backend tests/typecheck and mod tests/builds for 26.1.2, 26.2 and 26.3; verify built contents and record results.
- [ ] Run one fresh whole-branch review, fix material findings with regression tests, reconcile relevant documentation and record final local commit/artifacts.
- [ ] Deliver locally tested backend/mod and request only actual missing Cloudflare access for deployment. Do not push or install.
