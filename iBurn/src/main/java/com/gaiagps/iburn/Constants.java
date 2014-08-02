package com.gaiagps.iburn;

import android.text.format.DateUtils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class Constants {

    public static final double MAN_LAT = 40.788800;
    public static final double MAN_LON = -119.203150;

    // External storage directory relative to sdcard
    public static final String IBURN_ROOT = "iburn";
    // External storage directory for tiles, relative to IBURN_ROOT
    public static final String TILES_DIR = "tiles";

    //public static final String MBTILE_DESTINATION = "iburn2013_transparent.mbtiles";
    public static final String MBTILE_DESTINATION = "iburn2013.mbtiles";

    // SharedPreferences keys

    public static final String EMBARGO_CLEAR = "embargo";
    public static final String HOME_LAT = "home_lat";
    public static final String HOME_LON = "home_lon";

    public enum PLAYA_ITEM { CAMP, ART, EVENT};
    public enum TAB_TYPE { MAP, CAMPS, ART, EVENTS};

    public static HashMap<TAB_TYPE, Integer> TAB_TO_TITLE = new HashMap<TAB_TYPE, Integer>() {{
        put(TAB_TYPE.MAP, R.string.map_tab);
        put(TAB_TYPE.CAMPS, R.string.camps_tab);
        put(TAB_TYPE.ART, R.string.art_tab);
        put(TAB_TYPE.EVENTS, R.string.events_tab);
    }};

    static final GregorianCalendar EMBARGO_DATE = new GregorianCalendar(2013, Calendar.AUGUST, 26);
}
