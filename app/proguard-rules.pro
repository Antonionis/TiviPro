# R8 / ProGuard Optimization Rules for TiviPro

# Keep Application, Activities & Android Components
-keep public class com.tivipro.TiviProApplication { *; }
-keep public class com.tivipro.MainActivity { *; }
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep ViewModels
-keep public class * extends androidx.lifecycle.ViewModel { *; }

# Keep all Data Models and Network DTOs
-keep class com.tivipro.model.** { *; }
-keepclassmembers class com.tivipro.model.** { *; }
-keep class com.tivipro.data.** { *; }
-keepclassmembers class com.tivipro.data.** { *; }

# Moshi & Retrofit
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn com.squareup.moshi.**

# OkHttp & Retrofit
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin Coroutines
-keepclassmembers class * {
    *** Companion;
}
