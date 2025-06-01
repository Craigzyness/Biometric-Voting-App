# ProGuard rules for the Biometric Voting App.

# Keep Biometric classes (as per initial guide, may need more specific rules based on libraries)
-keep class androidx.biometric.** { *; }
-keep interface androidx.biometric.** { *; }

# Keep security crypto classes if not covered by library's own rules
-keep class androidx.security.crypto.** { *; }
-keep interface androidx.security.crypto.** { *; }

# Keep data transfer objects (DTOs) used with Gson/Retrofit if they are being obfuscated.
# This ensures that field names expected by the backend are preserved.
-keep class com.example.biometricvotingapp.data.network.dto.** { *; }

# Keep attributes often needed for reflection or annotation processing by libraries like Gson.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses

# Add any other rules specific to your libraries here.
# For example, if using Kotlin Coroutines extensively with reflection or complex serialization:
# -keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
# -keepnames class kotlinx.coroutines.flow.** { *; }

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
