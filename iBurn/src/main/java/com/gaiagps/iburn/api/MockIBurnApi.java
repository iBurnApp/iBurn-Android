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

import retrofit.client.Response;
import rx.Observable;
import rx.schedulers.Schedulers;

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
        ResourceManifest art = new ResourceManifest("art.json.js", new Date(0));
        ResourceManifest event = new ResourceManifest("event.json.js", new Date(0));
        ResourceManifest map = new ResourceManifest("map.mbtiles.jar", new Date(0));

        // Do trigger an update
        ResourceManifest camp = new ResourceManifest("camp.json.js", new Date());

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
                        InputStream is = context.getAssets().open("json/camps.json");
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
        // currently unused
        return null;
    }

    @Override
    public Observable<List<Event>> getEvents() {
        // currently unused
        return null;
    }

    @Override
    public Observable<Response> getTiles() {
        // currently unused
        return null;
    }
}
