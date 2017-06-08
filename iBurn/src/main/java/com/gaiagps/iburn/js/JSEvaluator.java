package com.gaiagps.iburn.js;

import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.android.schedulers.AndroidSchedulers;
import rx.Observable;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by dbro on 8/6/15.
 */
public class JSEvaluator {

    private static final PublishSubject jsEvaluatorSubject = PublishSubject.create();
    private static final AtomicBoolean initializedJsEvaluator = new AtomicBoolean();
    private static JSEvaluator jsEvaluator;

    private WebView webView;
    private Evaluator evaluator;

    // <editor-fold desc="Private API">

    public static Observable<JSEvaluator> getInstance(Context context) {
        return getInstance("", context);
    }

    public static Observable<JSEvaluator> getInstance(String url, Context context) {

        if (!initializedJsEvaluator.getAndSet(true)) {
            // We need to start initializing the singleton
            new JSEvaluator(url, new WebView(context), evaluator -> jsEvaluatorSubject.onNext(evaluator));
        } else if (jsEvaluator != null) {
            // We've already initialized the singleton
            return Observable.just(jsEvaluator);
        }
        // Another caller started initialization, await its result
        return jsEvaluatorSubject.first();
    }

    private JSEvaluator(String url, WebView webView, JSEvaluatorCallback callback) {
        this.webView = webView;
        this.evaluator = new LegacyEvaluator(webView);
        // Not sure I want to have two different javascript pipelines
        // especially bc there seems to be some inconsistent behavior:
        // https://github.com/evgenyneu/js-evaluator-for-android/issues/4
//        this.evaluator = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
//                new KitKatEvaluator() : new LegacyEvaluator(webView);
        setupWebView(url, callback);
    }

    private void setupWebView(String url, JSEvaluatorCallback callback) {
        webView.setWillNotDraw(true);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Timber.d("onPageFinished");
                AndroidSchedulers.mainThread().scheduleDirect(() -> {
                    Timber.d("Notifying callback");
                    jsEvaluator = JSEvaluator.this;
                    callback.onReady(JSEvaluator.this);
                });
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Timber.e("Webview error " + errorCode + " desc " + description);
            }
        });

        Timber.d("Loading html");
        webView.loadUrl(url);
    }

    // </editor-fold desc="Private API">

    // <editor-fold desc="Public API">

    public void reverseGeocode(final double latitude, final double longitude, Evaluator.EvaluatorCallback callback) {
        evaluator.evaluate(webView,
                String.format("return window.coder.reverse(%f, %f);", latitude, longitude),
                callback);
    }

    public void forwardGeocode(final String playaAddress, Evaluator.EvaluatorCallback callback) {
        evaluator.evaluate(webView,
                String.format("return window.coder.forwardAsString(\"%s\");", playaAddress),
                callback);
    }

    // </editor-fold desc="Public API">

    public interface JSEvaluatorCallback {
        void onReady(JSEvaluator evaluator);
    }

    public interface Evaluator {

        interface EvaluatorCallback {
            void onResult(String result);
        }

        void evaluate(WebView webView, String script, EvaluatorCallback callback);
    }
}
