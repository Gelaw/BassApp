# AGENTS.md — Bass Fretboard (Android)

Onboarding doc for agents working on this repo. The full plan trace lives in
[PLAN.md](PLAN.md); read it first.

## What this app is

A bass-guitar practice/play app: a flat 2D fretboard fills the screen. Tap or hold a
string at a fret to play that note (Karplus–Strong plucked-string synth, no samples).
Multi-touch plays chords. Lifting a finger mutes the note. Settings offer tunings
(4/5-string, drop D, D standard, piccolo), fret count, vertical/horizontal neck,
left-handed mode, and note-name labels.

## Quick start

- JDK 17, Android Studio (Ladybug or newer), Android SDK with platform 35.
- `gradle/wrapper/gradle-wrapper.jar` is vendored, so `./gradlew` works directly.
- Build: `./gradlew assembleDebug`
- Unit tests (pure JVM, no emulator needed): `./gradlew testDebugUnitTest`
- Instrumented Compose UI test (needs an emulator/device):
  `./gradlew connectedDebugAndroidTest`
- Run on device/emulator from Android Studio. Audio latency is best judged on a real
  device.

## Repo layout

```
app/src/main/java/com/vibe/bassfretboard/
  MainActivity.kt               entry point, Compose content
  model/                        pure Kotlin, no Android deps, unit-tested on JVM
    Notes.kt                    midi <-> frequency / note name
    Tuning.kt                   tuning presets (open-string midi per string)
    FretboardGeometry.kt        fret positions x(n)=L*(1-2^(-n/12)), hit test
    PointerNoteMapper.kt        touch (x,y) -> (string, fret, midi)
  audio/
    KarplusStrongVoice.kt       VENDORED Apache-2.0 code (see NOTICE) — do not rewrite
    AudioEngine.kt              interface: start/stop/noteOn/noteOff
    AudioTrackEngine.kt         AudioTrack low-latency stream, voice pool, mixer thread
  ui/
    FretboardScreen.kt          Compose Canvas fretboard + multi-touch pointerInput + settings dialog
    FretboardLayout.kt          screen<->logical neck coordinate mapping
    FretboardViewModel.kt       pointer events -> engine + UI state
    Settings.kt                 DataStore-backed AppSettings
app/src/test/...                JUnit4 tests for model/ and KarplusStrongVoice
```

## Conventions

- Kotlin, official code style; Jetpack Compose + MVVM; Gradle KTS with version catalog
  (`gradle/libs.versions.toml`).
- `model/` and `audio/KarplusStrongVoice.kt` must stay Android-free so they run in plain
  JVM unit tests. Keep it that way.
- No inline comments in code (license headers excepted).
- Touch handling: one pointer = one note; dragging along a string re-triggers
  (hammer-on/slide); release = 80 ms mute.

## Licensing rules (important)

- `audio/KarplusStrongVoice.kt` is ported from Apache-2.0
  [music-synthesizer-for-android](https://github.com/raphlinus/music-synthesizer-for-android-old).
  Keep its license header and the MODIFICATIONS notice; keep `NOTICE` in sync.
- GPL-3.0/AGPL-3.0 repos (TarsosDSP, open-fretboard, bass-fretboard-note-trainer) and the
  unlicensed Chorect app are **reference only**: never copy their code into this repo.
- CC0 assets (e.g. FreePats) may be shipped as-is.

## Testing

- `./gradlew testDebugUnitTest` — covers fret math, note math, pointer mapping, and the
  KS voice (finite output, decay, pitch accuracy, low-note buffer fit).
- `./gradlew connectedDebugAndroidTest` — Compose UI smoke test (`FretboardUiTest`).
- The UI/audio path needs a device or emulator; add instrumented tests when touching
  `AudioTrackEngine` or touch handling.

## Roadmap

See milestone checklist in [PLAN.md](PLAN.md) (M1 done, M2 device verification next).
