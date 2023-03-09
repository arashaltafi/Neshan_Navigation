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

-dontskipnonpubliclibraryclasses
-dontobfuscate
-forceprocessing
-optimizationpasses 5

#-keep class * extends android.app.Activity
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

#-dontwarn android.arch.util.paging.CountedDataSource
#-dontwarn android.arch.persistence.room.paging.LimitOffsetDataSource
#
#-keep class * extends androidx.room.RoomDatabase
#-keep @androidx.room.Entity class *
#-dontwarn androidx.room.paging.**

-keepclassmembers,allowobfuscation class * {
@com.google.gson.annotations.SerializedName <fields>;
}

-keep class org.neshan.data.model.response** { *; }

-keep class org.neshan.** {*;}

-keep class com.wang.avi.** { *; }
-keep class com.wang.avi.indicators.** { *; }