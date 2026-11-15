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

| Engine | Model | Where it runs |
|---|---|---|
| whisper.cpp | stock `small`, `medium` (quantized) | on-device baseline |
| whisper.cpp | Bengali fine-tuned Whisper (converted to ggml) | on-device |
| sherpa-onnx | Bangla-capable models, if available | on-device, streaming |
| Cloud | Google STT, Azure Speech | optional engine |

Record results in a table here (model, size, CER bn, WER en, CER mixed, RTF on an
M-series Mac and a mid-range Windows laptop) and pick the default from the data.

## Licenses

Check every model's license before shipping or redistributing it. Prefer permissive
licenses (MIT, Apache-2.0, CC-BY).
