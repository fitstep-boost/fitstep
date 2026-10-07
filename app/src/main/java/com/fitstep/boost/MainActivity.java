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
    private FrameLayout rootLayout;

    private static final String UNITY_GAME_ID = "800391367";
    private static final String PLACEMENT_REWARDED = "BP_Rewarded_Android";
    private static final String PLACEMENT_INTERSTITIAL = "BP_Interstitial_Android";
    private static final String PLACEMENT_BANNER = "BP_Banner_Android";
    private static final boolean TEST_MODE = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            rootLayout = new FrameLayout(this);
            rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            webView = new WebView(this);
            FrameLayout.LayoutParams webViewParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            webViewParams.bottomMargin = (int) (50 * getResources().getDisplayMetrics().density);
            webView.setLayoutParams(webViewParams);

            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);

            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

            // Live GitHub Pages URL
            webView.loadUrl("https://fitstep-boost.github.io/fitstep/");

            rootLayout.addView(webView);
            setContentView(rootLayout);

            // Unity Ads Initialization
            UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    loadBottomBanner();
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBottomBanner() {
        runOnUiThread(() -> {
            try {
                if (rootLayout == null) return;
                bottomBanner = new BannerView(MainActivity.this, PLACEMENT_BANNER, new UnityBannerSize(320, 50));
                bottomBanner.setListener(new BannerView.IListener() {
                    @Override
                    public void onBannerLoaded(BannerView bannerAdView) {
                        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        bannerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

                        if (bannerAdView.getParent() == null && rootLayout != null) {
                            rootLayout.addView(bannerAdView, bannerParams);
                        }
                    }

                    @Override
                    public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {}

                    @Override
                    public void onBannerClick(BannerView bannerAdView) {}

                    @Override
                    public void onBannerLeftApplication(BannerView bannerAdView) {}

                    @Override
                    public void onBannerShown(BannerView bannerAdView) {}
                });
                bottomBanner.load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public class WebAppInterface {

        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                try {
                    UnityAds.show(MainActivity.this, PLACEMENT_REWARDED, new IUnityAdsShowListener() {
                        @Override
                        public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                            if (webView != null) {
                                webView.evaluateJavascript("javascript:if(window.onAdFailed) window.onAdFailed();", null);
                            }
                        }

                        @Override
                        public void onUnityAdsShowStart(String placementId) {}

                        @Override
                        public void onUnityAdsShowClick(String placementId) {}

                        @Override
                        public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                            if (webView != null) {
                                webView.evaluateJavascript("javascript:if(window.onAdCompleted) window.onAdCompleted();", null);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @JavascriptInterface
        public void showInterstitialAd() {
            runOnUiThread(() -> {
                try {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
