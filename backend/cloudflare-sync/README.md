# Shared Safari standby service

Independent UUID-claim authentication, hibernating WebSocket rooms, monotonic visit routing and failover reconciliation. Serkosea remains the primary. This service does not authenticate Minecraft ownership and never needs Minecraft credentials. Unmodified clients cannot use the backup.

## Local checks

Use Node 24 or later. `npm ci`, `npm test`, `npm run check`. Tests run in Cloudflare's real local Workers/Durable Object runtime, including HTTP auth, WebSocket admission and hibernation recovery. No Cloudflare account is needed locally.

The single `COORDINATOR` SQLite object holds at most 128 retained logical rooms, each with an exact 2–4 player roster. Routes expire after ten minutes without a renewed participant registration; missing cohort participants do not thereby authorize automatic actions. Objective snapshots remain on bounded socket attachments; there is no permanent gameplay-history store. SQLite retains bounded routing, locks, acknowledgments, fairness and admission counters. No raw IP address, token or payload logging.

## Deployment (Free account only)

1. Sign in with `npx wrangler login` using the intended account. Review requested access in the browser. Do not place account tokens in the mod or source tree.
2. Generate an unpredictable signing secret of at least 32 characters outside version control. Set `SIGNING_SECRET` with `npx wrangler secret put SIGNING_SECRET`; never paste it into chat, commit it or put it in `wrangler.jsonc`. Keep a private recovery copy. Local tests use an explicitly test-only value supplied by the test config.
3. Deploy with `npm run deploy`. The `v1` migration creates a SQLite Durable Object namespace (available on Free). Use the generated `workers.dev` origin; no paid domain or subscription is needed. Do not select a Paid plan.
4. Run an authenticated TLS smoke test against the deployed origin: auth as two fixture UUIDs, register both, select backup only after reporting 30,000ms unusable, verify welcome mapping, finish both bootstrap transactions, verify atomic found/caught union precedes replay, then explicitly leave. Wrong origins, foreign identities and malformed frames must be rejected. Synthetic fixture UUIDs are not real player claims.
5. Integrate only that verified HTTPS origin in `PartySyncRouting.DEPLOYED_ORIGIN`, rerun mod tests and build the requested Minecraft target. The JVM property `sparklingmutuals.sync.backupOrigin` is an operator/test override, not a credential.

`BACKUP_ENABLED=false` disables new routing/auth/admission. Primary sharing remains available. To roll back a deployment use Cloudflare's prior Worker version and matching compatible migration; retain signing secret to avoid unnecessary session churn. To stop the new route in clients, publish a build with an empty backup origin. Existing adopted backup visits deliberately do not create a competing primary allocator halfway through a visit.

## Resource budget and monitoring

Cloudflare Free currently includes 100,000 Worker requests/day and separately 100,000 DO request units/day; DO compute allowance is 13,000 GB-s/day, SQLite 100,000 rows written/day, 5 million read/day and 5 GB stored. Incoming WebSocket messages count at 20:1 for DO compute-request billing; outgoing frames and protocol pings do not add that message charge. Auth, routing, handshakes and alarms also count. See [DO pricing](https://developers.cloudflare.com/durable-objects/platform/pricing/) and [Worker limits](https://developers.cloudflare.com/workers/platform/limits/). Verify these at rollout; exceeding Free limits interrupts service rather than guaranteeing uptime.

Ten clients, twelve eligible hours/day, 30-second route checks: about 14,400 HTTP requests and registration writes/day before auth, reconnects and migration. Each check also updates bounded global/IP/account admission counters (roughly 57,600 total counter/registration row writes/day). Daily global HTTP admission is capped at 24,000; IP budget at 6,000; UUID requests at 120/minute and auth at 60/hour. Users sharing an address also share the IP allowance. WS messages are capped at 40/10s/connection with 40 simultaneous connections globally. Clients use 36/10s. Gameplay snapshots use connection attachments; only changed coordination/routing records write SQLite. Sustained full-backup traffic must still be measured against all allowances before public activation. There is no healthy-primary gameplay mirror and no empty-room alarm loop.

Use dashboard request/compute/SQLite metrics during a small test. Measure requests, messages, active duration, rows read/written and retained bytes separately. Do not treat passing protocol tests as evidence of a free sustained workload, full shader/gameplay validation or coordinated unmodified-client failover. Both providers use Cloudflare infrastructure, so a platform-wide outage may affect both.

## Protocol extensions (backup only)

`POST /v1/route` version 1 contains lobby/self/exact roster/visit nonce/action/check plus continuous unusable milliseconds. Response binds generation, nonce, provider and admission. Auth tokens are HMAC-SHA256 signed for the requesting origin and expire after ten minutes.

Backup WebSockets additionally require `X-SM-Generation` and `X-SM-Visit`. Welcome advertises `failover-bootstrap-v1`. Before ordinary frames, send increasing-sequence `F` frames with JSON-string payloads: `{version:1,action:"begin"}`, up to eight `facts` (reporter/source/nests/38 irreversible fields/caught pair), `locks` (locked/instructed maps), optional `fairness` (run/revision/counts), then `done`. Stage-only data is not published. Commit merges catches before positives. A missing cohort blocks atomic positive snapshots and new assignment decisions, while objective sharing continues. `bootstrap-facts` is a separate server envelope preserving source and reporter; it never creates a current peer inventory.

Ordinary A/N/U/S/B/H wire formats and upstream hotspot allocation rules remain unchanged. Room positives are released atomically only after reconciliation; assignments never move, conflicts freeze new decisions, and leaders acknowledge instructions after actual game-chat submission. This is bounded observational sharing, not an authoritative capture ledger or permanent run database.
