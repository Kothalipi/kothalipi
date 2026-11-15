//! Voice activity detection.
//!
//! v0 is a simple energy (RMS) detector: cheap, dependency-free and good enough
//! for push-to-talk. A neural VAD (e.g. Silero via ONNX) is on the roadmap for
//! noisy environments and hands-free mode.

use std::ops::Range;

/// Energy-based voice activity detector.
#[derive(Debug, Clone)]
pub struct EnergyVad {
    /// RMS level (0.0–1.0) above which a frame counts as speech.
    pub threshold: f32,
    /// Analysis frame length in milliseconds.
    pub frame_ms: u32,
    /// Silence this long ends a speech segment.
    pub min_silence_ms: u32,
    /// Segments shorter than this are dropped as clicks/noise.
    pub min_speech_ms: u32,
}

impl Default for EnergyVad {
    fn default() -> Self {
        Self {
            threshold: 0.02,
            frame_ms: 30,
            min_silence_ms: 300,
            min_speech_ms: 150,
        }
    }
}

impl EnergyVad {
    /// Return sample ranges that contain speech.
    pub fn segments(&self, samples: &[f32], sample_rate: u32) -> Vec<Range<usize>> {
        let frame_len = (sample_rate as usize * self.frame_ms as usize / 1000).max(1);
        let ms_to_samples = |ms: u32| sample_rate as usize * ms as usize / 1000;
        let min_silence = ms_to_samples(self.min_silence_ms);
        let min_speech = ms_to_samples(self.min_speech_ms);

        let mut out = Vec::new();
        let mut start: Option<usize> = None;
        let mut last_voiced_end = 0;

        for (idx, frame) in samples.chunks(frame_len).enumerate() {
            let frame_start = idx * frame_len;
            let frame_end = frame_start + frame.len();
            if rms(frame) >= self.threshold {
                start.get_or_insert(frame_start);
                last_voiced_end = frame_end;
            } else if let Some(s) = start {
                if frame_end - last_voiced_end >= min_silence {
                    if last_voiced_end - s >= min_speech {
                        out.push(s..last_voiced_end);
                    }
                    start = None;
                }
            }
        }
        if let Some(s) = start {
            if last_voiced_end - s >= min_speech {
                out.push(s..last_voiced_end);
            }
        }
        out
    }
}

/// Root-mean-square level of a block of samples.
pub fn rms(samples: &[f32]) -> f32 {
    if samples.is_empty() {
        return 0.0;
    }
    (samples.iter().map(|s| s * s).sum::<f32>() / samples.len() as f32).sqrt()
}

#[cfg(test)]
mod tests {
    use super::*;

    const SR: u32 = 16_000;

    fn tone(secs: f32) -> Vec<f32> {
        let n = (SR as f32 * secs) as usize;
        (0..n)
            .map(|i| 0.5 * (2.0 * std::f32::consts::PI * 220.0 * i as f32 / SR as f32).sin())
            .collect()
    }

    fn silence(secs: f32) -> Vec<f32> {
        vec![0.0; (SR as f32 * secs) as usize]
    }

    #[test]
    fn silence_has_no_speech() {
        assert!(EnergyVad::default().segments(&silence(2.0), SR).is_empty());
    }

    #[test]
    fn finds_speech_between_silence() {
        let audio = [silence(1.0), tone(1.0), silence(1.0)].concat();
        let segs = EnergyVad::default().segments(&audio, SR);
        assert_eq!(segs.len(), 1);
        let frame = (SR * 30 / 1000) as usize;
        assert!(segs[0].start.abs_diff(16_000) <= frame, "{:?}", segs[0]);
        assert!(segs[0].end.abs_diff(32_000) <= frame, "{:?}", segs[0]);
    }

    #[test]
    fn splits_on_long_pause_and_drops_clicks() {
        let click = tone(0.05);
        let audio = [tone(0.5), silence(0.6), tone(0.5), silence(0.6), click].concat();
        assert_eq!(EnergyVad::default().segments(&audio, SR).len(), 2);
    }
}
