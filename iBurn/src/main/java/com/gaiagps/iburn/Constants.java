package com.gaiagps.iburn;

import java.util.HashMap;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class Constants {

    public static final double MAN_LAT = 40.782622;
    public static final double MAN_LON = -119.208264;

    public static final double WOMAN_LAT = 40.792622;
    public static final double WOMAN_LON = -119.218264;

    // External storage directory relative to sdcard
    public static final String IBURN_ROOT = "iburn";
    // External storage directory for tiles, relative to IBURN_ROOT
    public static final String TILES_DIR = "tiles";

    //public static final String MBTILE_DESTINATION = "iburn2013_transparent.mbtiles";
    public static final String MBTILE_DESTINATION = "iburn2013.mbtiles";

    // SharedPreferences keys
    public static final String GENERAL_PREFS = "gen";
    public static final String DB_POPULATED = "db_populated";

    public enum TAB_TYPE { MAP, CAMPS, ART, EVENTS};

    public static HashMap<TAB_TYPE, Integer> TAB_TO_TITLE = new HashMap<TAB_TYPE, Integer>() {{
        put(TAB_TYPE.MAP, R.string.map_tab);
        put(TAB_TYPE.CAMPS, R.string.camps_tab);
        put(TAB_TYPE.ART, R.string.art_tab);
        put(TAB_TYPE.EVENTS, R.string.events_tab);
    }};
}
