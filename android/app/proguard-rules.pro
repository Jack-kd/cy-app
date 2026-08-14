# 项目混淆规则（R8）
# 保留数据模型类，供 kotlinx.serialization 反射使用
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.cy.app.**$$serializer { *; }
-keepclassmembers class com.cy.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.cy.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
