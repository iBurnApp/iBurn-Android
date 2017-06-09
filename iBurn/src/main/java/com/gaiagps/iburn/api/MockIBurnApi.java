package com.gaiagps.iburn.api;

import android.content.Context;

import com.gaiagps.iburn.api.response.Art;
import com.gaiagps.iburn.api.response.Camp;
import com.gaiagps.iburn.api.response.DataManifest;
import com.gaiagps.iburn.api.response.Event;
import com.gaiagps.iburn.api.response.ResourceManifest;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Response;


/**
 * Created by dbro on 8/1/15.
 */
public class MockIBurnApi implements IBurnApi {

    private Context context;
    private DataManifest manifest;
    private Gson gson;

    public MockIBurnApi(Context context) {
        this.context = context;
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new PlayaDateTypeAdapter())
                .create();

        // Don't trigger an update
        ResourceManifest map = new ResourceManifest("map.mbtiles.jar", new Date(0));

        // Do trigger an update
        ResourceManifest art = new ResourceManifest("art.json.js", new Date());
        ResourceManifest camp = new ResourceManifest("camp.json.js", new Date());
        ResourceManifest event = new ResourceManifest("event.json.js", new Date());

        manifest = new DataManifest(art, camp, event, map);
    }

    @Override
    public Observable<DataManifest> getDataManifest() {
        return Observable.just(manifest);
    }

    @Override
    public Observable<List<Camp>> getCamps() {
        return Observable.just("json/camps.json")
                .observeOn(Schedulers.io())
                .map(path -> {
                    try {
                        InputStream is = context.getAssets().open(path);
                        Camp[] camps = gson.fromJson(new BufferedReader(new InputStreamReader(is)), Camp[].class);
                        List<Camp> campList = new ArrayList<>(Arrays.asList(camps));
                        return campList;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // How do we throw an exception or indicate error?
                    return null;
                });
    }

    @Override
    public Observable<List<Art>> getArt() {
        return Observable.just("json/art.json")
                .observeOn(Schedulers.io())
                .map(path -> {
                    try {
                        InputStream is = context.getAssets().open(path);
                        Art[] art = gson.fromJson(new BufferedReader(new InputStreamReader(is)), Art[].class);
                        List<Art> artList = new ArrayList<>(Arrays.asList(art));
                        return artList;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // How do we throw an exception or indicate error?
                    return null;
                });
    }

    @Override
    public Observable<List<Event>> getEvents() {
        return Observable.just("json/events.json")
                .observeOn(Schedulers.io())
                .map(path -> {
                    try {
                        InputStream is = context.getAssets().open(path);
                        Event[] events = gson.fromJson(new BufferedReader(new InputStreamReader(is)), Event[].class);
                        List<Event> eventsList = new ArrayList<>(Arrays.asList(events));
                        return eventsList;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // How do we throw an exception or indicate error?
                    return null;
                });
    }

    @Override
    public Observable<ResponseBody> getTiles() {
        // currently unused
        return null;
    }
}
