# CJ’s Revenge integration with the unpublished SM 4.0.2 source

## Intent and constraints

The human supplied a newer unpublished SM binary and its companion sources JAR and requested that CJ’s Revenge be integrated into it, reusing newer implementations where appropriate. Preserve the supplied SM behavior and the previously approved CJ .4 behavior. Keep everything local, separate from normal instances, and build/test/package Minecraft **26.1.2 only**. Do not push, create a PR/release, install, launch Minecraft, close a game session or send to Discord under this integration request. Never read `.devAuth`.

Inputs:

- New binary: `C:/Users/Administrator/Downloads/sparkling-mutuals-4.0.2+mc26.1.2.jar`, SHA-256 `b6d184b1a0a5ad6354d507b04e1a2c78f46fada92251bcf1da6ec2a0d89932fb`.
- Companion sources: `C:/Users/Administrator/Downloads/sparkling-mutuals-4.0.2-sources.jar`, SHA-256 `03cee9e058d4454caec8bdc1332a1280d6a4537c523e26d516b99a0e80cb9b37`.
- Existing CJ .4 source: `73432ef36cd9e9bc9c1c3666e93a31285dc36880`; recoverable on `experiment/cjs-revenge`, with its delivered JAR and receipts retained.
- Published GitHub master, read-only refreshed: `0d33d8e475e21140202a93280897575411a2cb16`. The unpublished sources are newer than that baseline.

The source archive supplies product source, resources and notices, but no Gradle build configuration or tests. Reuse the existing build harness and tests; verify source-to-binary consistency rather than assuming identical version labels prove it. Do not represent a source rebuild as bit-for-bit reproduction unless actually established.

## Approaches

**Recommended: newer source as the base, with a focused CJ overlay.** Import the supplied product source/resources/notices into the existing isolated checkout on a new integration branch. Reapply only the entry package and its narrow lifecycle/config/detector/guard/diagnostic hooks. This retains the newer backend synchronization, invitations, capture handling and configuration migration. Routine file mapping and test reconciliation are implementation decisions.

**Port selected new classes into the old CJ base.** Rejected: the incoming synchronization and lifecycle changes span shared owners; cherry-picking a few classes risks silently retaining obsolete call paths or discarding improvements.

**Replace CJ’s freshness protocol with the new shared Sparkling bitmap.** Rejected for the present increment: `PartySyncState` merges discovery/capture bits for a visit, and its received state has no sighting time, nonce-bound request or current entity identity. A connection/reconnect snapshot does not prove a creature is still freshly observed. Preserve that state for its upstream display/invitation consumers; never turn it directly into movement authorization.

This document records a proposed merge boundary for approval; it does not claim that this integration approach has already been approved.

## Ownership and resulting behavior

- Retain the supplied `SafariPartySync`, transport/protocol/sites, readiness/contest handling, returning invitations, online-player status, alert sequencing, settings navigation, tracking and other source/resource changes. Preserve accompanying third-party notices.
- Reuse the newer `SafariAssist` positive name/skin detector and capture filtering as CJ’s local observation source. Add canonical identity to its internal sighting and forward completed scans, including empty scans. The START-tick path must withdraw unloaded/captured identities before considering a fresh sighting; observing a retained snapshot must not refresh its age.
- Preserve CJ’s existing party exchange: one query per verified arrival, 12-second preparation, 3-second reply deadline, 5-second effective evidence age including round-trip time, bounded pacing and current unique roster/profile attribution. Preserve capture retirement and party-continuity invalidation. Shared sync discoveries may remain visible to the upstream systems without granting CJ authorization.
- Keep CJ’s world-scoped personal admission check. The newer run’s pending ticket/admission evidence is useful for tracking, but it can be carried into a run for up to 120 seconds; it is not a substitute for CJ’s current-world action gate. Broad `inSafari`/sidebar labels and another player’s entry are not personal admission.
- Reapply a read-only approach-readiness method to the newer guard. It must honor current roster/presence, unresolved input, kick and any applicable new contest/downtime hold, without simulating an NPC click or applying Shift bypass. Do not replace the newer guard with the old file.
- Preserve CJ .4’s genuine party-warp binding and once-per-warp approach. Face one uniquely nameplate-correlated loaded Manager, prime crouch, then use ordinary forward/sneak with the existing cumulative one-block budget, early release, stopping-distance reservation and input ownership. Retain all focus/screen/input/terrain/context/capture/deadline cancellations.
- **Manager interaction and ticket selection remain manual in the CJ test flow.** The unregistered admission controller remains unregistered. Automatic entry activation is a separate future increment, not implied by merging these source trees.
- Keep the new upstream warp implementation and its 28-second fresh default; preserve explicit saved values. CJ must not impose its old 26-second default on the new base.
- Keep the new configuration migration under the `sparkling-mutuals` config directory. Persist existing CJ setting keys through it. Status exchange defaults on, approach defaults off, and automatic ticket selection defaults off for this experimental package; preserve existing explicit choices.
- Package as `4.0.2-cjs-revenge.5`, Fabric display name `CJ’s Revenge`, mod ID unchanged. Supply the upstream developer metadata value as `false`; do not silently enable developer-only recording/HUD features.

## Verification and delivery

First establish a buildable incoming-source baseline using the reused harness, accounting for the source archive’s `${developer}` metadata placeholder and target preprocessing. Preserve a provenance manifest of imported, absent and intentionally overlaid files. Investigate test failures against actual source contracts; do not delete or weaken coverage merely to obtain a green build.

Regression coverage must verify the newer backend state cannot authorize CJ movement, changed/unknown party data cancels old authorization, upstream capture resolution invalidates local CJ evidence, fresh defaults and saved settings survive the migrated config location, and all current guard holds also prevent approach. Preserve the existing exchange, admission, ToggleKeyMapping and stopping-distance tests. Run the complete 26.1.2 suite and build, then obtain independent final review and correct material findings.

Verify the final JAR’s identity, target dependency, embedded dependency, developer flag, expected classes/resources and SHA-256. Package the JAR with concise instructions, changed behavior, source/binary provenance and validation receipts. Update the affected feature documents and reconcile documentation without claiming the all-document manifest has been fully revalidated.

Live Hypixel/Alpha timing, backend availability, two-client protocol delivery, actual movement and accepted Manager/ticket interactions remain unverified unless tested separately. Unit tests and an offline build are not live-game confirmation. Existing normal/CJ installed JARs, settings, recordings and supplied archives remain untouched.

## Last updated

2026-11-04 — Proposed integration based on the supplied archives and the approved CJ .4 design; no integration implementation or new build claimed.
