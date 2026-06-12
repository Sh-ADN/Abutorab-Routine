# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Moshi
-keep class com.abutorab.routine.data.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-dontwarn com.squareup.moshi.**

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Bouncy Castle and other missing classes
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Ignore all missing classes from these packages
-ignorewarnings

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn com.android.tools.r8.**

# R8 specific
-dontnote **
