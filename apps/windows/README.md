# Kothalipi for Windows

**Status:** Phase 3.

- **Language:** Rust (`windows-rs`), so no extra bindings are needed
- **UI:** system tray first; settings window in Win32 or WinUI 3
- **Text insertion:** `SendInput` with `KEYEVENTF_UNICODE` (works for Bangla script)
- **Hotkey:** `RegisterHotKey`
- **Acceleration:** whisper.cpp with Vulkan/CUDA where available
