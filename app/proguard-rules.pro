-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

-keep class com.sleepsmart.app.data.db.entity.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**
