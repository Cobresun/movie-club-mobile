---
name: tech-debt-backlog
description: Open tech-debt backlog for the Movie Club app (cross-feature dependencies, error messages, pagination, UiText). Use when choosing cleanup work or deciding what to fix next.
---

### Gradual Improvement Areas

These are known issues to address over time. They don't block development but should be fixed when touching related code:

#### High Priority

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

