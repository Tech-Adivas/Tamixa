-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlin.Metadata <methods>;
}
-keep class com.araro.** { *; }
-dontwarn org.conscrypt.**
