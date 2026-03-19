-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlin.Metadata <methods>;
}
-keep class com.tamixa.** { *; }
-dontwarn org.conscrypt.**
