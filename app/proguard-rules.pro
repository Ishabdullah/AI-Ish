# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Room database entities
-keep class com.ishabdullah.aiish.data.local.** { *; }

# Keep native methods (JNI)
-keep class com.ishabdullah.aiish.ml.LLMInferenceEngine {
    *;
    native <methods>;
}
-keep class com.ishabdullah.aiish.vision.VisionManager {
    *;
    native <methods>;
}

# Keep the data classes used in the domain layer
-keep class com.ishabdullah.aiish.domain.models.** { *; }

# Keep Timber
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Compose related classes that might be affected by obfuscation
# Generally handled by default R8 rules but good to be explicit for certain edge cases
-keep class * implements androidx.compose.runtime.Applier { *; }
-keep class * implements androidx.compose.ui.tooling.preview.PreviewParameterProvider { *; }
-keep class androidx.compose.ui.tooling.ComposeViewAdapter { *; }