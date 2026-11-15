<p align="center">
  <img src="assets/brand/lockups/kothalipi-horizontal-light.svg" alt="Kothalipi" width="420">
</p>

<p align="center">
  <b>Speak. It's written.</b> · <b>বলুন, লেখা হয়ে যাবে।</b><br>
  Open-source voice typing for macOS, Windows, Linux, Android and iOS.<br>
  Bangla-first. Every language welcome.
</p>

<p align="center">
  <a href="https://github.com/Kothalipi/kothalipi/actions/workflows/ci.yml"><img src="https://github.com/Kothalipi/kothalipi/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-006A4E" alt="MIT license"></a>
  <a href="https://github.com/Kothalipi/kothalipi/discussions"><img src="https://img.shields.io/badge/discussions-join-F42A41" alt="Discussions"></a>
  <a href="https://kothalipi.tashanto.com"><img src="https://img.shields.io/badge/website-kothalipi.tashanto.com-16211C" alt="Website"></a>
</p>

---

## What is Kothalipi?

Kothalipi (কথালিপি, "speech-script") turns your voice into text in **any app**:
press a hotkey (or the mic on your keyboard), speak, and the text appears where your cursor is.

| Mode | You say | You get |
|---|---|---|
| **Bangla** | আমি ভাত খাই | আমি ভাত খাই। |
| **English** | I'm eating rice | I'm eating rice. |
| **Banglish** | আমি ভাত খাই | ami bhat khai. |

- **Private by default.** Speech recognition runs on your device. Cloud engines are optional.
- **Fast and light.** A shared Rust core, native apps per platform (no Electron).
- **Built in the open.** Started in Bangladesh 🇧🇩, designed so any community can add its language.

> **Status:** early. Phase 1 (CLI prototype to measure Bangla accuracy) is in progress.

## Try the prototype

```bash
# 1. Install whisper.cpp (macOS shown; see models/README.md for others)
brew install whisper-cpp

# 2. Get a model into models/ (see models/README.md)
export KOTHALIPI_MODEL=models/ggml-small.bin

# 3. Transcribe
cargo run -p kothalipi-cli -- transcribe samples/hello.wav --mode bangla
cargo run -p kothalipi-cli -- transcribe samples/hello.wav --mode banglish

# Bangla → Banglish without audio
cargo run -p kothalipi-cli -- translit "আমি বাংলায় কথা বলি।"
# ami banglay kotha boli.
```

## How it's built

```
            ┌──────────────── kothalipi-core (Rust) ────────────────┐
 mic ──────►│ audio → VAD → speech engine → mode (bn/en/banglish)  │──► text
            └───────────────────────────────▲───────────────────────┘
                                             │ UniFFI bindings
   macOS (Swift) · Windows (Rust) · Linux (Rust) · Android (Kotlin) · iOS (Swift)
            hotkey / keyboard UI + inserting text into other apps
```

| Path | What |
|---|---|
| [`crates/kothalipi-core`](crates/kothalipi-core) | Shared engine: audio, VAD, speech engines, Banglish transliteration |
| [`crates/kothalipi-cli`](crates/kothalipi-cli) | `kothalipi` command-line prototype |
| [`apps/`](apps) | Native apps per platform (planned) |
| [`docs/`](docs) | [Architecture](docs/ARCHITECTURE.md), [Roadmap](docs/ROADMAP.md), [Models & accuracy](docs/MODELS.md) |
| [`assets/brand`](assets/brand) | Logo, icons, colors |

## Contributing

We especially need:

- 🎙️ **Bangla voice samples** with correct transcripts (for the accuracy test set)
- 🔤 **Banglish rules**: help the transliterator sound like real chat Banglish
- 🌍 **New languages**: see [CONTRIBUTING.md](CONTRIBUTING.md#adding-a-language)
- 🍎🪟🐧🤖 **Platform developers** for the native apps

Read [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md) to get started.

## License

[MIT](LICENSE). The Hind Siliguri font used in the logo is under the [SIL Open Font License](assets/brand/fonts/HindSiliguri-OFL.txt).
