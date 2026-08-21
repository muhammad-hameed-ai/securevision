# Contributing to SecureVision

Thanks for your interest. This document covers how to get a working build, what
the code review will look for, and the few rules that are not negotiable.

## Getting a working build

### Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 |
| Gradle | 8.13 — use the wrapper, don't install it |
| Android SDK | compileSdk 34, minSdk 26 |
| Device | Physical device. The camera pipeline cannot be exercised on an emulator. |

### Setup

```bash
git clone https://github.com/muhammad-hameed-ai/securevision.git
cd securevision
./gradlew test
```

The tests pass without the model files, so this works on a fresh clone.

### The model files

Neither `.tflite` is committed — together they are ~57 MB. Regenerate both:

```bash
pip install deepface tensorflow ultralytics
python scripts/export_facenet.py
python scripts/export_weapon_yolo.py --data path/to/data.yaml
```

See [Regenerating the models](README.md#regenerating-the-models) for the full
recipe and the dataset link.

**An absent model is a supported state, not a bug.** Faces are still detected,
aligned and boxed; recognition reports itself unavailable. If you change model
loading, that behaviour must survive — a missing asset must never crash the app
or silently produce meaningless embeddings.

## Workflow

```bash
git checkout -b feature/your-feature-name
git commit -m "feat: add multi-photo enrolment"
git push origin feature/your-feature-name
# then open a Pull Request against main
```

CI runs tests, lint and a debug assemble on every push and pull request. A red
build will not be merged.

Before you open the PR:

```bash
./gradlew test lint
```

## House rules

These are enforced in review.

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- New behaviour ships with unit tests. `./gradlew test lint` must stay green.
- No hardcoded user-facing strings — use `strings.xml`.
- No hardcoded colours — use the `core-ui` theme tokens.
- KDoc on public APIs.
- No `TODO` or `FIXME` in `src/main`. If it is worth marking, it is worth an issue.
- **Never commit** `.tflite`, `.pt`, keystores, `local.properties`, or anything
  under `runs/`. `.gitignore` covers all of these; do not use `git add -f` to
  get around it.

### Architecture boundaries

Dependencies point inward only:

```
presentation  →  domain  ←  data
```

Concretely:

- `feature-*` modules must not depend on `core-data`. They talk to `core-domain`
  contracts, never to repository implementations.
- `core-domain` must not depend on Android framework classes or on any `ml-*`
  module — it defines the engine interfaces, and the ML modules implement them.
- `:app` is the only module that sees both the domain contracts and their
  implementations. That is where the Hilt graph closes, and it is the reason it
  is the only place a wiring change belongs.

A change that needs one of these lines crossed is a design discussion, not a
patch — open an issue first.

### Composables and state

- One sealed `UiState` per screen, exposed as a `StateFlow` from the ViewModel.
- No business logic in Composables. If a Composable is making a decision, that
  decision belongs in the ViewModel or a use case.
- Never block the main thread. Camera analysis and inference run off it.

## Testing

```bash
./gradlew test                    # all unit tests
./gradlew :ml:ml-face:test        # a single module
./gradlew lint                    # Android Lint, all modules
```

397 unit tests currently pass with zero failures. Room DAO and migration tests
run on the JVM under Robolectric, so the database layer is covered without a
device.

**Database changes need a migration.** The schema is at version 5, schemas are
exported and committed, and there is no destructive fallback — a change without
a tested migration will wipe user data on upgrade. Add the migration and a test
that exercises it, following the existing ones in `Migrations.kt`.

### What tests should assert

Prefer tests that read constants rather than hardcoding their values. Several
tests were rewritten after threshold changes made copies of `0.10f` and `31f`
go stale while still passing. A test that hardcodes a threshold stops testing
the code the day someone tunes it.

## Reporting bugs

For anything involving recognition or detection, include:

- Device model and Android version.
- Whether it reproduces in portrait, landscape, or both — several past bugs
  were orientation-specific.
- Relevant logcat, filtered:

```bash
adb logcat -s FaceRecognitionEngine:V FaceEmbedder:V WeaponDetector:V PipelineTiming:V CameraFlip:V
```

Do **not** attach photographs of people who have not agreed to appear in a
public issue tracker.

## Security

This app handles biometric data. If you find a vulnerability — particularly
anything that could move face embeddings, profile photos, or credentials off
the device — please report it privately to the maintainer rather than opening a
public issue.

The three-permission list (`CAMERA`, `VIBRATE`, `POST_NOTIFICATIONS`) is a
design guarantee, not a default. A change that adds a permission, especially
`INTERNET`, needs an explicit justification and will be scrutinised. Verify
against a built APK rather than the source:

```bash
./gradlew :app:assembleDebug
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

## Documentation

If your change alters an observable number — a threshold, a tensor shape, a
model, a module count — update the README in the same commit. The README was
once badly out of step with the code, describing a different face model and
advertising classifiers that did not exist. Keeping it accurate is part of the
change, not follow-up work.

## Licence

By contributing you agree that your contributions are licensed under the
[MIT License](LICENSE).
