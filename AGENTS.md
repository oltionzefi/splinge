# Agent Guidelines for Splinge

This document provides detailed instructions for AI agents working on the Splinge repository. Following these guidelines ensures consistency and stability in the codebase.

## Tech Stack
- **Language**: Kotlin 2.x
- **JDK**: Java 21 (Required)
- **UI Framework**: Compose Multiplatform (Android & iOS)
- **Architecture**: MVI-ish state management.
    - Global state lives in `App.kt` via `MutableStateFlow`.
    - Components observe state using `collectAsState`.
- **Database**: SQLDelight for local persistence.
- **Build System**: Gradle (Kotlin DSL).
- **Dependency Management**: Version Catalogs (`gradle/libs.versions.toml`).

## Directory Structure
- `composeApp/src/commonMain/kotlin/org/oltionzefi/splinge/`
    - `model/`: Plain data classes (e.g., `Group`, `Member`, `Expense`).
    - `logic/`: Pure business logic (e.g., `SplitCalculator`). **Strictly no Compose or UI imports.**
    - `db/`: `AppRepository` and platform drivers.
    - `navigation/`: `Screen` sealed class for routing.
    - `ui/`:
        - `screens/`: Full-screen Composables (one file per screen).
        - `components/`: Reusable UI widgets.
        - `theme/`: Theme and color definitions.
    - `util/`: Stateless helpers (e.g., `ShareUtil`, formatting).
- `composeApp/src/commonMain/sqldelight/`: SQL schema and queries.

## Coding Conventions & Patterns

### 1. UI Layer
- **No logic in Composables**: Keep screens as thin as possible. Delegate math and data processing to `logic/` or `App.kt`.
- **Navigation**: Use the custom `BackHandler` for system back events.
- **Theming**: Use `MaterialTheme` colors and typography. Avoid hardcoding colors.

### 2. Business Logic
- Must be unit-testable and independent of Android/iOS specific APIs.
- Currency rounding: Always use `ShareUtil.Double.format` to round to 2 decimal places before displaying or persisting calculated values.

### 3. Database (SQLDelight)
- **Safety**: Wrap all repository calls in `try-catch` blocks. Return safe defaults (empty lists, null) on failure.
- **ID Generation**: Use `(max(id) ?: 0) + 1` for new entities to avoid collisions after deletions.
- **Migrations**: Since the app uses a simple setup, ensure any schema changes in `.sq` files are handled safely in `AppRepository`.

### 4. State Management
- Update state in `App.kt` by modifying the underlying data in the database first, then refreshing the flow, or by observing database flows directly.
- Prefer `StateFlow` for exposure to the UI.

## Development Workflow
- **Environment**: 
    - Use Java 21 for all Gradle tasks.
    - Full **Xcode** installation is required for any iOS-related tasks (compilation, native tests). Command Line Tools are not enough.
- **Linting**: Run `./gradlew :composeApp:lintDebug` (Android) to check for common issues.
- **Tests**: Run `./gradlew :composeApp:commonTest`. If the iOS environment is unavailable, use `./gradlew :composeApp:testDebugUnitTest` to at least verify common logic on the JVM. Always add/update tests in `commonTest` for logic changes.
- **Changelog**: Every PR/change should include a brief entry in `CHANGELOG.md`.

## Agent Behavior Guidelines
- **Test-First**: Reproduce bugs with a test case before fixing.
- **Minimalism**: Don't over-engineer. Prefer simple Kotlin structures over complex design patterns.
- **Defensive Programming**: Assume data from the database might be corrupted or in an old format. Handle nulls and parsing errors gracefully.
- **Cascading Deletes**: When deleting a Group, ensure all associated Members, Expenses, and Splits are also removed (handled in `AppRepository`).
