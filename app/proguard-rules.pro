# SecureVision release keep rules.
#
# AGP applies the consumer rules published by Kotlin, Compose, Hilt/Dagger, Room,
# CameraX and Media3, so those need nothing here. This file holds only what those
# rules cannot infer — reflection that happens across a boundary R8 cannot see.
#
# These rules are verified by building the release APK and exercising it, not by
# reading this file. Phase 1 already produced an R8 failure the rules alone did
# not predict.

# Domain models are Room entities and are reconstructed reflectively by the
# generated DAO code, which R8 cannot always trace back to the constructor.
-keep class com.securevision.core.model.** { *; }

# TFLite loads its native delegates reflectively; without this the GPU delegate
# silently falls back to CPU in a release build.
#
# Note there is deliberately no `-dontwarn` for org.tensorflow.lite.gpu here. R8
# suggests one for GpuDelegateFactory$Options$GpuBackend, but that class is
# genuinely absent unless `tensorflow-lite-gpu-api` is on the classpath — which
# it is. Suppressing the warning instead would have turned a build error into a
# runtime NoClassDefFoundError the first time a GpuDelegate is constructed.
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }

# ML Kit face detection loads its bundled model and its native pipeline through
# reflection over generated internal classes. Stripping them produces a detector
# that returns zero faces at runtime with no error — the worst possible failure
# for this app, since it looks exactly like nobody being in frame.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**

# Keep the line numbers that make a release crash report readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
