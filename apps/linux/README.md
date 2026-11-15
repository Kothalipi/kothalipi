# Kothalipi for Linux

**Status:** Phase 5 (best effort).

- **Language:** Rust, tray app (GTK4 settings window later)
- **X11:** inject text with XTest
- **Wayland:** no standard way to type into other apps. Options: an IBus/Fcitx5
  input-method engine (most robust), `wtype` (wlroots compositors), or clipboard paste.
- Note: Canonical ships a dictation tool called Myna for Ubuntu. Learn from it,
  and stand out with Bangla quality.
