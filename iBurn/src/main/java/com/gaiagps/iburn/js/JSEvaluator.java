package com.gaiagps.iburn.js;

import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by dbro on 8/6/15.
 */
public class JSEvaluator {

    // <editor-fold desc="Singleton management">
    private static PublishSubject<JSEvaluator> jsEvaluatorSubject = PublishSubject.create();
    private static Observable<JSEvaluator> observable;
    private static AtomicBoolean initializedEvaluator = new AtomicBoolean(false);

    public static Observable<JSEvaluator> getInstance(WebView webView) {
        if (!initializedEvaluator.getAndSet(true)) {
            observable = jsEvaluatorSubject.cache(1);
            Observable.just(webView)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(context1 -> {
                        JSEvaluator evaluator = new JSEvaluator(webView);
                        Timber.d("Created JSEvaluator");
                    });
        }

        return observable;
    }

    // </editor-fold desc="Singleton management">

    private WebView webView;
    private Evaluator evaluator;
    private PublishSubject<String> reverseGeoCoderSubject = PublishSubject.create();

    // <editor-fold desc="Private API">

    private JSEvaluator(WebView webView) {
        this.webView = webView;
        // TODO : Select Evaluator based on platform version
        this.evaluator = new KitKatEvaluator();
        setupWebView();
    }

    private void setupWebView() {
        try {
            byte[] buffer = new byte[10 * 1024];
            int bytesRead;
            InputStream is = null;
            is = webView.getContext().getAssets().open("js/example.html");
            StringBuilder string = new StringBuilder();
            while ((bytesRead = is.read(buffer)) > 0) {
                string.append(new String(buffer, 0, bytesRead));
            }

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Timber.d("onPageFinished");
                    evaluator.evaluate(webView, "window.coder = prepare(); return 'complete';",
                            result -> jsEvaluatorSubject.onNext(JSEvaluator.this));
                    super.onPageFinished(view, url);
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Timber.e("Webview error " + errorCode + " desc " + description);
                }
            });
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);

            webView.loadDataWithBaseURL("file:///android_asset/js/", string.toString(), "text/html", "utf-8", "");
        } catch (IOException e) {
            Timber.e(e, "Error loading webview content");
        }
    }

    // </editor-fold desc="Private API">

    // <editor-fold desc="Public API">

    public Observable<String> reverseGeocode(final double latitude, final double longitude) {
        evaluator.evaluate(webView,
                String.format("return reverseGeocode(window.coder, %f, %f); })();", latitude, longitude),
                reverseGeoCoderSubject::onNext);

        return reverseGeoCoderSubject;
    }

    // </editor-fold desc="Public API">

    public interface Evaluator {

        interface EvaluatorCallback {
            void onResult(String result);
        }

        void evaluate(WebView webView, String script, EvaluatorCallback callback);
    }
}
