# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. See "Design Philosophy" section for principles that guide architectural decisions.

## Project Overview

This is a Kotlin Multiplatform mobile application for a movie club, targeting Android and iOS using Compose Multiplatform. The app allows users to authenticate, join movie clubs, manage watchlists, and review movies with integration to The Movie Database (TMDB) API.

## Required Setup

**TMDB API Key**: Before building, you must add your TMDB API key to `local.properties` at the project root:
```
TMDB_API_KEY=your_api_key_here
```
The build will fail without this key.

## Common Commands

**IMPORTANT:** Always use `-q` (quiet mode) for Gradle commands to minimize output and conserve tokens in Claude Code's context window.

### Building
- `./gradlew -q build` - Build all targets (Android + iOS)
- `./gradlew -q :androidApp:assembleDebug` - Build Android debug APK
- `./gradlew -q :androidApp:assembleRelease` - Build Android release APK

### Testing
- `./gradlew -q allTests` - Run all tests across all targets
- `./gradlew -q :composeApp:testAndroidHostTest` - Run shared tests on the JVM (Android host tests)
- `./gradlew -q iosSimulatorArm64Test` - Run iOS simulator tests
- `./gradlew -q connectedDebugAndroidTest` - Run Android instrumentation tests on connected device

### Code Quality
- `./gradlew -q check` - Run all verification checks
- `./gradlew -q lint` - Run Android lint
- `./gradlew -q lintFix` - Apply safe lint fixes

### Platform-Specific
- `./gradlew -q linkDebugFrameworkIosSimulatorArm64` - Link iOS framework for simulator

## Architecture

### Code Organization (in `commonMain`)

The codebase follows a clean architecture pattern with feature-based modules:

- **Domain Layer** (`domain/`): Business logic interfaces (repositories, domain models)
- **Data Layer** (`data/`): Implementations of domain interfaces
  - `network/` - API data sources (Ktor-based)
  - `repository/` - Repository implementations
  - `dto/` - Data transfer objects
  - `mappers/` - DTO to domain model converters
- **Presentation Layer** (`presentation/`): ViewModels and Composables
  - ViewModels handle state management
  - Screens/components for UI

**Important data access policies:**
Presentation -> Domain <- Data
Presentation layer can access Domain, and Data layer can access Domain, but Domain can never access either of them.
Presentation cannot access Data, and Data cannot access Presentation.

See the "Design Philosophy" section below for detailed guidance on maintaining these boundaries.

### Dependency Injection
Uses **Koin** (4.2) for DI:
- `di/Modules.kt` holds one module per feature (`coreModule`, `authModule`, `reviewsModule`, ...), gathered by `appModule` with `includes(...)`; `platformModule` (`Modules.android.kt` / `Modules.ios.kt`) adds platform implementations
- Constructor DSL by default: `singleOf(::Impl) { bind<Interface>() }`, `viewModelOf(::SomeViewModel)`; a `single { }` lambda only when a parameter isn't a plain `get()` (defaults, callbacks, factory functions)
- `initKoin()` runs once per process: Android `MainApplication.onCreate`, iOS `iOSApp.init` (via `setUpKoin()`). Compose uses that instance (no `KoinApplication`/`KoinContext`)
- `KoinModulesTest` (androidHostTest) runs `verify()` over the whole graph, so a missing binding fails the tests

### Networking
- **Ktor** client with platform-specific engines (OkHttp for Android, Darwin for iOS)
- Session cookie authentication automatically managed by Ktor HttpCookies plugin
- Cookies persisted via `SessionCookieStorage` (DataStore-based)
- Base API URL: `https://movie-club.app/api`

### Data Persistence
- **DataStore** for session cookie persistence (platform-specific implementations via expect/actual)
- Session cookies serialized as delimited strings (`||` separator)
- Cookies automatically sent/received by Ktor client on all requests

