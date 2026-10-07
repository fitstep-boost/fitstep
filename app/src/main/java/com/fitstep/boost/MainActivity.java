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
import android.widget.LinearLayout;
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
    private LinearLayout rootLayout;

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
            // वर्टिकल लेआउट: ऊपर वेबव्यू पूरा स्पेस लेगा, नीचे बैनर रहेगा
            rootLayout = new LinearLayout(this);
            rootLayout.setOrientation(LinearLayout.VERTICAL);
            rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            webView = new WebView(this);
            LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0, 1.0f);
            webView.setLayoutParams(webParams);

            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setAllowFileAccess(true);
            s.setAllowContentAccess(true);

            // Google OAuth के लिए मानक Chrome एजेंट
            String ua = s.getUserAgentString();
            s.setUserAgentString(ua.replace("; wv", ""));

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(webView, true);

            // पुरानी ऐप की तरह साधारण और साफ़ क्लाइंट
            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

            rootLayout.addView(webView);
            setContentView(rootLayout);

            webView.loadUrl("https://fitstep-boost.github.io/fitstep/");

            // Unity SDK इनिशियलाइज़ेशन
            UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    runOnUiThread(() -> loadBanner());
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String msg) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBanner() {
        try {
            if (bottomBanner != null) return;
            bottomBanner = new BannerView(MainActivity.this, PLACEMENT_BANNER, new UnityBannerSize(320, 50));
            LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (50 * getResources().getDisplayMetrics().density));
            bannerParams.gravity = Gravity.CENTER_HORIZONTAL;
            bottomBanner.setLayoutParams(bannerParams);

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

            rootLayout.addView(bottomBanner);
            bottomBanner.load();
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
