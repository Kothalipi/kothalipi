# 0001: Desktop app architecture

- **Status:** accepted and implemented in [kothalipi-desktop](https://github.com/Kothalipi/kothalipi-desktop)
- **Date:** 2026-11-16

## Decision

1. **Desktop (macOS, Windows, Linux) is one Tauri app** (Rust backend, React interface) in
   `kothalipi-desktop`. Its engine, hotkeys and text-insertion layer began as
   [Handy](https://github.com/cjpais/Handy) by CJ Pais (MIT); that code is credited in the
   app's About page, the README, `NOTICE.md` and `LICENSE`.
2. **Everything users see is Kothalipi's own:** onboarding, interface, branding, Bangla
   models, language keys, cloud fallback, device-aware model recommendations.
3. **Kothalipi-specific code lives in its own modules** (`kothalipi_models.rs`,
   `language_hotkeys.rs`, `cloud_stt.rs`, `key_conflicts.rs`, `device.rs`) so engine
   updates stay easy to merge.
4. **Mobile (Android, iOS) stays native** (Kotlin keyboard, Swift keyboard extension) using
   `kothalipi-core` through UniFFI.

## Why

- Bangla quality is decided by the model, not the UI toolkit. Bangla-trained models cut
  errors several times compared with stock Whisper.
- The engine layer (hotkeys, typing into other apps, permissions, GPU inference) is the
  hard, platform-specific part and is already proven on three OSes. Rewriting it in a native
  Rust GUI would cost months and save little memory next to a ~900 MB model.

## Open items

See [ROADMAP.md](../ROADMAP.md) and [AUDIT.md](../AUDIT.md).