### Offline Support
- `core/data/OfflineStore` keeps the last good answer of each club read on disk as JSON (re-encoded DTOs, one file per key; Android `filesDir/offline-v1`, iOS Application Support). Keys live in `OfflineKeys` (`club/<slug>/...`); bump `OfflineStore.DIRECTORY_NAME` when stored DTO shapes change incompatibly (undecodable files are dropped anyway)
- It sits beneath the in-memory `SessionCaches`: repositories wrap fetches in `offline.readThrough(key, serializer) { network }` (network first; on NO_INTERNET/timeout/429/5xx the stored answer is returned as Success). `forgetClub` / `clear` (sign-in, sign-out) remove disk entries too
- Stale-while-revalidate: `savedClubs`/`savedMember`/`savedMembers`/`savedReviews`/`savedLists`/`savedItems` return the kept answer without a request; `MemberViewModel`, `ReviewsViewModel` and `ListsViewModel` paint it on first load, then revalidate
- `NetworkMonitor` (Android ConnectivityManager, iOS NWPathMonitor) feeds `HttpClientFactory`'s offline gate (requests fail at once with `OfflineException` → NO_INTERNET, no retries) and `AppConnectivity` (domain `Connectivity`): on reconnect it invalidates `SessionCaches`, then emits `reconnected`; `OnReconnect { }` reloads the club shell's tabs quietly. `OfflineBanner` shows in the club shell while offline
- Writes offline fail fast; view models roll back optimistic changes and offer Retry. The one exception is setting a score: `reviews/data/ScoreOutbox` keeps it on disk (`saveScore` returns `ScoreSave.QUEUED`, `getReviews` shows it) and `AppConnectivity` sends it on reconnect or launch, checking the member's current score first so it's never created twice. Deletes, moves, reorders and other destructive or order-dependent writes are never queued
- Images: `ConfigureImageLoader` (in `App`) gives Coil a persistent 300 MB disk cache (iOS Caches dir instead of tmp), so seen posters show offline

### Theming

The app uses Material Design 3 with a complete dark color scheme.

**Theme Structure:**
- `/theme/Color.kt` - Color palette and ColorScheme definitions
- `/theme/Theme.kt` - AppTheme composable configuration

**Color Usage Guidelines:**
- Always use `MaterialTheme.colorScheme.*` properties
- Never use hardcoded `Color.White`, `Color.Gray`, etc.
- Never use `Color(0x...)` hex literals in composables
- Use semantic color roles:
  - `primary` - Main brand color (blue)
  - `secondary` - Accent/highlight color (gold)
  - `error` - Destructive actions (red)
  - `surfaceVariant` - Differentiated surfaces (chips, cards)
  - `onSurfaceVariant` - Subdued text
  - `outline` - Borders and dividers

**Adding New Colors:**
1. Define color value in `Color.kt` palette (e.g., `val Purple80 = Color(0xFF...)`)
2. Add to `MovieClubDarkColorScheme` using appropriate Material 3 role
3. Use `MaterialTheme.colorScheme.{role}` in composables

**Future Enhancements:**
- Light theme support (add `MovieClubLightColorScheme`)
- Custom typography (add `Type.kt`)
- Custom shapes (add `Shape.kt`)

