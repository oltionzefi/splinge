# Splinge Agent Guide

All coding conventions, directory structure, tech stack details, development commands, and agent behavior guidelines are documented in **AGENTS.md**. Read it completely before making any changes to this repository.

## Essential Commands

- **Build Android**: `./gradlew :composeApp:assembleDebug`
- **Run All Tests**: `./gradlew :composeApp:commonTest`
- **Run Android Tests**: `./gradlew :composeApp:testDebugUnitTest` (Use if iOS environment is missing)
- **Check Lint**: `./gradlew :composeApp:lintDebug` (if available) or use IDE inspection.
- **Generate SQLDelight Code**: `./gradlew generateSqlDelightInterface`

## Environment Requirements

- **Java Version**: Java 21 is required for building and testing.
- **iOS Development**: A full installation of **Xcode** is required for Kotlin/Native (iOS) compilation and tests. Command Line Tools alone are NOT sufficient.
- **Xcode Error**: If you see `xcrun: error: tool 'xcodebuild' requires Xcode`, ensure Xcode is installed and selected: `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`

## Project Context
- **Primary State**: Managed in `App.kt` using `MutableStateFlow`.
- **UI**: Compose Multiplatform (Android & iOS).
- **Persistence**: SQLDelight (`.sq` files in `commonMain/sqldelight`).
- **Logic**: Pure Kotlin in `logic/` package (no Compose imports).
