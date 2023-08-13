package com.gaiagps.iburn.api.response;

/**
 * Represents the Data
 * Created by davidbrodsky on 6/26/15.
 */
public class DataManifest {

    public ResourceManifest art;
    public ResourceManifest camps;
    public ResourceManifest events;

    public DataManifest(ResourceManifest art, ResourceManifest camps, ResourceManifest events) {
        this.art = art;
        this.camps = camps;
        this.events = events;
    }
}
