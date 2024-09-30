# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

#-dontshrink
-dontobfuscate
#-dontoptimize

-keepattributes Signature, RuntimeVisibleAnnotations

# Keep Gson's data model classes
-keep class sun.misc.Unsafe { *; }
-keep class com.yourapp.** { *; }
-keepclassmembers class com.yourapp.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Retrofit-related classes
-keep class retrofit2.** { *; }
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Glide uses annotations
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.ResourceDecoder { *; }

# Keep all Kotlin data classes
-keep class kotlin.**Kt$Data { *; }

# Keep all classes that implement Serializable
-keep class * implements java.io.Serializable { *; }

## Remove all Android log calls (Log.e, Log.d, Log.w, etc.)
#-assumenosideeffects class android.util.Log {
#    public static int d(...);
#    public static int e(...);
#    public static int i(...);
#    public static int v(...);
#    public static int w(...);
#    public static int wtf(...);
#}

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn androidx.test.platform.app.AppComponentFactoryRegistry
-dontwarn androidx.test.platform.concurrent.DirectExecutor
-dontwarn com.sun.jna.FunctionMapper
-dontwarn com.sun.jna.JNIEnv
-dontwarn com.sun.jna.LastErrorException
-dontwarn com.sun.jna.Library
-dontwarn com.sun.jna.Memory
-dontwarn com.sun.jna.Native
-dontwarn com.sun.jna.NativeLibrary
-dontwarn com.sun.jna.Platform
-dontwarn com.sun.jna.Pointer
-dontwarn com.sun.jna.Structure
-dontwarn com.sun.jna.platform.win32.Advapi32
-dontwarn com.sun.jna.platform.win32.Kernel32
-dontwarn com.sun.jna.platform.win32.Win32Exception
-dontwarn com.sun.jna.platform.win32.WinBase$OVERLAPPED
-dontwarn com.sun.jna.platform.win32.WinBase$SECURITY_ATTRIBUTES
-dontwarn com.sun.jna.platform.win32.WinDef$DWORD
-dontwarn com.sun.jna.platform.win32.WinDef$LPVOID
-dontwarn com.sun.jna.platform.win32.WinNT$ACL
-dontwarn com.sun.jna.platform.win32.WinNT$HANDLE
-dontwarn com.sun.jna.platform.win32.WinNT$SECURITY_DESCRIPTOR
-dontwarn com.sun.jna.ptr.IntByReference
-dontwarn com.sun.jna.win32.StdCallLibrary
-dontwarn com.sun.jna.win32.W32APIOptions
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.lang.instrument.ClassDefinition
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn java.lang.instrument.IllegalClassFormatException
-dontwarn java.lang.instrument.Instrumentation
-dontwarn java.lang.instrument.UnmodifiableClassException
-dontwarn org.apiguardian.api.API$Status
-dontwarn org.apiguardian.api.API

-keep class java.lang.instrument.** { *; }
-dontwarn java.lang.instrument.**
-keep class net.bytebuddy.** { *; }
-dontwarn net.bytebuddy.**
-keep class androidx.test.** { *; }
-dontwarn androidx.test.**

-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
-keep class edu.umd.cs.findbugs.annotations.** { *; }
-dontwarn edu.umd.cs.findbugs.annotations.**
-keep class org.apiguardian.** { *; }
-dontwarn org.apiguardian.**

-keep class org.apache.commons.logging.impl.Log4JLogger { *; }

-keep class com.google.android.gms.location.FusedLocationProviderClient { *; }
-keep class com.google.android.gms.location.LocationCallback { *; }
-dontwarn com.google.android.gms.**

-keepattributes Exceptions
-keep class ** implements java.lang.reflect.ParameterizedType { *; }

-keep class org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponse { *; }
-keep class org.smartregister.fhircore.engine.data.remote.model.response.OAuthResponseImpl { *; }
