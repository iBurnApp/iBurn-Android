package com.gaiagps.iburn.api.response;

import java.util.List;

/**
 * Created by davidbrodsky on 6/26/15.
 */
public class Art extends PlayaItem {

    public String artist;
    public String artistLocation;
    public String audioTourUrl;

    public List<ArtImage> images;
}
