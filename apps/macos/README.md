# Kothalipi for macOS

**Status:** next after the CLI prototype (Phase 2).

- **Language:** Swift (AppKit), menu bar app
- **Core:** `kothalipi-core` via UniFFI-generated Swift bindings
- **Hotkey:** global push-to-talk shortcut
- **Text insertion:** Accessibility API (AXUIElement), with clipboard + ⌘V as a fallback
- **Acceleration:** Metal / Core ML through whisper.cpp
- **Permissions:** Microphone + Accessibility. The app must walk the user through both.
- **Icon:** `assets/brand/kothalipi.icns`, menu bar: `assets/brand/png/menubarTemplate*.png`
