# Edge TTS Engine

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-keep class com.edgetts.engine.edge.** { *; }
-keep class com.edgetts.engine.EdgeTtsService { *; }

-keep class androidx.compose.material3.** { *; }
-keep interface androidx.compose.material3.** { *; }
-dontwarn androidx.compose.material3.**

-dontwarn okhttp3.**
-dontwarn okio.**
