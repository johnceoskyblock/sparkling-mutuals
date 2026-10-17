# Synchronizing source with the documentation

Every Markdown document has a **Last updated** section. Update that section whenever its rules or guidance change, with the date and a short description of the change. Dates use America/New_York.

Before a request to bring SRC up to date, run this from the repository root:

```powershell
./scripts/DocumentationSync.ps1
```

The command lists documents added, edited or removed since the last implemented source baseline. It compares SHA-256 hashes, so multiple edits on the same day are detected. Read those documents first and follow their linked shared rules when needed. A timestamp alone cannot identify every revision.

Hashes normalize CRLF to LF so a Git checkout on another platform does not report false document changes.

The baseline is stored in `docs/source-documentation.json`. After implementing and validating all listed changes, record the new baseline:

```powershell
./scripts/DocumentationSync.ps1 -Record
```

Recording a baseline means the source has been reviewed against that document revision. It does not certify a Minecraft test. Keep build/unit-test results separate from the in-game test checklist and record only tests actually performed. Do not record a baseline with unresolved implementation changes merely to hide the change list.

The Macaw Full Clear rule changed on 2026-10-15: once all known bird food is used, one shared Macaw catch completes Macaw even while other Macaws remain visible. Other species keep their own requirements.

## Latest source review

2026-10-17 downtime/ticket follow-up: local notices now address the mod user and list every waiting requester. A verified leader's !dt contest seeds a hold for each current party member, preserving existing individual reasons and retaining unresolved members by UUID. Each sender clears only their own hold with affirmative readiness or a ticket report. The common parser accepts readiness phrases and short r/keyboard typos, excludes nr, unrelated words, questions and negative/future readiness. Entry GUI Shift quick-move on server ticket controls is normalized to ordinary selection while the shared Shift guard bypass remains active. The focused 26.1.2 entry/inbox/boundary/downtime suite passed 44 tests. Actual Hypixel GUI interaction and party chat remain live IntelliJ checks.

2026-10-17 tab-priority follow-up: Safari entry now follows full current-party tab presence, except for two seconds after a verified kick-containing party message (timed from receipt). Pending identity/old generic kick state cannot prolong that pause when everyone is present; missing or unknown presence still blocks. Torrhus downtime remains sender-specific and now accepts r or any case-insensitive ticket substring in a verified party message. Shift bypasses all blockers without clearing them. The 26.1.2 focused entry/inbox/boundary/downtime suite passed 39 tests; obsolete indefinite kick-recovery cases were replaced with the requested two-second/tab-priority cases.

2026-10-17 names/Shift follow-up: unresolved current-party UUIDs now receive asynchronous Mojang profile-name lookup, independently of tab presence or control messages, with roster validation, caching and retry. Holding either physical Shift key bypasses every guarded entry route for the current interaction without mutating holds. Refusal notices and initial downtime messages explain the override; the config tooltip/docs agree. The 26.1.2 focused guard/boundary/downtime suite passed 31 tests, including a new all-route Shift/release boundary case. Live profile lookup and input behavior remain in-game checks.

2026-10-17 spam-click follow-up on fix/safari-entry-guard: the live log showed a blocked Manager interaction followed by admission without an allowed decision log, indicating a recognition gap. Manager nameplate/proxy identities are now retained within the client world, including aligned interaction/armor-stand targets; local/current-party players are excluded and world transfer resets recognition. Entry decisions still run for every attempt independently of notice throttling. The 26.1.2 focused recognition/guard/boundary suite passed 15 tests, including one new repeated-proxy/nameplate-loss case; existing boundary checks repeatedly cancel all blocked entry routes. Live Hypixel retesting remains required.

2026-10-17 guard-message follow-up on fix/safari-entry-guard: shortened local entry/downtime messages, replaced the presence/loading diagnostic with a missing count and sorted player names, and retained display-name mappings across transfers. Unknown names remain a short loading notice instead of raw UUIDs. The guard decisions, party announcements and three-second local-message limit are unchanged. The 26.1.2 focused guard/downtime suite passed 27 tests, including one formatter/name-list case.

2026-10-17 timer-first hive follow-up: restored hive_7 to (-705, 92, 225), migrating prior -704 saved keys back. Personal loot timestamps rebuild exact one-hour default deadlines; only matched full/refill holograms override them, with corrections persisted separately from the original loot time. Block levels are diagnostic-only. Queen Bee still clears the timer immediately. All 36 focused 26.1.2 hive tests passed, including updated block-independence, coordinate migration and restart correction coverage.

2026-10-17 notification follow-up: identified Sparkling GUI/sound alerts and party announcements now deduplicate independently by species for the entire run, across capsule/escape UUID changes. Capture identities and completion obligations remain independent; unknown particle locations keep their separate identity-based notification handling. The 26.1.2 focused notification/detector suite passed 11 tests, including one new replacement-UUID/reset regression.

