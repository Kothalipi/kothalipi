//! Accuracy metrics for speech recognition evaluation.
//!
//! Provides Word Error Rate (WER) and Character Error Rate (CER) calculation
//! using Levenshtein distance alignment.

/// Breakdown of alignment errors between reference and hypothesis.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct ErrorCounts {
    /// Replaced tokens.
    pub substitutions: usize,
    /// Tokens present in reference but missing in hypothesis.
    pub deletions: usize,
    /// Tokens present in hypothesis but not in reference.
    pub insertions: usize,
    /// Exact matching tokens.
    pub hits: usize,
    /// Total reference tokens (substitutions + deletions + hits).
    pub total_ref: usize,
}

impl ErrorCounts {
    /// Calculate error rate: (S + D + I) / N.
    ///
    /// Returns 0.0 if both reference and hypothesis are empty.
    /// Returns 1.0 if reference is empty and hypothesis is non-empty.
    pub fn error_rate(&self) -> f64 {
        if self.total_ref == 0 {
            if self.insertions > 0 {
                1.0
            } else {
                0.0
            }
        } else {
            (self.substitutions + self.deletions + self.insertions) as f64 / self.total_ref as f64
        }
    }

    /// Total error count (substitutions + deletions + insertions).
    pub fn total_errors(&self) -> usize {
        self.substitutions + self.deletions + self.insertions
    }

    /// Merge counts from another evaluation instance.
    pub fn accumulate(&mut self, other: &ErrorCounts) {
        self.substitutions += other.substitutions;
        self.deletions += other.deletions;
        self.insertions += other.insertions;
        self.hits += other.hits;
        self.total_ref += other.total_ref;
    }
}

/// Align reference and hypothesis token slices to compute Levenshtein error counts.
pub fn align<T: PartialEq>(reference: &[T], hypothesis: &[T]) -> ErrorCounts {
    let n = reference.len();
    let m = hypothesis.len();

    if n == 0 && m == 0 {
        return ErrorCounts::default();
    }
    if n == 0 {
        return ErrorCounts {
            insertions: m,
            total_ref: 0,
            ..Default::default()
        };
    }
    if m == 0 {
        return ErrorCounts {
            deletions: n,
            total_ref: n,
            ..Default::default()
        };
    }

    // dp[i][j] holds edit distance between reference[..i] and hypothesis[..j]
    let mut dp = vec![vec![0usize; m + 1]; n + 1];

    for (i, row) in dp.iter_mut().enumerate() {
        row[0] = i;
    }
    for (j, cell) in dp[0].iter_mut().enumerate() {
        *cell = j;
    }

    for i in 1..=n {
        for j in 1..=m {
            let cost = if reference[i - 1] == hypothesis[j - 1] {
                0
            } else {
                1
            };
            let del = dp[i - 1][j] + 1;
            let ins = dp[i][j - 1] + 1;
            let sub = dp[i - 1][j - 1] + cost;
            dp[i][j] = del.min(ins).min(sub);
        }
    }

    // Backtrack to count operations. Prioritize hits, then insertions/deletions, then substitutions.
    let mut i = n;
    let mut j = m;
    let mut substitutions = 0;
    let mut deletions = 0;
    let mut insertions = 0;
    let mut hits = 0;

    while i > 0 || j > 0 {
        if i > 0 && j > 0 && reference[i - 1] == hypothesis[j - 1] && dp[i][j] == dp[i - 1][j - 1] {
            hits += 1;
            i -= 1;
            j -= 1;
        } else if j > 0 && dp[i][j] == dp[i][j - 1] + 1 {
            insertions += 1;
            j -= 1;
        } else if i > 0 && dp[i][j] == dp[i - 1][j] + 1 {
            deletions += 1;
            i -= 1;
        } else if i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1 {
            substitutions += 1;
            i -= 1;
            j -= 1;
        } else {
            break;
        }
    }

    ErrorCounts {
        substitutions,
        deletions,
        insertions,
        hits,
        total_ref: n,
    }
}

/// Compute Word Error Rate (WER) between reference and hypothesis text.
///
/// Tokens are extracted by splitting on whitespace.
pub fn wer(reference: &str, hypothesis: &str) -> ErrorCounts {
    let ref_tokens: Vec<&str> = reference.split_whitespace().collect();
    let hyp_tokens: Vec<&str> = hypothesis.split_whitespace().collect();
    align(&ref_tokens, &hyp_tokens)
}

/// Compute Character Error Rate (CER) between reference and hypothesis text.
///
/// Characters are evaluated excluding whitespace.
pub fn cer(reference: &str, hypothesis: &str) -> ErrorCounts {
    let ref_chars: Vec<char> = reference.chars().filter(|c| !c.is_whitespace()).collect();
    let hyp_chars: Vec<char> = hypothesis.chars().filter(|c| !c.is_whitespace()).collect();
    align(&ref_chars, &hyp_chars)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_wer_identical() {
        let text = "আমি বাংলায় কথা বলি";
        let res = wer(text, text);
        assert_eq!(res.total_errors(), 0);
        assert_eq!(res.hits, 4);
        assert_eq!(res.error_rate(), 0.0);
    }

    #[test]
    fn test_wer_substitution() {
        let r = "the quick brown fox";
        let h = "the fast brown fox";
        let res = wer(r, h);
        assert_eq!(res.substitutions, 1);
        assert_eq!(res.hits, 3);
        assert_eq!(res.deletions, 0);
        assert_eq!(res.insertions, 0);
        assert_eq!(res.total_ref, 4);
        assert_eq!(res.error_rate(), 0.25);
    }

    #[test]
    fn test_wer_deletions_insertions() {
        let r = "hello world again";
        let h = "hello again now";
        let res = wer(r, h);
        assert_eq!(res.deletions, 1); // "world" deleted
        assert_eq!(res.insertions, 1); // "now" inserted
        assert_eq!(res.hits, 2);
        assert_eq!(res.total_ref, 3);
        assert_eq!(res.total_errors(), 2);
    }

    #[test]
    fn test_cer_bangla() {
        let r = "কথালিপি";
        let h = "কথালিবি";
        let res = cer(r, h);
        assert_eq!(res.substitutions, 1);
        assert_eq!(res.hits, 6);
        assert_eq!(res.total_ref, 7);
    }

    #[test]
    fn test_empty_strings() {
        let res = wer("", "");
        assert_eq!(res.error_rate(), 0.0);

        let res_ins = wer("", "extra");
        assert_eq!(res_ins.insertions, 1);
        assert_eq!(res_ins.error_rate(), 1.0);

        let res_del = wer("missing", "");
        assert_eq!(res_del.deletions, 1);
        assert_eq!(res_del.error_rate(), 1.0);
    }
}
