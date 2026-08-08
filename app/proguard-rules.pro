# SecureVision release keep rules.
#
# AGP already supplies rules for Kotlin, Compose, Hilt/Dagger, Room and Firebase
# through their published consumer rules, so this file holds only what those
# cannot infer.

# Domain models are plain data holders today, but they become Room entities and
# Firestore documents in Phase 2/3, at which point both libraries reflect over
# their constructors and field names.
-keep class com.securevision.core.model.** { *; }

# TFLite loads its native delegates reflectively; without this the GPU delegate
# silently falls back to CPU in a release build.
#
# Note there is deliberately no `-dontwarn` for org.tensorflow.lite.gpu here. R8
# suggests one for GpuDelegateFactory$Options$GpuBackend, but that class is
# genuinely absent unless `tensorflow-lite-gpu-api` is on the classpath — which
# it now is. Suppressing the warning instead would have turned a build error into
# a runtime NoClassDefFoundError the first time Phase 4 constructs a GpuDelegate.
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }

# Keep the line numbers that make a release crash report readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
