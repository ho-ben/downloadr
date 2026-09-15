package com.benho.downloadr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String POE_HOME = "https://poe.com/chats";
    private static final String BRIDGE_NAME = "DownloadrNative";

    private WebView webView;
    private ProgressBar pageProgress;
    private Button exportButton;
    private TextView pageLabel;
    private String exporterScript;
    private String exportedText;
    private String exportedTitle = "poe-chat";
    private final DownloadBridge bridge = new DownloadBridge();

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exporterScript = readAsset("poe-export.js");
        setContentView(buildInterface());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        String browserAgent = settings.getUserAgentString()
            .replace("; wv", "")
            .replace("Version/4.0 ", "");
        settings.setUserAgentString(browserAgent);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                pageProgress.setProgress(progress);
                pageProgress.setVisibility(progress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                configureBridgeFor(url);
                updatePageState(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                configureBridgeFor(url);
                updatePageState(url);
                CookieManager.getInstance().flush();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                    toast("That link could not be opened.");
                }
                return true;
            }

            @Override
            public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse response) {
                response.backToSafety(true);
                toast("Android blocked an unsafe page.");
            }
        });

        WebView.startSafeBrowsing(this, success -> { });
        webView.loadUrl(savedInstanceState == null ? POE_HOME : POE_HOME);
    }

    private View buildInterface() {
        int cream = Color.rgb(246, 242, 233);
        int ink = Color.rgb(29, 43, 36);
        int green = Color.rgb(49, 94, 73);
        int orange = Color.rgb(233, 92, 53);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(cream);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(8), dp(12), dp(8));
        TextView brand = new TextView(this);
        brand.setText("Downloadr");
        brand.setTextColor(ink);
        brand.setTextSize(20);
        brand.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(48), 1));
        pageLabel = new TextView(this);
        pageLabel.setText("Poe");
        pageLabel.setTextColor(green);
        pageLabel.setTextSize(13);
        pageLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        header.addView(pageLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        FrameLayout browserFrame = new FrameLayout(this);
        webView = new WebView(this);
        browserFrame.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pageProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pageProgress.setMax(100);
        pageProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(orange));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
        progressParams.gravity = Gravity.TOP;
        browserFrame.addView(pageProgress, progressParams);
        root.addView(browserFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(8), dp(8), dp(8), dp(8));
        controls.addView(toolbarButton("‹", v -> goBack()), weightedButton());
        controls.addView(toolbarButton("›", v -> goForward()), weightedButton());
        controls.addView(toolbarButton("Chats", v -> webView.loadUrl(POE_HOME)), weightedButton());
        controls.addView(toolbarButton("Reload", v -> webView.reload()), weightedButton());
        exportButton = toolbarButton("Export", v -> exportCurrentChat());
        exportButton.setTextColor(Color.WHITE);
        exportButton.setBackground(roundRect(orange, 14));
        controls.addView(exportButton, new LinearLayout.LayoutParams(0, dp(48), 1.25f));
        root.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));
        return root;
    }

    private LinearLayout.LayoutParams weightedButton() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 0.8f);
        params.setMarginEnd(dp(5));
        return params;
    }

    private Button toolbarButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(Color.rgb(49, 94, 73));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setBackground(roundRect(Color.TRANSPARENT, 14));
        button.setOnClickListener(listener);
        return button;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private void configureBridgeFor(String url) {
        webView.removeJavascriptInterface(BRIDGE_NAME);
        if (isPoeUrl(url)) webView.addJavascriptInterface(bridge, BRIDGE_NAME);
    }

    private void updatePageState(String url) {
        boolean chat = isPoeChat(url);
        exportButton.setEnabled(chat);
        exportButton.setAlpha(chat ? 1f : 0.45f);
        pageLabel.setText(chat ? "Chat ready" : isPoeUrl(url) ? "Sign in · open a chat" : "Secure sign-in");
    }

    private boolean isPoeUrl(String value) {
        try {
            String host = Uri.parse(value).getHost();
            return host != null && (host.equals("poe.com") || host.endsWith(".poe.com"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isPoeChat(String value) {
        if (!isPoeUrl(value)) return false;
        String path = Uri.parse(value).getPath();
        return path != null && path.matches("^/chat/[^/]+.*");
    }

    private void exportCurrentChat() {
        if (!isPoeChat(webView.getUrl())) {
            toast("Open the Poe conversation you want to save first.");
            return;
        }
        exportedText = null;
        exportButton.setEnabled(false);
        exportButton.setText("Reading…");
        webView.evaluateJavascript(exporterScript, null);
    }

    private void showExportDialog(int messageCount) {
        String size = String.format(Locale.getDefault(), "%,d messages · %,d characters", messageCount, exportedText.length());
        new AlertDialog.Builder(this)
            .setTitle("Conversation ready")
            .setMessage(size + "\n\nNothing was uploaded. Save the file or copy it to the clipboard.")
            .setNegativeButton("Copy text", (dialog, which) -> copyExport())
            .setPositiveButton("Download .txt", (dialog, which) -> saveExport())
            .setNeutralButton("Close", null)
            .show();
    }

    private void copyExport() {
        if (exportedText == null) return;
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(exportedTitle, exportedText));
            toast("Conversation copied.");
        } catch (RuntimeException error) {
            toast("This chat is too large for Android's clipboard. Use Download .txt.");
        }
    }

    private void saveExport() {
        if (exportedText == null) return;
        String filename = safeFilename(exportedTitle) + ".txt";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Downloadr");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            toast("Android could not create the download.");
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new IOException("No output stream");
            output.write(exportedText.getBytes(StandardCharsets.UTF_8));
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(uri, values, null, null);
            toast("Saved to Downloads/Downloadr/" + filename);
        } catch (IOException error) {
            getContentResolver().delete(uri, null, null);
            toast("The conversation could not be saved.");
        }
    }

    private String safeFilename(String value) {
        String cleaned = (value == null ? "" : value)
            .replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isEmpty()) cleaned = "poe-chat";
        if (cleaned.length() > 90) cleaned = cleaned.substring(0, 90).trim();
        return cleaned.replaceAll("[. ]+$", "");
    }

    private String readAsset(String name) {
        try (InputStream input = getAssets().open(name)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] bytes = new byte[8192];
            int count;
            while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (IOException error) {
            throw new IllegalStateException("Missing exporter asset", error);
        }
    }

    private void goBack() {
        if (webView.canGoBack()) webView.goBack();
    }

    private void goForward() {
        if (webView.canGoForward()) webView.goForward();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface(BRIDGE_NAME);
            webView.destroy();
        }
        super.onDestroy();
    }

    public final class DownloadBridge {
        private ByteArrayOutputStream buffer;
        private String activeExport;

        @JavascriptInterface
        public synchronized void begin(String exportId, int expectedBytes) {
            activeExport = exportId;
            buffer = new ByteArrayOutputStream(Math.max(32_768, expectedBytes));
        }

        @JavascriptInterface
        public synchronized void chunk(String exportId, String encoded) {
            if (buffer == null || !exportId.equals(activeExport)) return;
            try {
                byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
                buffer.write(bytes);
            } catch (Exception error) {
                fail("Android could not receive this part of the conversation.");
            }
        }

        @JavascriptInterface
        public void status(String message, int progress) {
            runOnUiThread(() -> {
                exportButton.setText(progress > 0 ? progress + "%" : "Reading…");
                pageLabel.setText(message);
            });
        }

        @JavascriptInterface
        public synchronized void finish(String exportId, String title, int messageCount) {
            if (buffer == null || !exportId.equals(activeExport)) return;
            exportedText = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            exportedTitle = title == null || title.trim().isEmpty() ? "poe-chat" : title.trim();
            buffer = null;
            activeExport = null;
            runOnUiThread(() -> {
                exportButton.setText("Export");
                updatePageState(webView.getUrl());
                showExportDialog(messageCount);
            });
        }

        @JavascriptInterface
        public synchronized void fail(String message) {
            buffer = null;
            activeExport = null;
            runOnUiThread(() -> {
                exportButton.setText("Export");
                updatePageState(webView.getUrl());
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Could not export this chat")
                    .setMessage(message == null ? "Poe's page changed. Update Downloadr and try again." : message)
                    .setPositiveButton("OK", null)
                    .show();
            });
        }
    }
}
