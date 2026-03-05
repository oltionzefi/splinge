# Contributing to Splinge

Thank you for considering contributing to Splinge! 🎉

## Getting Started

1. **Fork** the repository and clone your fork.
2. Create a new branch from `main`:
   ```shell
   git checkout -b feature/your-feature-name
   ```
3. Make your changes, then run the build to verify nothing is broken:
   ```shell
   ./gradlew :composeApp:assembleDebug
   ```
4. Push your branch and open a **Pull Request** against `main`.

## Project Structure

| Path | Purpose |
|------|---------|
| `composeApp/src/commonMain/` | Shared Kotlin/Compose code (UI, logic, models) |
| `composeApp/src/androidMain/` | Android-specific implementations (Platform, MainActivity) |
| `composeApp/src/iosMain/` | iOS-specific implementations (Platform, MainViewController) |
| `composeApp/src/commonTest/` | Shared unit tests |
| `iosApp/` | Xcode project / SwiftUI entry point |

### Package layout inside `commonMain`

```
org.oltionzefi.splinge
├── model/          # Data classes (Group, Member, Expense, …)
├── logic/          # Pure business logic (SplitCalculator)
├── navigation/     # Screen sealed class
├── ui/
│   ├── components/ # Reusable Composables (dialogs, …)
│   ├── screens/    # Full-screen Composables
│   └── theme/      # Color scheme & MaterialTheme wrapper
└── util/           # Stateless helpers (ShareUtil, formatting)
```

## Code Style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Keep Composables in the correct `ui/screens/` or `ui/components/` package — **do not add screen-level composables to `App.kt`**.
- Business logic must live in `logic/` and must **not** import Compose.
- Keep `model/` classes plain data classes annotated with `@Serializable` where persistence is needed.

## Adding a New Screen

1. Create `ui/screens/YourScreen.kt` with the composable function.
2. Add the route to `navigation/Screen.kt`.
3. Add the `when` branch in `App.kt`.

## Running Tests

```shell
./gradlew :composeApp:commonTest
```

## Reporting Bugs / Requesting Features

Open a [GitHub Issue](../../issues) with a clear description and, for bugs, steps to reproduce.

## Code of Conduct

Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before contributing.

