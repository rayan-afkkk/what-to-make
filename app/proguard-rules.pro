# ---------------------------------------------------------------------------
# What to Cook - R8 / ProGuard rules
# Most libraries (Retrofit, OkHttp, Room, Hilt, Billing, Firebase,
# kotlinx.serialization) ship their own consumer rules. The rules below cover
# the app's own reflection-sensitive code and a few belt-and-braces keeps.
# ---------------------------------------------------------------------------

# Keep line numbers for readable crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep generated serializers for the app's @Serializable classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.whattocook.app.** {
    *** Companion;
    static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.whattocook.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.whattocook.app.**$$serializer { *; }

# Network DTOs and stored models are (de)serialized by name.
-keep class com.whattocook.app.data.remote.dto.** { *; }
-keep class com.whattocook.app.domain.model.** { *; }

# --- Retrofit (R8 full mode) ---
-keepattributes Signature, Exceptions, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation interface com.whattocook.app.data.remote.ApiService

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
