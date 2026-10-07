package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.unity3d.ads.UnityAds;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;
import com.unity3d.services.banners.BannerErrorInfo;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private BannerView bottomBanner;

    // Unity Ads Configuration
    private static final String UNITY_GAME_ID = "800391367";
    private static final String PLACEMENT_REWARDED = "BP_Rewarded_Android";
    private static final String PLACEMENT_INTERSTITIAL = "BP_Interstitial_Android";
    private static final String PLACEMENT_BANNER = "BP_Banner_Android";
    private static final boolean TEST_MODE = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Root Layout Setup
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // WebView Setup
        webView = new WebView(this);
        FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        // Space at bottom for banner ad
        webViewParams.bottomMargin = (int) (50 * getResources().getDisplayMetrics().density);
        webView.setLayoutParams(webViewParams);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        // Load Local Asset HTML
        webView.loadUrl("file:///android_asset/index.html");

        rootLayout.addView(webView);
        setContentView(rootLayout);

        // Initialize Unity Ads
        UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                loadBottomBanner(rootLayout);
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                // Unity initialization failed
            }
        });
    }

    // Load Footer Banner
    private void loadBottomBanner(FrameLayout rootLayout) {
        runOnUiThread(() -> {
            bottomBanner = new BannerView(MainActivity.this, PLACEMENT_BANNER, new UnityBannerSize(320, 50));
            bottomBanner.setListener(new BannerView.IListener() {
                @Override
                public void onBannerLoaded(BannerView bannerAdView) {
                    FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    bannerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    
                    if (bannerAdView.getParent() == null) {
                        rootLayout.addView(bannerAdView, bannerParams);
                    }
                }

                @Override
                public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {}

                @Override
                public void onBannerClick(BannerView bannerAdView) {}

                @Override
                public void onBannerLeftApplication(BannerView bannerAdView) {}
            });
            bottomBanner.load();
        });
    }

    // JavaScript Bridge for Web Interaction
    public class WebAppInterface {

        // Rewarded Video Ad (Call from HTML when user watches ad)
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                UnityAds.show(MainActivity.this, PLACEMENT_REWARDED, new IUnityAdsShowListener() {
                    @Override
                    public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                        webView.evaluateJavascript("javascript:if(window.onAdFailed) window.onAdFailed();", null);
                    }

                    @Override
                    public void onUnityAdsShowStart(String placementId) {}

                    @Override
                    public void onUnityAdsShowClick(String placementId) {}

                    @Override
                    public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                        // Notify HTML that ad completed successfully
                        webView.evaluateJavascript("javascript:if(window.onAdCompleted) window.onAdCompleted();", null);
                    }
                });
            });
        }

        // Interstitial Ad (Full-screen ad after steps sync)
        @JavascriptInterface
        public void showInterstitialAd() {
            runOnUiThread(() -> {
                UnityAds.show(MainActivity.this, PLACEMENT_INTERSTITIAL, new IUnityAdsShowListener() {
                    @Override
                    public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {}

                    @Override
                    public void onUnityAdsShowStart(String placementId) {}

                    @Override
                    public void onUnityAdsShowClick(String placementId) {}

                    @Override
                    public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {}
                });
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (bottomBanner != null) {
            bottomBanner.destroy();
        }
        super.onDestroy();
    }
}
