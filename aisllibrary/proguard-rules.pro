# 代码混淆压缩比，在0~7之间
-optimizationpasses 5
-dontoptimize
# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames
# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses
# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify
-verbose
# 避免混淆泛型
-keepattributes Signature

-dontskipnonpubliclibraryclassmembers
-dontwarn dalvik.**
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses
#google推荐算法
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
# 避免混淆Annotation、内部类、泛型、匿名类
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
# 重命名抛出异常时的文件名称
-renamesourcefileattribute SourceFile
# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable
# 处理support包
-dontnote android.support.**
-dontwarn android.support.**
# 保留继承的
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.support.annotation.**

# 保留R下面的资源
-keep class **.R$* {*;}
# 保留四大组件，自定义的Application等这些类不被混淆
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Appliction
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

# 保留在Activity中的方法参数是view的方法，
# 这样以来我们在layout中写的onClick就不会被影响
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}
# 对于带有回调函数的onXXEvent、**On*Listener的，不能被混淆
-keepclassmembers class * {
    void *(**On*Event);
    void *(**On*Listener);
}
# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
   static final long serialVersionUID;
   private static final java.io.ObjectStreamField[]   serialPersistentFields;
   private void writeObject(java.io.ObjectOutputStream);
   private void readObject(java.io.ObjectInputStream);
   java.lang.Object writeReplace();
   java.lang.Object readResolve();
}
#assume no side effects:删除android.util.Log输出的日志
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
#保留Keep注解的类名和方法
-keep,allowobfuscation @interface android.support.annotation.Keep
-keep @android.support.annotation.Keep class *
-keepclassmembers class * {
    @android.support.annotation.Keep *;
}

-keep class org.json.JSONObject

-keep class com.mv.engine.*{*;}
-keepclasseswithmembernames class com.mv.engine.FaceDetector{
native <methods>;
}
-keepclasseswithmembernames class com.mv.live.EngineWrapper{
public <methods>;
}
-keep class com.mv.live.EngineWrapper{*;}
-keep class com.apex.licensecrypto.*{*;}

#-dontobfuscate


-keepattributes *JavascriptInterface*


 #gosn
 -keep class com.google.gson.stream.** { *; }
 -keep class com.google.gson.examples.android.model.** { *; }
 -keep class com.google.gson.* { *;}

-keep interface com.apex.aisllibrary.interfaces.*{
*;
}


#------------------  下方是共性的排除项目         ----------------
# 方法名中含有“JNI”字符的，认定是Java Native Interface方法，自动排除
# 方法名中含有“JRI”字符的，认定是Java Reflection Interface方法，自动排除

-keepclasseswithmembers class * {
    ... *JNI*(...);
}

-keepclasseswithmembernames class * {
	... *JRI*(...);
}

-keep class **JNI* {*;}

#okhttp
-dontwarn okhttp3.**
-keep class okhttp3.**{*;}

#okio
-dontwarn okio.**
-keep class okio.**{*;}


-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

#net.java.dev.jna
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }

-keep class com.jiangdg.mediacodec4mp4.FaceDetectorListener{
*;
}
-keep class com.jiangdg.mediacodec4mp4.RecordMp4{
*;
}
-keep class org.easydarwin.sw.**{
*;
}
-keep class com.jiangdg.mediacodec4mp4.RecordMp4.**
-dontnote com.jiangdg.mediacodec4mp4.RecordMp4.**
-dontwarn com.jiangdg.mediacodec4mp4.RecordMp4.**
#-keepclassmembers class com.jiangdg.mediacodec4mp4.* { public *; }
-keep interface com.jiangdg.mediacodec4mp4.interfaces.*{
*;
}
-keep class com.jiangdg.mediacodec4mp4.bean.*{
*;
}
-keep enum com.jiangdg.mediacodec4mp4.bean.*{
*;
}
-keep class com.jiangdg.mediacodec4mp4.mtcnn.*{
*;
}
-keep class com.jiangdg.mediacodec4mp4.tflite.*{
*;
}
-keep class com.jiangdg.mediacodec4mp4.utils.CameraManager{
*;
}
-keep class com.jiangdg.mediacodec4mp4.utils.Size{
*;
}

-keep class com.jiangdg.mediacodec4mp4.model.*{
*;
}

-keep public enum com.jiangdg.mediacodec4mp4.model.H264EncodeConsumer$** {
  **[] $VALUES;
  public *;
}

-keep public enum com.jiangdg.mediacodec4mp4.model.AACEncodeConsumer$**{
**[] $VALUES;
public *;
}

-keep public class com.tencent.cloud.qcloudasrsdk.*
