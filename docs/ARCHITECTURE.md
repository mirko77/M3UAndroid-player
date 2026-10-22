# M3UAndroid — Architecture

Source of truth for the system's design. Read it before changing anything; cross-check any candidate finding against it.

M3UAndroid is a modular Kotlin Android IPTV player targeting modern Android smartphones and tablets (API 29–36). The project uses Jetpack Compose with Material 3 Adaptive layouts, Media3 / ExoPlayer for multimedia streaming, Room with SQLite for local persistence, WorkManager for background synchronization, and an IPC-based extension plugin subsystem.

| Module Area | Path | Responsibility | Primary Stack / Libraries |
|---|---|---|---|
| Application Host | `app/smartphone` | Compose UI host, Activity lifecycle, navigation graph, Hilt root | AndroidX Compose, Hilt, Jetpack Navigation |
| Business Logic | `business/*` | Feature ViewModels, MVI UI state holders, user actions | Kotlin Coroutines, StateFlow, Hilt |
| Core Foundation | `core/foundation` | Lightweight shared contracts, preferences, DLNA casting | Kotlin Coroutines, Kotlinx Serialization |
| Data Layer | `data` | Room database, DAO layer, repositories, background workers, playback service | Room, Media3 ExoPlayer, WorkManager, OkHttp |
| Extension Plugin System | `extension/*` | Extension API contracts, Android IPC transport (AIDL/Binder), plugin SDK | Android Binder/AIDL, Kotlin Multiplatform |
| Parsers | `parser/*` | M3U playlist format and Xtream Codes API stream parsers | Kotlinx Serialization |
| Localization | `i18n` | Multilingual UI string resources and locale fallback | Android Resources |
| Native Support | `native-load-gradle-plugin`, `native-packs` | 16 KB page-size native library unpacking and loader | Custom Gradle Plugin, Native Zip packs |
| Testing & Benchmarks | `testing/*` | Macrobenchmarks, embedded test mock server, reference plugin | AndroidX Benchmark, NanoHTTPD |

---

# Part 1 — Smartphone Application (`com.m3u.smartphone`)

## Entry point and core wiring

- **Application Class**: [`com.m3u.smartphone.M3UApplication`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/M3UApplication.kt) annotated with `@HiltAndroidApp`. Initializes logging and global application-level dependencies.
- **Main Activity**: [`com.m3u.smartphone.MainActivity`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/MainActivity.kt) annotated with `@AndroidEntryPoint`. Configures splash screen via `installSplashScreen()`, enables edge-to-edge system insets via `enableEdgeToEdge()`, binds the [`Helper`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/common/helper/Helper.kt) window controller, and hosts the Compose [`Toolkit`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/common/internal/Toolkit.kt) wrapping [`App`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/App.kt).
- **Player Activity**: [`com.m3u.smartphone.ui.business.channel.PlayerActivity`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/business/channel/PlayerActivity.kt) provides a dedicated singleTop activity supporting Picture-in-Picture (`android:supportsPictureInPicture="true"` in [`AndroidManifest.xml`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/AndroidManifest.xml)).
- **Startup Providers**: Uses `androidx.startup.InitializationProvider` in [`AndroidManifest.xml`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/AndroidManifest.xml) for early initialization while disabling automatic WorkManager setup in favor of on-demand Hilt WorkManager configuration.

## Rendering and UI

- Built entirely with Jetpack Compose using Compose BOM (`androidx-compose-bom` in [`gradle/libs.versions.toml`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/gradle/libs.versions.toml)) and Material 3 Adaptive navigation.
- Root UI shell in [`app/smartphone/src/main/java/com/m3u/smartphone/ui/App.kt`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/App.kt) coordinates top app bars, snackbars, navigation bars, and content panes across phone and tablet form factors.
- Screen implementations reside under `app/smartphone/src/main/java/com/m3u/smartphone/ui/business/` and observe feature ViewModels from `business/*`.

## Routing and navigation

- Navigation graph defined in [`com.m3u.smartphone.ui.common.AppNavHost`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/common/AppNavHost.kt) using Jetpack Navigation Compose (`androidx.navigation.compose.NavHost`).
- Primary destinations enumerated in [`com.m3u.smartphone.ui.material.components.Destination`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/material/components/Destination.kt):
  - `Foryou`: Home feed showing playlists and recently watched channels ([`business/foryou`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/business/foryou)).
  - `Favorite`: Saved channels and custom groups ([`business/favorite`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/business/favorite)).
  - `Setting`: Preferences, playlist management, extensions, and theme controls ([`business/setting`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/business/setting)).
- Sub-routes:
  - `Playlist`: Channel browsing within a selected playlist ([`business/playlist`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/business/playlist)).
  - `PlaylistConfiguration`: Stream source details, custom headers, auto-sync parameters ([`business/playlist-configuration`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/business/playlist-configuration)).
- Navigation UI implemented as [`FloatingAppNavigationBar`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/navigation/FloatingAppNavigationBar.kt) with adaptive positioning for portrait and landscape tablet layouts.

## Data layer and persistence

- **Database**: Local SQLite managed by AndroidX Room in [`com.m3u.data.database.M3UDatabase`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/database/M3UDatabase.kt).
  - Schema migrations defined in [`com.m3u.data.database.DatabaseMigrations`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/database/DatabaseMigrations.kt).
  - Schema export directory: `data/schemas/` with automated migration tests in `data/src/androidTest/`.
