package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private AdView adView;
    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;

    private static final String ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    private static final String ADMOB_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";
    private static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        webView = new WebView(this);
        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        webView.setLayoutParams(webParams);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);

        String ua = s.getUserAgentString();
        s.setUserAgentString(ua.replace("; wv", ""));

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        rootLayout.addView(webView);

        adView = new AdView(this);
        adView.setAdUnitId(ADMOB_BANNER_ID);
        adView.setAdSize(AdSize.BANNER);
        rootLayout.addView(adView);

        setContentView(rootLayout);

        webView.loadUrl("https://fitstep-boost.github.io/fitstep/");

        MobileAds.initialize(this, initializationStatus -> {
            loadBannerAd();
            loadRewardedAd();
            loadInterstitialAd();
        });
    }

    private void loadBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, ADMOB_REWARDED_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
            }
        });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, ADMOB_INTERSTITIAL_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAd = null;
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (rewardedAd != null) {
                    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            loadRewardedAd();
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            loadRewardedAd();
                            if (webView != null) webView.evaluateJavascript("javascript:if(window.onAdFailed) window.onAdFailed();", null);
                        }
                    });
                    rewardedAd.show(MainActivity.this, rewardItem -> {
                        if (webView != null) {
                            webView.evaluateJavascript("javascript:if(window.onAdCompleted) window.onAdCompleted();", null);
                        }
                    });
                } else {
                    loadRewardedAd();
                    if (webView != null) {
                        webView.evaluateJavascript("javascript:alert('Ad लोड हो रहा है, कृपया 2 सेकंड बाद दबाएँ');", null);
                        webView.evaluateJavascript("javascript:if(window.onAdFailed) window.onAdFailed();", null);
                    }
                }
            });
        }

        @JavascriptInterface
        public void showInterstitialAd() {
            runOnUiThread(() -> {
                if (interstitialAd != null) {
                    interstitialAd.show(MainActivity.this);
                    loadInterstitialAd();
                } else {
                    loadInterstitialAd();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void恢复() {
        super.onResume();
        if (adView != null) adView.resume();
    }

    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        super.onDestroy();
    }
}
