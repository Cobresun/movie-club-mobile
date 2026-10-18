# CLAUDE.md

Kotlin Multiplatform (Compose Multiplatform) movie club app for Android and iOS, backed by the web app's API at `https://movie-club.app/api` and TMDB.

## Setup and Commands

- `local.properties` needs `TMDB_API_KEY=...` or the build fails
- Always pass `-q` to Gradle
- Shared tests on the JVM (fastest): `./gradlew -q :composeApp:testAndroidHostTest`
- Android app: `./gradlew -q :androidApp:assembleDebug`
- iOS compile check: `./gradlew -q :composeApp:compileKotlinIosSimulatorArm64`; full app: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp`
- `KoinModulesTest` runs `verify()` over the whole DI graph, so a missing binding fails the tests

## Architecture Rules

Feature packages (`auth`, `club`, `reviews`, `lists`, ...) each have `domain/`, `data/` (`network/`, `repository/`, `dto/`, `mappers/`) and `presentation/`. Shared code lives in `core/`.

- Presentation → Domain ← Data. Domain imports neither; presentation never sees DTOs or data sources; composables talk to view models, not repositories
- Features don't import another feature's `data/` or `presentation/`; anything shared goes in `core/`. (`ClubDestination`/`ClubScreen` compose other features' screens as the nav host; that's expected)
- View models expose `StateFlow` state with `AsyncResult<T>` fields; screens use `collectAsStateWithLifecycle()`

## Dependency Injection (Koin)

- `di/Modules.kt`: one module per feature, gathered by `appModule`; `platformModule` (`Modules.android.kt` / `Modules.ios.kt`) adds platform implementations
- Prefer the constructor DSL: `singleOf(::Impl) { bind<Interface>() }`, `viewModelOf(::X)`. Use a `single { }` lambda only when a parameter isn't a plain `get()`
- View models with several route arguments (or a nullable one) destructure them: `viewModel { (slug: String, workId: String) -> X(slug, workId, get()) }`
- `initKoin()` runs once per process (Android `MainApplication`, iOS `setUpKoin()`); Compose uses that instance, so no `KoinApplication`/`KoinContext`

## Networking and Errors

- Session auth is a cookie (BetterAuth) handled by Ktor `HttpCookies`, persisted in DataStore. Cookie rules are in `core/data/CookieJar.kt`; see `auth/CLAUDE.md`
- Data sources wrap requests in `safeCall` (→ `Result<T, DataError.Remote>`) or `apiCall` (→ `Result<T, ApiError>`, which keeps the server's message and HTTP status). Use `apiCall` when the server explains failures (slug taken, invite expired)
- Both wrappers decode off the main thread. Ktor decodes on the caller's dispatcher, which is Main from `viewModelScope`
- User-facing error text always comes from `toUserMessage(action)`, e.g. `"Couldn't save your score. Check your connection and try again."`. Offer Retry only when `error.isRetryable`
- Log with `AppLog` (`core/platform`), never `println`. Debug lines only print in debug builds; warnings and errors also reach `AppLog.addSink` sinks
- iOS: never call `NSLog` with varargs from Kotlin (`NSLog("%@", s)` segfaults); pass one escaped string

## Offline

- `OfflineStore` keeps the last good answer of each club read on disk (re-encoded DTOs, keys in `OfflineKeys` as `club/<slug>/...`). Bump `OfflineStore.DIRECTORY_NAME` when a stored DTO shape changes incompatibly
- Repositories wrap reads in `offline.readThrough(key, serializer) { network }`: network first, stored answer on NO_INTERNET/timeout/429/5xx. It sits beneath the in-memory `SessionCaches`; `forgetClub`/`clear` (sign-in, sign-out) drop disk entries too
- `saved*()` repository methods return the kept answer without a request, so view models paint it first and then revalidate
- `NetworkMonitor` gates requests: offline ones fail at once with no retries. On reconnect `AppConnectivity` invalidates caches, then emits `reconnected`; screens reload via `OnReconnect { }`
- Writes offline fail fast; view models roll back optimistic changes. Only scores are queued (`reviews/data/ScoreOutbox`, sent on reconnect/launch after checking the current score). Never queue deletes, moves, reorders or other order-dependent writes

## Theming

- Dark scheme by default (matches the web app); a light scheme is user-selectable in Profile → Appearance (`AppearanceSettings`)
- Use `MaterialTheme.colorScheme.*` roles only, with no `Color.White` or `Color(0x...)` in composables. New colors go in `theme/Color.kt` and into both `MovieClubDarkColorScheme` and `MovieClubLightColorScheme`, keeping 4.5:1 text contrast in light
- Fixed-color exceptions: `WrappedColors` (shared story cards) and `chartSeriesColor`
- iOS appearance is set on the window (`SystemChromeAppearance`); native views (tab bar, sheets) inherit it, so don't force a style on them. "Match system" must clear the override, because iOS `isSystemInDarkTheme()` reads the window's traits

## Navigation (Navigation 3)

- Back stacks are lists of the `@Serializable` routes in `app/Route.kt`, saved across process death. New routes must implement the sealed `Route` (it is registered for polymorphic serialization with `subclassesOfSealed`, since iOS has no reflection)
- `App.kt` holds the top-level stack (`LandingPage` → `AuthGraph` → `ClubGraph`); `ClubNavHost` in `ClubDestination.kt` holds the signed-in one
- Use `push` (single-top), `pop` (never empties the stack) and `resetTo` from `app/Navigation.kt`
- Route arguments reach view models as constructor parameters via `koinViewModel { parametersOf(...) }`. `SavedStateHandle` is only for UI state that must survive process death (selected tab, search text, open sheet)
- Results between entries are plain state hoisted into the nav host (e.g. Wrapped's "Go to Reviews" sets `tabRequest`)

## Platform Gotchas

- Bottom tabs use `PlatformTabBar`. On iOS it's a native `UITabBar` in `UIKitView(placedAsOverlay = true)` (for Liquid Glass), which draws above all Compose content: it hides while `LocalNativeChromeVisible` is false. Compose dialogs are a separate layer and cover it fine
- Content scrolls under the iOS tab bar: tab screens add `LocalTabBarInset` to list `contentPadding` and FAB offsets (0.dp on Android)
- Bottom sheets: always `AppBottomSheet` + `rememberAppSheetState()`. On iOS each one is a real `UISheetPresentationController` hosting its own `ComposeUIViewController`, sized to the content's measured height, composed with the opener's locals. UIKit ignores touches while a sheet animates in or out
- Deep links (`app/DeepLinks.kt`): `/join-club/<token>`, `/club/<slug>[/<section>]`, `/share/club/<slug>/review/<workId>` and `movieclub://` equivalents. `ClubNavHost` consumes invites, `ClubScreenRoot` consumes club links. Verified App Links / universal links would need `assetlinks.json` and `apple-app-site-association` on the web host
- Material 3 Expressive APIs (`MotionScheme.expressive()`, etc.) are internal in CMP material3 1.9.0; don't use them yet

Open cleanup work: the `tech-debt-backlog` skill.
