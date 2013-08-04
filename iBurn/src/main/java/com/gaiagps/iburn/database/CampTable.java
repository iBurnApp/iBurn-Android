package com.gaiagps.iburn.database;

public class CampTable {
	
	//CAMP TABLE
    public static final String TABLE_NAME = "camps";
    
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_HOMETOWN = "hometown";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_CAMP_ID = "camp_id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_CONTACT = "contact";
    
    public static final String COLUMN_FAVORITE = "favorite";
    public static final String COLUMN_USER_ADDED = "user_added";
    
    public static final String[] COLUMNS = {COLUMN_ID, COLUMN_NAME, COLUMN_DESCRIPTION, COLUMN_HOMETOWN, COLUMN_URL,
    	COLUMN_YEAR, COLUMN_CAMP_ID, COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_CONTACT, COLUMN_LOCATION,
    	COLUMN_FAVORITE, COLUMN_USER_ADDED};
    
    public static final String CREATE_TABLE_STATEMENT = "create table " + TABLE_NAME + " ("+ COLUMN_ID +" integer primary key autoincrement, " 
	        +  COLUMN_NAME + " text, "+ COLUMN_DESCRIPTION + " text, " 
	        +  COLUMN_HOMETOWN + " text, "+ COLUMN_URL +" text, " 
	        +  COLUMN_YEAR + " integer, " + COLUMN_CAMP_ID +" integer, " 
	        +  COLUMN_LATITUDE + " real, " + COLUMN_LONGITUDE +" real," 
	        +  COLUMN_LOCATION + " text, " + COLUMN_CONTACT + " text,"
	        +  COLUMN_FAVORITE + " integer default 0," + COLUMN_USER_ADDED + " integer default 0,"
	        + "unique("+COLUMN_CAMP_ID+") on conflict replace);";

}
