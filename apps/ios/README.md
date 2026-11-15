# Kothalipi for iOS

**Status:** last (Phase 6), because of platform limits.

- **Language:** Swift, keyboard extension + host app
- **Limit:** keyboard extensions get ~60 MB of memory and restricted mic access,
  so large models cannot run inside the keyboard.
- **Workaround:** the mic button opens the host app, which records and transcribes,
  then hands the text back to the keyboard (shared App Group container).
