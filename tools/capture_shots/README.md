# Capture-shot tools

## Purpose

Replay locally recorded capsule evidence and inspect provisional visual/contact diagnostics. No network access, uploads or gameplay controls. The [feature guide](../../docs/CAPTURE_SHOT_RECORDING.md) owns collection and privacy rules.

## Prerequisites

Python **3.11+**, standard library only. Extract the Actions `capture-shot-tools` ZIP; keep `tools/`, `docs/` and `src/test/resources/shots/` together. Use a new output directory for each analysis.

## Procedure

```text
python tools/capture_shots/replay.py session.jsonl --output new-replay-directory
python tools/capture_shots/contacts.py session.jsonl --output new-contact-directory
python tools/capture_shots/analyze.py --fit fit.jsonl --validation validation.jsonl --curation choices.json --output new-analysis-directory
python -m unittest discover -s tools/capture_shots/tests -p "test_*.py" -v
```


Start with [the complete collection guide](../../docs/CAPTURE_SHOT_RECORDING.md). It explains local recording controls, privacy, bounds, curation and the first small ordinary-capsule sanity check. The larger collection matrix follows after useful fields are confirmed.

Replay keeps raw updates, smooth samples, display state, targets and typed/manual feedback separate. Partial files remain incomplete; malformed interior records and unsupported schemas are rejected. Reports use new output directories and never edit inputs.

Analysis reports **visual diagnostics**, not an aiming or server capture model. Default scoring requires complete, unambiguous, curated pairs with matching split labels. `--provisional` is an explicit diagnostic option. Fitting uses first-half training; independent validation freezes variant-specific parameters and excludes its first two conditioning samples. Same-session/hash overlap is rejected. Variants without fitting evidence remain unavailable. Last observed position error is not impact error.

Try the synthetic arithmetic example (this says nothing about actual server mechanics):

```text
python tools/capture_shots/analyze.py --fit src/test/resources/shots/accelerating-fit.jsonl --validation src/test/resources/shots/accelerating-validation.jsonl --provisional --output synthetic-analysis
```

Deliberately invalid fixtures are named `invalid`, `nonfinite` and `reused-token`; `partial` and `utf8-tail` exercise recovery. `service-export` was emitted through the Kotlin coordinator/writer under controlled test observations, rather than recorded from Hypixel.

The checked [v1 schema](../../docs/schemas/capture-shots-v1.schema.json) and [v2 schema](../../docs/schemas/capture-shots-v2.schema.json) are the producer/consumer contracts. Recorder policy v3 uses schema v2 and adds one bounded contact probe per eligible ordinary input. Replay saves these in `contact-predictions.json`, separately from per-shot observed timelines. `contacts.json` reports the frozen hypothesis, proxy versions, coverage, truncation, typed/manual outcomes and raw removals. `predicted_contact_provisional` marks known contacts with incomplete target coverage or truncated probe detail; it does not establish the earliest server contact. Endpoint error remains null because no independently observed terminal coordinates exist. A finished raw export can contain a truncated probe; inspect `probe_incomplete` separately. No calibrated model ships in this kit.


## Expected results

Separate raw observations, predictions and curated outcomes. A successful replay does not establish a server hit/capture or independently measured endpoint. See [protocol](protocol.py), [replay](replay.py), [analysis](analyze.py), [contacts](contacts.py) and the existing [tests](tests).

## Troubleshooting

Unsupported schemas and malformed interior records fail. Partial exports remain incomplete. Do not repair raw files to make them appear complete or reuse an existing output directory. Supply missing evidence in a new recording and retain the original input.

## Last updated

2026-10-19 (America/New_York) — Consolidated navigation and audited contributor guidance. Reviewed source: `93de7ff3b0de2ccdd4a42bf48bc19efda6e9caee`; documentation-only changes, no local runtime differences or new live validation.
