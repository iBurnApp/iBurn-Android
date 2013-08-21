package com.gaiagps.iburn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;


/**
 * 
 * @author davidbrodsky
 * @description SQLiteWrapper is written to be application agnostic.
 * requires Strings: DATABASE_NAME, DATABASE_VERSION,
 * CREATE_TABLE_STATEMENT, TABLE_NAME
 */
public class DBWrapper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "iburn";
    private static final int DATABASE_VERSION = 1;

    static Context c;

    public DBWrapper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        c = context;
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
        c.getContentResolver().notifyChange(uri, null);
        return result;
    }

}