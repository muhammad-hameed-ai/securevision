# SecureVision v3

An Android security monitoring app with **on-device AI**. Live camera detection of
people, faces, weapons and motion, with recognition of enrolled individuals — all
inference runs on the phone.

> **Status: Phase 5a of 7 complete.** Live camera face recognition works on
> hardware: an enrolled face draws a green box with a name and score, an
> unenrolled one draws red. Motion detection, the attribute framework and coarse
> emotion are live; weapon detection is built and wired but inert until a
> `weapon_detector.tflite` is dropped into the app assets, which it reports
> honestly rather than failing silently. Alarms and notifications are Phase 5b;
> recording and the remaining galleries are Phases 6–7.

---

## The two profile systems

These are separate on purpose and must not be conflated.

| | **App login account** | **Enrolled person profile** |
|---|---|---|
| Fields | username, full name, password, CNIC | photo, name, age, face embedding |
| Storage | Room, BCrypt-hashed, **on-device** | Room + internal storage, on-device |
| Leaves the device | Never | Never |
| Model | `UserAccount` | `EnrolledProfile` |

**Nothing in this app touches the network.** Firebase was dropped in Phase 3 in
favour of offline authentication, which has one consequence worth stating plainly:
the account does **not** survive a reinstall, and there is no password reset
channel. The mitigation is the one-time recovery code issued at sign-up, stored
only as a second BCrypt hash. Lose both the password and the code and the only
way back in is clearing app data — which also destroys the enrolled profiles.

Cloud backup and device-to-device transfer are disabled outright
(`allowBackup="false"` plus `data_extraction_rules.xml`) because enrolled profiles
hold biometric data.

---

## Architecture

Clean Architecture with boundaries the build enforces, not just convention.

```
:app                     ← Hilt graph closes here; the only module that sees both
  │                        contracts and their implementations
  ├─ :feature:*          ← presentation (8 modules), depends on domain only
  │     ↓
  ├─ :core:core-domain   ← repository + engine contracts, use cases
  │  :core:core-model    ← pure Kotlin/JVM, zero Android on the classpath
  │  :core:core-common   ← Result, DispatcherProvider, extensions
  │  :core:core-ui       ← Material 3 design system
  │     ↑
  ├─ :core:core-data     ← Room, DataStore, file storage, BCrypt
  └─ :ml:*               ← on-device inference engines (5 modules)
```

**The rules, and how they are proved.** Every phase re-runs a compile probe that
writes a deliberately illegal import and asserts it fails:

| Boundary | Probe | Result |
|---|---|---|
| `core-model` sees no Android | `import android.graphics.Bitmap` | Unresolved |
| `feature` cannot reach `core-data` | `import …core.data.database.SecureVisionDatabase` | Unresolved |
| `feature-auth` cannot reach the hasher | `import …core.data.security.PasswordHasher` | Unresolved |
| `feature-live` cannot reach the embedder | `import …ml.face.embed.FaceEmbedder` | Unresolved |
| `feature-live` cannot reach the weapon detector | `import …ml.weapon.WeaponDetector` | Unresolved |

`core-model` uses the `securevision.jvm.library` plugin — no Android Gradle Plugin
at all — so its purity is a compiler guarantee rather than a review convention.
Every screen has a ViewModel exposing one sealed `UiState` via `StateFlow`.

---

## Face recognition

The pipeline is fixed and every stage is mandatory
(`ml/ml-face/FacePipelineStage.kt`):

```
DETECT → ASSESS_QUALITY → ALIGN → EMBED → MATCH → VOTE
```

A previous version of this app returned roughly **0.23 similarity for every
face** — known and unknown alike — because `ALIGN` was missing and unaligned
crops were fed straight to the embedder. There is no code path from the quality
gate to the embedder that bypasses alignment, and `FaceAligner.align()` returns
`null` rather than falling back to an unaligned crop, because a silent unaligned
crop is indistinguishable from a working one until match scores collapse.

**Alignment** solves a 2-D similarity transform — scale, rotation, translation,
deliberately **no shear** — fitting the five detected landmarks onto the ArcFace
reference template scaled to 160×160. Four parameters rather than six: a full
affine would stretch a face into the template instead of rotating it into place.
Solved in closed form rather than by SVD, which is exact and cannot fail to
converge on a degenerate frame.

**Matching** accepts an identity only when the best cosine score clears the
threshold *and* leads the runner-up by a margin. The margin is what prevents
confidently naming the wrong person when two enrolled faces score almost equally.
**Voting** requires 3 of the last 4 frames to agree per tracking id. Threshold,
margin and vote count are all user-tunable via `AppSettings`.

### The model

Not committed to git. Supply a float32 TFLite face-embedding model at:

```
ml/ml-face/src/main/assets/facenet_512.tflite      # 1×160×160×3 in, 1×N out
```

