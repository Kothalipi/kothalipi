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
| whisper.cpp / transcribe-cpp | [bengaliAI/tugstugi_bengaliai-asr_whisper-medium](https://huggingface.co/bengaliAI/tugstugi_bengaliai-asr_whisper-medium) (Apache-2.0) | transformers → convert to ggml | Strong Bangla fine-tune |
| whisper.cpp / transcribe-cpp | [bengaliAI/tugstugi_bengaliai-regional-asr_whisper-medium](https://huggingface.co/bengaliAI/tugstugi_bengaliai-regional-asr_whisper-medium) | transformers → convert | Regional dialects |
| transcribe-cpp | [sheikhmufrad/Qwen3-ASR-0.6B-Bengali-GGUF](https://huggingface.co/sheikhmufrad/Qwen3-ASR-0.6B-Bengali-GGUF) | GGUF | Smaller than Whisper medium; could be faster |
| sherpa-onnx | [SayedShaun/bangla-zipformer-onnx](https://huggingface.co/SayedShaun/bangla-zipformer-onnx) | ONNX | Small, streaming: candidate for Android/iOS |
| NeMo → ONNX | [bengaliAI/BanglaConformer](https://huggingface.co/bengaliAI/BanglaConformer) | NeMo | Conformer alternative |
| Cloud | Google Gemini, Google STT, Azure Speech | API | Best for Bangla mixed with English |

Goal: measure these on a public Bangla test set (read speech, phone recordings, Bangla mixed with English) and publish the results here.

Record results in a table here (model, size, CER bn, WER en, CER mixed, RTF on an
M-series Mac and a mid-range Windows laptop) and pick the default from the data.

## Licenses

Check every model's license before shipping or redistributing it. Prefer permissive
licenses (MIT, Apache-2.0, CC-BY).
