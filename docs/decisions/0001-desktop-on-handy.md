# 0001: Build the desktop app on Handy

- **Status:** accepted, implemented in [kothalipi-desktop](https://github.com/Kothalipi/kothalipi-desktop)
- **Date:** 2026-11-16

## Context

[Shobdo](https://khalidfarhan.com/shobdo) is a free Bangla + English dictation app for Mac
and Windows. Its site says it is built on the open-source app
[Handy](https://github.com/cjpais/Handy) and uses the
[Bengali.AI Whisper medium](https://huggingface.co/bengaliAI/tugstugi_bengaliai-asr_whisper-medium)
model offline, with Google Gemini as an optional cloud engine. Its source code is not
public (only a releases repo).

Shobdo's own published test (October 2026, 110 Bangla clips, their numbers):

| Engine | Wrong letters per 100 |
|---|---|
| Whisper large-v3-turbo | 42 |
| Bengali.AI Whisper medium, offline | 10 |
| Google Gemini, cloud | 8 |

Lesson: **the model decides Bangla quality**, and a Bangla fine-tuned model cuts errors
roughly 4x compared with stock Whisper.

**Handy** facts (checked 2026-11-16): MIT license, ~38.8k stars, actively maintained,
Tauri v2 (Rust + TypeScript), runs on macOS, Windows and Linux, push-to-talk hotkeys,
paste into any app, model download manager, LLM post-processing hook, and runs every
model through `transcribe-cpp` (GGUF/ggml) with Metal on Apple Silicon and Vulkan on Windows.

## Decision

1. **Desktop (macOS, Windows, Linux): fork Handy** as `Kothalipi/kothalipi-desktop`, keep
   the upstream remote, and regularly merge upstream fixes. This replaces the
   hand-written Swift/Win32/GTK shells planned in `ARCHITECTURE.md` for desktop.
2. **Mobile (Android, iOS): stays native** (Kotlin keyboard, Swift keyboard extension) using
   `kothalipi-core` through UniFFI. Handy does not target mobile.
3. **`kothalipi-core` becomes the shared Bangla layer** used by both: modes, Bangla text
   cleanup, evaluation tools.

## How Kothalipi will be better than Shobdo

| | Shobdo | Kothalipi |
|---|---|---|
| Source code | Closed (releases only) | Fully open (MIT), community-driven |
| Cloud | Gemini only | Gemini → OpenAI → offline, automatic fallback |
| Platforms | Mac, Windows | Mac, Windows, **Linux, Android, iOS** |
| Offline models | One (Whisper medium, ~830 MB) | A **catalog**: Bengali.AI medium, Bengali.AI regional (dialects), smaller/faster options such as Qwen3-ASR-0.6B-Bengali (GGUF), pick by speed vs accuracy |
| Benchmark | Private test set | **Public, reproducible** eval set + `kothalipi eval`, results published |
| Cloud | Gemini (own key) | Bring-your-own key for several providers (Gemini, Google STT, Azure, OpenAI) |
| Bangla text polish | — | Dari (।), Bangla/English numbers, personal dictionary for names, keep English words in Latin script option |
| Long term | — | Fine-tune our own Bangla model (e.g. on large-v3-turbo for speed) from open datasets |

## Consequences

- We ship a usable desktop app in weeks, not months.
- We inherit Handy's stack (Tauri, TypeScript UI). Contributors need Rust + TS.
- We must keep Handy's MIT copyright notice and credit Handy clearly.
- Upstream merges need care when we change the same files; keep Kothalipi-specific
  code in separate modules where possible.

## Next steps

1. ~~Fork Handy, rebrand (name, icons, bundle id).~~ Done.
2. ~~Add Bangla models (Qwen3-ASR 0.6B Bangla, Bengali.AI Whisper medium).~~ Done.
3. ~~Per-language keys: Bangla, English.~~ Done.
4. Banglish was built, then dropped (not good enough to ship). Bangla text cleanup next.
5. Build the public eval set and publish the first comparison table.
6. Own signing keys, CI builds and the first release on all three OSes.
