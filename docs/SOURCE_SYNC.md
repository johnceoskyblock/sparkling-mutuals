# Reviewing source and documentation revisions

## Purpose

Detect changed documentation and review its relationship to source. The current implementation reference describes the [audited baseline](IMPLEMENTATION_AUDIT.md); approved behavior changes update their owning docs. Historical prose does not automatically override a newer request.

## Prerequisites

Read [contributing](../CONTRIBUTING.md), [the documentation index](README.md), [the common conventions](DOCUMENTATION_GUIDE.md), and the assigned issue with its comments. Inspect [the synchronization script](../scripts/DocumentationSync.ps1) and [its manifest](source-documentation.json) before recording anything. Use PowerShell 7 for its current APIs.

## Procedure

1. Inspect actual branch, commit, working tree and applicable `AGENTS.md` instructions. Preserve unrelated work. Safely refresh the intended remote baseline.
2. Inventory root Markdown and all `docs/**/*.md`, including untracked files. Initial onboarding reads the complete maintained set, including all 37 species; later requests re-read changed/new pages and relevant dependencies.
3. Run `./scripts/DocumentationSync.ps1` without `-Record`. Compare with the last reviewed source commit using Git diffs/history as well as the manifest. The script cannot inspect unseen remote revisions.
4. Trace the relevant owner, caller/registration, configuration, mode, lifecycle and UI consumers. Existing tests supplement this review. Distinguish current behavior from proposals and server uncertainty.
5. Implement the approved scope; update affected owners and species exceptions together. Update **Last updated** with the substantive date in America/New_York and a short explanation.
6. Reconcile source/docs/issue updates again before final delivery. Record actual automated and live validation separately, including omissions.
7. Only run `./scripts/DocumentationSync.ps1 -Record` when the manifest's all-document source-review contract is satisfied. It records every inventoried page, not only the current issue's files. If unresolved implementation changes remain, leave it unchanged and state the reviewed subset/baseline.

## Expected results

| Evidence | What it means | What it does not mean |
| --- | --- | --- |
| Last updated | Date and explanation of the page's latest substantive revision | A unique content revision or live verification |
| Normalized SHA-256 | Content identity after CRLF→LF normalization, compared against the saved manifest | Automatic detection of code/docs semantic mismatches |
| Reviewed source commit | The immutable source revision audited, plus any stated local source differences | Proof that a newer source commit has been reviewed |
| Automated validation | The named static checks/tests/builds actually executed | Minecraft/Hypixel behavior or every shader pack |
| Live validation | The stated scenario, client/server and observed result | Unmeasured server guarantees or exhaustive coverage |

The current documentation-only audit leaves the manifest unchanged: its script comment requires corresponding implementation/testing before recording, while this issue explicitly forbids gameplay fixes and retains unresolved findings. The report identifies the real source baseline rather than claiming that clearing a hash list fixes behavior.

## Troubleshooting

New/Updated/Removed rows are review prompts, not failures to hide. Several revisions can share a date. Filesystem modification times change on checkout and are not review evidence. Hash equality cannot establish correctness; a passing build cannot certify prose. Read untracked files directly because ordinary Git diffs omit their content.

Keep proposed durable template automation in a separate issue; this cleanup does not change scripts or add code to enforce prose structure.

## Last updated

2026-10-19 (America/New_York) — Consolidated navigation and audited contributor guidance. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; documentation-only changes, no local runtime differences or new live validation.
