# Plan trace — Bass Fretboard Android app

Status: **M1 in progress** (scaffold + audio core + fretboard UI implemented, unit tests written).
Origin: plan agreed in the planning session; this file is the living trace (update the
milestone checklist and decision log as work lands).

## Goal

Android app: a flat 2D fretboard of a bass guitar. Tapping or holding a string/fret
position plays that note (Karplus–Strong plucked-string synthesis). Multi-touch
supports chords. Lifting the finger mutes the note (short release envelope).

## Reuse map (licenses verified via GitHub API)

| Need | Resource | License | Status |
|---|---|---|---|
| Plucked-string synth | [raphlinus/music-synthesizer-for-android-old](https://github.com/raphlinus/music-synthesizer-for-android-old) `KarplusStrong.java` | Apache-2.0 | **Vendored** as `app/.../audio/KarplusStrongVoice.kt` (ported to Kotlin, modification notice in header, see `NOTICE`) |
| Low-latency audio output | [google/oboe](https://github.com/google/oboe) | Apache-2.0 | Deferred — see D1 |
| Android wiring reference | [maks/droid-synth](https://github.com/maks/droid-synth) | Apache-2.0 | Reference only (patterns, not code) |
| Full engine alternative | [igorski/MWEngine](https://github.com/igorski/MWEngine) | MIT | Not used (too heavy for v1) |
| Bass KS parameter ideas | [swannekim/bass-fretboard-note-trainer](https://github.com/swannekim/bass-fretboard-note-trainer), [ArthurNTI/open-fretboard](https://github.com/ArthurNTI/open-fretboard) | AGPL-3.0 / GPL-3.0 | Reference only — parameters reimplemented, no code copied |
| Realistic samples (optional) | [FreePats CC0 electric bass](https://freepats.zenvoid.org/ElectricGuitar/clean-electric-bass.html) | CC0 | Optional "real bass" sound pack, M3 |
| C++ synth upgrade path | [thestk/stk](https://github.com/thestk/stk) `Mandolin` | permissive | Only if profiling shows Java KS too heavy |
| Pitch detection (tuner, optional) | YIN algorithm (TarsosDSP is GPL-3.0 — do not copy) | — | Implement from paper if wanted |
| Fretboard UX reference | [nadavleh/guitar-app](https://github.com/nadavleh/guitar-app) | no LICENSE file | Reference only — do not copy code |

## Architecture

```
ui (Compose)  →  pointer → (string, fret) via PointerNoteMapper  →  NoteEvent
                                                                ↓
model: Notes / Tuning / FretboardGeometry (pure Kotlin, unit-tested)
                                                                ↓
audio: KarplusStrongVoice (vendored Apache-2.0) → AudioTrackEngine (voice pool + mixer thread)
```

- `model/` — no Android imports, plain JVM unit tests.
- `audio/KarplusStrongVoice.kt` — no Android imports, JVM-testable DSP.
- `audio/AudioEngine` interface — Oboe/NDK backend can be swapped in later without touching UI.
- `ui/` — Compose Canvas + `pointerInput` multi-touch, DataStore-backed settings.

## Milestones

- [x] **M1** — Scaffold, vendored KS voice, AudioTrack engine, fretboard canvas, tap→note mapping, unit tests.
- [ ] **M2** — Verify on device: latency, multi-touch chords, hold-to-ring, release mute, vibration visuals.
- [ ] **M3** — Settings polish (tunings incl. 5-string, fret count 12–24, vertical, left-handed, note names); optional CC0 sample pack.
- [ ] **M4** — Perf pass, Play Store prep (signing, icons, privacy policy).

## Decision log

- **D1 (v1 audio output):** pure-Kotlin `AudioTrack` in `PERFORMANCE_MODE_LOW_LATENCY`
  instead of Oboe/NDK. Rationale: Oboe's own FAQ notes that for audio generated in
  Kotlin, JNI overhead may not be worth it; keeps the whole app compilable without the
  NDK; `AudioEngine` interface leaves the Oboe swap open. Oboe samples (`RhythmGame`,
  `MegaDrone`) remain the template if latency profiling demands it.
- **D2:** synth = Karplus–Strong ported from the Apache-2.0 repo (not written from
  scratch), with injectable `Random` for deterministic tests.
- **D3:** GPL-3.0/AGPL-3.0/unlicensed projects are reference-only. No code from them is
  in this repo. If the app is ever released under GPL, the rule can be revisited.
- **D4:** release semantics = lift finger → fast mute (80 ms fade), like a real bass;
  the note rings naturally while held.
- **D5:** v1 fretboard shows 12–24 frets fit-to-screen (no pan/zoom); pinch-zoom/pan is
  a later milestone.
- **D6:** mono 44.1 kHz 16-bit PCM output, max 8 voices, tanh soft-clip on the mix bus.

## Open questions

- Target GitHub repo for delivery (branch + PR) — pending.
- Sampled "real bass" mode (FreePats CC0) in v1 or M3?
- Ship a tuner (YIN from paper) or skip mic features entirely?
