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

## Last updated

2026-10-15 — Added revision-based source synchronization and the Macaw override; normalized line endings for cross-platform checkouts.
