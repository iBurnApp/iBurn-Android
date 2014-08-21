package com.gaiagps.iburn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.gaiagps.iburn.PlayaClient;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;


/**
 *
 * This class handles bootstrapping the application database
 * from a pre-populated SQL database stored in assets.
 *
 * To test bootstrapping the application database from JSON, comment
 * out the entirety of this class.
 *
 * @author davidbrodsky
 * @description SQLiteWrapper is written to be application agnostic.
 * requires Strings: DATABASE_NAME, DATABASE_VERSION,
 * CREATE_TABLE_STATEMENT, TABLE_NAME
 */
public class DBWrapper extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "playaDatabase.db";
    private static final int DATABASE_VERSION = PlayaClient.DATABASE_VERSION;

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

        if (oldVersion == 2 && newVersion == 3) {
            // Add UserPoiTable
            // Restore all favorites from camps, art, events
            ArrayList<Integer> favoriteArtPlayaIds          = new ArrayList<>();
            recordAllFavoriteIds(db, PlayaDatabase.ART,     favoriteArtPlayaIds);

            ArrayList<Integer> favoriteCampPlayaIds         = new ArrayList<>();
            recordAllFavoriteIds(db, PlayaDatabase.CAMPS,   favoriteCampPlayaIds);

            ArrayList<Integer> favoriteEventPlayaIds        = new ArrayList<>();
            recordAllFavoriteIds(db, PlayaDatabase.EVENTS,  favoriteEventPlayaIds);

            recreateAllTables(db);

            db.beginTransaction();
            commitAllFavorites(db, PlayaDatabase.ART,       favoriteArtPlayaIds);
            commitAllFavorites(db, PlayaDatabase.CAMPS,     favoriteCampPlayaIds);
            commitAllFavorites(db, PlayaDatabase.EVENTS,    favoriteEventPlayaIds);
            db.endTransaction();
        } else {
            recreateAllTables(db);
        }

    }

    private void recreateAllTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PlayaDatabase.CAMPS);
        db.execSQL("DROP TABLE IF EXISTS " + PlayaDatabase.EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + PlayaDatabase.ART);
        onCreate(db);
    }

    private void recordAllFavoriteIds(SQLiteDatabase db, String table, ArrayList<Integer> favoritePlayaIds) {
        Cursor favoriteArt = db.query(table, new String[] { PlayaItemTable.playaId }, PlayaItemTable.favorite + " = ?", new String[] {"1"}, null, null, null);
        if (favoriteArt != null) {
            while (favoriteArt.moveToNext()) {
                favoritePlayaIds.add(favoriteArt.getInt(favoriteArt.getColumnIndex(PlayaItemTable.playaId)));
            }
            favoriteArt.close();
        }
    }

    private void commitAllFavorites(SQLiteDatabase db, String table, ArrayList<Integer> favoritePlayaIds) {
        ContentValues values = new ContentValues();
        values.put(PlayaItemTable.favorite, 1);
        for (Integer favoritePlayaId : favoritePlayaIds) {
            db.update(table, values, PlayaItemTable.playaId + " = ?", new String[] { String.valueOf(favoritePlayaId)});
        }
    }
}