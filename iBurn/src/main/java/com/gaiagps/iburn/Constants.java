package com.gaiagps.iburn;

import java.util.Calendar;
import java.util.GregorianCalendar;

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

    public static final String MBTILE_DESTINATION = "iburn2015.mbtiles";

    // SharedPreferences keys
    public static final String EMBARGO_CLEAR = "embargo";
    public static final String HOME_LAT = "home_lat";
    public static final String HOME_LON = "home_lon";

    public enum PlayaItemType { CAMP, ART, EVENT, ALL};
    public enum TabType { MAP, CAMPS, ART, EVENTS};

    static final GregorianCalendar EMBARGO_DATE = new GregorianCalendar(2014, Calendar.AUGUST, 25, 12, 0);
}