2026-10-17 exceptional-hive follow-up: level 5 still establishes full, while other levels consult matched Click to loot/refill status text. Restored live hologram sampling and expanded its vertical association range to include observed armor-stand origins just below the block. Existing contest display gating remains. All 36 focused 26.1.2 hive tests passed, with updated exception/fallback and carrier-coordinate coverage.

2026-10-17 Sparkling/coin follow-up: retained every matching label alias per Sparkling model and omitted resolved identities from detected HUD, live ESP remainders and population-target observations. Lingering captured entities can no longer inflate the ordinary Full Clear target or recreate an obligation through a second retained label. Coin eligibility now stays on throughout Full Clear and follows only party Gimmiegold discovery need in Unique/Sparkling; holdings/pickups never apply a permanent cutoff. The 26.1.2 focused detector/capture/Haunted/integration suite passed 53 tests, including two new regressions and stale captured-Gazer population coverage.

2026-10-17 incense follow-up: all five known incense floor drops are now eligible whenever needed in every mode, without distance ranking or reduced starting-incense slots. Existing four-floor-pickup/four-held cutoffs remain sticky across spending and reentry. The 26.1.2 focused incense/Haunted suite passed 10 tests with updated all-five and pickup-threshold checks.

2026-10-17 block-authority follow-up: loaded hive honey levels now exclusively determine observed readiness (5/full, 0/empty), overriding saved timers/Queen Bee flags; holograms remain diagnostic only. Corrected hive_7 to (-704, 92, 225) and migrated saved coordinate keys, retaining conservative duplicate deadlines. The 26.1.2 focused hive suite passed all 36 tests, including updated conflicting-label/block and saved-coordinate coverage.

2026-10-17 hive visual follow-up: loaded status text now precedes saved timers and actual BeehiveBlock.HONEY_LEVEL supplies a 5/full or 0/empty fallback when no label is recognized. Unknown/intermediate levels preserve evidence; an empty block alone does not create a loot or deadline. Queen Bee retains a two-second grace and rejects lone stale countdowns, while corroborated live empty visuals can revalidate it afterward. Rendering uses the observed state, and the 26.1.2 focused honeyhive suite passed all 36 tests with updated visual-priority cases.

2026-10-17 PB storage follow-up: personal bests now save to `sparkling-mutuals-pbs.properties`. Startup migrates old general-config pb keys after safely writing the new file, preserving other settings and faster existing records. Legacy boss categories still normalize through the existing loader. The 26.1.2 focused config/PB suite passed 16 tests, including two new migration regressions.

2026-10-17 hive follow-up: matched Queen Bee/loot responses immediately refresh beacons, Queen-first response ordering preserves instant-full priority over delayed loot/countdown state, and restart scope selection reuses the same player's saved profile. Exact loot timestamps, cooldown deadlines and descriptive full/empty state are saved; natural expiry persists even outside Canyon. The 26.1.2 focused honeyhive suite passed 36 tests, including two new ordering/restart regressions. Live Hypixel validation remains required.

2026-10-17 reminder wording follow-up: unused-resource party messages now identify floor-drop items/quantities and the relevant party-missing Sparkling discoveries, grouping bird foods under one explanation. Both older compact and verbose formats remain accepted. The 26.1.2 focused resource/Gimmiegold/integration suite passed 33 tests, including one new mixed-food round-trip case.

2026-10-17 nest-highlight follow-up: Full Clear unpunched nest highlights/counters no longer retire from fd or captures plus live bee sightings. They remain until the verified initial nest population has shared captures; individual punched sites still disappear immediately. Unique/Sparkling keep existing report/local-check trust. The 26.1.2 focused nest suite passed 34 tests, including one new mode/report/punch regression case.

2026-10-17 Alpha follow-up: scoreboard-address detection now excludes Alpha runs and Sparkling entries from live history and ignores their ticket counters. Exclusion sticks to each run through departure; address state resets per connection. Live Safari tracking remains active. The 26.1.2 focused history suite passed 19 tests, including two new server-policy cases. Historical log imports and existing records are unchanged.

2026-10-17 particle follow-up: added Safari-only WAX_ON packet observation, per-run anonymous last-seen locations, world/HUD waypoints and retirement on nearby independent name/skin identification. Particle origins do not invent species, UUIDs or completed surveys. 26.1.2 compilation and the focused particle/detector/memory tests passed; live particle range and mixin behavior remain in-game checks.

2026-10-17 follow-up: retained safari-wide Rockmite mound/Snooper wall observations now satisfy Unique with a shared catch and Sparkling without captures. Unique unseen zero-spawn cases require resolved sources plus party cd. Positive intact sources and pending Sparklings remain blockers. Full Clear rules remain unchanged. The 26.1.2 suite passed all 715 tests (one new regression case). No release JAR was prepared or copied to Downloads.

