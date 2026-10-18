---
name: tech-debt-backlog
description: Open tech-debt backlog for the Movie Club app (pagination, cache deltas, UiText, error monitoring). Use when choosing cleanup work or deciding what to fix next.
---

### Gradual Improvement Areas

Known issues to address over time. They don't block development but should be fixed when touching related code.

#### Medium Priority

**1. No crash/error reporting service**
- **Where:** `core/platform/AppLog.kt`
- **Issue:** Warnings and errors only reach Logcat/NSLog. `AppLog.addSink` is the hook, but nothing ships them anywhere
- **Fix:** Pick a service (e.g. Sentry or Crashlytics, both have KMP SDKs), add a sink at startup (`MainApplication` / `setUpKoin`) and report uncaught exceptions
- **Impact:** Medium (production failures are invisible today)

#### Low Priority

**2. No Pagination**
- **Files:** All repository implementations
- **Issue:** All lists loaded at once, could be slow with large datasets
- **Fix:** Add pagination support when data grows large (needs server support)
- **Impact:** Low (not needed yet)

**3. Full File Replacement in Cache**
- **Files:** `core/data/OfflineStore.kt`
- **Issue:** Each read rewrites its whole JSON file
- **Fix:** Delta updates or merge strategies if payloads grow large
- **Impact:** Low (works fine for current data volumes)

**4. UiText not used**
- **File:** `core/presentation/UiText.kt`
- **Issue:** UiText exists for localized strings, but view models and `toUserMessage` build English `String`s
- **Fix:** When localizing, move strings to compose resources and return `UiText` from view models and `toUserMessage`
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
