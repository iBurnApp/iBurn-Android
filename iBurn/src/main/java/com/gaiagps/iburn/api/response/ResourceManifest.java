package com.gaiagps.iburn.api.response;

import java.util.Date;

/**
 * Created by davidbrodsky on 6/26/15.
 */
public class ResourceManifest {

    public String file;
    public Date updated;

    public ResourceManifest(String file, Date updated) {
        this.file = file;
        this.updated = updated;
    }
}
