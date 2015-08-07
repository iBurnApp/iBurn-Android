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

    private static PublishSubject<JSEvaluator> jsEvaluatorSubject = PublishSubject.create();
    private static Observable<JSEvaluator> observable;
    private static AtomicBoolean initializedEvaluator = new AtomicBoolean(false);


    public static Observable<JSEvaluator> getInstance(Context context) {
        if (!initializedEvaluator.getAndSet(true)) {
            observable = jsEvaluatorSubject.cache(1);
            Observable.just(context)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(context1 -> {
                        JSEvaluator evaluator = new JSEvaluator(new WebView(context1));
                        Timber.d("Created JSEvaluator");
                    });
        }

        return observable;
    }

    private WebView webView;
    private PublishSubject<String> reverseGeoCoderSubject = PublishSubject.create();

    private JSEvaluator(WebView webView) {
        this.webView = webView;
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
                    webView.evaluateJavascript("(function() { window.coder = prepare(); return 'complete'; })();", value -> {
                        Timber.d("prepared coder");
                        jsEvaluatorSubject.onNext(JSEvaluator.this);
                    });
                    super.onPageFinished(view, url);
                }
            });
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);

            webView.loadDataWithBaseURL("file:///android_asset/js/", string.toString(), "text/html", "utf-8", "");
        } catch (IOException e) {
            Timber.e(e, "Error loading webview content");
        }
    }

    public Observable<String> reverseGeocode(final double latitude, final double longitude) {
        webView.evaluateJavascript(
                String.format("(function() { return reverseGeocode(window.coder, %f, %f); })();", latitude, longitude),
                result -> {
                    Timber.d("Got geocoder result " + result);
                    reverseGeoCoderSubject.onNext(result);
                });

        return reverseGeoCoderSubject;
    }
}
