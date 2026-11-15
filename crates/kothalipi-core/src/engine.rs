//! Speech-to-text engines.
//!
//! Every engine implements [`SpeechEngine`], so apps can switch between
//! on-device and cloud engines without changing anything else.
//!
//! Available now:
//! - [`WhisperCli`]: runs a whisper.cpp binary (`whisper-cli`) as a subprocess.
//!   Zero build dependencies, perfect for the accuracy-testing prototype.
//!
//! Planned: in-process whisper.cpp (whisper-rs), sherpa-onnx, and optional
//! cloud engines (Google, Azure, OpenAI) behind feature flags.

use std::path::PathBuf;
use std::process::Command;
use std::sync::atomic::{AtomicU64, Ordering};

use crate::audio::{write_wav_mono_i16, Audio};
use crate::{Error, Language, Result};

/// A speech-to-text engine.
pub trait SpeechEngine {
    /// Short human-readable name, e.g. "whisper-cli".
    fn name(&self) -> &str;

    /// Transcribe audio spoken in `language`. Returns raw text; apply
    /// [`crate::Mode::finalize`] before typing it.
    fn transcribe(&self, audio: &Audio, language: Language) -> Result<String>;
}

/// whisper.cpp via its command-line binary.
///
/// Install on macOS with `brew install whisper-cpp`, or build from
/// <https://github.com/ggml-org/whisper.cpp>.
#[derive(Debug, Clone)]
pub struct WhisperCli {
    /// Path or name of the binary (`whisper-cli` if it is on PATH).
    pub binary: PathBuf,
    /// Path to a ggml model file, e.g. `models/ggml-small.bin`.
    pub model: PathBuf,
    /// CPU threads; `None` lets whisper.cpp decide.
    pub threads: Option<u32>,
}

impl WhisperCli {
    pub fn new(model: impl Into<PathBuf>) -> Self {
        Self {
            binary: PathBuf::from("whisper-cli"),
            model: model.into(),
            threads: None,
        }
    }
}

impl SpeechEngine for WhisperCli {
    fn name(&self) -> &str {
        "whisper-cli"
    }

    fn transcribe(&self, audio: &Audio, language: Language) -> Result<String> {
        if !self.model.exists() {
            return Err(Error::Invalid(format!(
                "model not found: {} (see models/README.md)",
                self.model.display()
            )));
        }

        let wav = temp_wav_path();
        write_wav_mono_i16(&wav, &audio.to_16k())?;

        let mut cmd = Command::new(&self.binary);
        cmd.arg("-m")
            .arg(&self.model)
            .arg("-f")
            .arg(&wav)
            .arg("-l")
            .arg(language.code())
            .arg("-nt") // no timestamps
            .arg("-np"); // no progress/system prints
        if let Some(t) = self.threads {
            cmd.arg("-t").arg(t.to_string());
        }

        let output = cmd.output();
        std::fs::remove_file(&wav).ok();
        let output = output.map_err(|e| {
            Error::Engine(format!(
                "could not run `{}`: {e} (is whisper.cpp installed?)",
                self.binary.display()
            ))
        })?;

        if !output.status.success() {
            return Err(Error::Engine(
                String::from_utf8_lossy(&output.stderr).trim().to_string(),
            ));
        }
        Ok(String::from_utf8_lossy(&output.stdout).trim().to_string())
    }
}

fn temp_wav_path() -> PathBuf {
    static COUNTER: AtomicU64 = AtomicU64::new(0);
    let n = COUNTER.fetch_add(1, Ordering::Relaxed);
    std::env::temp_dir().join(format!("kothalipi-{}-{n}.wav", std::process::id()))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn missing_model_is_a_clear_error() {
        let engine = WhisperCli::new("/definitely/not/here.bin");
        let audio = Audio {
            samples: vec![0.0; 16_000],
            sample_rate: 16_000,
        };
        let err = engine.transcribe(&audio, Language::Bangla).unwrap_err();
        assert!(err.to_string().contains("model not found"));
    }
}
