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

        // 2. Initialize Unity Ads
        UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, UNITY_TEST_MODE, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                loadUnityRewarded();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                // Background log
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
                if (showAdWhenLoaded) {
                    showAdWhenLoaded = false;
                    // AdMob fail -> Try Unity Fallback immediately
                    tryShowUnityOrNotify("AdMob उपलब्ध नहीं (Error: " + loadAdError.getCode() + ")");
                }
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mRewardedAd = rewardedAd;
                isAdLoading = false;
                if (showAdWhenLoaded) {
                    showAdWhenLoaded = false;
                    runOnUiThread(() -> showAdMobNow());
                }
            }
        });
    }

    private void showAdMobNow() {
        if (mRewardedAd != null) {
            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mRewardedAd = null;
                    loadAdMobRewarded();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    mRewardedAd = null;
                    loadAdMobRewarded();
                    // AdMob display failed -> Fallback to Unity
                    tryShowUnityOrNotify("AdMob डिस्प्ले विफल");
                }
            });
            mRewardedAd.show(MainActivity.this, rewardItem -> {
                mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
            });
        }
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
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                isUnityLoaded = false;
                isUnityLoading = false;
            }
        });
    }

    private void tryShowUnityOrNotify(String previousError) {
        runOnUiThread(() -> {
            if (isUnityLoaded) {
                mainWebView.evaluateJavascript("javascript:addLog('⚠️ " + previousError + " ➔ Unity ऐड्स शुरू हो रहा है...');", null);
                showUnityAdNow();
            } else {
                // If Unity is not pre-loaded, try to load again and alert
                loadUnityRewarded();
                mainWebView.evaluateJavascript("javascript:addLog('⚠️ दोनों नेटवर्क व्यस्त हैं। कृपया 3-4 सेकंड बाद प्रयास करें।');", null);
                mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
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
                    mainWebView.evaluateJavascript("javascript:addLog('❌ Unity Ads एरर: " + message + "');", null);
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
                if (mRewardedAd != null) {
                    showAdMobNow();
                } else {
                    // AdMob is not ready right now -> trigger AdMob load, but if Unity is ready, show Unity immediately
                    if (isUnityLoaded) {
                        mainWebView.evaluateJavascript("javascript:addLog('ℹ️ Unity Backup ऐड लोड हुआ...');", null);
                        showUnityAdNow();
                        loadAdMobRewarded(); // Preload AdMob for next time
                    } else {
                        showAdWhenLoaded = true;
                        loadAdMobRewarded();
                        loadUnityRewarded();
                        mainWebView.evaluateJavascript("javascript:addLog('⏳ ऐड तैयार किया जा रहा है, कृपया 2-3 सेकंड रुकें...');", null);
                    }
                }
            });
        }
    }
}
