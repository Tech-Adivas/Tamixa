-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlin.Metadata <methods>;
}
-keep class com.araro.** { *; }
-dontwarn org.conscrypt.**
# Ktor / slf4j - avoid missing StaticLoggerBinder (optional logger binding)
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.**
# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.araro.domain.** { *; }
-keep,includedescriptorclasses class com.araro.network.** { *; }
