# Full ESP consistency implementation plan

Approved design: Full and Full No Auto Update only. Add intact Rockmite mound always-show and Unique remaining-check overrides, both default off. Independent per-species glow/box dropdowns preserve existing choices. Use boxes for models that cannot reliably glow. Investigate Full Clear Forest hive omissions.

Implementation inline, using existing Kotlin config/catalog and ESP trackers. Standard settings/runtime must retain behavior and preserve Full-only settings on save/reset.

- [x] Add failing regressions for independent species styles, persistence, defaults and edition isolation; implement config and dropdowns.
- [x] Add regressions for mound/Unique completion overrides; implement shared eligible-target rules with master switches respected.
- [x] Trace glow entity support and Forest hive gates; add regression coverage before focused fixes. Preserve captured/removed target exclusions.
- [x] Update feature docs/next notes. Run documentation audit, Full/NoAutoUpdate configured target tests/builds and Standard boundary verification; record receipts. No publishing or installation in this task.

Validation: Full, Full No Auto Update and Standard buildAllTargets passed on all three targets; verifyReleaseMatrix verified all nine. Initial red test failed on missing new API; UI annotation/default failures were fixed before the successful matrix. Live Hypixel visuals remain unverified. Receipts: outputs/full-esp-20261126 in the Codex workspace. No GitHub publishing or instance replacement.
