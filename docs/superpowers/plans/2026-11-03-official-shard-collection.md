# Official shard quote collection implementation increment

> **For agentic workers:** Use superpowers:executing-plans under the existing Native execution method. This is the approved data-acquisition increment within the shard guidance plan; preserve its task workspace and unfinished feature status.

**Goal:** Start one bounded local collection of public official quotes after tests and review.

**Architecture:** A Python standard-library helper runs independently of Minecraft. One child performs each HTTPS request, allowing the parent to enforce a 20-second total deadline including DNS. A locked, atomically persisted ledger reserves the maximum response budget before every attempt; interrupted attempts retain the reservation, so a restart cannot reset limits. Store selected quote records only, never complete responses.

**Tech Stack:** Python 3.14, unittest, HTTPS, multiprocessing, Windows file locks.

**Spec:** `outputs/shard-sale-guidance-20261103/official-collection-proposal.txt` outside this checkout, explicitly approved in chat on 2026-11-03. The human's approval authorizes building/testing and starting this helper, with a compact implementation plan before launch; no additional plan approval is required. The original feature plan remains incomplete.

## Global constraints

- Local only; no GitHub writes, mod installation, Minecraft closure, scheduled task or monitor resumption.
- 37 fixed Safari IDs. Anonymous GET only to `https://api.hypixel.net/v2/skyblock/bazaar`.
- At least 300 seconds between attempts, no catch-up burst or immediate retry. One in-flight request, 20-second deadline.
- Stop at 15 days, 4,320 attempts, 3 GiB received-body charges or 64 MiB output, whichever comes first.
- Encoded response at most 1 MiB; expanded body at most 8 MiB. Failed/interrupted attempts count; uncertain transfer charges the full reservation.
- Persist original deadline, counters and next-attempt time across manual restarts. Duplicate workers fail before network access.
- Save exact validated best asks/bids, server time, actual receipt time, explicit missing/error reasons. No interpolation or weighted-price fallback.
- The runtime model's accuracy/source gates are unchanged. This collection cannot promise a qualifying model or order fills.
- Minecraft work remains 26.1.2-only; this helper does not generate any mod artifact.

## Review focus

- Crash between request and state update must never reset budget or reuse an attempt ID.
- A slow DNS/read or gzip expansion must not bypass time/byte bounds.
- Duplicate workers and restarts near a sampling boundary must not multiply requests.
- Sleep, clock changes, malformed data and stale snapshots must remain visible gaps.
- Disk exhaustion or an unexpected/corrupt output file must fail closed without deleting evidence.

### Task 1: Bounded official quote reader

**Files:** `tools/shard_market/official_quotes.py`, `tools/shard_market/test_official_quotes.py`.

**Interfaces:** `parse_quotes(raw: bytes, received_at: int) -> dict`; `read_body(stream, limit: int, counter: list[int]) -> bytes`; `decode_body(body: bytes, encoding: str) -> bytes`; `fetch_official(limit: int) -> dict`.

- [ ] Write failing tests: best ask/bid versus weighted values; allowlist isolation; malformed/crossed books; future/stale timestamps; encoded stream and gzip expansion limits.
- [ ] Run `python -B -m unittest discover -s tools/shard_market -p test_official_quotes.py -v`; expect failures for missing reader.
- [ ] Implement the fixed public reader, rejecting redirect/non-200 responses and retaining no full payload in output.
- [ ] Rerun the reader tests; expect all pass.

### Task 2: Persistent bounded run and controls

**Files:** `tools/shard_market/official_collect.py`, `tools/shard_market/test_official_collect.py`, `tools/shard_market/README.md` and feature status docs.

**Interfaces:** `Store(path, limits).begin(now) -> reservation | None`, `finish(result, now)`, `SingleWorker(path)`; CLI `run/status/stop --data-dir PATH`.

- [ ] Write failing tests for persisted deadline/request/download limits, interrupted reservations, missing state, corrupt data, single-worker exclusion, stop marker, output cap, restart throttle, clock regression, timeout termination and successful selected-record persistence.
- [ ] Run the collector tests; expect failures for missing collector.
- [ ] Implement atomic state and bounded append, fail-closed loads, OS-held lock, parent-enforced child timeout, interruptible waiting and fixed CLI. No reset switch, startup registration or automatic restart.
- [ ] Run all helper tests, existing repository Python suites and `:26.1.2:test`; expect zero failures. Run read-only documentation sync and `git diff --check`.
- [ ] Commit locally. Obtain one fresh review of this collector increment, fix Important findings with RED→GREEN tests, and rerun affected/full checks. This is not the unfinished feature's final review.
- [ ] Start exactly one hidden local process in an output directory outside the checkout. Verify PID identity, first actual official quote, limits and status. Provide status/stop commands and actual deadline. Record launch receipt; leave collection running without waiting 15 days in this session.

## Self-review and execution

Both tasks map to the approved bounded proposal; counter safety belongs to the store, transport limits to the reader, total deadline and single-worker exclusion to the runner. All five review conditions have named tests. Preserve Native execution and the existing ledger. No additional dependency, target or release work is included.
