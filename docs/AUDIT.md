# Product audit: Kothalipi desktop

- **Date:** 2026-11-16 · **Version:** 0.1.0
- **Scope:** onboarding, voice-to-type, UI/UX, animation, typing, performance, release.
- **How:** the whole UI was walked through in a browser preview with a mocked backend
  (`bun run dev` → `/?mock`, see `src/dev/mockTauri.ts` in kothalipi-desktop) on
  macOS / Windows looks, light and dark, and low / standard / high-memory devices.

## Decision: keep the proven engine, own the experience

Rewriting in "raw Rust" with a native GUI was considered and rejected for now:

- The engine (hotkeys, typing into other apps, permissions, GPU inference) is already
  Rust and battle-tested on three OSes. Rewriting it is months of work and new bugs.
- A native Rust GUI saves ~80–150 MB of webview memory, while the Bangla model alone
  uses ~900 MB. Rust GUI toolkits also still render Bangla conjuncts less reliably
  than a webview.
- Instead we cut memory where it matters (below) and make every screen Kothalipi's own.

## Fixed in this pass

| Area | Problem | Fix |
|---|---|---|
| Brand | Inherited pink accents, mascot and tray icon | Bangladesh green for actions, flag red for recording, Kothalipi bubble mascot and tray icon |
| Onboarding | One model per language, no guidance | Device-aware picks: reads memory, cores and chip; "Best for this computer" badge; alternatives under "Other models" |
| Onboarding | Low-memory computers pushed to a large offline model | 4 GB machines see Cloud marked "Recommended for this computer" |
| Onboarding | Download button icon stacked above its label | Inline icon + label |
| Keys | "Hold ⌘ and talk" didn't say which Command key | One-sided keys read "Right ⌘", "Right Alt" |
| Release | Unsigned macOS builds failed: empty `APPLE_CERTIFICATE` made Tauri try to import a certificate | Signing variables exported only when signing |
| Dev | UI could only be seen in a native build | `/?mock` browser preview with fixtures generated from Rust defaults |

## Done in the following pass

- Product is Kothalipi everywhere (binary, config, paths, UI, docs, installers); the engine
  credit lives in About, README, NOTICE and LICENSE. Dev credit: TA Shanto, tashanto.com.
- Bangla interface (partial: core screens; the rest falls back to English).
- Recording pill shows **BN** / **EN** for the active key.
- Website: download page, docs for every OS, terminal installers, privacy; HTTPS.

## Next, by priority

### P0: trust and correctness
1. **Code signing.** macOS Developer ID + notarization (Apple Developer Program) and
   Windows signing (e.g. Azure Trusted Signing). Without them users see Gatekeeper and
   SmartScreen warnings. Needs the owner's accounts; the pipeline is ready
   (`KOTHALIPI_SIGN=true` + secrets).
2. **Auto-update.** Generate Kothalipi's own updater key, publish `latest.json` with
   releases, re-enable updater artifacts.
3. **Real accuracy numbers.** Record a public Bangla test set (read, phone, mixed
   Bangla-English) and measure Qwen3-ASR Bangla, Bengali.AI Whisper, Gemini, OpenAI.
   Publish in `docs/MODELS.md` and on the website.
4. **Live-test cloud engines** with real Gemini and OpenAI keys (request formats follow
   the official docs and are unit-tested, but not yet exercised end to end).

### P1: speed and memory
5. **No reload when switching keys.** Each key can load a different model, and switching
   reloads (1–3 s). On `high`-tier machines keep the Bangla and English models resident;
   on others, preload the other model in the background after the first dictation.
6. **Idle memory.** Default the model unload timeout to a few minutes on `light`/`standard`
   machines, and destroy the settings webview when its window closes (tray-only mode
   then idles at native-app memory).
7. **Overlay shows the language.** The recording pill should say বাংলা or English, so a
   wrong key is obvious before speaking.

### P1: Bangla polish
8. **Bangla app UI** (`bn` locale) — the interface is still English for Bangla users.
9. **Text polish:** Bangla digits vs Latin digits preference, dari and spacing cleanup,
   common mis-hearings dictionary.

### P2: reach
10. Android keyboard and iOS keyboard (shared Rust core via UniFFI).
11. Linux Wayland: guided `ydotool`/`wtype` setup inside the app.
12. Per-app language memory (e.g. Bangla in Messenger, English in VS Code).
