1. Add shared `C` completion relay and replay on `/v1/sync`, and advertise `critter-completion-v1` when available. Retain the agreed complete 37-species snapshots, per-reporter replacement, 45-second expiry and server-computed replay age. The client now supports the welcome capability.

2. Finalize the run-identity wire contract. Use the server-generated `hotspots.run` UUID as the authoritative `runId`. Define the exact H hello fields for `visitId` and `resumeRunId`, which subsequent H operations require `runId`, and the versioned C payload and relay-envelope shapes containing `runId`. The current C decoder accepts the previously proposed version-1 rows format, so adding required run fields needs a coordinated client update.

3. Provide exact reconciliation response examples for `resumed`, `joined`, `new` and `pending`. Bind pre-hello reports to their originating visit. Withhold retained assignments and completion evidence while run identity is ambiguous. Confirmed new runs must isolate reports and locks while preserving the party's fairness history.

4. Define the exact end-of-replay message, including how it identifies the reconciled run and current connection. Send it after assignments and unexpired completion snapshots, before subsequent live updates. Clients need this contract before implementing the corresponding readiness gate.

5. Confirm the primary backend's hotspot timing matches the requested behavior. Wait until every expected roster member is present, admitted and has reported, or until 30 seconds after the first eligible report. At that deadline, assign only admitted attendees. Preserve the deadline through retries and reconnects, retain pre-hello reports for reconciliation, and process late eligible arrivals in 250ms batches. The local fallback timing has already been updated.

6. Confirm assignment-instruction recipients are selected by active WebSocket assignment-display capability. Manual hotspot reporting must not classify a connected display client as a non-mod player. Keep instructions leader-only, with stable notification IDs and acknowledgments after actual chat submission.

7. Confirm the informational counting units for `checks` before relying on them across clients. Current Mutuals values represent observed individuals/captures, with Gazer capture-plus-render accounting capped at four; bird values currently reflect captures. Nest, mound and wall completion is carried through qualified flags and existing source messages. Completion decisions use flags rather than generic numerical thresholds.
