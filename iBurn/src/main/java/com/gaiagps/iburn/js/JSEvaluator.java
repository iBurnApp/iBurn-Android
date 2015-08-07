package com.gaiagps.iburn.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by dbro on 8/6/15.
 */
public class JSEvaluator {

    private static final String JS_PATH = "js/bundle.js";
    private static PublishSubject<JSEvaluator> subject = PublishSubject.create();
    private static Observable<JSEvaluator> observable;
    private static AtomicBoolean initializedEvaluator = new AtomicBoolean(false);


    private Context context;
    private Scriptable scope;
    private Object loadResult;

    public static Observable<JSEvaluator> getInstance(android.content.Context androidContext) {
        if (!initializedEvaluator.getAndSet(true)) {
            observable = subject.cache(1);
            Observable.just(androidContext)
                    .observeOn(Schedulers.io())
                    .subscribe(context -> {
                        JSEvaluator evaluator = new JSEvaluator(context);
                        subject.onNext(evaluator);
                    });
        }

        return observable;
    }

    private JSEvaluator(android.content.Context androidContext) {
        context = Context.enter();
        context.setOptimizationLevel(-1); // Can't compile due to 64k method limit
        scope = context.initStandardObjects();
        context.setLanguageVersion(Context.VERSION_1_8);
        try {
            InputStream is = androidContext.getAssets().open(JS_PATH);

            int bytesRead;
            byte[] buffer = new byte[10 * 1024];
            StringBuilder jsStringBuilder = new StringBuilder();
            jsStringBuilder.append("var window = this; ");
            while ((bytesRead = is.read(buffer)) > 0) {
                jsStringBuilder.append(new String(buffer, 0, bytesRead));
            }
            Object loadResult = context.evaluateString(scope, jsStringBuilder.toString(), "bundle.js", 1, null);

            Timber.d("Loaded script (%d bytes)", jsStringBuilder.length());
        } catch (IOException e) {
            Timber.e(e, "Failed to load script");
        }
    }

    public synchronized String reverseGeocode(double latitude, double longitude) {
        scope.put("coder", scope, "");

        context.evaluateString(scope, "coder = prepare();", "prepare", 1, null);

        Object result = context.evaluateString(scope, String.format("reverseGeocode(coder, %s, %s)", latitude, longitude), "geocoder", 1, null);

//        Function prepare = (Function) scope.get("prepare", scope);
//        Object coder = prepare.call(context, scope, scope, new Object[]{});

//        Function reverseGeocode = (Function) scope.get("reverseGeocode", scope);
//        Object geocodeArgs[] = {coder, latitude, longitude};
//        Object geocoderResult = reverseGeocode.call(context, scope, null, geocodeArgs);
//
//        Timber.d("Got geocoder result" + geocoderResult);

//        return (String) geocoderResult;
        return "42";
    }

    public synchronized void release() {
        Context.exit();
        initializedEvaluator.getAndSet(false);
    }
}
