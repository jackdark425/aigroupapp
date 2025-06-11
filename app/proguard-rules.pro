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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.osgi.framework.BundleActivator

# for proto-datastore
# see https://issuetracker.google.com/issues/168580258 and https://android-review.googlesource.com/c/platform/frameworks/support/+/1433465

# TODO: 参考 issue tracker 里的写法，即所有继承 GeneratedMessageLite 的类; 不要-keep而是只keep必要的 member
# why keepclassmembers not work
# other workaround:

#-keep class com.aigroup.aigroupmobile.data.models.AppPreferences { *; }
#-keep class com.aigroup.aigroupmobile.data.models.AppPreferences$* { *; }

-keep class * implements com.google.protobuf.MessageLiteOrBuilder {
   <fields>;
   # we using reflect to access getter or setter
   <methods>;
}

# realm
-keepnames public class * extends io.realm.kotlin.types.RealmObject {
   <fields>;
   <methods>;
}
-keepnames class io.realm.kotlin.types.RealmInstant

# Apache Tika and related libraries - ignore missing desktop Java classes
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.xml.bind.**
-dontwarn com.sun.msv.**
-dontwarn org.springframework.**
-dontwarn ucar.nc2.**
-dontwarn com.gemalto.jp2.**

# Keep Apache Tika classes that are actually used
-keep class org.apache.tika.** { *; }
-keep class org.apache.poi.** { *; }

# Keep PDFBox classes
-keep class com.tom_roush.pdfbox.** { *; }

# Langchain4j related
-keep class dev.langchain4j.** { *; }

# Additional exclusions for problematic dependencies
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.**
-dontwarn commons-logging.**
