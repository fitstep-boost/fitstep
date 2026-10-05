package com.fitstep.boost;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private WebView popupWebView;
    private RewardedAd rewardedAd;
    private boolean isAdLoading = false;
    private static final String TARGET_URL = "https://fitstep-boost.github.io/fitstep/";
    // AdMob Google Official Sample Rewarded Ad Unit ID
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. AdMob SDK इनिशियलाइज़ करें
        try {
            MobileAds.initialize(this, initializationStatus -> {
                runOnUiThread(this::loadRewardedAd);
            });
        } catch (Exception ignored) {}

        // 2. WebView सेटअप
        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(webView);

        configureSettings(webView);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // JavaScript ब्रिज
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());

        if (savedInstanceState == null) {
            webView.loadUrl(TARGET_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        // पहले ऐड को तुरंत लोड करने का बैकअप कॉल
        new Handler(Looper.getMainLooper()).postDelayed(this::loadRewardedAd, 1500);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (popupWebView != null) {
                    ((ViewGroup) popupWebView.getParent()).removeView(popupWebView);
                    popupWebView.destroy();
                    popupWebView = null;
                } else if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void loadRewardedAd() {
        if (isAdLoading || rewardedAd != null) {
            return;
        }
        isAdLoading = true;

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                isAdLoading = false;

                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedAd = null;
                        loadRewardedAd(); // अगला ऐड पहले से लोड करके रखें
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        rewardedAd = null;
                        loadRewardedAd();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                isAdLoading = false;
                // स्क्रीन पर साफ़ कारण दिखेगा अगर कोई एरर आए
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "ऐड लोड नहीं हुआ: " + loadAdError.getMessage(), Toast.LENGTH_SHORT).show();
                });
                // 5 सेकंड बाद बैकग्राउंड में दोबारा प्रयास करें
                new Handler(Looper.getMainLooper()).postDelayed(() -> loadRewardedAd(), 5000);
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (rewardedAd != null) {
                    rewardedAd.show(MainActivity.this, rewardItem -> {
                        // यूजर ने पूरा ऐड देख लिया
                        webView.evaluateJavascript("if(window.adRewardCompleted) { window.adRewardCompleted(); }", null);
                    });
                } else {
                    Toast.makeText(MainActivity.this, "ऐड तैयार किया जा रहा है, 2 सेकंड प्रतीक्षा करें...", Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
                }
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureSettings(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        String chromeAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
        settings.setUserAgentString(chromeAgent);
    }

    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            popupWebView = new WebView(MainActivity.this);
            configureSettings(popupWebView);

            CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true);

            popupWebView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            popupWebView.setWebViewClient(new WebViewClient());
            popupWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onCloseWindow(WebView window) {
                    if (popupWebView != null) {
                        ((ViewGroup) popupWebView.getParent()).removeView(popupWebView);
                        popupWebView.destroy();
                        popupWebView = null;
                    }
                }
            });

            addContentView(popupWebView, popupWebView.getLayoutParams());

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popupWebView);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            if (popupWebView != null) {
                ((ViewGroup) popupWebView.getParent()).removeView(popupWebView);
                popupWebView.destroy();
                popupWebView = null;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}
