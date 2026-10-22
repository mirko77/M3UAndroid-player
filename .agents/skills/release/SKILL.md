---
name: release
description: Cut an Android release — bump versionCode and versionName in app/smartphone/build.gradle.kts, generate release notes, and assemble release APKs
---

# Release

Cut a new release. Given an optional version number, determine the version and build code, bump them in `app/smartphone/build.gradle.kts`, prepend release notes to `CHANGELOG.md`, verify the build, assemble release APKs, and attach artifacts to the GitHub Release.

## Inputs

- `version` (optional): target version string (e.g. `2.0.11`) or build number (`211`).
  When omitted, the skill suggests the next patch version and increments `versionCode` by 1, asking for confirmation.

## Versioning scheme

- `versionName`: Semantic Versioning string `MAJOR.MINOR.PATCH` (e.g. `2.0.10`).
- `versionCode`: Monotonically increasing integer (e.g. `210`).
- Git tags match the version string: `<versionName>` (e.g. `2.0.10`).

## Steps

1. Test gate (fail fast)
   Run fast unit and contract verification:
   ```bash
   ./gradlew :i18n:testDebugUnitTest :extension:api:jvmTest :extension:runtime:test :data:testDebugUnitTest
   ```
   Do not proceed if any test fails.

2. Read current version state
   Read `app/smartphone/build.gradle.kts` to extract current `versionCode` and `versionName`.
   Check latest git tags with `git tag --list` and current branch commits.

3. Derive new version
   - `versionCode`: increment current integer by 1 (e.g. `210` -> `211`).
   - `versionName`: increment patch version (or apply user input).
   - Ask for confirmation before editing files.

4. Apply edits
   Update `app/smartphone/build.gradle.kts`:
   - `versionCode = <new_build>`
   - `versionName = "<new_version>"`
   Preserve exact formatting and indentation.

5. Generate release notes
   - Run `git log --oneline <base>..HEAD` (where base is the previous release tag).
   - Collapse commits into concise, user-facing release notes grouped by category (Features, Fixes, Improvements, Internal).
   - Prepend to `CHANGELOG.md` under `# <new_version> (<new_build>)`.

6. Build release APKs
   Compile and package release artifacts:
   ```bash
   ./gradlew :app:smartphone:assembleRelease
   ```
   Verify 16 KB native library alignment:
   ```bash
   testing/bin/verify-android-16kb.sh app/smartphone/build/outputs/published-apk/release/*.apk
   ```

7. Commit and tag (requires user confirmation)
   Show changed files (`app/smartphone/build.gradle.kts`, `CHANGELOG.md`).
   Ask before committing and tagging:
   - Commit message: `chore: bump to <new_version> (<new_build>)`
   - Annotated tag: `git tag -a <new_version> -m "Release <new_version>"`

8. Create GitHub Release and attach artifacts
   Once pushed to remote and confirmed:
   ```bash
   gh release create <new_version> --title "v<new_version>" --notes "<release_notes>"
   gh release upload <new_version> app/smartphone/build/outputs/published-apk/release/*.apk --clobber
   ```

9. Verify release
   ```bash
   gh release view <new_version> --json assets
   ```
