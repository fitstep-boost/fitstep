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

// Unity Ads Imports
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;

public class MainActivity extends AppCompatActivity {
    private static final String HOSTED_WEB_URL = "https://fitstep-boost.github.io/fitstep/";

    // Your Unity Ads Config with Test Mode ON
    private static final String UNITY_GAME_ID = "800391367";
    private static final String UNITY_PLACEMENT_ID = "BP_Rewarded_Android";
    private static final boolean UNITY_TEST_MODE = true;

    private FrameLayout rootContainer;
    private WebView mainWebView;
    private boolean isUnityInitDone = false;
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

        // Initialize Unity Ads
        UnityAds.initialize(getApplicationContext(), UNITY_GAME_ID, UNITY_TEST_MODE, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                isUnityInitDone = true;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('✅ Unity Ads SDK तैयार (Init OK)');", null);
                    }
                });
                loadUnityAd();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                isUnityInitDone = false;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('❌ Unity Init फ़ेल: " + error.toString() + " - " + message + "');", null);
                    }
                });
            }
        });
    }

    private void loadUnityAd() {
        if (!isUnityInitDone || isUnityLoaded || isUnityLoading) return;
        isUnityLoading = true;

        UnityAds.load(UNITY_PLACEMENT_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                isUnityLoaded = true;
                isUnityLoading = false;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('✅ Unity Ads लोड हो गया! अब बटन दबाएँ।');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    }
                });
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                isUnityLoaded = false;
                isUnityLoading = false;
                runOnUiThread(() -> {
                    if (mainWebView != null) {
                        mainWebView.evaluateJavascript("javascript:addLog('❌ Unity लोड फ़ेल: " + error.toString() + " | " + message + "');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    }
                });
            }
        });
    }

    private void showUnityAd() {
        UnityAds.show(MainActivity.this, UNITY_PLACEMENT_ID, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                isUnityLoaded = false;
                loadUnityAd();
                runOnUiThread(() -> {
                    mainWebView.evaluateJavascript("javascript:addLog('❌ Unity Show फ़ेल: " + message + "');", null);
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
                loadUnityAd();
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:window.adRewardCompleted();", null);
                    });
                } else {
                    runOnUiThread(() -> {
                        mainWebView.evaluateJavascript("javascript:addLog('⚠️ ऐड पूरा नहीं देखा गया।');", null);
                        mainWebView.evaluateJavascript("javascript:enableAdButton();", null);
                    });
                }
            }
        });
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void showRewardedAd() {
            runOnUiThread(() -> {
                if (!isUnityInitDone) {
                    mainWebView.evaluateJavascript("javascript:addLog('⚠️ Unity Init अभी पूरा नहीं हुआ है, कृपया प्रतीक्षा करें...');", null);
                    return;
                }

                if (isUnityLoaded) {
                    mainWebView.evaluateJavascript("javascript:addLog('▶️ Unity Ads शुरू हो रहा है...');", null);
                    showUnityAd();
                } else {
                    mainWebView.evaluateJavascript("javascript:addLog('⏳ Unity Ads लोड किया जा रहा है...');", null);
                    loadUnityAd();
                }
            });
        }
    }
}
