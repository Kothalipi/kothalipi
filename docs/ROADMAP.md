# Roadmap

Order matters: prove Bangla accuracy first, then build UIs.

## Phase 1: CLI prototype (now)
- [x] Rust workspace, `kothalipi-core` + `kothalipi` CLI
- [x] WAV loading, resampling, energy VAD
- [x] whisper.cpp engine (via `whisper-cli`)
- [ ] Live microphone capture (cpal) + push-to-talk in the CLI
- [ ] Accuracy test set: 100+ Bangla, English and mixed sentences
- [ ] `kothalipi eval` command: WER/CER per model
- [ ] Pick the default Bangla model
- [ ] In-process whisper.cpp (whisper-rs) instead of a subprocess

## Phase 2: Desktop app (macOS, Windows, Linux)
See [decision 0001](decisions/0001-desktop-architecture.md) and
[kothalipi-desktop](https://github.com/Kothalipi/kothalipi-desktop).
- [x] App id, branding, installers for all three OSes
- [x] Bangla models first: Qwen3-ASR 0.6B Bangla, Bengali.AI Whisper medium
- [x] Per-language keys (Bangla / English), each with its own model
- [x] Cloud mode: Gemini → OpenAI → offline fallback, key checks
- [x] Kothalipi onboarding (offline or cloud, keys, models, key conflicts)
- [x] Release pipeline for macOS, Windows, Linux; terminal installers
- [ ] Bangla UI translation
- [ ] Own signing keys, CI builds, signed + notarized release

## Phase 3: Public Bangla benchmark
- [ ] Open test set (read, phone, mixed Bangla-English)
- [ ] Compare all Bangla models, publish results

## Phase 4: Android keyboard
- [ ] UniFFI Kotlin bindings, `InputMethodService`, mic button
- [ ] Model download on first run

## Phase 5: iOS
- [ ] Keyboard extension + host app recording flow

## Later
- [ ] Optional cloud engines (Google, Azure, OpenAI) behind feature flags
- [ ] Neural VAD (Silero) for noisy rooms and hands-free mode
- [ ] Smart punctuation and number formatting (Bangla + English)
- [ ] More languages from the community: Hindi, Urdu, Arabic, Indonesian…
