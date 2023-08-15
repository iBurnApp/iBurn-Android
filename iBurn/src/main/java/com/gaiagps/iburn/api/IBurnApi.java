package com.gaiagps.iburn.api;

import com.gaiagps.iburn.api.response.Art;
import com.gaiagps.iburn.api.response.Camp;
import com.gaiagps.iburn.api.response.DataManifest;
import com.gaiagps.iburn.api.response.Event;

import java.util.List;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Streaming;

/**
 * IBurn API Definition
 * Created by dbro on 8/1/15.
 */
public interface IBurnApi {

    @GET("update.json")
    Observable<DataManifest> getDataManifest();

    @GET("camp.json")
    Observable<List<Camp>> getCamps();

    @GET("art.json")
    Observable<List<Art>> getArt();

    @GET("event.json")
    Observable<List<Event>> getEvents();

    @GET("iburn.mbtiles.jar")
    @Streaming
    Observable<ResponseBody> getTiles();
}