Reference: [Material Design 3 Color System](https://m3.material.io/styles/color/roles)

### Navigation
Uses Jetpack Compose Navigation with type-safe routes defined in `app/Route.kt`:
- `LandingPage` - Entry point
- `AuthGraph` - Authentication flow
- `ClubGraph` - Main app (clubs, watchlist, reviews)
- `Club(clubId)` - Individual club view

### State Management
- ViewModels use `StateFlow` for reactive state
- `AsyncResult<T>` wrapper for async operations (Loading, Success, Error states)
- `collectAsStateWithLifecycle()` in Composables

See "Design Philosophy - Error Handling" for best practices on handling errors in ViewModels.

### Layer Abstraction Boundaries

**Dependency Rule:** Presentation → Domain ← Data (Domain never depends on either)

**What Each Layer Should Know:**

**Domain Layer** (`domain/`):
- ✅ Business concepts (Member, Club, Review, WatchListItem)
- ✅ Repository contracts (interfaces only)
- ✅ Error types (DataError.Remote)
- ❌ How data is fetched (no HTTP, no SQL)
- ❌ How data is cached (no JSON files, no DataStore)
- ❌ UI concerns (no Composables, no ViewModels)

**Data Layer** (`data/`):
- ✅ Repository implementations
- ✅ Network data sources (Ktor clients)
- ✅ DTOs and mappers
- ✅ Can import from domain layer
- ❌ Cannot import from presentation layer

**Presentation Layer** (`presentation/`):
- ✅ ViewModels, Composables, UI state
- ✅ Can import from domain layer
- ❌ Cannot import from data layer (no DTOs, no data sources)
- ❌ Cannot import repositories directly in Composables (use ViewModels)

**Good Pattern - Clean Boundary:**
`ClubViewModel` (lines 28-33) depends only on repository interfaces:
```kotlin
class ClubViewModel(
    savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val watchListRepository: WatchListRepository,
    private val tmdbRepository: TmdbRepository
)
```

#### Gradual Improvements

**Short-term (low effort):**
- Add helper function `DataError.Remote.toUserMessage(): String` to centralize error messages
- Remove println statements, add proper logging framework
- Add `isRetryable()` property to DataError types

**Medium-term (moderate effort):**
- Enhance DataError with context:
  ```kotlin
  sealed class DataError {
      data class Remote(
          val type: RemoteErrorType,
          val httpCode: Int? = null,
          val message: String? = null
      ) : DataError
  }
  ```
- Add retry mechanism to ViewModels for transient errors
- Distinguish "loading cached data" from "loading fresh data" in UI

**Long-term (higher effort):**
- Implement proper error boundaries (separate flows for data vs errors)
- Add error analytics/monitoring
- Create user-facing error recovery UI (pull-to-refresh, retry buttons)

## Development Notes

- Platform chrome: the club's bottom tabs use `PlatformTabBar` (expect/actual). Android renders a Material `NavigationBar`; iOS hosts a native `UITabBar` via `UIKitView(placedAsOverlay = true)` so iOS 26 gives it Liquid Glass. Overlay views draw above all Compose content, so the bar hides itself while `LocalNativeChromeVisible` is false (for anything Compose draws over it in the same layer). Compose dialogs render in their own layer and cover it correctly.
- Bottom sheets: always `AppBottomSheet` + `rememberAppSheetState()` (expect/actual). Android draws a Material `ModalBottomSheet`; iOS presents a real `UISheetPresentationController` page sheet hosting the content in its own `ComposeUIViewController` (native grabber, spring, dimming, swipe-to-dismiss, sheet-on-sheet stacking). Its one detent is the content's measured height, capped at the system maximum. The sheet composes with the opener's composition locals (theme, Koin, view-model owner) under its own platform ones (focus, keyboard, interop). UIKit ignores touches while a sheet animates in or out, as in native apps.
- Content scrolls beneath the iOS glass tab bar: `ClubScreen` skips the bottom Scaffold padding where `tabBarOverlaysContent` is true and provides `LocalTabBarInset`; tab screens add it to their list `contentPadding` and FAB offsets (it is 0.dp on Android).
- Deep links: `app/DeepLinks.kt` parses `https://movie-club.app/join-club/<token>`, `/club/<slug>[/<section>]` and shared reviews `/share/club/<slug>/review/<workId>` (plus the `movieclub://` equivalents); Android `MainActivity` and iOS `onOpenURL` call `DeepLinks.handle`. `ClubNavHost` consumes invite links once signed in and opens the linked club; `ClubScreenRoot` then consumes club links (tab + review details sheet). Verified Android App Links / iOS universal links would need `/.well-known/assetlinks.json` and `apple-app-site-association` on the web host.
- Material 3 Expressive APIs (`MotionScheme.expressive()`, etc.) are internal in the stable CMP material3 1.9.0 artifact, so they aren't used yet.
- The app currently uses a dark color scheme by default (see `AppTheme` in `app/App.kt`)

