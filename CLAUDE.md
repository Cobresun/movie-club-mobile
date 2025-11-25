# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
- **Wire Protocol Buffers** for serialization (see `proto/bearer_token.proto`)
- Generated code in `build/generated/` (SQLDelight appears to be configured but tables are in build directory)

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

## Development Notes

- **Known Issue**: There's a KSP version mismatch warning (ksp-2.0.20-1.0.24 vs kotlin-2.1.21) that appears on builds. This doesn't block compilation but should be addressed eventually.
- The app currently uses a dark color scheme by default (see `AppTheme` in `app/App.kt`)
- Protocol Buffers source files are in `composeApp/src/commonMain/proto/`
