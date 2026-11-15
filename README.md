<p align="center">
  <img src="assets/brand/lockups/kothalipi-horizontal-light.svg" alt="Kothalipi" width="420">
</p>

<p align="center">
  <b>Speak. It's written.</b> · <b>বলুন, লেখা হয়ে যাবে।</b><br>
  Open-source voice typing in Bangla and English, in any app.<br>
  macOS · Windows · Linux, with Android and iOS on the way.
</p>

<p align="center">
  <a href="https://github.com/Kothalipi/kothalipi/actions/workflows/ci.yml"><img src="https://github.com/Kothalipi/kothalipi/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-006A4E" alt="MIT license"></a>
  <a href="https://github.com/Kothalipi/kothalipi/discussions"><img src="https://img.shields.io/badge/discussions-join-F42A41" alt="Discussions"></a>
  <a href="https://kothalipi.tashanto.com"><img src="https://img.shields.io/badge/website-kothalipi.tashanto.com-16211C" alt="Website"></a>
</p>

---

## What is Kothalipi?

Kothalipi (কথালিপি, "speech-script") turns your voice into text in **any app**. Hold a key,
speak, let go, and the text appears where your cursor is.

| Key | macOS | Windows | You get |
|---|---|---|---|
| **বাংলা** | Right ⌘ Command | Right Alt | Bangla in Bangla script (mixing in English works) |
| **English** | fn | Right Ctrl | Clean English |

- **Free offline, or cloud.** Offline runs a Bangla model on your computer. Cloud uses your own
  Google Gemini (free tier) or OpenAI key and is the most accurate for mixed Bangla and English.
- **Never stops typing.** Gemini → ChatGPT → offline: when a key is over its limit or you're
  offline, Kothalipi moves to the next.
- **Light and fast.** Rust, low memory, quiet in the background.
- **Open source.** MIT. Started in Bangladesh 🇧🇩, open to every language.

## Install

```bash
curl -fsSL https://kothalipi.tashanto.com/install.sh | sh      # macOS, Linux
```

```powershell
irm https://kothalipi.tashanto.com/install.ps1 | iex           # Windows
```

Or download from [kothalipi.tashanto.com](https://kothalipi.tashanto.com/download.html).
Guides: [macOS](https://kothalipi.tashanto.com/docs/macos.html) ·
[Windows](https://kothalipi.tashanto.com/docs/windows.html) ·
[Linux](https://kothalipi.tashanto.com/docs/linux.html) ·
[Cloud & API keys](https://kothalipi.tashanto.com/docs/cloud.html).

## Repositories

| Repo | What |
|---|---|
| [kothalipi-desktop](https://github.com/Kothalipi/kothalipi-desktop) | **The app** for macOS, Windows and Linux. Built on [Handy](https://github.com/cjpais/Handy) with Bangla models, language keys, cloud fallback and a Kothalipi onboarding |
| **kothalipi** (this repo) | Shared Rust core for the coming mobile apps, CLI for model testing, docs, roadmap, brand |
| [website](https://github.com/Kothalipi/website) | [kothalipi.tashanto.com](https://kothalipi.tashanto.com) and the install scripts |

## This repo

| Path | What |
|---|---|
| [`crates/kothalipi-core`](crates/kothalipi-core) | Audio, VAD, speech-engine trait, Bangla/English modes |
| [`crates/kothalipi-cli`](crates/kothalipi-cli) | `kothalipi` CLI for testing models on recordings |
| [`apps/`](apps) | Android and iOS keyboards (planned) |
| [`docs/`](docs) | [Audit](docs/AUDIT.md), [Architecture](docs/ARCHITECTURE.md), [Roadmap](docs/ROADMAP.md), [Models & accuracy](docs/MODELS.md), [Decisions](docs/decisions) |
| [`assets/brand`](assets/brand) | Logo, icons, colors |

```bash
cargo test --workspace
cargo run -p kothalipi-cli -- transcribe samples/hello.wav --mode bangla --model models/ggml-small.bin
```

## Contributing

We especially need:

- 🎙️ **Bangla voice samples** with correct transcripts (for a public accuracy benchmark)
- 📊 **Model testing**: compare Bangla models on real speech
- 🌍 **New languages**: see [CONTRIBUTING.md](CONTRIBUTING.md#adding-a-language)
- 📱 **Mobile developers** for the Android and iOS keyboards

Read [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

[MIT](LICENSE). The Hind Siliguri font in the logo is under the [SIL Open Font License](assets/brand/fonts/HindSiliguri-OFL.txt).
