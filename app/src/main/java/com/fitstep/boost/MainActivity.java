package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
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
    private static final boolean TEST_MODE = true;

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

            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setAllowFileAccess(true);
            s.setAllowContentAccess(true);

            // Google OAuth को अनुमति देने के लिए मानक User-Agent
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

            webView.loadUrl("https://fitstep-boost.github.io/fitstep/");

            rootLayout.addView(webView);
            setContentView(rootLayout);

            // बैनर व्यू सेटअप
            setupBottomBanner();

            // Unity Ads इनिशियलाइज़ेशन
            UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    runOnUiThread(() -> {
                        if (bottomBanner != null) {
                            bottomBanner.load();
                        }
                    });
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String msg) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupBottomBanner() {
        try {
            bottomBanner = new BannerView(MainActivity.this, PLACEMENT_BANNER, new UnityBannerSize(320, 50));
            FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            bannerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            rootLayout.addView(bottomBanner, bannerParams);

            bottomBanner.setListener(new BannerView.IListener() {
                @Override
                public void onBannerLoaded(BannerView bannerAdView) {}
                @Override
                public void onBannerFailedToLoad(BannerView bannerAdView, BannerErrorInfo errorInfo) {}
                @Override
                public void onBannerClick(BannerView bannerAdView) {}
                @Override
                public void onBannerLeftApplication(BannerView bannerAdView) {}
                @Override
                public void onBannerShown(BannerView bannerAdView) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                try {
                    UnityAds.show(MainActivity.this, PLACEMENT_REWARDED, new IUnityAdsShowListener() {
                        @Override
                        public void onUnityAdsShowFailure(String id, UnityAds.UnityAdsShowError err, String msg) {
                            if (webView != null) webView.evaluateJavascript("javascript:if(window.onAdFailed) window.onAdFailed();", null);
                        }
                        @Override
                        public void onUnityAdsShowStart(String id) {}
                        @Override
                        public void onUnityAdsShowClick(String id) {}
                        @Override
                        public void onUnityAdsShowComplete(String id, UnityAds.UnityAdsShowCompletionState state) {
                            if (webView != null) webView.evaluateJavascript("javascript:if(window.onAdCompleted) window.onAdCompleted();", null);
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
                        public void onUnityAdsShowFailure(String id, UnityAds.UnityAdsShowError err, String msg) {}
                        @Override
                        public void onUnityAdsShowStart(String id) {}
                        @Override
                        public void onUnityAdsShowClick(String id) {}
                        @Override
                        public void onUnityAdsShowComplete(String id, UnityAds.UnityAdsShowCompletionState state) {}
                    });
                } catch (Exception e) {
                    e.printStackTrace();
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
    protected void onDestroy() {
        if (bottomBanner != null) bottomBanner.destroy();
        super.onDestroy();
    }
}