Reviewed the 59 maintained root/docs Markdown files on **2026-10-17**, on `master` based on `dad4f9fca2816342032eeca6b07af4981ec4fc9d`, including the existing uncommitted implementation. Historical multi-version plans are implementation records; their old branch/delivery steps do not supersede the current requested master workflow. The Billygoat example records a proposal difference, not an extra ordinary Unique scan requirement.

The audit retained the implemented local Full Clear progress/missing split, actual-nest Honeybug accounting, per-species Unique trust, run-retained checks and Sparkling capture obligations. It fixed report bypasses for zero-catch Macaw, cross-biome positive remainders and known unused/additional supplies. Starting-item reminders now activate from local clearing even when another player's report suppresses our own announcement; repeated polls cannot postpone delivery indefinitely. Stale simultaneous-Gemzie and Unique post-catch wording was reconciled with the newer rules.

Validation: `:26.1.2:build :26.1.2:verifyTargetJar testBuildLogic` passed, with **705 feature tests** and **21 build-helper tests**, zero failures/errors/skips. Only the requested 26.1.2 target was rebuilt. Minecraft/Hypixel checks were **not performed**; use [source synchronization regressions](IMPLEMENTATION_TEST_CHECKLIST.md#source-synchronization-regressions) alongside affected feature checks. The baseline records source review, not live-server certification.

## Gimmiegold follow-up

2026-10-17: local Sparkling checking now uses all initial coin slots collected, empty coin inventory and captured + currently rendered population against that slot total plus deduplicated extra coins. Outside Haunted, party hd can override ordinary checks; Unique/Full Clear still require their real captures. These changes supersede the earlier three-pickup/retained-Gimmiegold checking path. The updated 26.1.2 build/JAR verification passed **712 feature tests**, zero failures/errors/skips. Live Minecraft verification remains pending.

## Honeybug and checklist follow-up

2026-10-17: retained the initial actual nest survey/count through the whole run. Full Clear Honeybug now completes from the required shared captures in any biome. Unique's Honeybug row still clears at one catch, but needed Sparkling confirmation separately gates progress through local checking or trusted fd. Sparkling needs checking only, and Honeybug fd trust survives Forest entry. Every Sparkling biome checklist is named Unchecked, retaining its saved layout/settings.

Focused regressions and the final 26.1.2 build/JAR verification passed **714 feature tests**. Two new cases cover the row/progress distinction and all-biome Unchecked naming; existing nest/report tests were updated for the new rules. Live Minecraft checks remain pending in the manual checklist.

## Last updated

2026-10-17 - Added whole-party contest readiness, multi-requester local guidance, readiness typo handling and Shift ticket selection; 44 focused checks passed.

2026-10-17 - Implemented tab-priority entry, receipt-time two-second kick pauses and broad ticket-message readiness.

2026-10-17 - Added missing-player profile-name lookup, Shift bypass and local override guidance.

2026-10-17 - Closed Manager recognition gaps during spam clicking.

2026-10-17 - Cleaned Safari guard local messages and named missing party members.

2026-10-17 - Adopted timer-first hive readiness with hologram-only corrections and restored -705 waypoint.

2026-10-17 - Made identified Sparkling notifications once per species per run.

2026-10-17 - Restored lower-level hive hologram fallback for exception hives.

2026-10-17 - Fixed stale captured-Sparkling aliases/remainders and mode-specific coin ESP lifetime.

2026-10-17 - Removed incense waypoint ranking and validated all-five selection.

2026-10-17 - Adopted honey-level authority and migrated the incorrect hive waypoint.

2026-10-17 - Implemented loaded hive visual priority and honey-level fallback.

2026-10-17 - Corrected PB filename to sparkling-mutuals-pbs.properties; also migrate the briefly used display-name filename safely.

2026-10-17 - Added dedicated PB configuration storage and automatic migration.

2026-10-17 - Implemented immediate Queen Bee beacon refresh and persistent restart-safe hive timers.

2026-10-17 - Clarified party-facing unused floor-drop messages and validated backward-compatible parsing.

2026-10-17 - Fixed Full Clear nest highlight lifetime after party fd.

2026-10-17 - Implemented and validated the live Alpha history exclusion.

2026-10-17 - Implemented Safari-only wax-on location evidence and focused validation.

2026-10-17 - Implemented and tested safari-wide Cavern source checks; source-only delivery for IntelliJ.

2026-10-17 - Recorded retained Honeybug nests/checking trust and mode-specific Unchecked naming.

2026-10-17 - Recorded the slot-derived Gimmiegold Sparkling follow-up and outside-Haunted party-report exception.

2026-10-17 - Reviewed all maintained documents and synchronized current source rules on master, including local Full Clear proof, retained checks, reports, resources, helpers and defaults. Automated validation is recorded separately from pending Minecraft checks.

2026-10-15 — Added revision-based source synchronization and the Macaw override; normalized line endings for cross-platform checkouts.
