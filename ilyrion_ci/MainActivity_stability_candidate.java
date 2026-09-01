package cl.javix.ilyrion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
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

        WebView view = new WebView(getApplicationContext());
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
        view.setWebViewClient(new WebViewClient());
        view.setBackgroundColor(Color.rgb(7, 9, 13));
        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(view);

        String html = readAsset("index.html");
        String css = readAsset("styles.css");
        String javascript = readAsset("game.js");
        html = html.replace("<link rel=\"stylesheet\" href=\"styles.css\" />", "<style>" + css + "</style>");
        html = html.replace("<script src=\"game.js\"></script>", "<script>" + javascript + "</script>");
        view.loadDataWithBaseURL("https://ilyrion.local/", html, "text/html", "UTF-8", null);
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
        text.setText("Ilyrion no pudo iniciar.\n\nError: " + detail + "\n\nEnvía una captura de esta pantalla para identificar el fallo exacto.");
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
