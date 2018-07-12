# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/shangjie/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#-keepattributes Keep
-keep class com.wireless.wireless.alpha.Keep extends java.lang.annotation.Annotation { *; }
-keep interface * extends java.lang.annotation.Annotation { *; }
-keepattributes *Annotation*
-keepattributes *com.wireless.wireless.alpha.Keep*
#-keepclasseswithmembers class * { @com.wireless.wireless.alpha.Keep *; }
#-keep @com.wireless.wireless.alpha.Keep class * { *; }
#-keepclasseswithmembers class * { @com.wireless.wireless.alpha.Keep <fields>; }
#-keepclasseswithmembers class * { @com.wireless.wireless.alpha.Keep <methods>; }
-keepclasseswithmembers class * { @com.wireless.wireless.alpha.Keep <init>(...); }