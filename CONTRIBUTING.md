# Contributing to Kothalipi

Thank you! Kothalipi is built by its community. You can help even if you don't write Rust.
Issues and PRs are welcome in **English or Bangla**.

## Ways to help

- **Record samples:** read sentences in Bangla, English or mixed, with a correct transcript.
  Only contribute your own voice or voices with clear consent.
- **Report wrong words:** open a *Wrong transcription* issue with what you said and what
  Kothalipi typed.
- **Test models:** run the CLI with different models and report accuracy (see `docs/MODELS.md`).
- **Code:** core features, platform apps, docs. Check issues labelled `good first issue`.

## Development setup

```bash
git clone https://github.com/Kothalipi/kothalipi
cd kothalipi
cargo test --workspace
cargo run -p kothalipi-cli -- translit "আমি বাংলায় কথা বলি।"
```

Before opening a PR:

```bash
cargo fmt --all
cargo clippy --workspace --all-targets -- -D warnings
cargo test --workspace
```

## Pull requests

- One change per PR, with a clear title and description.
- Add tests for new behaviour (especially transliteration rules).
- Never commit model files or personal voice recordings (they are git-ignored).

## Adding a language

1. Open a **Language request** issue so others can join.
2. Add a `Language` variant (ISO 639-1 code) in `crates/kothalipi-core/src/mode.rs`.
3. If the language has a common romanized form (like Hinglish or Arabizi), note it in
   the issue; a transliteration option can be discussed there.
4. Add test samples and record accuracy in `docs/MODELS.md`.
5. Become that language's maintainer if you like. We will add you to a language team.

## Code of Conduct

Everyone must follow our [Code of Conduct](CODE_OF_CONDUCT.md).
