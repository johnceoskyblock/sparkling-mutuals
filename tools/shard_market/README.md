# Official shard quote research collector

An explicitly authorized, local research helper, independent of Minecraft. It records public quotes for the 37 Safari shards. It does not enable advice, inspect accounts/orders, install a mod or publish anything.

Requires Python 3.10+; the current Windows run is tested with Python 3.14. Standard library only. From this directory:

```powershell
python -B official_collect.py run --data-dir C:/path/to/new-collection
python -B official_collect.py status --data-dir C:/path/to/new-collection
python -B official_collect.py stop --data-dir C:/path/to/new-collection
```

`run` stays in the foreground unless a caller explicitly launches it hidden. No scheduled task, startup entry or automatic restart is installed. `stop` creates a persistent marker; the worker notices between requests, or after the active request's maximum 20 seconds. A terminal collection never automatically starts again. `status` reports whether the OS worker lock is held; `status: running` with `workerRunning: false` means the process exited and manual investigation is needed.

## Fixed limits

One anonymous [official Bazaar GET](https://api.hypixel.net/) at least five elapsed minutes after the previous attempt finishes, one request at a time. A manual restart waits at least another five elapsed minutes, so a wall-clock adjustment cannot shorten the interval. No redirect following or immediate retries; numeric Retry-After can delay polling further. Each request has a 20-second deadline, including DNS. Windows Job Objects kill the request child when its parent disappears; a gated start and independent child watchdog also enforce lifetime bounds. The non-Windows path requires Linux parent-death signaling; the current deployment and orphan regression are verified on Windows. Stop at the first of 15 days, 4,320 attempts, 3 GiB of received-body charges or 64 MiB of output. Individual encoded/expanded bodies are capped at 1/8 MiB. Headers, HTTP framing and TLS overhead are outside the body budget. Sizes may change; the estimated 2.1 GB for 15 days is not a transfer guarantee.

Before each request, atomically reserve its maximum possible encoded bytes. Only a known completed read refunds unused bytes. Crashes, socket failures and timeouts keep the full reservation, so the conservative charged total can exceed actual transfers. Restarts reuse the original limits, deadline, counters and next-attempt time. OS locking rejects duplicate workers for the same data directory. There is no reset option. Integrity hashes protect the state and chained evidence records from accidental corruption; they are not signatures or authentication against intentional local edits. Startup audits the entire bounded evidence file and its state counters. An append-before-state-save crash can recover its single identified pending record, retaining the full uncertain byte charge. Missing/corrupt evidence otherwise fails closed without deletion. Save output outside the checkout.

Sleep/reboot stops or interrupts collection; no missing period is backfilled. More than 60 seconds of clock discontinuity stops a worker. A manual restart cannot reset the persisted deadline. A reboot requires a manual restart, not an automation.

## Evidence

`samples.jsonl` contains one bounded result per completed attempt: server snapshot time, actual receipt time, request/finish time, exact minimum buy-summary ask and maximum sell-summary bid, missing/error reasons and response identity hash. A malformed level makes the affected product unknown; a worse surviving level is never certified as exact best. The full response, quick-status weighted prices and unrelated products are discarded. Stale (>3 minutes), excessively future (>60 seconds), crossed or invalid quotes are rejected. Duplicate/older source snapshots are explicitly marked and do not count as fresh accepted snapshots. Invalid-response diagnostics currently retain the broad status/exception type rather than each validation reason; finer reason codes remain a deferred minor.

Receipt time is availability evidence, not a claim that the server clock equals the local clock. Preserve both timestamps for later source/clock qualification. Five-minute observations do not reconstruct intrabin peaks or actual order fills. Collection can stop short of adequate coverage; a model must still pass the approved earlier-training/final-three-day validation gates before any Hunting Box colors or alerts are enabled.

## Tests

From the repository root:

```powershell
python -B -m unittest discover -s tools/shard_market -p "test_*.py" -v
```

2026-11-03 — Local bounded research acquisition added under direct human approval. This is not a completed market-guidance feature.
