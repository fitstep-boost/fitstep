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
            webView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setAllowFileAccess(true);
            s.setAllowContentAccess(true);

            // Google OAuth के लिए मानक क्रोम यूज़र-एजेंट
            String ua = s.getUserAgentString();
            s.setUserAgentString(ua.replace("; wv", ""));

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            cm.setAcceptThirdPartyCookies(webView, true);

            // पुराने वर्किंग कोड की तरह ही सामान्य वेबव्यू क्लाइंट
            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

            rootLayout.addView(webView);
            setContentView(rootLayout);

            // लाइव वेब ऐप लोड करें
            webView.loadUrl("https://fitstep-boost.github.io/fitstep/");

            // Unity Ads शुरू करें
            UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    loadBottomBanner();
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String msg) {}
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBottomBanner() {
        runOnUiThread(() -> {
            try {
                if (rootLayout == null || bottomBanner != null) return;
                bottomBanner = new BannerView(MainActivity.this, PLACEMENT_BANNER, new UnityBannerSize(320, 50));
                bottomBanner.setListener(new BannerView.IListener() {
                    @Override
                    public void onBannerLoaded(BannerView v) {
                        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                        if (v.getParent() == null && rootLayout != null) {
                            rootLayout.addView(v, p);
                        }
                    }
                    @Override
                    public void onBannerFailedToLoad(BannerView v, BannerErrorInfo err) {}
                    @Override
                    public void onBannerClick(BannerView v) {}
                    @Override
                    public void onBannerLeftApplication(BannerView v) {}
                    @Override
                    public void onBannerShown(BannerView v) {}
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
