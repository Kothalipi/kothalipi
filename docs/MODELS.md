# Models and accuracy testing

## Why test first

Kothalipi lives or dies on Bangla accuracy. Before building any UI, we measure.

## Test set

Store pairs in `samples/` (git-ignored by default; share consented samples via a
separate dataset repo):

- `bn-*.wav` + `.txt`: Bangla only
- `en-*.wav` + `.txt`: English only
- `mx-*.wav` + `.txt`: mixed Bangla + English (code-switching)

Aim for variety: male/female voices, Dhaka and regional accents, quiet room and
street noise, phone mic and laptop mic.

## Metrics

- **CER** (character error rate): the main metric for Bangla script
- **WER** (word error rate): main metric for English
- **Real-time factor**: processing time ÷ audio length (the CLI already prints this);
  must be well under 1.0 for typing to feel instant

## Candidates to compare

Found on Hugging Face (2026-11-16). Check each license before shipping.

| Engine | Model | Format | Why test it |
|---|---|---|---|
| whisper.cpp / transcribe-cpp | stock `large-v3-turbo` | ggml | Baseline most dictation apps use |
| whisper.cpp / transcribe-cpp | [bengaliAI/tugstugi_bengaliai-asr_whisper-medium](https://huggingface.co/bengaliAI/tugstugi_bengaliai-asr_whisper-medium) (Apache-2.0) | transformers → convert to ggml | Strong Bangla fine-tune; what Shobdo ships offline |
| whisper.cpp / transcribe-cpp | [bengaliAI/tugstugi_bengaliai-regional-asr_whisper-medium](https://huggingface.co/bengaliAI/tugstugi_bengaliai-regional-asr_whisper-medium) | transformers → convert | Regional dialects |
| transcribe-cpp | [sheikhmufrad/Qwen3-ASR-0.6B-Bengali-GGUF](https://huggingface.co/sheikhmufrad/Qwen3-ASR-0.6B-Bengali-GGUF) | GGUF | Smaller than Whisper medium; could be faster |
| sherpa-onnx | [SayedShaun/bangla-zipformer-onnx](https://huggingface.co/SayedShaun/bangla-zipformer-onnx) | ONNX | Small, streaming: candidate for Android/iOS |
| NeMo → ONNX | [bengaliAI/BanglaConformer](https://huggingface.co/bengaliAI/BanglaConformer) | NeMo | Conformer alternative |
| Cloud | Google Gemini, Google STT, Azure Speech | API | Best for Bangla mixed with English |

Reference point: Shobdo published 42 wrong letters per 100 for stock large-v3-turbo,
10 for Bengali.AI medium and 8 for Gemini on their private 110-clip test (Oct 2026).
Our job is to reproduce this on a public test set and beat it.

Record results in a table here (model, size, CER bn, WER en, CER mixed, RTF on an
M-series Mac and a mid-range Windows laptop) and pick the default from the data.

## Licenses

Check every model's license before shipping or redistributing it. Prefer permissive
licenses (MIT, Apache-2.0, CC-BY).
