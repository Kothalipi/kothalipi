//! `kothalipi`: command-line prototype.
//!
//! Phase 1 goal: measure Bangla accuracy before building any UI.
//!
//! ```text
//! kothalipi transcribe sample.wav --mode banglish --model models/ggml-small.bin
//! kothalipi translit "আমি ভাত খাই।"
//! kothalipi vad sample.wav
//! ```

use std::path::PathBuf;
use std::time::Instant;

use anyhow::{Context, Result};
use clap::{Parser, Subcommand};
use kothalipi_core::audio::load_wav;
use kothalipi_core::engine::{SpeechEngine, WhisperCli};
use kothalipi_core::translit::to_banglish;
use kothalipi_core::vad::EnergyVad;
use kothalipi_core::Mode;

#[derive(Parser)]
#[command(
    name = "kothalipi",
    version,
    about = "Kothalipi: speak in Bangla, English or Banglish, get text."
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
        /// Output mode: bangla, english or banglish.
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
    /// Convert Bangla script to Banglish.
    Translit {
        /// Bangla text.
        #[arg(required = true)]
        text: Vec<String>,
    },
    /// Show where speech is detected in a WAV file.
    Vad { wav: PathBuf },
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
        Command::Translit { text } => println!("{}", to_banglish(&text.join(" "))),
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
    }
    Ok(())
}
