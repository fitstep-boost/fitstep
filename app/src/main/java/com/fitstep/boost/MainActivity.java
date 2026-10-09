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
import androidx.appcompat.app.AppCompatActivity;

// Start.io Imports
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;
import com.startapp.sdk.adsbase.Ad;

public class MainActivity extends AppCompatActivity {
    private static final String STARTIO_APP_ID = "209703459";
    private static final String HOSTED_WEB_URL = "https://fitstep-boost.github.io/fitstep/";

    private FrameLayout rootContainer;
    private WebView mainWebView;
    private StartAppAd startAppRewardedAd;
    private boolean isStartIoLoading = false;
    private boolean showAdWhenLoaded = false;

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

        // Start.io SDK Initialization with Test Ads ON
        StartAppSDK.init(this, STARTIO_APP_ID, false);
        StartAppSDK.setTestAdsEnabled(true);

        startAppRewardedAd = new StartAppAd(this);
        loadStartIoRewardedAd();

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

    private void loadStartIoRewardedAd() {
        if (isStartIoLoading) return;
        isStartIoLoading = true;

        startAppRewardedAd.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                isStartIoLoading = false;
                runOnUiThread(() -> {
                    mainWebView.evaluateJavascript("javascript:addLog('✅ Start.io टेस्ट ऐड लोड हो गया!');", null);
                    if (showAdWhenLoaded) {
                        showAdWhenLoaded = false;
                        showStartIoAdNow();
                    }
                });
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
                isStartIoLoading = false;
                runOnUiThread(() -> {
                    String err = (ad != null && ad.getErrorMessage() != null) ? ad.getErrorMessage() : "Unknown Error";
                    mainWebView.evaluateJavascript("javascript:addLog('⚠️ Start.io लोड फ़ेल: " + err + "');", null);
                    mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                });
            }
        });
    }

    private void showStartIoAdNow() {
        if (startAppRewardedAd.isReady()) {
            startAppRewardedAd.setVideoListener(new VideoListener() {
                @Override
                public void onVideoCompleted() {
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
                    });
                }
            });

            startAppRewardedAd.showAd(new AdDisplayListener() {
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
                        mainWebView.evaluateJavascript("javascript:addLog('⚠️ Start.io डिस्प्ले नहीं हो सका');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    });
                }
            });
        } else {
            showAdWhenLoaded = true;
            loadStartIoRewardedAd();
            runOnUiThread(() -> {
                mainWebView.evaluateJavascript("javascript:addLog('⏳ Start.io ऐड लोड हो रहा है, कृपया रुकें...');", null);
            });
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (startAppRewardedAd != null && startAppRewardedAd.isReady()) {
                    showStartIoAdNow();
                } else {
                    showAdWhenLoaded = true;
                    loadStartIoRewardedAd();
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ Start.io टेस्ट ऐड लोड हो रहा है...');", null);
                }
            });
        }
    }
}
