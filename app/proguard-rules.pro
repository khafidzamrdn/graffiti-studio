# Graffiti Studio release shrinker rules.
#
# Minification is currently disabled for release builds, but this file is still
# referenced by app/build.gradle.kts and must exist so release builds can be
# enabled safely later.

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Kotlinx Serialization (.gsp manifest and project payloads) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.graffitistudio.**$$serializer { *; }
-keepclassmembers class com.graffitistudio.** {
    *** Companion;
}
-keepclasseswithmembers class com.graffitistudio.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Domain models used by reflection-based serializers ---
-keep class com.graffitistudio.core.domain.model.** { *; }
