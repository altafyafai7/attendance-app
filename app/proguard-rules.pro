# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /data/data/com.termux/files/usr/share/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For Apache POI
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class javax.xml.** { *; }
-keep class org.w3c.dom.** { *; }
-dontwarn org.apache.poi.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
