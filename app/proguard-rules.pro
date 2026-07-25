# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.* <methods>;
    @javax.inject.Inject <init>(...);
}
-dontwarn dagger.hilt.**

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# Gson
-keepattributes Signature
-keep class com.meitu.generator.data.remote.dto.** { *; }
-keep class com.meitu.generator.data.model.** { *; }
-keep class com.meitu.generator.data.local.entity.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Compose
-dontwarn androidx.compose.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# App specific - Agent tools (need to be discoverable by Hilt)
-keep class com.meitu.generator.data.tools.** { *; }
-keep class com.meitu.generator.data.agent.** { *; }
-keep class com.meitu.generator.di.** { *; }
