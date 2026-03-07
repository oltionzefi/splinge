# Changelog

All notable changes to Splinge will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

## [1.1.0] - 2026-03-07

### Added
- Expense editing: enabled modifying existing expenses with field prepopulation and persistence.
- "Share My PayPal" feature: simplified PayPal link format (`paypal.me/username`) and added system share sheet integration.
- `navigation/Screen.kt` — `Screen` sealed class extracted from `App.kt` into its own package for clear separation of navigation concerns.
- `CONTRIBUTING.md` — contributor guide covering project structure, code style, and workflow.
- `CODE_OF_CONDUCT.md` — Contributor Covenant 2.1.
- `LICENSE` — MIT License.
- `CHANGELOG.md` — this file.

### Changed
- Refactored `AppRepository` and `Platform` to use platform-specific `ioDispatcher`, resolving compilation issues in Kotlin Multiplatform.
- Migrated deprecated icons (`ArrowForward`, `ReceiptLong`) to `AutoMirrored` versions in `GroupDetailScreen.kt`.
- `App.kt` refactored: duplicate screen composables removed; file now only contains `App()` and delegates to `ui/screens/`.
- `.gitignore` expanded with keystore files, environment files, and OS-specific entries.

### Removed
- PayPal "Scan Me" functionality, replaced by the new share feature.
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

