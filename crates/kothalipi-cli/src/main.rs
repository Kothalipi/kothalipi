//! `kothalipi`: command-line prototype.
//!
//! Phase 1 goal: measure Bangla accuracy before building any UI.
//!
//! ```text
//! kothalipi transcribe sample.wav --mode bangla --model models/ggml-small.bin
//! kothalipi vad sample.wav
//! kothalipi eval --dir samples/ --model models/ggml-small.bin
//! kothalipi eval --reference "hello world" --hypothesis "hello word"
//! ```

use std::fs;
use std::path::{Path, PathBuf};
use std::time::Instant;

use anyhow::{bail, Context, Result};
use clap::{Parser, Subcommand};
use kothalipi_core::audio::load_wav;
use kothalipi_core::engine::{SpeechEngine, WhisperCli};
use kothalipi_core::metrics::{cer, wer, ErrorCounts};
use kothalipi_core::vad::EnergyVad;
use kothalipi_core::Mode;

#[derive(Parser)]
#[command(
    name = "kothalipi",
    version,
    about = "Kothalipi: speak Bangla or English, get text."
)]
struct Cli {
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Transcribe a WAV file.
    Transcribe {
        /// Input WAV file (any sample rate, mono or stereo).
        wav: PathBuf,
        /// Output mode: bangla or english.
        #[arg(long, short, default_value = "bangla")]
        mode: Mode,
        /// whisper.cpp ggml model file.
        #[arg(long, env = "KOTHALIPI_MODEL")]
        model: PathBuf,
        /// whisper.cpp binary.
        #[arg(long, env = "KOTHALIPI_WHISPER_BIN", default_value = "whisper-cli")]
        whisper_bin: PathBuf,
        /// CPU threads for whisper.cpp.
        #[arg(long, short)]
        threads: Option<u32>,
    },
    /// Show where speech is detected in a WAV file.
    Vad { wav: PathBuf },
    /// Benchmark speech recognition accuracy (WER and CER).
    Eval {
        /// Ground-truth reference string or path to a text file.
        #[arg(long)]
        reference: Option<String>,
        /// Hypothesis string or path to a text file.
        #[arg(long)]
        hypothesis: Option<String>,
        /// Directory containing pairs of .wav and .txt files.
        #[arg(long)]
        dir: Option<PathBuf>,
        /// whisper.cpp ggml model file (required when evaluating audio directory).
        #[arg(long, env = "KOTHALIPI_MODEL")]
        model: Option<PathBuf>,
        /// Output mode: bangla or english.
        #[arg(long, short, default_value = "bangla")]
        mode: Mode,
        /// whisper.cpp binary.
        #[arg(long, env = "KOTHALIPI_WHISPER_BIN", default_value = "whisper-cli")]
        whisper_bin: PathBuf,
        /// CPU threads for whisper.cpp.
        #[arg(long, short)]
        threads: Option<u32>,
    },
}

fn resolve_text_or_file(input: &str) -> Result<String> {
    let p = Path::new(input);
    if p.is_file() {
        fs::read_to_string(p).with_context(|| format!("reading {}", p.display()))
    } else {
        Ok(input.to_string())
    }
}

