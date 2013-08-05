package com.gaiagps.iburn.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.net.Uri;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


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
   
    //Comment this out to disable copying database from assets
    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
    	
    	boolean dbExist = checkDataBase();
    	Log.d("getWriteableDatabase","dbExist: " + String.valueOf(dbExist));
    	if(dbExist){
    		//do nothing - database already exist
    	}else{
 
    		//By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
        	this.getReadableDatabase();
        	Log.d("getReadableDatabase","going");
        	try {
 
    			copyDataBase();
 
    		} catch (IOException e) {
    			Log.d("getReadableDatabase","error copying");
        		throw new Error("Error copying database");
 
        	}
    	}
    	if(!dbReady && !sentReady)
    		sendDbReadyMessage(1);
    	return openDataBase(true);
    }
    
    private void sendDbReadyMessage(int result) { 
    	  Log.d("DBREADY","Sent");
	  	  Intent intent = new Intent("dbReady");
	  	  intent.putExtra("status", result);
	  	  LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
	}
    
    public static ContentValues cursorRowToContentValues(Cursor cursor){
    	ContentValues values = new ContentValues();
    	
    	if(cursor == null || !cursor.moveToFirst())
    		return values;
    	
    	AbstractWindowedCursor awc =
                (cursor instanceof AbstractWindowedCursor) ? (AbstractWindowedCursor) cursor : null;

        String[] columns = cursor.getColumnNames();
        int length = columns.length;
        for (int i = 0; i < length; i++) {
        	//Log.d("cursorRowToContentValues",columns[i] + " null: " + String.valueOf(cursor.isNull(i)));
        	if(cursor.isNull(i)){
        		// Don't insert null table records into ContentValues
        		continue;
        	}
        	else{
	            if (awc != null && awc.isBlob(i)) {
	                values.put(columns[i], cursor.getBlob(i));
	            } else {
	                values.put(columns[i], cursor.getString(i));
	            }
        	}
        }
        
    	return values;
    }
    
    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     */
    
    private void copyDataBase() throws IOException{
        if(!COPY_DB)
            return;
    	Log.d("CopyDataBase","Copying...");
    	//Open your local db as the input stream
        // TODO: Bundled DB will be stored in res/raw
        //InputStream myInput = c.getResources().openRawResource(R.raw.dbId);
    	InputStream myInput = c.getAssets().open(DATABASE_PATH + DATABASE_NAME);
 
    	// Path to the just created empty db
    	//String outFileName = DATABASE_DESTINATION_PATH + DATABASE_NAME;
 
    	//Open the empty db as the output stream
    	OutputStream myOutput = new FileOutputStream(DATABASE_DESTINATION_PATH);
 
    	//transfer bytes from the inputfile to the outputfile
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = myInput.read(buffer))>0){
    		myOutput.write(buffer, 0, length);
    	}
 
    	//Close the streams
    	myOutput.flush();
    	myOutput.close();
    	myInput.close();
 
    }
    
    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){
 
    	SQLiteDatabase checkDB = null;
 
    	try{
    		Log.i("DB-Dir:", DATABASE_DESTINATION_PATH);
    		File database_dir = new File(DATABASE_DESTINATION_PATH);
    		if(!database_dir.exists()){
    			database_dir.mkdir();
    			return false;
    		}
    		
    		//String myPath = DATABASE_DESTINATION_PATH + DATABASE_NAME;
    		checkDB = SQLiteDatabase.openDatabase(DATABASE_DESTINATION_PATH, null, SQLiteDatabase.OPEN_READONLY);
 
    	}catch(SQLiteException e){
 
    		//database does't exist yet post SDK 11
 
    	}
 
    	if(checkDB != null){
 
    		checkDB.close();
 
    	}
 
    	return checkDB != null ? true : false;
    }
    
    public SQLiteDatabase openDataBase(boolean write) throws SQLException{
    	 
    	//Open the copied database
        //String myPath = DATABASE_DESTINATION_PATH + DATABASE_NAME;
        if (write)
        	return SQLiteDatabase.openDatabase(DATABASE_DESTINATION_PATH, null, SQLiteDatabase.OPEN_READWRITE);
        else
        	return SQLiteDatabase.openDatabase(DATABASE_DESTINATION_PATH, null, SQLiteDatabase.OPEN_READONLY);
 
    }
    
    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{
 
    	boolean dbExist = checkDataBase();
 
    	if(dbExist){
    		//do nothing - database already exist
    	}else{
 
    		//By calling this method and empty database will be created into the default system path
               //of your application so we are gonna be able to overwrite that database with our database.
        	this.getReadableDatabase();
 
        	try {
 
    			copyDataBase();
 
    		} catch (IOException e) {
 
        		throw new Error("Error copying database");
 
        	}
    	}
 
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