# Bazaar shard unlist guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans for the preserved Native execution method. Steps use checkbox syntax and a per-plan execution ledger.

**Goal:** Mark qualifying owned Safari shard HOLD offers blue and suggest unlisting only after confirmed SELL-to-HOLD changes.

**Architecture:** Preserve the confirmed pricing engine. Extend parsed order evidence with slot identity, use the shared scoped ownership tracker for eligibility, and keep order markings in their own immutable render snapshot. Personal watches consume confirmed advice; the existing Hunting Box display keeps its faster display-only state.

**Tech Stack:** Kotlin, Fabric, JUnit5, Gradle, Minecraft26.1.2.

**Spec:** docs/superpowers/specs/2026-11-03-bazaar-unlist-design.md (approved).

## Global Constraints

- Local26.1.2 only; no GitHub writes, releases, installation or game-session closure.
- Safari37 only. Eligibility is current cached /profit top ten OR positive remembered Safari ownership for the current scope.
- Own SELL rows only: valid matching By name when present; absent By accepted only on personal Your Bazaar Orders. Co-op pages require the name.
- Use existing fresh verified account/profile evidence. Unknown scope, SELL, UNKNOWN, other-owner, buy, completed and ineligible rows are unmarked.
- No initial/already-HOLD notice. Actual decline and confirmed SELL-to-HOLD under the same rule only; retain distinct quotes at least sixty seconds apart, hysteresis, freshness and ten-minute cooldown.
- Existing37-product history/queue/cache/provider limits and shared quote throttle remain unchanged.
- No automatic menu navigation, cancellation, purchase, new price source or render-time IO.
- Existing clean Hunting Box total/top-ten presentation and quick display semantics remain unchanged.

## Review Focus

- Mixed-style server item components must preserve explicit SELL evidence without accepting arbitrary plain buy/control labels.
- Eligibility loss/reentry, model changes and UNKNOWN recovery cannot manufacture a transition or reuse an old baseline.
- Legacy ownership seeds must not override newer authoritative total-cache scopes or resurrect explicit deletions.
- Ownerless co-op/foreign/filled rows and menu/item replacement cannot retain blue slot marks.
- Grouped/background suggestions must retain product cooldowns and last-observed wording, without claiming an offer is still active.

### Task1: Slot-aware owner evidence and change-only watch policy

**Files:** modify market/SellOfferParsing.kt and market/SellOfferWatch.kt; create market/MarketSlotName.kt and market/BazaarUnlistPresentation.kt. Add/update their tests under the corresponding src/test/kotlin market package. Source paths are under src/main/kotlin/net/johnceo/sparklingmutuals/skyblock/.

**Interfaces:**
- OwnedSellOffer(slot:Int,evidence:OfferEvidence).
- SellOfferParsing.isOrders(title:String):Boolean and parseSlots(title:String,slots:List<MenuSlot>,localName:String):List<OwnedSellOffer>. Existing parse delegates and preserves its evidence-list API.
- MarketSlotName.capture(component:Component):String preserves explicit bold SELL component styling for the existing parser, including legacy formatted names; it must not invent bold styling for plain text.
- SellOfferWatch.update(advice,now):List<SaleAlert> retains its API but never sends initial-HOLD notices.
- BazaarUnlistPresentation.message(products:Set<String>):String? groups distinct valid Safari names alphabetically into one concise local suggestion.

- [ ] Add owner/slot tests: personal ownerless accepted, co-op ownerless rejected, matching name case-insensitive, foreign/duplicate/malformed By excluded, duplicate slots rejected, same-product distinct offers kept, BUY/filled/conflicting identity excluded from active selection. Test structured bold SELL components, split/inherited styling and unformatted lookalikes.
- [ ] Update watch requirements in tests: first HOLD silent; SELL then lower-ask HOLD alerts once; duplicate offers group; UNKNOWN/model changes or rising asks never alert; cooldown and return-to-SELL remain. Legacy four-field schema caches still read but cannot revive an initial notice. Add multi-name formatter tests.
- [ ] Run focused tests and observe RED assertion failures before implementation; compile-only API shells may establish new signatures.
- [ ] Implement parsing and style capture; remove initial-notice behavior while preserving legacy cache readability and validation bounds. Suggested message: Consider unlisting: Bluebird, Hideonwall. Guidance changed to HOLD; check your last observed offers are still active.
- [ ] Run full26.1.2 tests; expected GREEN. Commit locally.

### Task2: Shared ownership, eligibility and confirmed order decisions

**Files:** modify market/WholeBoxTotal.kt, market/SaleGuidanceSession.kt and market/ShardSaleGuidance.kt; update/add WholeBoxTotal, ProfitShardGuidance and order-session integration tests.

