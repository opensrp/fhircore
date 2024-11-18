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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

#-dontshrink
#-dontobfuscate
#-dontoptimize
-printmapping
-verbose

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class org.smartregister.fhircore.quest.** { *; }

# Keep Gson's data model classes
-keep class sun.misc.Unsafe { *; }
-keepclassmembers class org.smartregister.fhircore.quest.** {
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

-keep interface com.google.android.gms.location.FusedLocationProviderClient
-keep class com.google.android.gms.location.LocationCallback.** { *; }
-dontwarn com.google.android.gms.**

-keep class ** implements java.lang.reflect.ParameterizedType { *; }

-keep class com.auth0.jwt.interfaces.** { *; }

-keep class com.fasterxml.jackson.core.type.** { *; }

# Keep Jackson ObjectMapper and related serializers/deserializers
-keep class com.fasterxml.jackson.databind.ObjectMapper { *; }
-keep class com.fasterxml.jackson.databind.ObjectMapper$* { *; }
-keep class com.fasterxml.jackson.databind.** { *; }

# Keep TypeReference (used for generic types)
-keep class com.fasterxml.jackson.core.type.TypeReference { *; }

-keep class com.auth0.jwt.** { *; }

-keep class com.google.gson.** { *; }

-keep class org.apache.logging.log4j.** { *; }

-keep class org.apache.commons.** { *; }
-keep class org.apache.commons.logging.** { *; }
-keep interface org.apache.commons.logging.Log
-keep class org.apache.commons.logging.impl.** { *; }
-keep class org.apache.commons.logging.impl.LogFactoryImpl { *; }
-keep class org.apache.commons.logging.impl.Log4JLogger { *; }
-keep class org.apache.commons.logging.impl.Jdk14Logger { *; }
-keep class org.apache.commons.logging.impl.Jdk13LumberjackLogger { *; }
-keep class org.apache.commons.logging.impl.SimpleLog { *; }
-keep class org.apache.commons.logging.LogFactory.** { *; }
-keep class org.apache.commons.logging.LogConfigurationException
-keep class java.lang.ExceptionInInitializerError

-keepclassmembers class ** {
    static java.lang.ClassLoader getClassLoader();
}

# Keep all classes with references to reflection (necessary for LogFactory)
-keepclassmembers class * {
    public void set*(***);
    public *** get*(***);
}

## Keep Apache Commons BeanUtils classes, in case they’re needed
-keep class org.apache.commons.beanutils.** { *; }

-keep class org.apache.log4j.** { *; }
-keep class org.slf4j.** { *; }

-keep enum * { *; }

-keepclassmembers class * {
    *;
}

# Keep constructors for logging classes
-keepclassmembers class org.apache.commons.logging.** {
    public <init>(...);
}

# Keep all class members that could be accessed via reflection
-keep class * extends java.lang.reflect.** { *; }
-keepclassmembers class * {
    *;
}

#-keep class org.jeasy.rules.jexl.** { *; }
-keep class org.jeasy.rules.jexl.JexlRule { *; }
-keep class org.jeasy.rules.core.** { *; }

-keep class org.apache.commons.jexl3.** { *; }
-dontwarn org.apache.commons.jexl3.**
-keep class org.apache.commons.jexl3.JexlBuilder { *; }
-keep class org.apache.commons.jexl3.internal.** { *; }
-keep class org.apache.commons.jexl3.internal.Engine { *; }
-keep class org.apache.commons.jexl3.introspection.** { *; }
-keep class org.apache.commons.jexl3.introspection.JexlSandbox { *; }
-keep class org.apache.commons.jexl3.JexlEngine { *; }
-keep class org.apache.commons.jexl3.internal.introspection.Uberspect { *; }
-keep interface org.apache.commons.jexl3.introspection.JexlUberspect
-keep class org.apache.commons.jexl3.introspection.JexlUberspect$** { *; }

# Keep constructors for JEXL-related classes
-keepclassmembers class org.apache.commons.jexl3.** {
    public <init>(...);
}

-keepclassmembers class org.apache.commons.jexl3.internal.Engine {
   <init>();
   void getUberspect();
}

-keep class org.apache.commons.lang3.StringUtils { *; }
-keep class org.apache.commons.lang3.RegExUtils { *; }

-keepclasseswithmembers class ** {
    @kotlin.Metadata public final class *;
}

-keep class kotlinx.coroutines.** { *; }

-keep class javax.script.** { *; }
-dontwarn javax.script.**
-keep class java.beans.** { *; }
-dontwarn java.beans.**

-keep class org.apache.commons.jexl3.introspection.JexlSandbox { *; }
-keep interface org.apache.commons.jexl3.introspection.JexlUberspect

-keep class java.util.Map { *; }
-keep class java.nio.charset.Charset { *; }

-keep class kotlin.Metadata

-keep class timber.log.Timber { *; }

-keep class org.apache.log4j.** { *; }
-keep class org.apache.commons.logging.** { *; }
-dontwarn java.beans.**
-dontwarn org.apache.log4j.**

-assumenosideeffects class org.apache.log4j.Logger {
    public static *;
    public *;
}

# Keep Logback classes
-keep class ch.qos.logback.** { *; }

# Keep the logback.xml configuration file
-keep class * {
    public static final java.lang.String LOGBACK_CONFIG_FILE;
}
