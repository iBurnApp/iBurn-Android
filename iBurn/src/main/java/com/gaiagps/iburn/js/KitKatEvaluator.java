package com.gaiagps.iburn.js;

import android.annotation.TargetApi;
import android.os.Build;
import android.webkit.WebView;

import rx.subjects.PublishSubject;

/**
 * Evaluates Javascript on a WebView, returning the result to a {@link PublishSubject}
 * Created by dbro on 8/7/15.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class KitKatEvaluator implements JSEvaluator.Evaluator {

    @Override
    public void evaluate(WebView webView, String script, EvaluatorCallback callback) {
        webView.evaluateJavascript("(function() { " + script + " })();", callback::onResult);

    }
}
