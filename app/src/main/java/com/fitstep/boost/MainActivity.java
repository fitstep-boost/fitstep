package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FitStep_MainActivity";
    private static final String LIVE_REWARDED_AD_UNIT_ID = "ca-app-pub-4526276681965606/8548376683";
    private static final String HOSTED_WEB_URL = "https://fitstep-boost.github.io/fitstep/";

    private WebView mainWebView;
    private RewardedAd mRewardedAd;
    private boolean isAdLoading = false;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "AdMob Initialized");
        });
        loadRewardedAd();

        mainWebView = findViewById(R.id.webview);
        WebSettings webSettings = mainWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        mainWebView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        mainWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView popupWebView = new WebView(MainActivity.this);
                popupWebView.getSettings().setJavaScriptEnabled(true);
                popupWebView.getSettings().setDomStorageEnabled(true);
                popupWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        mainWebView.removeView(window);
                    }
                });
                popupWebView.setWebViewClient(new WebViewClient());
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        mainWebView.setWebViewClient(new WebViewClient());
        mainWebView.loadUrl(HOSTED_WEB_URL);
    }

    private void loadRewardedAd() {
        if (mRewardedAd != null || isAdLoading) return;
        isAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, LIVE_REWARDED_AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Ad silent background retry: " + loadAdError.getMessage());
                mRewardedAd = null;
                isAdLoading = false;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
                isAdLoading = false;
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (mRewardedAd != null) {
                    mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mRewardedAd = null;
                            loadRewardedAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                            mRewardedAd = null;
                            loadRewardedAd();
                        }
                    });
                    mRewardedAd.show(MainActivity.this, rewardItem -> {
                        mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
                    });
                } else {
                    Toast.makeText(MainActivity.this, "अगला ऐड लोड हो रहा है, 2 सेकंड बाद दबाएँ...", Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
                }
            });
        }
    }
}
