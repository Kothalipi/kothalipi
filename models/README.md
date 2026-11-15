# Models

Model files are **not** committed (they are large). Put them in this folder; it is git-ignored.

## Quick start (whisper.cpp)

```bash
brew install whisper-cpp            # macOS; other OSes: build github.com/ggml-org/whisper.cpp
# download a ggml model with whisper.cpp's models/download-ggml-model.sh, e.g. "small"
export KOTHALIPI_MODEL=models/ggml-small.bin
cargo run -p kothalipi-cli -- transcribe samples/your-recording.wav --mode bangla
```

## Choosing a Bangla model

Stock Whisper is weak in Bangla. For good accuracy:

1. Look for **Bengali fine-tuned Whisper** checkpoints (e.g. on Hugging Face) and convert
   them to ggml with whisper.cpp's conversion script.
2. Compare candidates on the same test set (see `docs/MODELS.md`) before picking a default.
3. Prefer quantized `small`/`medium` (q5/q8) for desktop, `base`/`small` for mobile.

Always check each model's license before redistributing it.
