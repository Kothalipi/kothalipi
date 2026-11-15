//! Audio helpers: load WAV files, mix to mono, resample to 16 kHz.
//!
//! Live microphone capture (cpal) will land here next; platform apps can also
//! capture natively and pass samples straight into the core.

use std::path::Path;

use crate::Result;

/// Sample rate every speech engine in Kothalipi expects.
pub const TARGET_SAMPLE_RATE: u32 = 16_000;

/// Mono PCM audio as f32 samples in -1.0..=1.0.
#[derive(Debug, Clone)]
pub struct Audio {
    pub samples: Vec<f32>,
    pub sample_rate: u32,
}

impl Audio {
    pub fn duration_secs(&self) -> f32 {
        self.samples.len() as f32 / self.sample_rate as f32
    }

    /// Return a copy at 16 kHz (no-op copy if already there).
    pub fn to_16k(&self) -> Audio {
        Audio {
            samples: resample(&self.samples, self.sample_rate, TARGET_SAMPLE_RATE),
            sample_rate: TARGET_SAMPLE_RATE,
        }
    }
}

/// Load any PCM or float WAV and mix it down to mono.
pub fn load_wav(path: impl AsRef<Path>) -> Result<Audio> {
    let mut reader = hound::WavReader::open(path)?;
    let spec = reader.spec();
    let interleaved: Vec<f32> = match spec.sample_format {
        hound::SampleFormat::Float => reader
            .samples::<f32>()
            .collect::<std::result::Result<_, _>>()?,
        hound::SampleFormat::Int => {
            let scale = (1_i64 << (spec.bits_per_sample - 1)) as f32;
            reader
                .samples::<i32>()
                .map(|s| s.map(|v| v as f32 / scale))
                .collect::<std::result::Result<_, _>>()?
        }
    };
    let channels = spec.channels.max(1) as usize;
    let samples = interleaved
        .chunks(channels)
        .map(|frame| frame.iter().sum::<f32>() / channels as f32)
        .collect();
    Ok(Audio {
        samples,
        sample_rate: spec.sample_rate,
    })
}

/// Write 16-bit mono WAV (the format whisper.cpp reads).
pub fn write_wav_mono_i16(path: impl AsRef<Path>, audio: &Audio) -> Result<()> {
    let spec = hound::WavSpec {
        channels: 1,
        sample_rate: audio.sample_rate,
        bits_per_sample: 16,
        sample_format: hound::SampleFormat::Int,
    };
    let mut writer = hound::WavWriter::create(path, spec)?;
    for s in &audio.samples {
        writer.write_sample((s.clamp(-1.0, 1.0) * i16::MAX as f32) as i16)?;
    }
    writer.finalize()?;
    Ok(())
}

/// Linear-interpolation resampler. Fine for speech; swap for a windowed-sinc
/// resampler if we ever need music-grade quality.
pub fn resample(samples: &[f32], from: u32, to: u32) -> Vec<f32> {
    if from == to || samples.is_empty() {
        return samples.to_vec();
    }
    let ratio = from as f64 / to as f64;
    let out_len = ((samples.len() as f64) / ratio).round() as usize;
    (0..out_len)
        .map(|i| {
            let pos = i as f64 * ratio;
            let idx = pos.floor() as usize;
            let frac = (pos - idx as f64) as f32;
            let a = samples[idx.min(samples.len() - 1)];
            let b = samples[(idx + 1).min(samples.len() - 1)];
            a + (b - a) * frac
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resample_changes_length_by_ratio() {
        let one_sec_48k = vec![0.25; 48_000];
        let out = resample(&one_sec_48k, 48_000, 16_000);
        assert_eq!(out.len(), 16_000);
        assert!(out.iter().all(|s| (s - 0.25).abs() < 1e-6));
    }

    #[test]
    fn wav_roundtrip() {
        let path = std::env::temp_dir().join(format!("kothalipi-test-{}.wav", std::process::id()));
        let audio = Audio {
            samples: vec![0.0, 0.5, -0.5, 0.25],
            sample_rate: 16_000,
        };
        write_wav_mono_i16(&path, &audio).unwrap();
        let back = load_wav(&path).unwrap();
        std::fs::remove_file(&path).ok();
        assert_eq!(back.sample_rate, 16_000);
        for (a, b) in audio.samples.iter().zip(&back.samples) {
            assert!((a - b).abs() < 1e-3);
        }
    }
}
