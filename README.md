# SleepSmart

A presentable, unit-tested Android app implementing the SleepSmart v0.1 spec.

SleepSmart listens to ambient sound while you sleep, derives features locally
in 30-second windows, classifies sleep stages with a heuristic baseline, and
fires an alarm at the lightest moment in your wake window. **Audio never
leaves your device** — no network, no logs, no persisted samples. The manifest
deliberately omits `INTERNET`.

## Quick start

```bash
# Unit tests (JVM, JUnit 5)
./gradlew test

# Install debug build
./gradlew installDebug
adb shell am start -n com.sleepsmart.app/.MainActivity
```

## Architecture

Single Gradle module, package-based layering inside `com.sleepsmart.app`.
Dependencies flow downward: `ui` → `domain` → `data` / `audio` / `classifier`
/ `wake`. The foreground tracking service orchestrates the audio pipeline,
classifier, smart-wake engine, and storage end-to-end.

Key seams:

- `audio.AudioCapture` — `Flow<ShortArray>` from `AudioRecord` (or synthetic).
- `audio.FeatureExtractor` — turns a 30 s window into a `FeatureVector`.
- `classifier.SleepStageClassifier` — `HeuristicS4M` (rule-based) ships;
  `tflite.TFLiteS4M` is a stub for v1.x.
- `classifier.HmmSmoother` — causal post-processor that prevents 1-epoch flips.
- `wake.SmartWakeEngine` — pure stateful function that picks the best
  fire-time within `[T - W, T]`.
- `data` — encrypted Room DB (SQLCipher + Android Keystore-wrapped passphrase),
  DataStore-backed prefs.

## Privacy invariant

Audio enters memory in exactly one place (`AudioCapture.samples()`) and exits
to derived numbers in exactly one place (`FeatureExtractor.extract()`).
Anywhere else is a bug. No raw PCM is ever written to disk, sent over the
network, or logged.

## Demo mode

Debug builds expose a Settings toggle that swaps `AudioCapture` for a
deterministic synthetic stream that compresses an 8-hour night into ~8 minutes
(60× speed). Lets reviewers see the full hypnogram + smart-wake fire +
morning report without sleeping.

## Testing

Heavy unit coverage on the algorithm layers:

- DSP — Hann window, FFT power-spectrum, MFCC golden tone, spectral
  centroid monotonicity, breathing-rate autocorrelation.
- Classifier — awake-on-burst, deep-sleep on slow regular breath, REM late at
  night, HMM smoother removes spikes.
- Smart wake — fires within window, no fire before window, urgency ramp,
  cooldown, fallback to T.
- Score — empty / healthy / mostly-awake.

UI is smoke-tested only.

## What's out of scope for v0.1

Trained S4M (TFLite), federated fine-tuning, Wear OS companion, trends/sleep
debt, sleep journal, curated wind-down sounds, multi-user / cloud sync,
encrypted export. Architectural seams exist; implementations don't.
