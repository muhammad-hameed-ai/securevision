# SecureVision v3

An Android security monitoring app with **on-device AI**. Live camera detection of
people, faces, weapons and motion, with recognition of enrolled individuals — all
inference runs on the phone.

> **Status: Phase 1 of 7 complete.** The module structure, domain model, design
> system and navigation shell are in place. Feature screens and the ML pipelines
> are built in later phases; every destination currently renders a placeholder.

---

## The two profile systems

These are separate on purpose and must not be conflated.

| | **App login account** | **Enrolled person profile** |
|---|---|---|
| Fields | username, full name, password, CNIC | photo, name, age, face embedding |
| Storage | Firebase Auth + Firestore | Room + internal storage, on-device only |
| Leaves the device | Yes — so it survives a reinstall | **Never** |
| Model | `UserAccount` | `EnrolledProfile` |

Cloud backup and device-to-device transfer are disabled outright
(`allowBackup="false"` plus `data_extraction_rules.xml`) because enrolled profiles
hold biometric data.

---

## Architecture

Clean Architecture with boundaries the build enforces, not just convention.

```
:app                     ← Hilt graph closes here; the only module that sees both
  │                        domain contracts and their implementations
  ├─ :feature:*          ← presentation (8 modules), depends on domain only
  │     ↓
  ├─ :core:core-domain   ← repository interfaces + use cases
  │  :core:core-model    ← pure Kotlin/JVM, zero Android on the classpath
  │  :core:core-common   ← Result, DispatcherProvider, extensions
  │  :core:core-ui       ← Material 3 design system
  │     ↑
  ├─ :core:core-data     ← Room, DataStore, Firebase (implements domain)
  └─ :ml:*               ← on-device inference engines (5 modules)
```

**The rules and why they hold:**

- `core-model` uses the `securevision.jvm.library` plugin — no Android Gradle
  Plugin at all — so an `android.*` import in a domain model is a compile error.
- Feature modules declare no dependency on `core-data` or any `ml` module, so a
  Composable physically cannot reach a DAO.
- `core-domain` declares interfaces; `core-data` supplies the bindings. Only
  `:app` sees both.
- Every screen has a ViewModel exposing one sealed `UiState` via `StateFlow`.
  Composables render state and emit events — no business logic.

---

## Build

**Toolchain** — Gradle 8.13 · AGP 8.6.1 · Kotlin 2.0.21 · JDK 17 ·
compileSdk 34 · minSdk 26 · targetSdk 34.

```bash
./gradlew build                 # all modules, lint + unit tests
./gradlew :app:assembleDebug    # produce the APK
./gradlew :app:installDebug     # install on a connected device or emulator
./gradlew projects              # print the module tree
```

On Windows, use `.\gradlew.bat`.

### Artifact sizes (Phase 1)

| Artifact | Size | Installable |
|---|---|---|
| `app-debug.apk` | 109.2 MB | yes — debug-keystore signed |
| `app-release-unsigned.apk` | 85.6 MB | no — needs a signing config |

R8 only accounts for the 24 MB difference because the bulk is **native `.so`
libraries** — TFLite plus its GPU delegate, and ML Kit — shipped for four ABIs
(`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`). Bytecode shrinking cannot touch
them. The lever for real size reduction is ABI splits or an app bundle, not R8;
a single-ABI `arm64-v8a` build is roughly a third of the size. Worth doing once
the ML modules actually carry models (Phase 4+).

### Conventions that are not optional

- **Every dependency version lives in `gradle/libs.versions.toml`.** A version
  literal in a module's `build.gradle.kts` is a review-blocking defect.
- **Every module build script is a plugin list plus project dependencies.**
  Shared configuration belongs in `build-logic/`.
- **No hardcoded user-facing strings** — `strings.xml`. **No hardcoded colours** —
  theme tokens from `core-ui`. `core-ui/theme/Color.kt` is the only file in the
  project permitted to contain a colour literal.
- **KDoc on public APIs.**

### Convention plugins (`build-logic/`)

| Plugin | Configures |
|---|---|
| `securevision.android.application` | SDK levels, Java 17, build types, R8 |
| `securevision.android.library` | SDK levels, Java 17, lint policy |
| `securevision.android.compose` | Compose compiler, BOM-managed dependencies |
| `securevision.android.hilt` | KSP + Hilt, `hilt-navigation-compose` where Compose is present |
| `securevision.android.firebase` | Firebase BOM; applies google-services **only if** `google-services.json` exists |
| `securevision.android.room` | Room + KSP, schema export |
| `securevision.jvm.library` | Pure Kotlin/JVM, no AGP |

---

## Firebase

Not yet provisioned. `app/google-services.json` is git-ignored and absent, so the
`google-services` plugin is skipped and the build logs a warning. Firebase Auth
and Firestore compile in but stay inert until Phase 3 adds the file — no code
change is needed at that point.

---

## Face recognition accuracy

The recognition pipeline is fixed and every stage is mandatory
(`ml/ml-face/FacePipelineStage.kt`):

```
DETECT → EXTRACT_LANDMARKS → ALIGN → EMBED → MATCH → VOTE
```

A previous version of this app returned roughly **0.23 similarity for every
face** — known and unknown alike — because `ALIGN` was missing and unaligned
crops were fed straight to the embedder. Five-point landmark alignment before
FaceNet-512 is not an optimisation; without it the embeddings are meaningless.

`MATCH` accepts an identity only when the best cosine score clears the threshold
*and* leads the runner-up by a margin. `VOTE` requires consecutive frames to
agree. Both thresholds and the frame count are user-tunable via `AppSettings`.

---

## Roadmap

| Phase | Delivers |
|---|---|
| **1** | ✅ Modules, convention plugins, domain model, design system, navigation shell |
| 2 | Room, DataStore, file storage; dashboard |
| 3 | Firebase auth; profile enrolment |
| 4 | CameraX live view; face detection, alignment, recognition, overlays |
| 5 | Weapon, motion and attribute detection; alarms and notifications |
| 6 | Recording with overlays; alerts, history and recordings galleries |
| 7 | Settings, retention, polish |
