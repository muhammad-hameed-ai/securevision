<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
<img src="https://img.shields.io/badge/ML-TensorFlow%20Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white"/>
<img src="https://img.shields.io/badge/AI-On%20Device-00C9A7?style=for-the-badge&logo=googlecloud&logoColor=white"/>

<br/><br/>

```
███████╗███████╗ ██████╗██╗   ██╗██████╗ ███████╗
██╔════╝██╔════╝██╔════╝██║   ██║██╔══██╗██╔════╝
███████╗█████╗  ██║     ██║   ██║██████╔╝█████╗  
╚════██║██╔══╝  ██║     ██║   ██║██╔══██╗██╔══╝  
███████║███████╗╚██████╗╚██████╔╝██║  ██║███████╗
╚══════╝╚══════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝
         V I S I O N
```

# 🔐 SecureVision

### AI-Based Face & Threat Detection System

**Real-time on-device face recognition, weapon detection, and security alerting — no cloud, no internet, no compromise.**

<br/>

[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.6-4285F4?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![TFLite](https://img.shields.io/badge/TFLite-2.14-FF6F00?style=flat-square&logo=tensorflow)](https://www.tensorflow.org/lite)
[![MLKit](https://img.shields.io/badge/ML%20Kit-16.1-4285F4?style=flat-square&logo=google)](https://developers.google.com/ml-kit)
[![Status](https://img.shields.io/badge/Status-Active-success?style=flat-square)]()
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen?style=flat-square)]()

<br/>

[**📱 Features**](#-features) • [**🏗 Architecture**](#-architecture) • [**🚀 Getting Started**](#-getting-started) • [**📸 Screenshots**](#-screenshots) • [**🧠 AI Models**](#-ai-models) • [**🗂 Module Structure**](#-module-structure) • [**🤝 Contributing**](#-contributing)

</div>

---

## 📖 Overview

**SecureVision** is a production-grade Android security application that brings enterprise-level AI surveillance capabilities to a smartphone. It uses **on-device machine learning** to perform real-time face recognition, weapon detection, and behavioral analysis — entirely offline, with zero cloud dependency.

> Designed for security professionals, researchers, and developers who need a privacy-first, offline-capable AI monitoring system.

```
┌─────────────────────────────────────────────────────────────────┐
│                     HOW IT WORKS                                │
│                                                                 │
│   📷 Camera Frame                                               │
│        │                                                        │
│        ├──► 🧠 ML Kit Face Detection ──► Bounding Boxes        │
│        │         │                                              │
│        │         ├──► ✂️  Face Crop + Quality Gate             │
│        │         │         │                                   │
│        │         │         └──► 🤖 MobileFaceNet TFLite        │
│        │         │                   │                         │
│        │         │              128-dim Embedding              │
│        │         │                   │                         │
│        │         │         ┌─────────▼──────────┐             │
│        │         │         │  Cosine Similarity  │             │
│        │         │         │  + Margin Check     │             │
│        │         │         │  + 3-Frame Buffer   │             │
│        │         │         └────────────────────-┘             │
│        │         │              │           │                  │
│        │    ✅ KNOWN         ❌ UNKNOWN                        │
│        │  (Green Box)       (Red Box + Alarm)                  │
│        │                                                        │
│        └──► 🔫 YOLO Weapon Detection ──► Orange Box + 🚨       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🎥 Live Camera Monitoring
- Real-time 30fps CameraX feed
- Front & back camera toggle
- Animated scan reticle with corner brackets
- Per-face ML Kit tracking IDs
- Live stats bar (Total / Known / Unknown / Weapons)

### 👤 Face Recognition
- **128-dimensional MobileFaceNet** embeddings
- Cosine similarity matching with margin check
- **3-frame stability buffer** (eliminates false positives)
- Configurable threshold (default: 0.75)
- GREEN overlay → Known person
- RED overlay → Unknown person

### 🔫 Weapon Detection
- TFLite YOLO-based object detection
- Detects: gun, knife, pistol, rifle, firearm
- ORANGE bounding box overlay
- Immediate Critical alarm trigger

</td>
<td width="50%">

### 👥 Profile Management
- Enrol unlimited known individuals
- Photo + Name + Age + Gender per profile
- Watchlist flag for heightened alerts
- Face quality gate on enrolment
- 2-column responsive grid with search

### 🚨 Dual-Level Alarm System
- **Medium** (Unknown): beep + light vibration
- **Critical** (Weapon): siren + heavy vibration + screen flash
- 5s / 8s debounce to prevent alarm spam
- AlertBanner slides in from top with dismiss

### 📊 Analytics & History
- Full alert history in Room (SQLite)
- Filter: All / Unknown / Weapon / Today
- Swipe-to-dismiss with undo
- Unread badge count on nav tab
- Background local notifications

</td>
</tr>
</table>

### 🎯 AI Attributes (Live)
| Attribute | Model | Output |
|---|---|---|
| Age | TFLite attribute model | Estimated age (e.g. "28") |
| Gender | TFLite attribute model | Male / Female / Unknown |
| Emotion | TFLite attribute model | Happy / Sad / Angry / Neutral / Surprised |
| Face Mask | Binary TFLite classifier | Masked / No Mask |

---

## 📸 Screenshots

> _Screenshots from physical Android device running SecureVision_

<table>
<tr>
<td align="center" width="25%">
<img src="docs/screenshots/dashboard.jpeg" width="180" alt="Dashboard"/>
<br/><sub><b>🏠 Dashboard</b></sub>
</td>
<td align="center" width="25%">
<img src="docs/screenshots/live_detection.jpeg" width="180" alt="Live Camera"/>
<br/><sub><b>🎥 Live Camera</b></sub>
</td>
<td align="center" width="25%">
<img src="docs/screenshots/profiles.jpeg" width="180" alt="Profiles"/>
<br/><sub><b>👥 Profiles</b></sub>
</td>
<td align="center" width="25%">
<img src="docs/screenshots/alerts.jpeg" width="180" alt="Alerts"/>
<br/><sub><b>🚨 Alerts</b></sub>
</td>
</tr>
<tr>
<td align="center" width="25%">
<img src="docs/screenshots/hero_face_and_weapon.jpeg" width="180" alt="Face and weapon detected together"/>
<br/><sub><b>🔫 Weapon Alert</b></sub>
</td>
<td align="center" width="25%">
<img src="docs/screenshots/recordings.jpeg" width="180" alt="Recordings"/>
<br/><sub><b>🎬 Recordings</b></sub>
</td>
<td align="center" width="25%">
<img src="docs/screenshots/settings.jpeg" width="180" alt="Settings"/>
<br/><sub><b>⚙️ Settings</b></sub>
</td>
<td align="center" width="25%">
</td>
</tr>
</table>

---

## 🏗 Architecture

SecureVision follows **Clean Architecture + MVVM** with a strict multi-module Gradle structure. Each module has a single responsibility.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                           │
│   ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│   │  feature │  │  feature │  │  feature │  │  feature         │  │
│   │  -live   │  │ -profiles│  │  -alerts │  │  -dashboard      │  │
│   └──────────┘  └──────────┘  └──────────┘  └──────────────────┘  │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │                      core-ui                                 │  │
│   │         (Shared Compose Components, Theme, Typography)       │  │
│   └──────────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────────┤
│                         DOMAIN LAYER                                │
│   ┌──────────────────────────────────────────────────────────────┐  │
│   │                     core-domain                              │  │
│   │    (Domain Models, Use Case Interfaces, Business Rules)      │  │
│   └──────────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────────┤
│                          DATA LAYER                                 │
│   ┌──────────────┐   ┌──────────────┐   ┌────────────────────────┐ │
│   │  core-data   │   │   ml-face    │   │       ml-weapon        │ │
│   │  Room DB     │   │  ML Kit +    │   │  TFLite YOLO Detection │ │
│   │  DAOs, Repos │   │  TFLite Emb  │   │                        │ │
│   └──────────────┘   └──────────────┘   └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
CameraX Frame (every 500ms)
        │
        ├─────────────────────────────────────────────┐
        ▼                                             ▼
 ML Kit Face Detection                    YOLO Weapon Detection
        │                                             │
   Face Crop                                  Weapon BBox
   Quality Gate                                    │
        │                                    Orange Overlay
  MobileFaceNet                           Critical Alarm (if hit)
   TFLite Inference
        │
   128-dim Embedding (L2 normalised)
        │
   FaceMatcher (cosine similarity + margin)
        │
   FaceStabilityBuffer (3-frame confirm)
        │
     ┌──┴──┐
     │     │
   KNOWN  UNKNOWN
  (Green) (Red + Medium Alarm)
     │
 LiveCameraViewModel (StateFlow)
     │
 Compose UI (recompose on state change)
     │
 Room DB (persist alert events)
```

---

## 🗂 Module Structure

```
SecureVision/
│
├── app/                          # App entry, DI component, NavGraph
│
├── core/
│   ├── core-domain/              # Domain models + use case interfaces
│   ├── core-data/                # Room DB, DAOs, Repository implementations
│   └── core-ui/                  # Shared Compose components, Theme, Colors
│
├── feature/
│   ├── feature-live/             # Live camera screen + overlays + alarms
│   ├── feature-profiles/         # Profile grid + enrolment form
│   ├── feature-alerts/           # Alert history + filters
│   ├── feature-history/          # Session history + statistics
│   ├── feature-dashboard/        # Home dashboard + quick stats
│   └── feature-settings/         # Threshold, alarms, resolution config
│
└── ml/
    ├── ml-face/                  # ML Kit + TFLite face embedding pipeline
    │   └── src/main/assets/
    │       ├── face_embedding.tflite       ← MobileFaceNet model
    │       └── face_embedding.properties   ← Model config
    │
    └── ml-weapon/                # TFLite YOLO weapon detection pipeline
        └── src/main/assets/
            └── weapon_detection.tflite
```

---

## 🧠 AI Models

### Face Recognition — MobileFaceNet

| Property | Value |
|---|---|
| Model Type | MobileFaceNet (TFLite) |
| Input Size | 112 × 112 × 3 (RGB) |
| Normalisation | `(pixel - 127.5) / 128.0` |
| Output | 128-dimensional float vector |
| Post-processing | L2 normalisation |
| Matching | Cosine similarity |
| Default Threshold | 0.75 |
| Margin Check | 0.08 (best vs second-best) |
| Stability Buffer | 3 consecutive frames |
| File Size | ~1.9 MB |
| Inference Time | ~30ms on mid-range device |

```kotlin
// face_embedding.properties
input_size=112
input_channels=3
embedding_size=128
input_mean=127.5
input_std=128.0
model_name=MobileFaceNet
threshold=0.75
```

### Weapon Detection — YOLO TFLite

| Property | Value |
|---|---|
| Model Type | YOLOv5 / SSD MobileNet (TFLite) |
| Detected Classes | gun, knife, pistol, rifle, firearm |
| Input | 416 × 416 |
| Min Confidence | 0.70 (configurable) |
| Output | Bounding boxes + class labels |
| Alarm Level | 🚨 Critical |

### Why No Cloud?

```
✅ Zero latency        — inference on-device, results in <50ms
✅ Full privacy        — no biometric data ever leaves the phone
✅ Works offline       — no internet connection required
✅ No ongoing cost     — no API keys, no subscriptions
✅ GDPR-friendly       — all data stays local on device
```

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Minimum Version |
|---|---|
| Android Studio | Iguana (2023.2.1+) |
| Android SDK | API 26 (Android 8.0) |
| Kotlin | 2.0+ |
| Gradle | 8.4+ |
| Physical Android Device | Recommended for testing |
| JDK | 17+ |

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/SecureVision.git
cd SecureVision
```

### 2. Add the AI Model Files

> ⚠️ **The TFLite model files are not included in the repo** due to size. You must add them manually.

**Step 1 — Download MobileFaceNet:**
```bash
# Option A: Download from GitHub
# Go to: https://github.com/shubham0204/FaceRecognition_With_FaceNet_Android
# Navigate: app/src/main/assets/ → download facenet.tflite
# Rename to: face_embedding.tflite

# Option B: Generate with Python (recommended)
pip install deepface tensorflow
python scripts/export_facenet.py
```

**Step 2 — Place model files:**
```
ml/ml-face/src/main/assets/
    ├── face_embedding.tflite        ← place here
    └── face_embedding.properties    ← already included
```

**Step 3 — Verify Logcat on first run:**
```bash
adb logcat -s FaceModel
# Should show: "Interpreter created successfully!"
# Input shape: [1, 112, 112, 3]
# Output shape: [1, 128]
```

### 3. Build & Install

```bash
# Connect your Android device via USB
adb devices

# Uninstall any previous build (avoids signature conflict)
adb uninstall com.securevision

# Build and install via Android Studio
# OR via command line:
./gradlew installDebug
```

### 4. First-Time Setup on Device

```
1. Open SecureVision
2. Grant Camera + Notification permissions when prompted
3. Go to Profiles → tap [+] to add a known person
4. Take a clear, frontal photo — face must occupy > 8% of frame
5. Fill in Name, Age, Gender → tap Save
6. Go to Live Camera tab → point camera at the enrolled person
7. GREEN box = recognised ✅   RED box = unknown ❌
```

---

## ⚙️ Configuration

### Threshold Tuning Guide

```kotlin
// In Settings screen → Confidence Threshold slider
// OR edit face_embedding.properties → threshold=0.75

// ┌──────────────┬──────────────────┬─────────────────────────┐
// │  Threshold   │  False Positives │  False Negatives        │
// ├──────────────┼──────────────────┼─────────────────────────┤
// │    0.70      │  Some (3–5%)     │  Very few               │
// │    0.75 ◄    │  Rare (< 1%)     │  Occasional (side view) │  ← Default
// │    0.80      │  None observed   │  More misses (low light) │
// │    0.82      │  None            │  Many misses            │
// └──────────────┴──────────────────┴─────────────────────────┘
```

### Settings Reference

| Setting | Default | Description |
|---|---|---|
| `confidence_threshold` | `0.75` | Minimum cosine similarity for KNOWN match |
| `margin_check` | `0.08` | Gap between best and second-best match |
| `stability_frames` | `3` | Consecutive matches before showing KNOWN |
| `min_face_ratio` | `0.08` | Minimum face width as fraction of frame |
| `face_padding` | `0.20` | Bounding box expansion before crop |
| `unknown_debounce_ms` | `5000` | Cooldown between unknown alarms |
| `weapon_debounce_ms` | `8000` | Cooldown between weapon alarms |
| `data_retention_days` | `30` | Alert history retention period |

---

## 🔧 Tech Stack

```
┌──────────────────────────────────────────────────────────────────┐
│  ANDROID LAYER                                                   │
│  ┌─────────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │ Jetpack Compose │  │    CameraX   │  │ Jetpack Navigation │  │
│  └─────────────────┘  └──────────────┘  └────────────────────┘  │
│  ┌─────────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │   Hilt (DI)     │  │ Room SQLite  │  │ Kotlin Coroutines  │  │
│  └─────────────────┘  └──────────────┘  └────────────────────┘  │
├──────────────────────────────────────────────────────────────────┤
│  ML / AI LAYER                                                   │
│  ┌─────────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │  ML Kit v16.1   │  │ TFLite 2.14  │  │   MobileFaceNet    │  │
│  │  Face Detection │  │  Runtime     │  │   128-dim Embeddings│ │
│  └─────────────────┘  └──────────────┘  └────────────────────┘  │
│  ┌─────────────────┐  ┌──────────────┐                          │
│  │   YOLO TFLite   │  │  TFLite GPU  │                          │
│  │ Weapon Detection│  │  Delegate    │                          │
│  └─────────────────┘  └──────────────┘                          │
├──────────────────────────────────────────────────────────────────┤
│  STORAGE / STATE                                                 │
│  ┌─────────────────┐  ┌──────────────┐  ┌────────────────────┐  │
│  │   Room (SQLite) │  │     MMKV     │  │   Kotlin Flow      │  │
│  │  Profiles+Alerts│  │   Settings   │  │   State Management │  │
│  └─────────────────┘  └──────────────┘  └────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🧪 Testing

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests on device
./gradlew connectedAndroidTest

# Specific module tests
./gradlew :ml-face:test
./gradlew :core-data:test
```

### Test Coverage

| Module | Test Type | Coverage |
|---|---|---|
| `FaceMatcher` | Unit — cosine similarity + margin logic | ✅ |
| `FaceStabilityBuffer` | Unit — frame counting logic | ✅ |
| `EmbeddingConverter` | Unit — FloatArray ↔ ByteArray | ✅ |
| `DatabaseService` | Unit — CRUD operations | ✅ |
| `AlarmService` | Unit — debounce logic | ✅ |
| `LiveCameraScreen` | Compose UI — overlay rendering | ✅ |
| `ProfilesScreen` | Compose UI — grid + search | ✅ |
| `End-to-End (Device)` | Physical device — full pipeline | ✅ |

### Key Test Cases

```
TC-01: Enrolled face, good light, frontal        → KNOWN, correct name    ✅
TC-02: Unknown face                              → UNKNOWN + alarm        ✅
TC-03: Two enrolled persons simultaneously       → Separate correct labels ✅
TC-04: Weapon (knife) in frame                  → Orange box + Critical  ✅
TC-05: Enrol with no face in photo              → Error: "No face found" ✅
TC-06: Enrol with very small / blurry face      → Error: "Face too small" ✅
TC-07: Same face, 3 consecutive frames confirm  → Stable KNOWN label     ✅
TC-08: Dismiss alarm banner                     → Sound stops, DB kept   ✅
TC-09: Background notification on weapon detect → Push notification sent ✅
TC-10: Threshold 0.80 in Settings              → Fewer false positives   ✅
```

---

## 🚧 Known Limitations

| Limitation | Detail | Workaround |
|---|---|---|
| Side-profile recognition | Accuracy drops for > 45° face angle | Enrol multiple photos |
| Low-light accuracy | Embeddings degrade in poor lighting | Ensure adequate lighting |
| Single-photo enrolment | One photo per profile | Multi-photo support planned |
| No anti-spoofing | Photo of enrolled person may fool system | Liveness detection planned |
| Weapon model classes | Limited to predefined weapon classes | Model can be swapped |

---

## 🗺 Roadmap

- [x] Real-time face detection (ML Kit)
- [x] 128-dim face recognition (MobileFaceNet)
- [x] Cosine similarity + margin matching
- [x] 3-frame stability buffer
- [x] Weapon detection (YOLO TFLite)
- [x] Dual-level alarm system
- [x] Profile management (Room + BLOB)
- [x] Alert history screen
- [x] Age / Gender / Emotion attributes
- [x] Face mask detection
- [ ] **Multi-photo enrolment** (enrol 3–5 photos, average embeddings)
- [ ] **ArcFace model** upgrade for higher accuracy
- [ ] **Liveness detection** (anti-spoofing)
- [ ] **IP camera / RTSP stream** support
- [ ] **Crowd density heatmap** analytics
- [ ] **Encrypted cloud backup** (optional, opt-in)
- [ ] **Export alerts** to PDF / CSV

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

```bash
# 1. Fork the repository
# 2. Create a feature branch
git checkout -b feature/your-feature-name

# 3. Make your changes with meaningful commits
git commit -m "feat: add multi-photo enrolment support"

# 4. Push to your fork
git push origin feature/your-feature-name

# 5. Open a Pull Request against main
```

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- All new features must have corresponding unit tests
- All Compose UI must be tested with `@Preview`
- No hardcoded strings — use `strings.xml`
- No hardcoded colours — use theme tokens from `core-ui`

---

## 📄 License

```
MIT License

Copyright (c) 2025 Muhammad Hameed

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

## 🙏 Acknowledgements

| Resource | Use |
|---|---|
| [Google ML Kit](https://developers.google.com/ml-kit) | Face detection API |
| [TensorFlow Lite](https://www.tensorflow.org/lite) | On-device ML runtime |
| [MobileFaceNet Paper](https://arxiv.org/abs/1804.07573) | Face embedding architecture |
| [YOLOv5](https://github.com/ultralytics/yolov5) | Weapon detection backbone |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | UI framework |
| [shubham0204/FaceRecognition_Android](https://github.com/shubham0204/FaceRecognition_With_FaceNet_Android) | FaceNet TFLite reference |

---

<div align="center">

**Built with ❤️ by Muhammad Hameed**

*CECOS University of IT & Emerging Sciences, Peshawar*

*Mobile Application Development — Final Year Project*

<br/>

[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/muhammad-hameed-ai)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/muhammad-hameed-4803ba2b2)
[![Email](https://img.shields.io/badge/Email-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:eng.hameed2943@gmail.com)

<br/>

```
⭐ If this project helped you, please consider giving it a star!
```

</div>
