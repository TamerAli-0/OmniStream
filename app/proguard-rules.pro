# OmniStream ProGuard Rules

# Keep source interfaces for reflection-based source loading
-keep class com.omnistream.source.** { *; }
-keep interface com.omnistream.source.model.** { *; }

# Keep domain models
-keep class com.omnistream.domain.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }
-keeppackagenames org.jsoup.nodes

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Coil
-keep class coil.** { *; }

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
