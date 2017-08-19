package com.gaiagps.iburn.api.response;

/**
 * Represents the Data
 * Created by davidbrodsky on 6/26/15.
 */
public class DataManifest {

    public ResourceManifest art;
    public ResourceManifest camps;
    public ResourceManifest events;
    public ResourceManifest points;

    public DataManifest(ResourceManifest art, ResourceManifest camps, ResourceManifest events, ResourceManifest points) {
        this.art = art;
        this.camps = camps;
        this.events = events;
        this.points = points;
    }
}
