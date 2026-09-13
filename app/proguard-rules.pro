# ProGuard rules for Rotary Goods Assistant
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class org.rotary.goodsassistant.model.** { *; }
