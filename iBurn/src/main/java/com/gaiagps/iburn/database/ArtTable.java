package com.gaiagps.iburn.database;

public class ArtTable {
	
	//CAMP TABLE
    public static final String TABLE_NAME = "art";
    
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ARTIST = "artist";
    public static final String COLUMN_ARTIST_LOCATION = "artist_location";
    public static final String COLUMN_CIRCULAR_STREET = "circ_street";
    public static final String COLUMN_TIME_ADDRESS = "time_address";
    public static final String COLUMN_DISTANCE = "distance";
    public static final String COLUMN_HOUR = "hour";
    public static final String COLUMN_MINUTE = "minute";

    public static final String COLUMN_URL = "url";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_ART_ID = "art_id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_CONTACT = "contact";
    
    public static final String COLUMN_FAVORITE = "favorite";
    public static final String COLUMN_USER_ADDED = "user_added";
    
    public static final String[] COLUMNS = {COLUMN_ID, COLUMN_NAME, COLUMN_DESCRIPTION, COLUMN_ARTIST, COLUMN_ARTIST_LOCATION, COLUMN_URL,
    	COLUMN_CIRCULAR_STREET, COLUMN_TIME_ADDRESS, COLUMN_DISTANCE, COLUMN_HOUR, COLUMN_YEAR, COLUMN_MINUTE, COLUMN_ART_ID, COLUMN_LATITUDE, 
    	COLUMN_LONGITUDE, COLUMN_CONTACT, COLUMN_FAVORITE, COLUMN_USER_ADDED};
    
    public static final String CREATE_TABLE_STATEMENT = "create table " + TABLE_NAME + " ("+ COLUMN_ID +" integer primary key autoincrement, " 
	        +  COLUMN_NAME + " text, "+ COLUMN_DESCRIPTION + " text, " 
	        +  COLUMN_CIRCULAR_STREET + " text, " + COLUMN_TIME_ADDRESS + " text,"
	        +  COLUMN_DISTANCE + " real, " + COLUMN_HOUR + " integer,"
	        +  COLUMN_MINUTE + " integer, " + COLUMN_ARTIST_LOCATION + " text,"
	        +  COLUMN_ARTIST + " text, "+ COLUMN_URL +" text, " 
	        +  COLUMN_YEAR + " integer, " + COLUMN_ART_ID +" integer, " 
	        +  COLUMN_LATITUDE + " real, " + COLUMN_LONGITUDE +" real," 
	        +  COLUMN_FAVORITE + " integer default 0," + COLUMN_USER_ADDED + " integer default 0,"
	        +  COLUMN_CONTACT + " text,"
	        + "unique("+COLUMN_ART_ID+") on conflict replace);";

}
