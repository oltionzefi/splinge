# Changelog

All notable changes to Splinge will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.0] - 2026-06-01

### Added
- Total spent display in `GroupDetailScreen`: The total amount spent in a group is now visible next to the "Balances" header.
- Total spent in reports: Shareable group reports now include the total amount spent.
- Percentage-based splitting: A new "Percentage" algorithm option in group settings allows splitting all expenses based on pre-defined member percentages.
- Name input during group creation: Users are now prompted to set their name if it's missing when they try to add themselves to a new group.

### Fixed
- App crash on startup due to database schema mismatch on existing installations.
- Resiliency of data loading flows to prevent UI crashes on database errors.
- Build-time `StackOverflowError` in SQLDelight by removing redundant and non-standard migration queries from `.sq` files.

## [1.2.0] - 2026-05-02

### Added
- `AGENTS.md` and `CLAUDE.md`: Comprehensive guidelines and documentation for AI agents.
- `BackHandler`: Multiplatform component for handling system back events.
- Grouped view in `GroupDetailScreen` and reports: Balances are now grouped by creditor for better clarity.

### Changed
- Improved `App.kt` stability: Resolved race conditions during seeding and settings loading.
- Enhanced currency formatting: Global use of `ShareUtil.Double.format` for consistent 2-decimal rounding.
- UI Refinement: Moved "Delete Expense" button to the top app bar for better visibility.
- Robust ID generation: Switched to `max(id) + 1` pattern to prevent database collisions.

### Fixed
- Reappearing sample data: Fixed logic ensuring sample groups don't return after being deleted.
- Database safety: Added `try-catch` blocks to all `AppRepository` operations to prevent crashes on schema mismatches.

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

