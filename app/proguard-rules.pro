# Keep crypto and model classes
-keep class com.mssp.qrverifier.crypto.** { *; }
-keep class com.mssp.qrverifier.model.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-keep class androidx.camera.** { *; }
