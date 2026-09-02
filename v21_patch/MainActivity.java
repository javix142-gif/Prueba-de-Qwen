package cl.javix.ilyrion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final String LOCAL_HOST = "ilyrion.local";
    private WebView webView;

    @SuppressLint({"SetJavaScriptEnabled", "GestureBackNavigation"})
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            showGame();
        } catch (Throwable error) {
            showStartupError(error);
        }
    }

    @SuppressWarnings("deprecation")
    private void showGame() throws IOException {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        WebView view = new WebView(this);
        webView = view;
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        view.setWebChromeClient(new WebChromeClient());
        view.setWebViewClient(new LocalAssetClient());
        view.setBackgroundColor(Color.rgb(7, 9, 13));
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(view);

        String html = readAsset("index.html");
        view.loadDataWithBaseURL("https://" + LOCAL_HOST + "/", html, "text/html", "UTF-8", null);
    }

    private final class LocalAssetClient extends WebViewClient {
        @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !LOCAL_HOST.equalsIgnoreCase(uri.getHost())) {
                return null;
            }
            String path = uri.getPath();
            if (path == null || path.length() <= 1) return null;
            String assetPath = path.substring(1);
            if (assetPath.contains("..") || assetPath.startsWith("/")) return null;
            try {
                InputStream input = getAssets().open(assetPath);
                return new WebResourceResponse(mimeType(assetPath), null, input);
            } catch (IOException ignored) {
                return null;
            }
        }

        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            return !("https".equalsIgnoreCase(uri.getScheme()) && LOCAL_HOST.equalsIgnoreCase(uri.getHost()));
        }
    }

    private static String mimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }

    private String readAsset(String name) throws IOException {
        try (InputStream input = getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void showStartupError(Throwable error) {
        webView = null;
        TextView text = new TextView(this);
        text.setTextColor(Color.WHITE);
        text.setBackgroundColor(Color.rgb(7, 9, 13));
        text.setPadding(32, 32, 32, 32);
        text.setTextSize(16f);
        String detail = error.getClass().getSimpleName();
        if (error.getMessage() != null) detail += ": " + error.getMessage();
        text.setText(getString(R.string.startup_error, detail));
        setContentView(text);
    }

    private void handleBack() {
        if (webView == null) {
            finish();
            return;
        }
        webView.evaluateJavascript("window.gameBack ? window.gameBack() : false", value -> {
            if ("false".equals(value) && !isFinishing()) finish();
        });
    }

    @Override public void onBackPressed() {
        handleBack();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
