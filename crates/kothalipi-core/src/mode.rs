use std::fmt;
use std::str::FromStr;

use crate::translit::to_banglish;
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
/// - `Bangla`: speak Bangla, get Bangla script (আমি ভাত খাই)
/// - `English`: speak English, get English
/// - `Banglish`: speak Bangla, get romanized Bangla (ami bhat khai)
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Mode {
    #[default]
    Bangla,
    English,
    Banglish,
}

impl Mode {
    pub const ALL: [Mode; 3] = [Mode::Bangla, Mode::English, Mode::Banglish];

    /// Which language the speech engine should recognise for this mode.
    pub fn language(self) -> Language {
        match self {
            Mode::Bangla | Mode::Banglish => Language::Bangla,
            Mode::English => Language::English,
        }
    }

    /// Turn raw engine output into the final text that gets typed.
    pub fn finalize(self, raw: &str) -> String {
        let text = normalize_whitespace(raw);
        match self {
            Mode::Bangla | Mode::English => text,
            Mode::Banglish => to_banglish(&text),
        }
    }
}

impl fmt::Display for Mode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(match self {
            Mode::Bangla => "bangla",
            Mode::English => "english",
            Mode::Banglish => "banglish",
        })
    }
}

impl FromStr for Mode {
    type Err = Error;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s.trim().to_ascii_lowercase().as_str() {
            "bangla" | "bn" | "bengali" => Ok(Mode::Bangla),
            "english" | "en" => Ok(Mode::English),
            "banglish" | "romanized" => Ok(Mode::Banglish),
            other => Err(Error::Invalid(format!(
                "unknown mode `{other}` (use bangla, english or banglish)"
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
        assert_eq!("banglish".parse::<Mode>().unwrap(), Mode::Banglish);
        assert!("klingon".parse::<Mode>().is_err());
    }

    #[test]
    fn banglish_mode_listens_for_bangla() {
        assert_eq!(Mode::Banglish.language(), Language::Bangla);
        assert_eq!(Mode::English.language().code(), "en");
    }

    #[test]
    fn finalize_cleans_and_transliterates() {
        assert_eq!(Mode::Bangla.finalize("  আমি\n ভাত  খাই। "), "আমি ভাত খাই।");
        assert_eq!(Mode::Banglish.finalize("আমি\nভাত খাই।"), "ami bhat khai.");
    }
}
