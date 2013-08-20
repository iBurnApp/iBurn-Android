package com.gaiagps.iburn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;


/**
 * 
 * @author davidbrodsky
 * @description SQLiteWrapper is written to be application agnostic.
 * requires Strings: DATABASE_NAME, DATABASE_VERSION,
 * CREATE_TABLE_STATEMENT, TABLE_NAME
 */
public class DBWrapper extends SQLiteOpenHelper {

    public static boolean COPY_DB = false; // Should bundled db be copied on first launch?
	
	//DATABASE INFO
    public static final String DATABASE_NAME = "iburn.db";
    // database path in /assets
    public static final String DATABASE_PATH = "db/";
    
    public static final int DATABASE_VERSION = 1;
    
    // typically /data/data/<namespace>/databases
    private static String DATABASE_DESTINATION_PATH;
    
    private SQLiteDatabase mDB; 
    
    private boolean sentReady = false;
    private boolean copying = false; // don't allow copying to be called more than once
    public static boolean dbReady = false;

    static Context c;
    
    //TABLE INFO
    // public static final String CREATE_TABLE_STATEMENT = CampTable.CREATE_TABLE_STATEMENT;
    // public static final String TABLE_NAME = CampTable.TABLE_NAME;

    //Schema: Number, Name, Datetime [YYYYMMDDKKMMSS]
    /**
     * Constructor
     * @param context the application context
     */
    public DBWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        DATABASE_DESTINATION_PATH = context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
        c = context;
    }
    
    /**
     * Called at the time to create the DB.
     * The create DB statement
     * @param db SQLite DB
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CampTable.CREATE_TABLE_STATEMENT);
        db.execSQL(EventTable.CREATE_TABLE_STATEMENT);
        db.execSQL(ArtTable.CREATE_TABLE_STATEMENT);
        Log.d("DBWrapper","Creating DB");
    }
    
    /**
     * Invoked if a DB upgrade (version change) has been detected
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, 
       int oldVersion, int newVersion) {
        // Drop old table and re-create
    	db.execSQL("DROP TABLE IF EXISTS " + CampTable.TABLE_NAME);
    	db.execSQL("DROP TABLE IF EXISTS " + EventTable.TABLE_NAME);
    	db.execSQL("DROP TABLE IF EXISTS " + ArtTable.TABLE_NAME);
		onCreate(db);
    }

    public static int insertContentValuesToTable(ArrayList<ContentValues> cv, Uri uri){
        if(c == null)
            return 0;
        int size = cv.size();
        int result = 0;
        ContentValues[] cvList = new ContentValues[1];
        cvList = cv.toArray(cvList); // toArray requires an initialized array of type equal to desired result :/
        result = c.getContentResolver().bulkInsert(uri, cvList);
        return result;
    }

}