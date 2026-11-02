# Skills overflow tooltips

## Control

Advanced → SkyBlock → Helpers → **Skills overflow**, on by default. This is the Skills menu tooltip feature only; it adds no HUD, XP/hour display, ETA, chat announcement or command.

## Display and calculation

In SkyBlock's **Your Skills** menu, hover a maxed standard skill to see:

- Its virtual level, keeping the menu's Roman or decimal format.
- Percentage and XP progress toward the next virtual level.
- Total overflow XP above the cap shown by the menu.

The calculation follows SkyHanni's SkillUtil and SkillTooltip. Use the normal XP table through level 60, then 7.6 million XP for level 61, increasing the level cost by 600,000 XP each level; the slope doubles after every tenth virtual level. These are display estimates beyond the real cap, not additional server skill levels or rewards.

Read the total XP following **Max Skill level reached!** directly from the tooltip. Abbreviated K/M/B/T values use their displayed precision. Missing or invalid XP leaves the tooltip unchanged. Non-maxed skills, Social, Runecrafting, other menus and non-SkyBlock screens remain unchanged. Existing overflow text from another mod is not duplicated. No item stack, name, count, skill statistic or inventory is modified; only the rendered tooltip list changes.

Normal-skill XP metadata is bundled from Hypixel's public skill resource. There are no runtime API requests, profile lookups, stored player XP records or cross-profile cache entries.

## Validation

Focused checks cover exact level thresholds, the post-60 curve and slope changes, lower displayed caps, Roman/decimal names, abbreviated/malformed XP, missing data, duplicate tooltip protection and setting persistence. All supported versions compile. In game, hover a maxed skill in Your Skills, compare its total XP, and toggle the feature off to confirm the original tooltip returns.

## Last updated

2026-11-02 — Added the requested tooltip-only Skills overflow adaptation. See packaged SkyHanni skill attribution and LGPL notice.