- **Primary DAOs** ([`data/src/main/java/com/m3u/data/database/dao/`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/database/dao/)):
  - `PlaylistDao`: Playlist metadata, source URLs, and configuration flags.
  - `ChannelDao`: Stream channels, category groups, license metadata, and favorite status.
  - `ProgrammeDao`: Electronic Programme Guide (EPG) data mapped by channel identity and timestamp.
  - `ExtensionDao`: Registered extension plug-ins and user trust declarations.
- **Repositories** ([`data/src/main/java/com/m3u/data/repository/`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/repository/)):
  - `PlaylistRepository`: Manages playlist creation, import from local files or network URLs, and database synchronization.
  - `ChannelRepository`: Queries and filters streams with pagination support.
  - `FavouriteRepository`: Manages user favorites and channel grouping.
  - `ProgrammeRepository`: Queries current and upcoming EPG schedules.
- **Background Synchronization**: WorkManager workers in [`com.m3u.data.worker.SubscriptionWorker`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/worker/SubscriptionWorker.kt) handle periodic background refresh of remote playlists and EPG feeds.

## Playback engine and media pipeline

- Integrated with AndroidX Media3 / ExoPlayer ([`gradle/libs.versions.toml`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/gradle/libs.versions.toml)).
- Central playback controller: [`com.m3u.data.service.PlayerManager`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/service/PlayerManager.kt) implemented by [`PlayerManagerImpl`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/service/internal/PlayerManagerImpl.kt).
- Supported protocols: HLS (`application/x-mpegURL`), DASH, RTSP, RTMP, and progressive HTTP media sources.
- DRM support: Widevine, PlayReady, and ClearKey integration via `DefaultDrmSessionManager`.
- Video UI renderer: [`com.m3u.smartphone.ui.material.components.Player`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/material/components/Player.kt) with touch control masks in `ChannelMask.kt`.

## Extension subsystem and IPC

- Modular extension architecture allows third-party Android apps to supply playlists, EPG feeds, or stream decoders.
- Public contracts defined in [`extension/api`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/extension/api) as platform-agnostic Kotlin interfaces.
- Inter-Process Communication (IPC) implemented via Android Binder and AIDL in [`extension/transport-android`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/extension/transport-android) ([`CallerBoundHostBridge`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/extension/transport-android/src/main/java/com/m3u/extension/transport/android/CallerBoundHostBridge.kt)).
- Host runtime broker in [`data/src/main/java/com/m3u/data/extension/`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/data/src/main/java/com/m3u/data/extension/) enforces cryptographic package signature verification and permission gates before binding extensions (`com.m3u.permission.BIND_EXTENSION_HOST`).

## Configuration and preferences

- Backed by key-value storage in [`core/foundation/src/main/java/com/m3u/core/foundation/architecture/preferences/`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/core/foundation/src/main/java/com/m3u/core/foundation/architecture/preferences/).
- Stores UI themes, auto-refresh intervals, user-agent overrides, video zoom defaults, and hardware acceleration options.
- No sensitive keys or user credentials stored in unencrypted SharedPreferences.

## Imports and module resolution

- Multi-project Gradle setup configured via [`settings.gradle.kts`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/settings.gradle.kts).
- Dependency injection provided by Dagger Hilt across module boundaries.
- Project modules: `:app:smartphone`, `:business:*`, `:core`, `:core:foundation`, `:data`, `:extension:*`, `:i18n`, `:parser`, `:testing:*`.

## Styling and themes

- Jetpack Compose Material 3 design tokens in [`app/smartphone/src/main/java/com/m3u/smartphone/ui/material/model/Theme.kt`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/src/main/java/com/m3u/smartphone/ui/material/model/Theme.kt).
- Supports dynamic color palette extraction on Android 12+, pre-defined themes, dark mode, and backdrop blur effects using `Haze` (`dev.chrisbanes.haze:haze`).

## Utils and domain helpers

- [`core/foundation`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/core/foundation): Lightweight shared helpers, Coroutine Flow operators, DLNA casting primitives, and packaging abstractions.

## Tests and benchmarking

- Unit tests: Located in `src/test/` of each module, executed via `./gradlew testDebugUnitTest`.
- Instrumented tests: Located in `src/androidTest/` across `:app:smartphone` and `:data`, executed via `./gradlew connectedCheck`.
- Macrobenchmarks: Located in [`testing/device-benchmark`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/testing/device-benchmark) using AndroidX Benchmark to measure startup time, frame timing, and scroll performance.
- Embedded mock server: Located in [`testing/mock-server`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/testing/mock-server) providing deterministic HTTP and HLS streams for tests.

---

# Build, release and deploy

## Build pipeline

- Configured using Gradle Kotlin DSL with Java 17 and Kotlin 2.4+.
- Build dependencies centrally versioned in [`gradle/libs.versions.toml`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/gradle/libs.versions.toml).
- Native library extraction and 16 KB page-size compatibility enforced by `dev.oxyroid.native-load` and verified via [`testing/bin/verify-android-16kb.sh`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/testing/bin/verify-android-16kb.sh).

## Artifacts

- `./gradlew :app:smartphone:assembleRelease` compiles and optimizes release builds.
- Root task `CopyPublishedApks` moves generated APKs to `app/smartphone/build/outputs/published-apk/release/` with ABI suffix naming (e.g. `2.0.10_arm64-v8a.apk`, `2.0.10_universal.apk`).

## Versions

- Application versioning resides in [`app/smartphone/build.gradle.kts`](file:///Users/mmenegazzo/Sites/M3UAndroid-fork/app/smartphone/build.gradle.kts):
  - `versionCode = <build>` (integer, monotonically increasing)
  - `versionName = "<new>"` (Semantic Versioning `X.Y.Z`)
- Git tags follow the release version string (e.g. `2.0.10`).
