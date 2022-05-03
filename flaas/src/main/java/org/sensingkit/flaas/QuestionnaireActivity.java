package org.sensingkit.flaas;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.sensingkit.flaas.rest.NetworkManager;

public class QuestionnaireActivity extends AppCompatActivity {

    private static final String formUrl = "https://docs.google.com/forms/d/e/1FAIpQLScUTKtOu-fUHmNWPj_UfR13jKqXQUC5WSpf70b4s2ccqu1eFA/viewform?hl=en&usp=pp_url";

    WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire);

        // connect WebView
        mWebView = (WebView) findViewById(R.id.webview);

        // Enable JavaScript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Load form prefilled with the username
        String username = NetworkManager.getInstance(this).getSession().getUsername();
        mWebView.loadUrl(formUrl + "&entry.514285780=" + username);
    }
}