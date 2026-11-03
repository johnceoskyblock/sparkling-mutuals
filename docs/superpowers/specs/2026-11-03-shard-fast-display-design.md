# Faster shard guidance display

Approved in this chat on 2026-11-03: remove the extra display confirmation minute, retain confirmed chat alerts, and prioritize price decisions over history work. Continue the already selected Native execution method.

The Hunting Box's borders, tooltips and saved sidebar use a separate display state. A fresh validated ask below 98.5% of its target changes a displayed SELL to HOLD immediately; SELL still enters at 99.5%. Keep the hysteresis band, all unavailable-data checks and profile/connection resets. Display state is memory-only, bounded by the advice products and never passed to sell-offer watches.

Confirmed model recommendations and chat alerts retain the two distinct server quotes at least sixty seconds apart, economic-decline checks, cooldown and scope protections. Publish current shared-quote decisions against valid cached history before parsing/writing a pending history response; publish again after accepted history.

Do not change source polling, history request serialization, provider limits, target math, top-ten selection or saved ownership. Cold missing history can still load one product at a time. This removes an extra UI wait, not the price provider's own update interval.

Only build Minecraft 26.1.2. Keep source/build changes local; no GitHub writes or live-instance replacement.
