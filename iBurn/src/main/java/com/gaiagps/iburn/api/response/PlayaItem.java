package com.gaiagps.iburn.api.response;

/**
 * Represents the base data shared by Events, Art and Camps on the PlayaEvents API JSON format
 * Created by davidbrodsky on 6/26/15.
 */
public class PlayaItem {

    public String contactEmail;
    public String description;
    public String uid;
    public Location location;
    public Location burnermap_location;
    public String name;
    public String url;
    public int year;
}
