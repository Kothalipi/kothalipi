# Architecture

## Principle: one Rust core, native shells

Writing five separate apps means five codebases. Putting everything in Electron or
Flutter makes the app heavy and slow. So Kothalipi splits the work:

- **Rust core (`kothalipi-core`)**: audio capture, voice activity detection (VAD),
  speech-to-text inference, and text processing (Bangla / English / Banglish).
- **Native shells**: each platform's own language for UI, hotkeys and inserting
  text into other apps.

```
┌──────────────────────── kothalipi-core ────────────────────────┐
│ audio   : load / capture, mix to mono, resample to 16 kHz       │
│ vad     : find speech start/end (energy now, neural later)     │
│ engine  : SpeechEngine trait → whisper.cpp, sherpa-onnx, cloud │
│ mode    : Bangla | English | Banglish, final cleanup           │
│ translit: Bangla script → Banglish                             │
└──────────────────────────────▲─────────────────────────────────┘
                               │ UniFFI (Swift, Kotlin) / direct (Rust)
```

## Platforms

| Platform | Language | Text insertion | Notes |
|---|---|---|---|
| macOS | Swift (AppKit, menu bar) | Accessibility API | Core ML / Metal acceleration |
| Windows | Rust (`windows-rs`) | `SendInput` with Unicode | Tiny binary; Win32 or WinUI 3 UI |
| Linux | Rust (tray, GTK4 later) | X11: XTest; Wayland: IBus/Fcitx, `wtype` | Wayland has no standard, best effort |
| Android | Kotlin | `InputMethodService` keyboard | Core via UniFFI/JNI |
| iOS | Swift | Keyboard extension + host app | ~60 MB extension limit, mic restricted |

[UniFFI](https://github.com/mozilla/uniffi-rs) (Mozilla) generates the Swift and Kotlin
bindings from the Rust core, so the core is written once.

## Speech engine: the biggest decision

For Bangla, accuracy matters more than raw speed, and accuracy depends on the **model**,
not on the programming language.

| Option | Pros | Cons |
|---|---|---|
| On-device: whisper.cpp (quantized small/medium) | Private, offline, free | Stock Whisper is weak in Bangla → needs a Bengali fine-tuned model |
| On-device: sherpa-onnx | Bindings for Rust, Swift, Kotlin; streaming models | Fewer ready Bangla models |
| Cloud: Google STT, Azure Speech, Whisper API | Strong Bangla accuracy | Needs internet, costs money, privacy trade-off |

**Plan:** on-device by default, optional cloud engine. All engines sit behind the
`SpeechEngine` trait, so apps never care which one is running.

## Bangla, English and Banglish

- **Bangla mode:** recognise `bn`, keep Bangla script, keep the dari (।).
- **English mode:** recognise `en`.
- **Banglish mode:** recognise `bn`, then transliterate to romanized Bangla in the core
  (`translit.rs`). Doing it in the core means every platform gets identical Banglish.
- **Code-switching** (Bangla sentences with English words) is the hard part. Track it in
  the accuracy test set (`samples/mx-*`) from day one.

## Permissions users must grant

- macOS: Microphone + Accessibility
- Windows: Microphone (some apps running as admin will block `SendInput` from a non-admin app)
- Android: Microphone + enabling the keyboard in system settings
- iOS: Full Access for the keyboard + Microphone in the host app

Every app needs a short onboarding that explains *why* each permission is needed.
