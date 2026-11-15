# Roadmap

Order matters: prove Bangla accuracy first, then build UIs.

## Phase 1: CLI prototype (now)
- [x] Rust workspace, `kothalipi-core` + `kothalipi` CLI
- [x] WAV loading, resampling, energy VAD
- [x] whisper.cpp engine (via `whisper-cli`)
- [x] Bangla → Banglish transliteration v0
- [ ] Live microphone capture (cpal) + push-to-talk in the CLI
- [ ] Accuracy test set: 100+ Bangla, English and mixed sentences
- [ ] `kothalipi eval` command: WER/CER per model
- [ ] Pick the default Bangla model
- [ ] In-process whisper.cpp (whisper-rs) instead of a subprocess

## Phase 2: macOS menu bar app
- [ ] UniFFI bindings for Swift
- [ ] Menu bar app, global hotkey, mode switcher (Bangla / English / Banglish)
- [ ] Insert text via Accessibility API
- [ ] Onboarding for Microphone + Accessibility permissions
- [ ] Signed + notarized release

## Phase 3: Windows
- [ ] Tray app, `RegisterHotKey`, `SendInput` Unicode injection
- [ ] Installer (MSI or MSIX)

## Phase 4: Android keyboard
- [ ] UniFFI Kotlin bindings, `InputMethodService`, mic button
- [ ] Model download on first run

## Phase 5: Linux (best effort)
- [ ] X11 via XTest
- [ ] Wayland via IBus/Fcitx5 engine or `wtype`

## Phase 6: iOS
- [ ] Keyboard extension + host app recording flow

## Later
- [ ] Optional cloud engines (Google, Azure, OpenAI) behind feature flags
- [ ] Neural VAD (Silero) for noisy rooms and hands-free mode
- [ ] Smart punctuation and number formatting (Bangla + English)
- [ ] More languages from the community: Hindi, Urdu, Arabic, Indonesian…
