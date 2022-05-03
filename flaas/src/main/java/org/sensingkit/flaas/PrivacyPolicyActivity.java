package org.sensingkit.flaas;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private static final String url = "https://minoskt.github.io/download/flaas_privacy_policy.html";

    WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tncactivity);

        // connect WebView
        mWebView = (WebView) findViewById(R.id.webview);

        // Load form prefilled with the username
        mWebView.loadUrl(url);
    }
}