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

### Building
- `./gradlew build` - Build all targets (Android + iOS)
- `./gradlew assembleDebug` - Build Android debug APK
- `./gradlew assembleRelease` - Build Android release APK

### Testing
- `./gradlew allTests` - Run all tests across all targets
- `./gradlew testDebugUnitTest` - Run Android debug unit tests
- `./gradlew iosSimulatorArm64Test` - Run iOS simulator tests
- `./gradlew connectedDebugAndroidTest` - Run Android instrumentation tests on connected device

### Code Quality
- `./gradlew check` - Run all verification checks
- `./gradlew lint` - Run Android lint
- `./gradlew lintFix` - Apply safe lint fixes

### Platform-Specific
- `./gradlew linkDebugFrameworkIosSimulatorArm64` - Link iOS framework for simulator

## Architecture

### Module Structure
- `/composeApp` - Shared Kotlin Multiplatform code
  - `commonMain` - Code shared across all platforms (business logic, UI, networking)
  - `androidMain` - Android-specific implementations (Application, MainActivity, DataStore)
  - `iosMain` - iOS-specific implementations (MainViewController, DataStore)
  - `nativeMain` - Shared native platform code
- `/iosApp` - iOS app entry point and Xcode project

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

### Key Features/Modules
- `auth/` - Netlify Identity authentication with bearer token storage
- `club/` - Movie club management
- `member/` - Club membership management
- `reviews/` - Movie reviews with scoring system
- `watchlist/` - Movie watchlist/backlog
- `tmdb/` - The Movie Database API integration
- `core/` - Shared utilities, domain models, networking (HTTP client factory), and presentation components

### Dependency Injection
Uses **Koin** for DI:
- Platform-specific modules defined in `Modules.android.kt` and `Modules.ios.kt`
- Shared module defined in `Modules.kt`
- Initialized via `initKoin()` in platform entry points

### Networking
- **Ktor** client with platform-specific engines (OkHttp for Android, Darwin for iOS)
- Bearer token authentication via `BearerTokenStorage`
- Base API URL: `https://cobresun-movie-club.netlify.app`

### Data Persistence
- **DataStore** for bearer token storage (platform-specific implementations via expect/actual)
- **Wire Protocol Buffers** for bearer token serialization (see `proto/bearer_token.proto`)

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

## Design Philosophy

This section documents design principles inspired by "A Philosophy of Software Design" that guide development in this codebase. These principles help maintain deep modules with simple interfaces, clear abstractions, and robust error handling. The goal is strategic programming: investing in design that reduces long-term complexity.

### Core Principles

**Deep Modules:** Feature modules (auth, member, reviews, watchlist) expose simple repository interfaces (2-7 methods) while hiding complex implementation details (network calls, error handling, DTO mapping).

**Information Hiding:** Implementation details stay in the data layer. Domain layer defines only contracts. Presentation layer knows nothing about DTOs, HTTP clients, or databases.

**Explicit Error Handling:** Use typed errors (`Result<T, DataError.Remote>`) to make failure modes visible. Avoid silent failures or generic exceptions.

**Strategic Programming:** Invest design effort in boundaries that pay off long-term (repository interfaces, domain models, error types). Accept tactical shortcuts in low-change areas (UI layout details).

### Deep Modules & Information Hiding

**What makes a module "deep"?** A simple interface that hides significant complexity.

**Good Examples in this codebase:**

**Repository Interfaces** (e.g., `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/member/domain/MemberRepository.kt`):
- Interface: 2 methods, simple suspend functions
- Implementation: Hides network calls, error handling, DTO mapping
- Callers just call `getClubs()` and get a `Result<List<Club>, DataError.Remote>`

**Repository Implementations** (e.g., `MemberRepositoryImpl.kt`):
- Simple, direct network calls with immediate DTO-to-domain mapping
- Clean separation: network layer handles HTTP, repository handles business logic
- ViewModels don't know about network implementation details

