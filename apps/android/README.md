# Kothalipi for Android

Free Bangla and English voice typing as a **keyboard**. Add it once, then tap the mic in any app.

**Status:** v0.1.0, first working version (debug builds, not on the Play Store yet).

## What it does

- A voice keyboard (`InputMethodService`): big mic, বাংলা / English chips, `, । ? .` keys, space, delete, enter, and a globe key to switch back to your normal keyboard.
- **Tap the mic** to start and tap again to finish, or **hold** it and let go.
- Setup that explains itself: microphone permission, turn the keyboard on, choose it, then pick an engine. It moves on by itself as each step is done.
- **Engines**
  - **Cloud:** your own Google Gemini key (`gemini-3.5-transcribe`) and/or OpenAI key (`gpt-transcribe`). If one fails, the next is tried. If all fail, the next dictation uses on-device.
  - **On-device:** Android's built-in speech recognizer, with the Bangla language pack installed on the phone. Free, no key.
- API keys are encrypted with an Android Keystore key and stay on the phone. No account, no analytics.

## What it does not do yet

- No bundled offline model. The offline Qwen3-ASR / Whisper models used on the desktop need a native (JNI) build of the core; that is the next step. For now, "on-device" means Android's own recognizer, and its Bangla quality depends on the phone.
- No Play Store build or signing yet.
- The app UI is English. The keyboard's status messages have Bangla strings.

## Build

Requirements: JDK 17, Android SDK 36.

```bash
cd apps/android
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
./gradlew testDebugUnitTest assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open Kothalipi and follow the four setup steps.

## Layout

```
app/src/main/java/com/kothalipi/android/
  MainActivity.kt        setup + home (Compose)
  ui/                    theme, setup steps, system-state checks
  ime/KothalipiIme.kt    the keyboard
  stt/                   Recorder, WAV, CloudStt (Gemini/OpenAI), DeviceStt, Dictation
  data/Prefs.kt          settings + encrypted keys
```

The cloud requests match the desktop app (`cloud_stt.rs`), and `CloudSttTest` covers the request bodies, response parsing and error classification.
