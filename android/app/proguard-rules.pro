# Keep the methods Poe's page calls through the deliberately narrow JS bridge.
-keepclassmembers class com.benho.downloadr.MainActivity$DownloadBridge {
    @android.webkit.JavascriptInterface <methods>;
}