**What to preserve:**
- Keep repository interfaces small (2-10 methods maximum)
- Keep domain models free of implementation details (no DTOs, no network concerns)
- Keep feature modules independent (member/, reviews/, watchlist/ don't import from each other)

**Known Issue - Leaky Abstraction:**
`WatchListItem` domain model contains `TmdbExternalDataDto` (line 13 of `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/watchlist/domain/WatchListItem.kt`). This is a DTO leaking into domain layer.

**Gradual Improvement:** Create a pure domain model `TmdbExternalData` and map from DTO in data layer.

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

#### Checklist: Maintaining Clean Boundaries

When adding new code, verify:
- [ ] Domain interfaces import only other domain types (no data, no presentation)
- [ ] Domain models contain no DTOs, no network types, no database types
- [ ] Data layer mappers convert DTOs to domain models at the boundary
- [ ] Presentation layer imports only domain types (no importing `data.*` packages)
- [ ] ViewModels depend on repository interfaces, not implementations
- [ ] Composables depend on ViewModels, not repositories directly

### Error Handling Philosophy

**Principle:** Make errors explicit, typed, and actionable. Avoid silent failures.

**Current Error Architecture:**

**Typed Errors** (`DataError.Remote`):
```kotlin
enum class Remote: DataError {
    REQUEST_TIMEOUT, TOO_MANY_REQUESTS, NO_INTERNET,
    SERVER, SERIALIZATION, UNKNOWN
}
```

**Result Type** (`Result<T, E>`):
- Success case: `Result.Success(data)`
- Error case: `Result.Error(error)`
- Enables functional error handling with `onSuccess`, `onError`, `map`, `flatMap`

**Good Patterns:**

**1. Typed error conversion at network boundary** (`HttpClientExt.kt` lines 13-31):
```kotlin
suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse
): Result<T, DataError.Remote> {
    return try {
        execute()
    } catch (e: SocketTimeoutException) {
        Result.Error(DataError.Remote.REQUEST_TIMEOUT)
    } catch (e: UnresolvedAddressException) {
        Result.Error(DataError.Remote.NO_INTERNET)
    }
}
```

**2. User-facing error messages** (`ClubViewModel.kt`):
```kotlin
private val _errorMessage = MutableStateFlow<String?>(null)
val errorMessage = _errorMessage.asStateFlow()
```

**Known Issues:**

**1. Generic error messages** (ClubViewModel lines 85, 94, 103, etc.):
- Current: `"Failed to add movie to backlog"`
- Missing: Why it failed (network? timeout? server error?)

**2. No retry hints:**
- Errors don't indicate if retry would help
- `NO_INTERNET` should suggest "Check connection and retry"
- `SERVER` should suggest "Try again later"

**3. Debug println statements:**
- Line 24 in `HttpClientExt.kt`: `println("Error: $e")`
- Should use proper logging framework or remove in production

#### Checklist: Robust Error Handling

When implementing network operations:
- [ ] Use `Result<T, DataError.Remote>` for all repository methods that can fail
- [ ] Map specific exceptions to specific error types in `safeCall` wrapper
- [ ] Provide context in error types (consider adding error messages or codes)
- [ ] Convert errors to user-friendly messages in ViewModel
- [ ] Distinguish retryable errors from permanent failures
- [ ] Avoid silent failures - make errors visible to users when appropriate
- [ ] Never swallow errors without logging or user feedback

When implementing ViewModels:
- [ ] Expose error state as `StateFlow<String?>` or `StateFlow<UiText?>`
- [ ] Map DataError types to actionable user messages
- [ ] Include retry actions for transient errors (timeout, no internet)
- [ ] Clear error state after user acknowledgment
- [ ] Log errors for debugging (replace println with proper logger)

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

### Strategic vs Tactical Programming

**Strategic:** Invest design time in interfaces that will be called frequently or by many clients. Examples: repository interfaces, domain models, error types, core utilities.

**Tactical:** Accept quick solutions in code that changes rarely or has few clients. Examples: specific UI layouts, one-off data transformations, feature flags.

**Where to Invest (High ROI):**

**Repository Interfaces:**
- Called by all ViewModels in a feature
- Changes ripple through entire feature
- Keep methods minimal, well-named, obvious
- Example: `MemberRepository` (2 methods, both clear)

**Domain Models:**
- Used across all layers
- Changing fields affects DTOs, mappers, cache, UI
- Keep fields minimal, types strong
- Example: `Review`, `WatchListItem`, `Club`

**Error Types:**
- Used throughout application
- Poor error handling affects user experience
- Invest in making errors informative and actionable

**Core Utilities:**
- `Result<T,E>` type, `safeCall` wrapper, `AsyncResult` - used everywhere
- Getting these wrong affects entire codebase

**Where to Be Tactical (Low ROI):**

**UI Layout Details:**
- Spacing, colors, specific component arrangement
- Easy to change, affects only one screen
- Don't over-engineer

**One-off Mappers:**
- DTO to domain conversion for rarely-used types
- Simple transformation, called in one place

**Feature-Specific Logic:**
- Business rules unique to one feature
- Example: WatchList item ordering logic

**When in Doubt:** If the code has multiple clients or changes frequently, invest strategically. If it's isolated and stable, be tactical.

### Development Checklists

#### Adding a New Feature Module

Before starting:
- [ ] Identify the domain entities (business concepts)
- [ ] Design repository interface with minimal methods (aim for 2-7)
- [ ] Plan repository methods (read vs write operations)
- [ ] Plan error scenarios (what can fail? how should users be informed?)

Directory structure:
- [ ] Create `{feature}/domain/` - repository interface, domain models
- [ ] Create `{feature}/data/network/` - data source interface and Ktor implementation
- [ ] Create `{feature}/data/repository/` - repository implementation
- [ ] Create `{feature}/data/dto/` - network data transfer objects
- [ ] Create `{feature}/data/mappers/` - DTO to domain conversions
- [ ] Create `{feature}/presentation/` - ViewModel, Composables

Domain layer (write first):
- [ ] Define domain models (plain data classes, no DTOs)
- [ ] Define repository interface (methods return `suspend fun Result<T, E>`)
- [ ] Keep interface minimal - only methods ViewModels actually need
- [ ] Add KDoc explaining the purpose of each method

Data layer:
- [ ] Create DTOs matching API structure (use `@Serializable`)
- [ ] Create data source interface with minimal surface area
- [ ] Implement Ktor data source using `safeCall` wrapper
- [ ] Implement repository with direct network calls (see MemberRepositoryImpl as reference)
- [ ] Create mappers that convert DTOs → domain models at data layer boundary

Presentation layer:
- [ ] Create ViewModel depending only on repository interfaces
- [ ] Expose state as `StateFlow<State>` where State contains `AsyncResult<T>` fields
- [ ] Expose error state as `StateFlow<String?>` for user feedback
- [ ] Handle actions with sealed interface (e.g., `ClubAction`)
- [ ] Use `viewModelScope.launch` for operations, proper error handling

Dependency injection:
- [ ] Register in `Modules.kt` following existing patterns
- [ ] Use `.bind<Interface>()` to bind implementations to interfaces
- [ ] Register ViewModel with `viewModel { }` delegate

Testing:
- [ ] Add unit tests for mappers (DTO → domain)
- [ ] Add repository tests with mocked data sources
- [ ] Add ViewModel tests with mocked repositories

#### Creating/Modifying Repository Interfaces

Design principles:
- [ ] Keep interface small (2-10 methods, ideally 2-5)
- [ ] Return `suspend fun Result<T, DataError.Remote>` for all operations
- [ ] Use descriptive method names (getReviews, postReview, deleteReview)
- [ ] Avoid generic methods like `get(id: String): Any` - be specific

Parameters:
- [ ] Pass only domain types in parameters (no DTOs)
- [ ] Keep parameter lists short (1-3 parameters ideal)
- [ ] Use domain IDs as Strings (clubId, memberId, reviewId)

Return types:
- [ ] Wrap results in `Result<T, DataError.Remote>` to make errors explicit
- [ ] Return domain models, never DTOs

Documentation:
- [ ] Add KDoc to interface explaining overall purpose
- [ ] Document each method's behavior
- [ ] Document error cases (when does it return Error vs empty list?)
- [ ] If method has unusual behavior (like `postWatchListFromMovie` going to backlog), document it

Example:
```kotlin
/**
 * Repository for managing movie reviews.
 */
interface ReviewsRepository {
    /**
     * Gets reviews for a club from the network.
     * Returns error if network request fails.
     */
    suspend fun getReviews(clubId: String): Result<List<Review>, DataError.Remote>

    /** Posts a new review. */
    suspend fun postReview(clubId: String, review: NewReviewItem): Result<Unit, DataError.Remote>
}
```

#### Implementing Error Handling

In data sources (network layer):
- [ ] Wrap HTTP calls with `safeCall<T> { }` wrapper
- [ ] Let `safeCall` handle exception mapping to DataError types
- [ ] For custom errors, add new cases to `DataError.Remote` enum
- [ ] Never catch and ignore exceptions silently

In repositories:
- [ ] Return errors directly - let ViewModels handle error display logic
- [ ] Use `.map { }` to convert DTOs to domain models
- [ ] Document whether empty list means "no data" or "error occurred"
- [ ] Consider: should users distinguish empty state from error state?

In ViewModels:
- [ ] Expose `StateFlow<String?>` for error messages
- [ ] Use `.onError { }` to set user-friendly error messages
- [ ] Map DataError types to messages users can understand
- [ ] Clear error state after user acknowledgment (OnClearError action)
- [ ] For critical errors, prevent user actions until resolved

In UI:
- [ ] Display errors with Snackbar or alert dialog
- [ ] Provide retry option for transient errors (timeout, no internet)
- [ ] Use `AsyncResultHandler` composable for loading/error/success states
- [ ] Show empty state differently from error state

Error message quality:
- [ ] Avoid: "Failed to add movie" (too generic)
- [ ] Better: "Could not add movie. Check your connection and try again."
- [ ] Best: Use `UiText.StringResourceId` for localized, contextual messages

#### Writing ViewModels

Structure:
- [ ] Extend `ViewModel` from androidx.lifecycle
- [ ] Inject only repository interfaces (never implementations)
- [ ] Keep ViewModels feature-specific (one per screen/flow)

State management:
- [ ] Expose state as `StateFlow<State>` where State is a data class
- [ ] Use `MutableStateFlow` internally and expose via `.asStateFlow()`
- [ ] Load data in init block or in response to user actions
- [ ] Update state using `.update { }` for thread-safe modifications
- [ ] Never expose mutable state directly - use `.asStateFlow()`

Actions:
- [ ] Create sealed interface for actions (e.g., `ClubAction`)
- [ ] Handle all actions in single `onAction(action: Action)` method
- [ ] Launch operations in `viewModelScope.launch`
- [ ] Use `.onSuccess { }` and `.onError { }` for result handling

Error handling:
- [ ] Expose `StateFlow<String?>` for error messages
- [ ] Set errors in `.onError { }` blocks
- [ ] Provide action to clear errors (e.g., `OnClearError`)

Best practices:
- [ ] Launch coroutines in init block to load initial data
- [ ] Use `viewModelScope.launch` for all suspend operations
- [ ] Avoid nested `launch` calls - flatten with suspend functions
- [ ] Keep ViewModels testable - no Android dependencies in logic

Example pattern:
```kotlin
class FeatureViewModel(
    private val repository: FeatureRepository
) : ViewModel() {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _state = MutableStateFlow(FeatureState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(data = AsyncResult.Loading) }
            when (val result = repository.getData()) {
                is Result.Success -> {
                    _state.update { it.copy(data = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(data = AsyncResult.Error()) }
                }
            }
        }
    }

    fun onAction(action: FeatureAction) {
        when (action) {
            is FeatureAction.OnSomething -> {
                viewModelScope.launch {
                    repository.doSomething()
                        .onSuccess {
                            loadData() // Reload after mutation
                        }
                        .onError { _errorMessage.update { "User-friendly message" } }
                }
            }
            is FeatureAction.OnClearError -> _errorMessage.update { null }
        }
    }
}
```

#### Code Review Criteria

Architecture and boundaries:
- [ ] Domain layer imports only other domain types (no data, no presentation)
- [ ] Domain models contain no DTOs or implementation details
- [ ] Data layer mappers convert DTOs to domain models at boundaries
- [ ] Presentation layer depends only on domain interfaces
- [ ] Repository implementations are not imported in ViewModels (only interfaces)
- [ ] No cross-feature dependencies (reviews importing from members)

Error handling:
- [ ] All repository methods return `Result<T, DataError.Remote>`
- [ ] Errors are handled, not silently swallowed
- [ ] User-facing errors have clear messages
- [ ] No raw println() statements (use proper logging)

State management:
- [ ] ViewModels expose StateFlow, not mutable state
- [ ] Use `.update { }` for thread-safe state modifications
- [ ] No unmanaged coroutine scopes (use viewModelScope)
- [ ] Suspend functions execute in appropriate context

Code quality:
- [ ] Repository interfaces have KDoc explaining behavior
- [ ] Complex logic has explanatory comments
- [ ] No TODO() that would crash at runtime
- [ ] Mappers are pure functions (no side effects)

Testing:
- [ ] Mappers have unit tests
- [ ] Repository implementations can be tested with mocks
- [ ] ViewModels can be tested without Android dependencies

### Patterns to Follow

These patterns are well-established in the codebase and should be replicated in new features:

#### Simple Repository Pattern

**Reference:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/member/data/repository/MemberRepositoryImpl.kt`

**Key elements:**
1. Return `suspend fun Result<T, DataError.Remote>` from all methods
2. Use direct network calls via data source
3. Map DTOs to domain models using `.map { }`
4. Let ViewModels handle loading states and data refresh

**Benefits:** Simple, testable, clear separation of concerns. ViewModels control when data is loaded.

#### Typed Error Handling with safeCall

**Reference:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/core/data/HttpClientExt.kt` (lines 13-31)

**Pattern:**
```kotlin
suspend fun someNetworkCall(): Result<Data, DataError.Remote> {
    return safeCall<DataDto> {
        httpClient.get("/api/endpoint")
    }.map { dto -> dto.toDomain() }
}
```

**Benefits:** Consistent error mapping, exception safety, typed errors propagate to callers.

#### Sealed Action Pattern for ViewModels

**Reference:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/club/presentation/ClubViewModel.kt` (lines 172-181)

**Pattern:**
```kotlin
sealed interface FeatureAction {
    data class OnCreate(val item: Item) : FeatureAction
    data class OnDelete(val id: String) : FeatureAction
    data object OnClearError : FeatureAction
}

fun onAction(action: FeatureAction) {
    when (action) {
        is FeatureAction.OnCreate -> { /* handle */ }
        is FeatureAction.OnDelete -> { /* handle */ }
        is FeatureAction.OnClearError -> { /* handle */ }
    }
}
```

**Benefits:** Type-safe actions, exhaustive when expressions, clear API for UI layer.

#### Dependency Injection with Koin

**Reference:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/di/Modules.kt`

**Pattern:**
```kotlin
// In Modules.kt
single { DataSourceImpl(get()) }.bind<DataSource>()
single { RepositoryImpl(get()) }.bind<Repository>()
viewModel { FeatureViewModel(get()) }

// In ViewModel
class FeatureViewModel(
    private val repository: Repository  // Interface, not implementation
) : ViewModel()
```

**Benefits:** Interfaces in ViewModels, testability, single source of truth for dependencies.

#### Explicit State Loading Pattern

**Reference:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/club/presentation/ClubViewModel.kt`

**Pattern:**
```kotlin
private val _state = MutableStateFlow(FeatureState())
val state = _state.asStateFlow()

init {
    loadData()
}

private fun loadData() {
    viewModelScope.launch {
        _state.update { it.copy(data = AsyncResult.Loading) }
        when (val result = repository.getData()) {
            is Result.Success -> {
                _state.update { it.copy(data = AsyncResult.Success(result.data)) }
            }
            is Result.Error -> {
                _state.update { it.copy(data = AsyncResult.Error()) }
            }
        }
    }
}
```

**Benefits:** Explicit control over loading, easy to test, clear data flow, manual refresh on demand.

#### Domain Model Mapping

**Good Reference:** Most mappers in `data/mappers/` packages

**Pattern:**
```kotlin
// In data layer only
fun SomeDto.toDomain(): SomeModel {
    return SomeModel(
        id = this.id,
        name = this.name
        // Map all fields from DTO to domain
    )
}
```

**Benefits:** Keeps DTOs out of domain and presentation, single conversion point, testable.

### Gradual Improvement Areas

These are known issues to address over time. They don't block development but should be fixed when touching related code:

#### High Priority

**1. DTO Leaking into Domain Model**
- **File:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/watchlist/domain/WatchListItem.kt` (line 13)
- **Issue:** `TmdbExternalDataDto` should not be in domain layer
- **Fix:** Create pure `TmdbExternalData` domain model, map in data layer
- **Impact:** Low (used only in one feature)

**2. Two User Classes**
- **Files:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/auth/domain/User.kt` and `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/core/domain/User.kt`
- **Issue:** Different User models cause confusion
- **Fix:** Consolidate or rename (e.g., `AuthUser` vs `ClubUser`)
- **Impact:** Medium (potential for bugs if mixed up)

**3. Generic Error Messages**
- **File:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/club/presentation/ClubViewModel.kt` (lines 85, 94, 103, etc.)
- **Issue:** All errors say "Failed to..." without explaining why
- **Fix:** Create `DataError.toUserMessage()` extension function
- **Impact:** Medium (affects user experience)

**4. Debug println Statements**
- **File:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/core/data/HttpClientExt.kt` (line 24)
- **Issue:** println() in production code
- **Fix:** Add proper logging framework (Napier, Kermit) or remove
- **Impact:** Low (doesn't break functionality)

#### Medium Priority

**5. Cross-Feature Dependency**
- **File:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/reviews/data/repository/ReviewsRepositoryImpl.kt`
- **Issue:** ReviewsRepositoryImpl imports MemberLocalDataSource from different feature
- **Fix:** Move MemberLocalDataSource to core module
- **Impact:** Medium (creates coupling between features)

**6. Silent Failures in Repositories**
- **Files:** Various repository implementations
- **Issue:** Returns empty list on error, users can't distinguish "no data" from "error"
- **Fix:** Add separate error state to AsyncResult or expose errors differently
- **Impact:** Medium (affects offline UX)

**7. Missing Error Context**
- **File:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/core/domain/DataError.kt`
- **Issue:** Error types lack context (no HTTP codes, no messages, no retry hints)
- **Fix:** Enhance DataError with optional context:
  ```kotlin
  data class Remote(
      val type: RemoteErrorType,
      val httpCode: Int? = null,
      val retryable: Boolean = false
  ) : DataError
  ```
- **Impact:** Medium (improves error debugging and user feedback)

#### Low Priority

**8. No Pagination**
- **Files:** All repository implementations
- **Issue:** All lists loaded at once, could be slow with large datasets
- **Fix:** Add pagination support when data grows large
- **Impact:** Low (not needed yet)

**9. Full File Replacement in Cache**
- **Files:** All JSON-based local data sources
- **Issue:** Full file replacement on refresh, could use selective updates
- **Fix:** Implement delta updates or merge strategies for large datasets
- **Impact:** Low (works fine for current data volumes)

**10. UiText not used consistently**
- **File:** `composeApp/src/commonMain/kotlin/cobresun/movieclub/app/core/presentation/UiText.kt`
- **Issue:** UiText exists for localized strings, but ViewModels use String
- **Fix:** Return `UiText` from ViewModels instead of raw strings
- **Impact:** Low (matters only when adding localization)

#### When Addressing These Issues

**Before fixing:**
- Assess impact (does it block other work? affect users?)
- Estimate effort (hours vs days)
- Check if it's touched frequently (higher ROI for frequently changed code)

**When fixing:**
- Add tests before refactoring
- Update related documentation
- Consider backward compatibility if other code depends on it
- Don't fix multiple issues in one commit - separate concerns

## Development Notes

- **Known Issue**: There's a KSP version mismatch warning (ksp-2.0.20-1.0.24 vs kotlin-2.1.21) that appears on builds. This doesn't block compilation but should be addressed eventually.
- The app currently uses a dark color scheme by default (see `AppTheme` in `app/App.kt`)
- Protocol Buffers source files are in `composeApp/src/commonMain/proto/`

## Quick Reference

**Adding a new feature?** → See "Design Philosophy - Adding a New Feature Module"
**Modifying a repository?** → See "Design Philosophy - Creating/Modifying Repository Interfaces"
**Fixing a bug?** → See "Design Philosophy - Gradual Improvement Areas"
**Doing code review?** → See "Design Philosophy - Code Review Criteria"
