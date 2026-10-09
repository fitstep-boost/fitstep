package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
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
import com.google.android.gms.ads.AdListener;
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

// Start.io Imports
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.startapp.sdk.adsbase.Ad;

public class MainActivity extends AppCompatActivity {
    private static final String HOSTED_WEB_URL = "https://fitstep-boost.github.io/fitstep/";
    
    // Live AdMob IDs
    private static final String ADMOB_REWARDED_ID = "ca-app-pub-4526276681965606/8548376683";
    private static final String ADMOB_BANNER_ID = "ca-app-pub-4526276681965606/7711212827";
    private static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-4526276681965606/5621155978";
    
    // Start.io App ID & Config
    private static final String STARTIO_APP_ID = "209703459";
    private static final boolean STARTIO_TEST_MODE = true; // Still in test mode as requested

    private FrameLayout rootContainer;
    private FrameLayout bannerContainer;
    private WebView mainWebView;

    // Banner Views
    private AdView adMobBannerView;
    private Banner startIoBannerView;

    // Rewarded Ads
    private RewardedAd mAdMobRewardedAd;
    private boolean isAdMobLoading = false;
    private StartAppAd mStartAppRewardedAd;
    private boolean isStartIoLoading = false;
    private boolean showAdWhenReady = false;

    // Interstitial Ads
    private InterstitialAd mAdMobInterstitialAd;
    private StartAppAd mStartAppInterstitialAd;
    private boolean isInterstitialLoading = false;

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        mainWebView = new WebView(this);
        FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mainWebView.setLayoutParams(webParams);
        rootContainer.addView(mainWebView);
        setContentView(rootContainer);

        // 1. Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {});
        loadAdMobRewardedAd();
        loadInterstitialAd();

        // 2. Initialize Start.io
        StartAppSDK.init(this, STARTIO_APP_ID, false);
        StartAppSDK.setTestAdsEnabled(STARTIO_TEST_MODE);
        mStartAppRewardedAd = new StartAppAd(this);
        mStartAppInterstitialAd = new StartAppAd(this);
        loadStartIoRewardedAd();

        // 3. Setup Sticky Banner Slot with Fallback
        setupStickyBannerSlot();

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

    // --- Banner Slot Fallback Logic ---
    private void setupStickyBannerSlot() {
        bannerContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        containerParams.gravity = Gravity.BOTTOM;
        bannerContainer.setLayoutParams(containerParams);
        rootContainer.addView(bannerContainer);

        loadAdMobBannerWithFallback();
    }

    private void loadAdMobBannerWithFallback() {
        adMobBannerView = new AdView(this);
        adMobBannerView.setAdUnitId(ADMOB_BANNER_ID);
        adMobBannerView.setAdSize(AdSize.BANNER);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        adMobBannerView.setLayoutParams(params);

        adMobBannerView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // AdMob Banner loaded -> show it
                bannerContainer.removeAllViews();
                bannerContainer.addView(adMobBannerView);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // AdMob Banner failed -> fallback to Start.io Banner
                showStartIoBannerFallback();
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        adMobBannerView.loadAd(adRequest);
    }

    private void showStartIoBannerFallback() {
        bannerContainer.removeAllViews();
        startIoBannerView = new Banner(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL;
        startIoBannerView.setLayoutParams(params);
        bannerContainer.addView(startIoBannerView);
    }

    // --- Rewarded Ad Methods ---
    private void loadAdMobRewardedAd() {
        if (mAdMobRewardedAd != null || isAdMobLoading) return;
        isAdMobLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, ADMOB_REWARDED_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                mAdMobRewardedAd = rewardedAd;
                isAdMobLoading = false;
                if (showAdWhenReady) {
                    showAdWhenReady = false;
                    runOnUiThread(() -> showAdMobRewardedNow());
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mAdMobRewardedAd = null;
                isAdMobLoading = false;
                if (showAdWhenReady) {
                    showAdWhenReady = false;
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:addLog('⚠️ AdMob लोड नहीं हुआ (Code: " + loadAdError.getCode() + ") -> Start.io बैकअप चालू...');", null);
                        triggerStartIoRewardedFallback();
                    });
                }
            }
        });
    }

    private void showAdMobRewardedNow() {
        if (mAdMobRewardedAd != null) {
            runOnUiThread(() -> {
                mainWebView.evaluateJavascript("javascript:addLog('🟢 AdMob ऐड शुरू हो रहा है...');", null);
            });

            mAdMobRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mAdMobRewardedAd = null;
                    loadAdMobRewardedAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    mAdMobRewardedAd = null;
                    loadAdMobRewardedAd();
                    runOnUiThread(() -> triggerStartIoRewardedFallback());
                }
            });

            mAdMobRewardedAd.show(MainActivity.this, rewardItem -> {
                runOnUiThread(() -> {
                    mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
                });
            });
        } else {
            triggerStartIoRewardedFallback();
        }
    }

    private void loadStartIoRewardedAd() {
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

    private void triggerStartIoRewardedFallback() {
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
                    loadStartIoRewardedAd();
                }

                @Override
                public void adDisplayed(Ad ad) {}

                @Override
                public void adClicked(Ad ad) {}

                @Override
                public void adNotDisplayed(Ad ad) {
                    loadStartIoRewardedAd();
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:addLog('❌ दोनों नेटवर्क्स के ऐड उपलब्ध नहीं हैं।');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    });
                }
            });
        } else {
            loadStartIoRewardedAd();
            loadAdMobRewardedAd();
            runOnUiThread(() -> {
                mainWebView.evaluateJavascript("javascript:addLog('⏳ ऐड लोड हो रहा है, कृपया पुनः प्रयास करें...');", null);
                mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
            });
        }
    }

    // --- Interstitial Ad Methods ---
    private void loadInterstitialAd() {
        if (isInterstitialLoading || mAdMobInterstitialAd != null) return;
        isInterstitialLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, ADMOB_INTERSTITIAL_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mAdMobInterstitialAd = interstitialAd;
                isInterstitialLoading = false;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mAdMobInterstitialAd = null;
                isInterstitialLoading = false;
                mStartAppInterstitialAd.loadAd(StartAppAd.AdMode.AUTOMATIC);
            }
        });
    }

    private void showInterstitialNow() {
        if (mAdMobInterstitialAd != null) {
            mAdMobInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    mAdMobInterstitialAd = null;
                    loadInterstitialAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                    mAdMobInterstitialAd = null;
                    loadInterstitialAd();
                    if (mStartAppInterstitialAd.isReady()) {
                        mStartAppInterstitialAd.showAd();
                    }
                }
            });
            mAdMobInterstitialAd.show(this);
        } else if (mStartAppInterstitialAd != null && mStartAppInterstitialAd.isReady()) {
            mStartAppInterstitialAd.showAd();
        } else {
            loadInterstitialAd();
        }
    }

    // --- JavaScript Interface Bridge ---
    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (mAdMobRewardedAd != null) {
                    showAdMobRewardedNow();
                } else if (isAdMobLoading) {
                    showAdWhenReady = true;
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ AdMob कनेक्ट किया जा रहा है...');", null);
                } else if (mStartAppRewardedAd != null && mStartAppRewardedAd.isReady()) {
                    triggerStartIoRewardedFallback();
                } else {
                    showAdWhenReady = true;
                    loadAdMobRewardedAd();
                    loadStartIoRewardedAd();
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ ऐड लोड हो रहा है, कृपया प्रतीक्षा करें...');", null);
                }
            });
        }

        @JavascriptInterface
        public void showInterstitialAd() {
            runOnUiThread(() -> showInterstitialNow());
        }
    }
}
