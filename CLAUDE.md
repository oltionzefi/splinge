# Splinge Agent Guide

All coding conventions, directory structure, tech stack details, development commands, and agent behavior guidelines are documented in **AGENTS.md**. Read it completely before making any changes to this repository.

## Essential Commands

- **Build Android**: `./gradlew :composeApp:assembleDebug`
- **Run Tests**: `./gradlew :composeApp:commonTest`
- **Check Lint**: `./gradlew :composeApp:lintDebug` (if available) or use IDE inspection.
- **Generate SQLDelight Code**: `./gradlew generateSqlDelightInterface`

## Project Context
- **Primary State**: Managed in `App.kt` using `MutableStateFlow`.
- **UI**: Compose Multiplatform (Android & iOS).
- **Persistence**: SQLDelight (`.sq` files in `commonMain/sqldelight`).
- **Logic**: Pure Kotlin in `logic/` package (no Compose imports).
