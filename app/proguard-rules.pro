# ProGuard rules for the Biometric Voting App.

# Keep Biometric classes (as per initial guide, may need more specific rules based on libraries)
-keep class androidx.biometric.** { *; }
-keep interface androidx.biometric.** { *; }

# Keep security crypto classes if not covered by library's own rules
-keep class androidx.security.crypto.** { *; }
-keep interface androidx.security.crypto.** { *; }

# Keep data transfer objects (DTOs) from the data.model package.
# Note: Current DTOs (e.g., ElectionDto, RegistrationRequest) are in data.network.dto.
# This rule implies DTOs should be in/moved to data.model or this rule needs adjustment.
-keep class com.example.biometricvotingapp.data.model.** { *; }

# Keep nested request model classes within VoteApiRequest (e.g., RegistrationRequest, VoteRequest).
# Note: Current request objects (RegistrationRequest, VoteRequest) are top-level data classes
# in the data.network.dto package, not nested within a VoteApiRequest object.
# This rule implies a structural change to how request objects are defined or this rule needs adjustment.
-keep class com.example.biometricvotingapp.data.network.VoteApiRequest$* { *; }

# Keep domain models as well if they might be serialized or reflected upon (safer).
-keep class com.example.biometricvotingapp.domain.model.** { *; }

# Keep attributes often needed for reflection or annotation processing by libraries like Gson.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses

# Keep Kotlin Coroutines internal classes to prevent issues with obfuscation.
-keepnames class kotlinx.coroutines.internal.** { *; }
# -dontwarn kotlinx.coroutines.** # Often added if warnings appear, but start without.

# If you use @SerializedName annotation with Gson
-keep class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# If using Timber and want to remove all logging calls from release builds (beyond what R8 does):
# -assumenosideeffects class android.util.Log {
#     public static *** v(...);
#     public static *** d(...);
#     public static *** i(...);
#     public static *** w(...);
#     public static *** e(...);
#     public static *** wtf(...);
# }
# -assumenosideeffects class timber.log.Timber {
#     public static *** v(...);
#     public static *** d(...);
#     public static *** i(...);
#     public static *** w(...);
#     public static *** e(...);
#     public static *** wtf(...);
#     public static *** log(...);
# }
# Note: The above Timber rules are aggressive. Usually, Timber.plant(ReleaseTree()) is preferred.
# R8/ProGuard will already remove most `if (BuildConfig.DEBUG)` blocks and their contents.

# Rules for Hilt - generally Hilt's Gradle plugin handles this, but keep common ones if issues arise.
# -keepclassmembers class * extends androidx.lifecycle.ViewModel {
#    @com.google.dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
# }
# -keep class * implements dagger.hilt.EntryPoint { *; }
# -keep class * implements dagger.hilt.InstallIn { *; }
# -keep @dagger.hilt.InstallIn class * { *; }
# -keep @dagger.hilt.DefineComponent class * { *; }
# -keep @dagger.hilt.android.HiltAndroidApp class * { *; }
# -keep class dagger.hilt.internal.** { *; }
# -keep class * extends dagger.hilt.android.internal.** { *; }

# Rules for Firebase services (Crashlytics, Analytics often handled by their plugins/BoM)
# -keepattributes *Annotation*,EnclosingMethod,Signature
# -keepnames class com.google.android.gms.measurement.AppMeasurement
# -keepnames class com.google.firebase.analytics.FirebaseAnalytics
# -keep class com.google.firebase.crashlytics.** { *; }

# Rules for Retrofit, OkHttp, Gson (often handled by library defaults or specific rules if custom serialization is used)
# -keepattributes Signature
# -keepattributes InnerClasses
# -keep class retrofit2.** { *; }
# -keep class okhttp3.** { *; }
# -keep class com.google.gson.** { *; }