fn main() -> Result<()> {
    match Cli::parse().command {
        Command::Transcribe {
            wav,
            mode,
            model,
            whisper_bin,
            threads,
        } => {
            let audio = load_wav(&wav).with_context(|| format!("reading {}", wav.display()))?;
            let engine = WhisperCli {
                binary: whisper_bin,
                model,
                threads,
            };
            let started = Instant::now();
            let raw = engine.transcribe(&audio, mode.language())?;
            let elapsed = started.elapsed().as_secs_f32();
            println!("{}", mode.finalize(&raw));
            eprintln!(
                "[{} · mode {mode} · {:.1}s audio in {elapsed:.1}s · {:.2}x realtime]",
                engine.name(),
                audio.duration_secs(),
                elapsed / audio.duration_secs().max(0.001),
            );
        }
        Command::Vad { wav } => {
            let audio = load_wav(&wav).with_context(|| format!("reading {}", wav.display()))?;
            let segments = EnergyVad::default().segments(&audio.samples, audio.sample_rate);
            if segments.is_empty() {
                println!("no speech detected");
            }
            let sr = audio.sample_rate as f32;
            for s in segments {
                println!("{:7.2}s → {:7.2}s", s.start as f32 / sr, s.end as f32 / sr);
            }
        }
        Command::Eval {
            reference,
            hypothesis,
            dir,
            model,
            mode,
            whisper_bin,
            threads,
        } => {
            if let (Some(r), Some(h)) = (reference, hypothesis) {
                let ref_text = resolve_text_or_file(&r)?;
                let hyp_text = resolve_text_or_file(&h)?;

                let w = wer(&ref_text, &hyp_text);
                let c = cer(&ref_text, &hyp_text);

                println!("=== Evaluation ===");
                println!("Reference:  {}", ref_text.trim());
                println!("Hypothesis: {}", hyp_text.trim());
                println!();
                println!(
                    "WER: {:6.2}%  (errors: {}, ref words: {}, sub: {}, del: {}, ins: {}, hits: {})",
                    w.error_rate() * 100.0,
                    w.total_errors(),
                    w.total_ref,
                    w.substitutions,
                    w.deletions,
                    w.insertions,
                    w.hits
                );
                println!(
                    "CER: {:6.2}%  (errors: {}, ref chars: {}, sub: {}, del: {}, ins: {}, hits: {})",
                    c.error_rate() * 100.0,
                    c.total_errors(),
                    c.total_ref,
                    c.substitutions,
                    c.deletions,
                    c.insertions,
                    c.hits
                );
            } else if let Some(dir_path) = dir {
                let model_path =
                    model.context("--model is required when evaluating audio directory")?;
                let engine = WhisperCli {
                    binary: whisper_bin,
                    model: model_path,
                    threads,
                };

                let mut entries = Vec::new();
                for entry in fs::read_dir(&dir_path)
                    .with_context(|| format!("reading directory {}", dir_path.display()))?
                {
                    let entry = entry?;
                    let path = entry.path();
                    if path.is_file() && path.extension().and_then(|e| e.to_str()) == Some("wav") {
                        let txt_path = path.with_extension("txt");
                        if txt_path.exists() {
                            entries.push((path, txt_path));
                        }
                    }
                }

                entries.sort_by(|a, b| a.0.cmp(&b.0));

                if entries.is_empty() {
                    bail!(
                        "no matching .wav and .txt pairs found in {}",
                        dir_path.display()
                    );
                }

                println!(
                    "Evaluating {} audio sample pairs from {} using {}...\n",
                    entries.len(),
                    dir_path.display(),
                    engine.name()
                );

                let mut total_wer = ErrorCounts::default();
                let mut total_cer = ErrorCounts::default();
                let mut total_audio_secs = 0.0f32;
                let mut total_elapsed_secs = 0.0f32;

                for (wav_path, txt_path) in &entries {
                    let ref_text = fs::read_to_string(txt_path)
                        .with_context(|| format!("reading {}", txt_path.display()))?;
                    let audio = load_wav(wav_path)
                        .with_context(|| format!("reading {}", wav_path.display()))?;

                    let started = Instant::now();
                    let raw = engine.transcribe(&audio, mode.language())?;
                    let elapsed = started.elapsed().as_secs_f32();
                    let hyp_text = mode.finalize(&raw);

                    let w = wer(&ref_text, &hyp_text);
                    let c = cer(&ref_text, &hyp_text);

                    total_wer.accumulate(&w);
                    total_cer.accumulate(&c);
                    total_audio_secs += audio.duration_secs();
                    total_elapsed_secs += elapsed;

                    let file_name = wav_path.file_name().and_then(|n| n.to_str()).unwrap_or("?");
                    println!(
                        "{:<20} | WER: {:5.1}% | CER: {:5.1}% | audio: {:4.1}s | took: {:4.1}s",
                        file_name,
                        w.error_rate() * 100.0,
                        c.error_rate() * 100.0,
                        audio.duration_secs(),
                        elapsed
                    );
                }

                let rtf = total_elapsed_secs / total_audio_secs.max(0.001);

                println!("\n=== Evaluation Summary ({} samples) ===", entries.len());
                println!("Total audio:     {:.2}s", total_audio_secs);
                println!(
                    "Total time:      {:.2}s ({:.2}x realtime)",
                    total_elapsed_secs, rtf
                );
                println!(
                    "Overall WER:     {:6.2}%  ({}/{} words, sub: {}, del: {}, ins: {})",
                    total_wer.error_rate() * 100.0,
                    total_wer.total_errors(),
                    total_wer.total_ref,
                    total_wer.substitutions,
                    total_wer.deletions,
                    total_wer.insertions
                );
                println!(
                    "Overall CER:     {:6.2}%  ({}/{} chars, sub: {}, del: {}, ins: {})",
                    total_cer.error_rate() * 100.0,
                    total_cer.total_errors(),
                    total_cer.total_ref,
                    total_cer.substitutions,
                    total_cer.deletions,
                    total_cer.insertions
                );
            } else {
                bail!(
                    "specify either --dir <path> or --reference <text/path> and --hypothesis <text/path>"
                );
            }
        }
    }
    Ok(())
}
