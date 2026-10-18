---
name: add-feature-module
description: Checklist for adding a new feature module (domain, data, presentation, Koin registration) to the Movie Club app. Use when creating a new feature package.
---

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
- [ ] Add a `{feature}Module` in `di/Modules.kt` and list it in `appModule`'s `includes(...)`
- [ ] Register implementations with `singleOf(::Impl) { bind<Interface>() }`
- [ ] Register ViewModels with `viewModelOf(::FeatureViewModel)`; route arguments are constructor parameters passed with `koinViewModel { parametersOf(...) }` (see Navigation in CLAUDE.md)
- [ ] Run `KoinModulesTest` (`./gradlew -q :composeApp:testAndroidHostTest`) to check the graph

Testing:
- [ ] Add unit tests for mappers (DTO → domain)
- [ ] Add repository tests with mocked data sources
- [ ] Add ViewModel tests with mocked repositories

