# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bcd.technotes.**$$serializer { *; }
-keepclassmembers class com.bcd.technotes.** {
    *** Companion;
}
-keepclasseswithmembers class com.bcd.technotes.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bcd.technotes.core.**$$serializer { *; }
-keepclassmembers class com.bcd.technotes.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.bcd.technotes.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
