# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entities
-keep class com.billbharo.data.local.entities.** { *; }

# Keep Hilt components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Gson models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.billbharo.data.models.** { *; }
-keep class com.billbharo.data.local.entities.** { *; }

# Keep Speech Recognition classes
-keep class android.speech.** { *; }

# Keep iText PDF classes
-keep class com.itextpdf.** { *; }
