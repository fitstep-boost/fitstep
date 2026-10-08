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

// AdMob Imports
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

// Unity Ads Imports
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;

public class MainActivity extends AppCompatActivity {
    // AdMob IDs
    private static final String LIVE_REWARDED_AD_UNIT_ID = "ca-app-pub-4526276681965606/8548376683";
    private static final String HOSTED_WEB_URL = "https://fitstep-boost.github.io/fitstep/";

    // Unity Ads IDs
    private static final String UNITY_GAME_ID = "800391367";
    private static final String UNITY_PLACEMENT_ID = "BP_Rewarded_Android";
    private static final boolean UNITY_TEST_MODE = true;

    private FrameLayout rootContainer;
    private WebView mainWebView;

    // AdMob State
    private RewardedAd mRewardedAd;
    private boolean isAdLoading = false;
    private boolean showAdWhenLoaded = false;

    // Unity Ads State
    private boolean isUnityInitDone = false;
    private boolean isUnityLoaded = false;
    private boolean isUnityLoading = false;

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

        // 1. Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});
        loadAdMobRewarded();

        // 2. Initialize Unity Ads with strict feedback
        UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, UNITY_TEST_MODE, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                isUnityInitDone = true;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('✅ Unity Ads SDK सफलतापूर्वक शुरू (Init Done)');", null);
                    }
                });
                loadUnityRewarded();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                isUnityInitDone = false;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('❌ Unity Init फ़ेल: " + error.toString() + " - " + message + "');", null);
                    }
                });
            }
        });

        // 3. WebView Settings
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

    // ==========================================
    // ADMOB LOGIC
    // ==========================================
    private void loadAdMobRewarded() {
        if (mRewardedAd != null || isAdLoading) return;
        isAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, LIVE_REWARDED_AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
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

    // ==========================================
    // UNITY ADS LOGIC
    // ==========================================
    private void loadUnityRewarded() {
        if (isUnityLoaded || isUnityLoading) return;
        isUnityLoading = true;
        UnityAds.load(UNITY_PLACEMENT_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                isUnityLoaded = true;
                isUnityLoading = false;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('✅ Unity Ads रेडी हो गया! अब बटन दबाएँ।');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    }
                });
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                isUnityLoaded = false;
                isUnityLoading = false;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('❌ Unity लोड फ़ेल: " + error.toString() + " - " + message + "');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    }
                });
            }
        });
    }

    private void showUnityAdNow() {
        UnityAds.show(MainActivity.this, UNITY_PLACEMENT_ID, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                isUnityLoaded = false;
                loadUnityRewarded();
                runOnUiThread(() -> {
                    mainWebView.evaluateJavascript("javascript:addLog('❌ Unity दिखाने में एरर: " + message + "');", null);
                    mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                });
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {}

            @Override
            public void onUnityAdsShowClick(String placementId) {}

            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                isUnityLoaded = false;
                loadUnityRewarded();
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
                    });
                } else {
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:addLog('⚠️ ऐड बीच में छोड़ दिया गया।');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    });
                }
            }
        });
    }

    // ==========================================
    // JS BRIDGE INTERFACE
    // ==========================================
    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (!isUnityInitDone) {
                    mainWebView.evaluateJavascript("javascript:addLog('⚠️ Unity Init अभी पूरा नहीं हुआ है...');", null);
                }
                
                if (isUnityLoaded) {
                    mainWebView.evaluateJavascript("javascript:addLog('▶️ Unity Ads प्ले हो रहा है...');", null);
                    showUnityAdNow();
                } else {
                    loadUnityRewarded();
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ Unity Ads लोड किया जा रहा है... (अगर एरर होगा तो नीचे दिखेगा)');", null);
                }
            });
        }
    }
}
