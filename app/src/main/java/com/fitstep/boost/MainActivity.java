package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {
    private static final String LIVE_REWARDED_AD_UNIT_ID = "ca-app-pub-4526276681965606/8548376683";
    private static final String HOSTED_WEB_URL = "https://fitstep-boost.github.io/fitstep/";

    private FrameLayout rootContainer;
    private WebView mainWebView;
    private RewardedAd mRewardedAd;
    private boolean isAdLoading = false;
    private boolean showAdWhenLoaded = false;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mainWebView = new WebView(this);
        mainWebView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootContainer.addView(mainWebView);
        setContentView(rootContainer);

        MobileAds.initialize(this, initializationStatus -> {});
        loadRewardedAd();

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
                popupWebView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                WebSettings popupSettings = popupWebView.getSettings();
                popupSettings.setJavaScriptEnabled(true);
                popupSettings.setDomStorageEnabled(true);
                popupSettings.setSupportMultipleWindows(true);
                popupSettings.setJavaScriptCanOpenWindowsAutomatically(true);

                popupWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        rootContainer.removeView(window);
                    }
                });

                popupWebView.setWebViewClient(new WebViewClient());
                rootContainer.addView(popupWebView);

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
                mRewardedAd = null;
                isAdLoading = false;
                if (showAdWhenLoaded) {
                    showAdWhenLoaded = false;
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:addLog('⚠️ AdMob एरर: " + loadAdError.getMessage() + " (Code: " + loadAdError.getCode() + ")');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    });
                }
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
                isAdLoading = false;
                if (showAdWhenLoaded) {
                    showAdWhenLoaded = false;
                    runOnUiThread(() -> showAdNow());
                }
            }
        });
    }

    private void showAdNow() {
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
                    mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                }
            });
            mRewardedAd.show(MainActivity.this, rewardItem -> {
                mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
            });
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (mRewardedAd != null) {
                    showAdNow();
                } else {
                    showAdWhenLoaded = true;
                    loadRewardedAd();
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ लाइव ऐड लोड हो रहा है, कृपया 2-3 सेकंड रुकें...');", null);
                }
            });
        }
    }
}