Generate one with `deepface`:

```bash
pip install deepface tensorflow
python -c "
from deepface import DeepFace
import tensorflow as tf
m = DeepFace.build_model('Facenet512')
c = tf.lite.TFLiteConverter.from_keras_model(m.model)
open('facenet_512.tflite','wb').write(c.convert())
"
```

**Check the output dimension before committing to a model.** The `facenet.tflite`
used in most Android demos emits **128** and MobileFaceNet emits **192**, not 512.
The embedder reads its real dimension from the loaded tensor and logs it, and
refuses to match when stored profiles disagree — a model swap otherwise produces
plausible-looking but meaningless scores, which is the *second* way to reproduce
the 0.23 symptom. If you change models, delete and re-enrol every profile.

Without the asset the app degrades honestly: faces are still detected, aligned
and boxed, and the live screen states that recognition is unavailable and why.

---

## Build

**Toolchain** — Gradle 8.13 · AGP 8.6.1 · Kotlin 2.0.21 · JDK 17 ·
compileSdk 34 · minSdk 26 · targetSdk 34.

```bash
./gradlew build                 # all modules, lint + unit tests
./gradlew :app:assembleDebug    # produce the APK
./gradlew :app:installDebug     # install on a connected device
./gradlew projects              # print the module tree
```

On Windows, use `.\gradlew.bat`.

### Artifact size

| Build | Size | Notes |
|---|---|---|
| Phase 1 debug | 109.2 MB | no model |
| Phase 4 debug | 154.6 MB | includes the 45 MB model, **stored uncompressed** |

`androidResources { noCompress += "tflite" }` is set in the **application**
convention plugin, not just the library that owns the asset. `noCompress` takes
effect at packaging time, so a library-only declaration leaves the model
DEFLATED — and TFLite memory-maps its model file, so it fails to load with an
error pointing nowhere near the cause.

The rest of the bulk is native `.so` libraries — TFLite plus its GPU delegate,
and ML Kit — across four ABIs. R8 cannot shrink those; the lever is ABI splits or
an app bundle.

### Conventions that are not optional

- **Every dependency version lives in `gradle/libs.versions.toml`.** A version
  literal in a module's `build.gradle.kts` is a review-blocking defect.
- **Every module build script is a plugin list plus project dependencies.**
  Shared configuration belongs in `build-logic/`.
- **No hardcoded user-facing strings** — `strings.xml`. **No hardcoded colours** —
  theme tokens from `core-ui`. `core-ui/theme/Color.kt` is the only file permitted
  to contain a colour literal.
- **KDoc on public APIs.**

### Convention plugins (`build-logic/`)

| Plugin | Configures |
|---|---|
| `securevision.android.application` | SDK levels, Java 17, build types, R8, `noCompress` |
| `securevision.android.library` | SDK levels, Java 17, lint policy, test options |
| `securevision.android.compose` | Compose compiler, BOM-managed dependencies |
| `securevision.android.hilt` | KSP + Hilt, `hilt-navigation-compose` where Compose is present |
| `securevision.android.firebase` | Firebase BOM; applies google-services **only if** `google-services.json` exists |
| `securevision.android.room` | Room + KSP, schema export |
| `securevision.jvm.library` | Pure Kotlin/JVM, no AGP |

### Database

Room schemas are exported to `core-data/schemas` and **committed** — they are the
input Room needs to verify migrations. `DatabaseModule` deliberately does not set
`fallbackToDestructiveMigration`: on an offline-only app that would wipe the
operator's account and every enrolled profile, none of which exists anywhere else.

| Version | Adds |
|---|---|
| 1 | profiles, alerts, detection events, recordings |
| 2 | the app-login account |

---

## Roadmap

| Phase | Delivers | |
|---|---|---|
| 1 | Modules, convention plugins, domain model, design system, navigation shell | ✅ |
| 2 | Room, DataStore, file storage, live Dashboard | ✅ |
| 3 | Offline BCrypt auth, recovery code, session, My Account | ✅ |
| 4 | CameraX live view; detection, alignment, recognition, overlays, quick enrol | ✅ |
| 5a | Motion, attribute framework, coarse emotion, snapshots, weapon scaffolding | ✅ |
| 5b | Alarm engine and notifications | |
| 6 | Recording with overlays; alerts, history and recordings galleries | |
| 7 | Settings, retention, polish | |

**Phase 4 carries one temporary piece.** "Enrol face" on the live screen exists so
the recognition path is testable at all — without an enrolled profile, matching,
the margin rule and voting would all ship unexercised. The polished enrolment UI
belongs to Phase 6, and must **reuse** `FaceRecognitionEngine.embedForEnrolment`
rather than growing a second embedding path. Two paths would drift, and an
enrolment embedded differently from the queries compared against it is itself a
cause of uniformly low similarity.
