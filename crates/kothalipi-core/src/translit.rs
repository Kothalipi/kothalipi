//! Bangla script → Banglish (romanized Bangla) transliteration.
//!
//! This is a rule-based v0 that follows how people actually type Banglish on
//! chat apps ("ami bhat khai", "kemon acho"), not a scholarly scheme like ISO 15919.
//!
//! Rules:
//! - Every consonant carries an inherent "o" (ক = ko) unless it is followed by
//!   a vowel sign (কা = ka), a hasanta (ক্ = k), or ends the word (ভাত = bhat).
//! - Ya-phala (্য) becomes "y".
//! - Bangla digits and the dari (।) map to ASCII.
//!
//! Known gaps (good first issues!): mid-word schwa deletion (করব → korob, not
//! korbo), and juktakkhor with special spoken forms (ক্ষ, জ্ঞ).

const HASANTA: char = '\u{09CD}';

/// Transliterate Bangla text to Banglish. Non-Bangla characters pass through.
pub fn to_banglish(input: &str) -> String {
    let normalized = normalize_nukta(input);
    let chars: Vec<char> = normalized.chars().collect();
    let mut out = String::with_capacity(input.len());
    let mut prev_hasanta = false;
    let mut i = 0;

    while i < chars.len() {
        let c = chars[i];
        let next = chars.get(i + 1).copied();

        if let Some(base) = consonant(c) {
            // য after a hasanta is ya-phala: ব্যস্ত → byosto
            let base = if c == 'য' && prev_hasanta {
                "y"
            } else {
                base
            };
            out.push_str(base);
            prev_hasanta = false;

            match next {
                Some(n) if vowel_sign(n).is_some() => {
                    out.push_str(vowel_sign(n).unwrap_or_default());
                    i += 2;
                }
                Some(HASANTA) => {
                    prev_hasanta = true;
                    i += 2;
                }
                Some(n) if is_bangla_letter(n) => {
                    out.push('o');
                    i += 1;
                }
                _ => {
                    // Word-final consonant: inherent vowel is silent (ভাত → bhat).
                    i += 1;
                }
            }
            continue;
        }

        prev_hasanta = false;
        if let Some(v) = independent_vowel(c) {
            out.push_str(v);
        } else if let Some(m) = modifier(c) {
            out.push_str(m);
        } else if let Some(d) = digit(c) {
            out.push(d);
        } else if c == '।' {
            out.push('.');
        } else if c == '\u{200C}' || c == '\u{200D}' {
            // zero-width joiners: drop
        } else {
            out.push(c);
        }
        i += 1;
    }
    out
}

/// Fold decomposed nukta forms into their precomposed letters (য + ় → য়).
fn normalize_nukta(s: &str) -> String {
    s.replace("\u{09AF}\u{09BC}", "\u{09DF}")
        .replace("\u{09A1}\u{09BC}", "\u{09DC}")
        .replace("\u{09A2}\u{09BC}", "\u{09DD}")
}

fn is_bangla_letter(c: char) -> bool {
    consonant(c).is_some()
        || independent_vowel(c).is_some()
        || modifier(c).is_some()
        || vowel_sign(c).is_some()
}

fn consonant(c: char) -> Option<&'static str> {
    Some(match c {
        'ক' => "k",
        'খ' => "kh",
        'গ' => "g",
        'ঘ' => "gh",
        'ঙ' => "ng",
        'চ' => "ch",
        'ছ' => "chh",
        'জ' => "j",
        'ঝ' => "jh",
        'ঞ' => "n",
        'ট' => "t",
        'ঠ' => "th",
        'ড' => "d",
        'ঢ' => "dh",
        'ণ' => "n",
        'ত' => "t",
        'থ' => "th",
        'দ' => "d",
        'ধ' => "dh",
        'ন' => "n",
        'প' => "p",
        'ফ' => "f",
        'ব' => "b",
        'ভ' => "bh",
        'ম' => "m",
        'য' => "j",
        'র' => "r",
        'ল' => "l",
        'শ' => "sh",
        'ষ' => "sh",
        'স' => "s",
        'হ' => "h",
        '\u{09DC}' => "r",
        '\u{09DD}' => "rh",
        '\u{09DF}' => "y",
        'ৎ' => "t",
        _ => return None,
    })
}

fn independent_vowel(c: char) -> Option<&'static str> {
    Some(match c {
        'অ' => "o",
        'আ' => "a",
        'ই' | 'ঈ' => "i",
        'উ' | 'ঊ' => "u",
        'ঋ' => "ri",
        'এ' => "e",
        'ঐ' => "oi",
        'ও' => "o",
        'ঔ' => "ou",
        _ => return None,
    })
}

fn vowel_sign(c: char) -> Option<&'static str> {
    Some(match c {
        'া' => "a",
        'ি' | 'ী' => "i",
        'ু' | 'ূ' => "u",
        'ৃ' => "ri",
        'ে' => "e",
        'ৈ' => "oi",
        'ো' => "o",
        'ৌ' => "ou",
        _ => return None,
    })
}

fn modifier(c: char) -> Option<&'static str> {
    Some(match c {
        'ং' => "ng",
        'ঃ' => "h",
        'ঁ' => "",
        _ => return None,
    })
}

fn digit(c: char) -> Option<char> {
    let n = (c as u32).checked_sub('০' as u32)?;
    (n < 10).then(|| char::from_digit(n, 10)).flatten()
}

#[cfg(test)]
mod tests {
    use super::to_banglish;

    #[test]
    fn common_words() {
        for (bn, en) in [
            ("আমি", "ami"),
            ("তুমি", "tumi"),
            ("ভাত", "bhat"),
            ("খাই", "khai"),
            ("কথা", "kotha"),
            ("কেমন", "kemon"),
            ("আমার", "amar"),
            ("বাংলা", "bangla"),
            ("বাংলাদেশ", "bangladesh"),
            ("বন্ধু", "bondhu"),
            ("কথালিপি", "kothalipi"),
        ] {
            assert_eq!(to_banglish(bn), en, "{bn}");
        }
    }

    #[test]
    fn sentence_with_dari() {
        assert_eq!(to_banglish("আমি বাংলায় কথা বলি।"), "ami banglay kotha boli.");
    }

    #[test]
    fn digits_and_passthrough() {
        assert_eq!(to_banglish("২০২৬ সাল"), "2026 sal");
        assert_eq!(to_banglish("Hello কথা!"), "Hello kotha!");
    }

    #[test]
    fn decomposed_nukta_matches_precomposed() {
        assert_eq!(to_banglish("বাংলা\u{09AF}\u{09BC}"), to_banglish("বাংলায়"));
    }
}
