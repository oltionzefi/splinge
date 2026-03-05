# Changelog

All notable changes to Splinge will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- `navigation/Screen.kt` — `Screen` sealed class extracted from `App.kt` into its own package for clear separation of navigation concerns.
- `CONTRIBUTING.md` — contributor guide covering project structure, code style, and workflow.
- `CODE_OF_CONDUCT.md` — Contributor Covenant 2.1.
- `LICENSE` — MIT License.
- `CHANGELOG.md` — this file.

### Changed
- `App.kt` refactored: duplicate screen composables removed; file now only contains `App()` and delegates to `ui/screens/`.
- `.gitignore` expanded with keystore files, environment files, and OS-specific entries.

### Removed
- `Greeting.kt` — unused scaffold boilerplate deleted.

---

## [1.0.0] - 2026-03-05

### Added
- Group management: create, delete, and manage up to 10 groups.
- Expense tracking: add expenses with payer and per-member splits.
- Two split algorithms: **Basic** (everyone owes the payer) and **Smart** (debt simplification to minimise transactions).
- Multi-currency support (€, $, £) per group.
- PayPal "Scan Me" deep-link integration.
- Report sharing via system share sheet.
- Full dark/light mode support.
- Kotlin Multiplatform targeting Android and iOS.

