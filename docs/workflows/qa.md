# QA Generation Workflow

Generate QA documentation from a codebase change or QA spec file.

## Inputs

- base_ref (default: master)
  Branch, tag, or commit SHA

- qa_spec_file (optional)
  docs/QA-{version}.md

## Diff scope

git diff {{base_ref}}..HEAD

## Reconciliation logic

If QA spec exists:

- Use it as reference documentation
- Compare against git diff results
- Ensure all new changes are covered in QA output

## Responsibilities

Identify all user-impacting changes across:

- UI and Jetpack Compose screen modifications
- Media playback, ExoPlayer configuration, and decoder handling
- Room database schema migrations and query changes
- Background sync, WorkManager tasks, and periodic workers
- Navigation routes, destinations, and adaptive layout adjustments
- Extension API, Binder/AIDL IPC, and plugin integrations
- Multilingual resource keys and translations
- Build configuration, native packs, and 16 KB page-size alignment

## QA Generation Rules

For every meaningful change, generate at least one QA check.

Each QA check must include:

- **Test Description** — what is being tested.
- **Expected Result** — what success looks like.
- **Manual Action** — step-by-step staging instructions.

Rules:

- Must be executable on physical devices or Android emulators.
- No abstract descriptions.
- No unit/integration test instructions.
- Full coverage of changes required.
- No duplicates.
- The Baseline Regression Suite below is always emitted on top of the diff-derived checks; it is exempt from the "No duplicates" rule.

## Baseline Regression Suite (always included)

Every QA report MUST include the checks below, regardless of the diff. They are emitted even when the change does not touch playback, layout, insets, or extensions, and they are **not** diff-derived.

Do not deduplicate them against diff-derived checks, and do not drop them when the diff is small or unrelated. They are the standing guard for the shared core surfaces across phone and tablet layouts.

### B1. Status bar and navigation bar insets (`MainActivity.kt`, `Theme.kt`, `Toolkit.kt`)

- **Test Description:** Confirm screen content does not overlap system bars across orientation changes and edge-to-edge gestures.
- **Expected Result:** App content and top bars start cleanly below status bar; bottom navigation controls clear system navigation gestures; rotating between portrait and landscape maintains proper padding without content clipping.
- **Manual Action:** On phone and tablet staging devices; launch app; navigate between Foryou, Favorite, and Setting; rotate device to landscape and back; verify top bars and floating navigation bar respect system window insets without clipping.

### B2. Media playback and PlayerActivity lifecycle (`PlayerActivity.kt`, `PlayerManagerImpl.kt`, `ChannelMask.kt`)

- **Test Description:** Confirm live stream playback initiates reliably, responds to playback controls, and releases gracefully.
- **Expected Result:** Tapping a channel opens player; audio and video stream playback starts smoothly; mask controls (play/pause, aspect ratio, volume) respond; exiting player stops playback and releases audio focus.
- **Manual Action:** Open any live stream from a playlist; verify playback begins within 3 seconds; toggle player mask controls; press back or home to test background audio/PIP if enabled; return to app and verify player session closes cleanly.

### B3. Floating navigation bar and destination switching (`FloatingAppNavigationBar.kt`, `AppNavHost.kt`, `Destination.kt`)

- **Test Description:** Confirm navigation between primary destinations preserves tab state and scroll position.
- **Expected Result:** Switching between Foryou, Favorite, and Setting highlights the active tab icon; previous scroll positions are retained; back button returns to the home destination before exiting.
- **Manual Action:** Navigate to Favorite tab; scroll down; switch to Setting; switch back to Favorite; verify scroll position is intact; tap Back and confirm navigation returns to Foryou.

### B4. M3U playlist import and EPG background sync (`SubscriptionWorker.kt`, `PlaylistRepositoryImpl.kt`, `M3UDatabase.kt`)

- **Test Description:** Confirm adding a new M3U playlist parses streams, persists channels into Room, and schedules sync workers.
- **Expected Result:** Entering a valid M3U URL or importing a local file shows progress; channels populate in the UI; EPG guide displays programme schedules when configured; no database constraint exceptions occur.
- **Manual Action:** Go to Setting -> Add Playlist; enter test M3U playlist URL; confirm sync progress dialog completes; open the new playlist and verify channels list correctly.

### B5. Favorite channel toggle and persistence (`FavouriteViewModel.kt`, `FavoriteRepositoryImpl.kt`)

- **Test Description:** Confirm marking a channel as favorite persists across app restarts and updates the Favorite feed.
- **Expected Result:** Tapping favorite heart icon highlights the channel; channel appears in Favorite tab; restarting the application preserves favorite status.
- **Manual Action:** From Foryou or Playlist screen, tap the favorite action on a channel; navigate to Favorite tab and verify presence; force close the app from recent apps; reopen and verify channel is still in Favorite.

### B6. Playlist configuration and stream format (`PlaylistConfigurationScreen.kt`, `PlaylistConfigurationViewModel.kt`)

- **Test Description:** Confirm custom user-agent, auto-sync intervals, and stream source preferences save and apply.
- **Expected Result:** Modifying playlist configuration settings updates the database row; streams played from that playlist transmit the custom headers.
- **Manual Action:** Open Playlist Configuration for an existing playlist; change user-agent string or sync interval; save changes; play a stream and verify settings apply without crash.

### B7. Extension IPC discovery and connection (`ExtensionPluginRepositoryImpl.kt`, `CallerBoundHostBridge.kt`)

- **Test Description:** Confirm installed extension packages are discovered and bound securely via IPC.
- **Expected Result:** Installed extensions appear in Setting -> Extensions; declared permissions and signatures are validated; extension-provided playlists load into the player.
- **Manual Action:** Install reference extension APK on test device; open Setting -> Extensions; verify extension is recognized; grant access and confirm extension channels load.

### B8. Native 16 KB page-size codec pack extraction (`CodecPackRepository.kt`, `native-load.yml`)

- **Test Description:** Confirm bundled native media libraries unpack and load successfully on 16 KB page-size devices (Android 15+).
- **Expected Result:** App launches and plays streams requiring custom decoders without encountering `UnsatisfiedLinkError` or memory alignment crashes.
- **Manual Action:** On an Android 15/16 device configured with 16 KB page size; launch clean install; play stream requiring native codecs; verify video decodes smoothly with zero native crashes in logcat.

## Output Format

### 1. QA Report (Markdown file)

Grouped by feature or area.

### 2. QA CSV file (.csv)

Columns: `Action Description` | `Expected` | `Phone` | `Tablet`

> The `|` above is markdown table/list notation for readability only — it is NOT the file separator. The `.csv` file must be a standard comma-separated file (commas delimit columns), so it imports cleanly into Google Sheets.

- One row per QA check.
- Action must describe a human action in staging.
- Expected must describe observable result.
- The platform columns (`Phone`, `Tablet`) are filled in during the QA process to record outcome per device form factor (e.g. PASS/FAIL).
- Commas are used only as column separators, never inside cell text.
- Inside `Action Description` and `Expected`, replace commas with `;` to separate step lists or clauses, so they never collide with the comma column separator and the CSV imports cleanly into spreadsheet editors.
- Cell text must stay comma-free; no quoted/escaped fields are needed.

## Validation Rules

Before output, ensure:

- Full coverage of changes.
- No duplicates.
- All steps reproducible in staging.
- Diff fully mapped to QA checks, plus the Baseline Regression Suite.
- The Baseline Regression Suite is present in both the Markdown report and the CSV.
- No commas inside CSV cell text; commas only separate columns.
