package com.gaiagps.iburn;

import java.util.HashMap;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class Constants {

    public static final double MAN_LAT = 40.782622;
    public static final double MAN_LON = -119.208264;

    // External storage directory relative to sdcard
    public static final String IBURN_ROOT = "iburn";
    // External storage directory for tiles, relative to IBURN_ROOT
    public static final String TILES_DIR = "tiles";

    //public static final String MBTILE_DESTINATION = "iburn2013_transparent.mbtiles";
    public static final String MBTILE_DESTINATION = "iburn2013.mbtiles";

    // SharedPreferences keys
    public static final String GENERAL_PREFS = "gen";

    public enum TAB_TYPE { MAP, CAMPS, ART, EVENTS};

    public static HashMap<String, Integer> TAB_TO_TITLE = new HashMap<String, Integer>() {{
        put(TAB_TYPE.MAP.toString().toLowerCase(), R.string.map_tab);
        put(TAB_TYPE.CAMPS.toString().toLowerCase(), R.string.camps_tab);
        put(TAB_TYPE.ART.toString().toLowerCase(), R.string.art_tab);
        put(TAB_TYPE.EVENTS.toString().toLowerCase(), R.string.events_tab);
    }};
}
