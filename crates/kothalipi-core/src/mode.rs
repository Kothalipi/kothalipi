use std::fmt;
use std::str::FromStr;

use crate::Error;

/// The spoken language the speech engine should listen for.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Language {
    Bangla,
    English,
    /// Let the engine detect the language itself.
    Auto,
}

impl Language {
    /// ISO 639-1 code understood by Whisper-style engines.
    pub fn code(self) -> &'static str {
        match self {
            Language::Bangla => "bn",
            Language::English => "en",
            Language::Auto => "auto",
        }
    }
}

/// What the user wants typed out.
///
/// - `Bangla`: speak Bangla (mixing in English is fine), get Bangla script
/// - `English`: speak English, get English
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Mode {
    #[default]
    Bangla,
    English,
}

impl Mode {
    pub const ALL: [Mode; 2] = [Mode::Bangla, Mode::English];

    /// Which language the speech engine should recognise for this mode.
    pub fn language(self) -> Language {
        match self {
            Mode::Bangla => Language::Bangla,
            Mode::English => Language::English,
        }
    }

    /// Turn raw engine output into the final text that gets typed.
    pub fn finalize(self, raw: &str) -> String {
        normalize_whitespace(raw)
    }
}

impl fmt::Display for Mode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Mode::Bangla => "bangla",
            Mode::English => "english",
        })
    }
}

impl FromStr for Mode {
    type Err = Error;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s.trim().to_ascii_lowercase().as_str() {
            "bangla" | "bn" | "bengali" => Ok(Mode::Bangla),
            "english" | "en" => Ok(Mode::English),
            other => Err(Error::Invalid(format!(
                "unknown mode `{other}` (use bangla or english)"
            ))),
        }
    }
}

/// Collapse runs of whitespace (engines often emit newlines between segments).
pub fn normalize_whitespace(s: &str) -> String {
    s.split_whitespace().collect::<Vec<_>>().join(" ")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_modes() {
        assert_eq!("Bangla".parse::<Mode>().unwrap(), Mode::Bangla);
        assert_eq!("en".parse::<Mode>().unwrap(), Mode::English);
        assert!("banglish".parse::<Mode>().is_err());
        assert!("klingon".parse::<Mode>().is_err());
    }

    #[test]
    fn modes_pick_their_language() {
        assert_eq!(Mode::Bangla.language(), Language::Bangla);
        assert_eq!(Mode::English.language().code(), "en");
    }

    #[test]
    fn finalize_cleans_whitespace() {
        assert_eq!(Mode::Bangla.finalize("  আমি\n ভাত  খাই। "), "আমি ভাত খাই।");
    }
}
