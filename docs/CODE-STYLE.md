# M3UAndroid — Code Style and Conventions

Authoritative source of code style rules for this repository. These conventions override generic Android / Kotlin best practices.

## 1. Stack and Toolchain

- **Language**: Kotlin 2.4+ (targeting JVM 17). Kotlin only; do not introduce Java.
- **Platform**: Android SDK (compileSdk 36, minSdk 29, targetSdk 33).
- **UI Framework**: Jetpack Compose with Material 3 Adaptive (`androidx.compose.material3.adaptive`).
- **Media Engine**: Media3 / ExoPlayer 1.9+.
- **Persistence**: Room 2.8+ with Kotlin Symbol Processing (KSP).
- **Dependency Injection**: Dagger Hilt 2.59+.
- **Asynchronous / Reactive**: Kotlinx Coroutines 1.10+ and `StateFlow` / `SharedFlow`.
- **Serialization**: Kotlinx Serialization JSON.
- **Build System**: Gradle Kotlin DSL (`*.gradle.kts`) with version catalog in `gradle/libs.versions.toml`.

## 2. Formatting

- **Indentation**: 4 spaces per indent level; never use tabs.
- **Line Length**: Soft wrap around 100–120 characters; keep statements legible.
- **Trailing Commas**: Mandatory for multi-line function declarations, parameters, argument lists, enum entries, and collection literals.
- **Semicolons**: Never use semicolons at line ends.
- **Strings**: Use double quotes (`"..."`) and string templates (`"$variable"` or `"${expression}"`). Use multiline raw strings (`"""...""".trimIndent()`) for multiline blocks.
- **Braces**: Mandatory `{}` for all `if`, `else`, `when`, `for`, `while` control structures, even single-line statements.
- **Chaining**: Break method chains before the dot (`.map { ... }`, `.filter { ... }`) on a new indented line.

## 3. Naming Conventions

- **Classes / Interfaces / Objects**: `PascalCase` (e.g. `PlaylistRepository`, `PlayerManager`, `Destination`).
  - Interfaces describe capabilities or contracts; default implementation classes append `Impl` (e.g. `PlaylistRepositoryImpl`, `PlayerManagerImpl`).
- **Composable Functions**: `PascalCase` nouns or noun phrases (e.g. `ForyouScreen`, `PlayerMask`, `FloatingAppNavigationBar`).
- **Functions / Methods**: `camelCase` verbs or verb phrases (e.g. `observeAllCounts()`, `play()`, `navigateToPlaylist()`).
- **Properties / Variables**: `camelCase` (e.g. `playlistRepository`, `currentChannel`).
- **Constants**: `SCREAMING_SNAKE_CASE` for `const val` compile-time constants (e.g. `DEFAULT_BUFFER_MS`). Top-level or object values that are not compile-time constants use `PascalCase` or `camelCase` matching standard Kotlin conventions.
- **Package Names**: All lowercase, dot-separated without underscores (`com.m3u.smartphone`, `com.m3u.business.channel`).

## 4. Architectural Boundaries and Layering

Strict separation of concerns across modules:

- **UI Layer (`app/smartphone`)**:
  - Owns Compose screens, Navigation graph, Activity lifecycle, and platform integration.
  - UI composables must be pure, declarative presentation driven by state parameters and event lambdas.
  - Composable screens must never directly access Room databases, DAOs, parser internals, or raw network services.
- **Business Layer (`business/*`)**:
  - Owns feature ViewModels, MVI state containers, reducers, and workflow actions.
  - Exposes reactive `StateFlow` to the UI.
  - Independent of Android platform UI framework classes (`android.view.*`, `androidx.compose.*`).
- **Data Layer (`data`)**:
  - Owns Room database (`M3UDatabase`), entity models, DAO queries, and schema migrations.
  - Owns repository implementations, OkHttp/Retrofit network clients, parsers, and WorkManager background sync workers.
  - Repositories expose stable domain models; Room entities must not leak into business or UI layers.
- **Core Layer (`core/*`)**:
  - Lightweight shared helpers, architecture contracts, and foundation primitives.
  - Stays independent from `app` and `business` modules.
- **Extension Layer (`extension/*`)**:
  - IPC transport contracts (AIDL / Binder), runtime isolation, and plugin SDK.
- **Localization (`i18n`)**:
  - String resources and translations. No hardcoded user-facing strings in UI code.

## 5. State Management and Concurrency

- ViewModels expose state via Kotlin `StateFlow<T>` using `stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = ...)` or `SharingStarted.Lazily`.
- UI collects state with `collectAsStateWithLifecycle()` in Compose.
- Do not mutate state directly in ViewModels; use private `MutableStateFlow` and expose read-only `StateFlow`.
- Coroutine dispatchers: use `Dispatchers.IO` for disk and network I/O; `Dispatchers.Default` for CPU-intensive parsing; `Dispatchers.Main` for UI state updates.

## 6. Imports

- **No Star Imports**: Never use wildcard imports (`import foo.*`). Every imported class or function must be explicit.
- **Package Ordering**: Group standard library imports, AndroidX / Compose imports, third-party libraries, and internal project packages logically.
- **No FQCN in Code Bodies**: Package-qualified references belong in the import block. Use import aliases (`as`) for name collisions.

## 7. Testing Conventions

- Unit tests reside in `src/test/java/` or `src/test/kotlin/`.
- Instrumented tests reside in `src/androidTest/java/`.
- Use JUnit 4/5 assertions or Kotlin test utilities (`kotlin.test.*`).
- Avoid mocking framework overhead where test fakes or repository fakes provide simpler, deterministic behavior.
- Every Room schema migration must have a corresponding automated migration test.

## 8. Anti-Patterns to Avoid

- **No Java**: Kotlin only throughout the codebase.
- **No UI-in-Business Leakage**: Never pass `Context`, `View`, or Compose primitives into ViewModels or repositories.
- **No Unsafe Opt-Ins Without Justification**: Compiler opt-ins must be confined to Gradle build scripts or specific annotated call sites.
- **No Star Imports**: Disallowed by CI and project lint.
- **No Manual Database Edits**: Never modify Room entities or tables without generating the schema and writing an automated migration test.
- **No Inline Versions in Gradle**: All dependency coordinates and versions must go through `gradle/libs.versions.toml`.