**Interfaces:**
- WholeBoxTotal.ownedSafariProducts(scope:MarketScope?,connection:Any?):Set<String> exposes positive remembered Safari IDs only for its current active context.
- SaleGuidanceSession gains an ownedProducts callback defaulting to empty; orderMarkers():Map<Int,Recommendation> exposes only current visible qualified own confirmed-HOLD rows. Its existing tick return remains Hunting Box advice only.
- Task1 parser/watch/message APIs drive the session. The runtime supplies ownership from WholeBoxTotal after selecting current scope on each tick.

- [ ] Add failing tests for top-ten OR box-owned eligibility, positive/zero updates, profile/connection isolation, hidden-value-header ownership tracking, unsupported/outside-union rejection and dynamically changing rankings.
- [ ] Add session regressions: initial HOLD blue without chat; actual ShardTargetModel SELL-to-HOLD confirmation yields one grouped suggestion; repeated reads do not confirm; fast box-display HOLD cannot turn an order blue early. Unknown, disable, eligibility loss/reentry, scope changes and leaving orders clear visible markers.
- [ ] Track broad box ownership when either value or guidance is enabled. Preserve the separate value render gate. On initial load, seed validated legacy narrow ownership first, then restore the authoritative total-cache scopes; existing authoritative scopes, including empty scopes after zeros, win. Test the migration and late-live-over-restore safeguards.
- [ ] Evaluate cached profit ranking for box/orders/active-watch contexts only, retaining bootstrap behavior without adding idle refresh demand. Combine with scoped ownership, filter visible observations and history demand to eligible products, and feed only eligible confirmed recommendations to watches. Missing advice clears stale baselines. Keep last-observed background watches in the current verified scope.
- [ ] Populate orderMarkers from Task1 visible evidence and confirmed HOLD only. Group simultaneous alerts into one notification; preserve per-product watch accounting and cooldowns.
- [ ] Run full26.1.2 tests; expected GREEN. Commit locally.

### Task3: Passive blue markers, documentation and verified delivery

**Files:** add OrderMarkerSnapshot to market/BazaarUnlistPresentation.kt; modify market/ShardSaleGuidance.kt and relevant presentation tests. Update docs/SHARD_SALE_GUIDANCE.md with the owner fallback, union eligibility, blue markers and change-only alerts.

**Interfaces:**
- OrderMarkerSnapshot(menu:Any,revision:Int,items:Map<Int,Any>,slots:Map<Int,Recommendation>) exposes needsUnlist(menu,revision,index,item,now):Boolean.
- BazaarUnlistPresentation.COLOR is opaque bright cyan-blue0xFF00DFFF. The existing passive slot hook draws its outline for a valid order snapshot; ordinary box outlines still use their existing colors.
- The order snapshot is separate from SaleMenuSnapshot, keeping order markings out of Hunting Box advice tooltips and retaining existing constructor compatibility.

- [ ] Add failing snapshot tests for menu/revision/item/quote changes, HOLD-only selection and stale/unavailable suppression. Ensure no order guidance tooltip is appended.
- [ ] Extract server names with Task1 style capture on client ticks. Build/clear the order snapshot after session updates. Gate drawing by current menu, setting, SkyBlock and verified scope; render only the blue outline for valid own-HOLD slots, with no mark for other rows.
- [ ] Update concise documentation and run read-only DocumentationSync.ps1; do not Record over unrelated baseline differences.
- [ ] Run ./gradlew.bat --offline -I ../../outputs/shard-sale-guidance-20261103/local-test-temp.gradle :26.1.2:test :26.1.2:verifyTargetJar using existing JDK25. Expected: full suite and JAR verification pass.
- [ ] Extend the isolated NoFrills/owo native fixture with personal/co-op order pages, ownerless personal fallback, structured item names, duplicate owned HOLDs, foreign HOLD, own SELL/UNKNOWN, BUY, completed and ineligible rows. Verify blue pixels only on qualifying HOLD rows, first-HOLD silence, confirmed grouped chat change, slot/page invalidation, no order tooltip and unchanged clean Hunting Box overlays. Inspect screenshots and assertion reports.
- [ ] Commit locally; complete the ledger and obtain one fresh whole-increment review. Address Critical/Important findings in one TDD fix pass, ledger any Minor findings, then verify final JAR metadata/class payloads/hash and fresh test/native receipts. Deliver the local26.1.2 JAR with concise notes and live-menu limitations.

## Self-review and handoff

All approved requirements map to the three tasks: Task1 owns evidence and transitions, Task2 owns eligibility/lifecycle, Task3 owns passive rendering/delivery. Interfaces and unchanged constructor contracts agree. Builder choices above keep the existing pricing model/caps and existing saved preferences, while schema-compatible legacy ownership/watch data remains readable. Native execution is preserved; implementation starts after this plan is confirmed.
