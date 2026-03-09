# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Keep HostsManager class
-keep class com.carhosts.app.HostsManager { *; }

# Keep MainActivity
-keep class com.carhosts.app.MainActivity { *; }

# Standard Android optimizations
-dontwarn android.support.**
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
