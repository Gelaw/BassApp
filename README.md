# Bass Fretboard

Android app: play a bass guitar on a flat 2D fretboard. Tap or hold a string/fret
position to sound the note (Karplus–Strong plucked-string synthesis — no samples,
small APK). Multi-touch for chords; lifting a finger mutes. Tunings, fret count,
vertical/horizontal and left-handed neck in settings.

- New here? Read [AGENTS.md](AGENTS.md).
- Plan, milestones, reuse/license decisions: [PLAN.md](PLAN.md).
- Third-party code attribution: [NOTICE](NOTICE).

Build: `./gradlew assembleDebug` (JDK 17 + Android SDK 35). Tests: `./gradlew testDebugUnitTest`.
