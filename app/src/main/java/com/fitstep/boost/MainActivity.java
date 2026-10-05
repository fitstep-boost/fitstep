package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private RewardedAd rewardedAd;
    private boolean isLoadingAd = false;

    // आपकी AdMob Rewarded Ad Unit ID
    private final String REWARDED_AD_UNIT_ID = "ca-app-pub-4526276681965606/1556300765";
    // आपकी लाइव वेबसाइट URL
    private final String WEB_URL = "https://fitstep-boost.github.io/fitstep/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        MobileAds.initialize(this, initializationStatus -> {});

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        // JavaScript Bridge जोड़ना
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        loadRewardedAd();

        webView.loadUrl(WEB_URL);
    }

    private void loadRewardedAd() {
        if (rewardedAd != null || isLoadingAd) return;

        isLoadingAd = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, REWARDED_AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                isLoadingAd = false;
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                isLoadingAd = false;
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (rewardedAd != null) {
                    rewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                            webView.evaluateJavascript("if(window.adRewardCompleted) { window.adRewardCompleted(); }", null);
                            rewardedAd = null;
                            loadRewardedAd();
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "ऐड लोड हो रहा है, कृपया 3 सेकंड रुकें...", Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
