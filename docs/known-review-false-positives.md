# Known Review False Positives

Findings that look like defects but are by design. The review workflow (`docs/workflows/review.md`) instructs reviewers to read this file and drop any candidate that matches an entry. Keep this list current: when a design changes, update or remove the affected entries so stale entries do not mask real regressions.

<!-- Entries format:
- **<Summary>** (`<file>`, `<condition/branch>`)

  Reported as: "<finding Explanation verbatim>"

  Why it is by design: <reason>. See <Section> of `docs/ARCHITECTURE.md` for the full invariant.
-->
