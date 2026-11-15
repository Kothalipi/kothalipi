//! Kothalipi core: the shared engine behind every Kothalipi app.
//!
//! Platform apps (macOS, Windows, Linux, Android, iOS) only handle UI and
//! text insertion. Everything else lives here:
//!
//! - [`audio`]: WAV loading, resampling to 16 kHz mono
//! - [`vad`]: voice activity detection (find where speech starts and stops)
//! - [`engine`]: speech-to-text engines behind one [`engine::SpeechEngine`] trait
//! - [`mode`]: output modes (Bangla, English) and final text cleanup

pub mod audio;
pub mod engine;
pub mod error;
pub mod mode;
pub mod vad;

pub use error::{Error, Result};
pub use mode::{Language, Mode};
