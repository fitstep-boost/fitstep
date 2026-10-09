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

// Start.io Imports
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.startapp.sdk.adsbase.Ad;

public class MainActivity extends AppCompatActivity {
    private static final String HOSTED_WEB_URL = "https://fitstep-boost.github.io/fitstep/";
    
    // AdMob Live ID
    private static final String ADMOB_LIVE_REWARDED_ID = "ca-app-pub-4526276681965606/8548376683";
    
    // Start.io App ID & Config
    private static final String STARTIO_APP_ID = "209703459";
    private static final boolean STARTIO_TEST_MODE = true; // Testing mode ON for clear distinction

    private FrameLayout rootContainer;
    private WebView mainWebView;

    // AdMob Variables
    private RewardedAd mAdMobRewardedAd;
    private boolean isAdMobLoading = false;

    // Start.io Variables
    private StartAppAd mStartAppRewardedAd;
    private boolean isStartIoLoading = false;

    private boolean showAdWhenReady = false;

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
        loadAdMobAd();

        // 2. Initialize Start.io (Test Mode)
        StartAppSDK.init(this, STARTIO_APP_ID, false);
        StartAppSDK.setTestAdsEnabled(STARTIO_TEST_MODE);
        mStartAppRewardedAd = new StartAppAd(this);
        loadStartIoAd();

        // WebView Settings
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

    // --- AdMob Methods ---
    private void loadAdMobAd() {
        if (mAdMobRewardedAd != null || isAdMobLoading) return;
        isAdMobLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, ADMOB_LIVE_REWARDED_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mAdMobRewardedAd = rewardedAd;
                isAdMobLoading = false;
                if (showAdWhenReady) {
                    showAdWhenReady = false;
                    runOnUiThread(() -> showAdMobNow());
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mAdMobRewardedAd = null;
                isAdMobLoading = false;
                // If user was waiting for an ad, trigger Start.io fallback immediately
                if (showAdWhenReady) {
                    showAdWhenReady = false;
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:addLog('⚠️ AdMob लोड नहीं हुआ (Code: " + loadAdError.getCode() + ") -> Start.io बैकअप चालू...');", null);
                        triggerStartIoFallback();
                    });
                }
            }
        });
    }

    private void showAdMobNow() {
        if (mAdMobRewardedAd != null) {
            runOnUiThread(() -> {
                mainWebView.evaluateJavascript("javascript:addLog('🟢 AdMob लाइव ऐड शुरू हो रहा है...');", null);
            });

            mAdMobRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mAdMobRewardedAd = null;
                    loadAdMobAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    mAdMobRewardedAd = null;
                    loadAdMobAd();
                    // AdMob display failed -> fallback to Start.io
                    runOnUiThread(() -> triggerStartIoFallback());
                }
            });

            mAdMobRewardedAd.show(MainActivity.this, rewardItem -> {
                runOnUiThread(() -> {
                    mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
                });
            });
        } else {
            triggerStartIoFallback();
        }
    }

    // --- Start.io Methods (Fallback) ---
    private void loadStartIoAd() {
        if (isStartIoLoading) return;
        isStartIoLoading = true;

        mStartAppRewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                isStartIoLoading = false;
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
                isStartIoLoading = false;
            }
        });
    }

    private void triggerStartIoFallback() {
        if (mStartAppRewardedAd != null && mStartAppRewardedAd.isReady()) {
            runOnUiThread(() -> {
                mainWebView.evaluateJavascript("javascript:addLog('🔄 Start.io बैकअप ऐड दिखाया जा रहा है...');", null);
            });

            mStartAppRewardedAd.setVideoListener(new VideoListener() {
                @Override
                public void onVideoCompleted() {
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
                    });
                }
            });

            mStartAppRewardedAd.showAd(new AdDisplayListener() {
                @Override
                public void adHidden(Ad ad) {
                    loadStartIoAd();
                }

                @Override
                public void adDisplayed(Ad ad) {}

                @Override
                public void adClicked(Ad ad) {}

                @Override
                public void adNotDisplayed(Ad ad) {
                    loadStartIoAd();
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:addLog('❌ दोनों नेटवर्क्स के ऐड उपलब्ध नहीं हैं।');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    });
                }
            });
        } else {
            // Both not ready yet
            loadStartIoAd();
            loadAdMobAd();
            runOnUiThread(() -> {
                mainWebView.evaluateJavascript("javascript:addLog('⏳ ऐड लोड हो रहा है, कृपया 2 सेकंड बाद दोबारा दबाएँ...');", null);
                mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
            });
        }
    }

    // --- JavaScript Interface ---
    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                // 1. First priority: Check if AdMob is ready
                if (mAdMobRewardedAd != null) {
                    showAdMobNow();
                } 
                // 2. Second priority: If AdMob is loading, give it a shot, otherwise check Start.io
                else if (isAdMobLoading) {
                    showAdWhenReady = true;
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ AdMob कनेक्ट किया जा रहा है...');", null);
                } 
                // 3. Fallback immediately to Start.io
                else if (mStartAppRewardedAd != null && mStartAppRewardedAd.isReady()) {
                    triggerStartIoFallback();
                } 
                // 4. If neither is preloaded, start loading both
                else {
                    showAdWhenReady = true;
                    loadAdMobAd();
                    loadStartIoAd();
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ ऐड लोड हो रहा है, कृपया प्रतीक्षा करें...');", null);
                }
            });
        }
    }
}
