# Agent commands

Canonical source for every custom command/skill in this repository.

## Canonical location

```
.agents/skills/<name>/SKILL.md   # one directory per command, file must be named SKILL.md
```

Each `SKILL.md` has frontmatter `name` (must match directory) + `description` and a Markdown body. This is the [Agent Skills](https://agentskills.io) standard (also called "agentskills").

Edit **only** these files to change a command. Every other location is a thin wrapper that must not duplicate the body.

## Installed skills

| Skill | Description |
|---|---|
| `commit` | Create Conventional Commits from changes, splitting by type and scope |
| `release` | Cut an Android release — bump versionCode and versionName in app/smartphone/build.gradle.kts, generate release notes, and assemble release APKs |
| `review` | Review local changes against the repository base branch |
| `issue` | Convert the current AI plan into a copy-paste-ready GitHub issue, and save the draft to docs/issues/draft/ |
| `publish-issue` | Pick a pending issue from docs/issues/draft/, show it for review, then publish to GitHub on confirmation |
| `false-positive` | Mark a review finding as false positive — comment code and record in known-review-false-positives.md |
| `greptile-loop` | Drive a change to a clean greptile review — implement, commit, run "greptile review", fix findings, repeat until 0 comments / confidence 5/5 or only confirmed false positives remain |
| `qa` | Generate QA documentation from a codebase change or QA spec file |

## Per-agent mapping

| Agent | How it discovers the canonical skill | Native invocation | Wrapper needed? |
|---|---|---|---|
| **Command Code** | `.agents/skills/` — natively, shows `[.agents]` badge in `/skills` | `/<name>` or `/skill:<name>` | No |
| **OpenCode** | `.agents/skills/` — via `skill` tool (also lists `.agents/skills` automatically) | `/<name>` via wrapper | Yes — `.opencode/commands/<name>.md` |
| **Codex CLI** | `.agents/skills/` — every level from `cwd` up to repo root (+ `~/.agents/skills/`) | `$<name>` / `/skills` picker / implicit | No |
| **Claude Code / Cursor / Gemini CLI** | `.agents/skills/` — via the same standard | `/<name>` or skill picker | No |

Wrappers:

- `.opencode/commands/<name>.md` — frontmatter `description` + `agent: build`, body forwards to `.agents/skills/<name>/SKILL.md` and passes `$ARGUMENTS` through verbatim without interpreting them.

Do not add native commands/skills under `.commandcode/commands/`, `.opencode/skills/`, `.claude/skills/`, `.cursor/`, etc. to avoid shadowing the canonical source. If a harness complains about a missing `/<name>`, add a wrapper that references the canonical file — never copy the body.
