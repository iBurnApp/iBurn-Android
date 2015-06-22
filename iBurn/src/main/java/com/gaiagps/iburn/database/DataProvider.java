package com.gaiagps.iburn.database;

import android.content.ContentValues;
import android.content.Context;

import com.gaiagps.iburn.Constants;
import com.squareup.sqlbrite.SqlBrite;

import rx.Observable;
import timber.log.Timber;

/**
 * Class for interaction with our database via Reactive streams.
 * This is intended as an experiment to replace our use of {@link android.content.ContentProvider}
 * as it does not meet all of our needs (e.g: Complex UNION queries not possible with Schematic's
 * generated version, and I believe manually writing a ContentProvider is too burdensome and error-prone)
 *
 * Created by davidbrodsky on 6/22/15.
 */
public class DataProvider {

    /** Computed column indicating type for queries that union results across tables */
    public static String VirtualType = "type";

    static final String[] Projection = new String[]{
            PlayaItemTable.id,
            PlayaItemTable.name,
            PlayaItemTable.latitude,
            PlayaItemTable.longitude
    };

    static String generatedProjectionString;

    static {
        StringBuilder builder = new StringBuilder();
        for (String column : Projection) {
            builder.append(column);
            builder.append(',');
        }
        // Remove the last comma
        generatedProjectionString = builder.substring(0, builder.length() - 1);
    }

    private static DataProvider provider;

    private SqlBrite db;

    public static DataProvider getInstance(Context context) {
        if (provider == null) {
            com.gaiagps.iburn.database.generated.PlayaDatabase db = com.gaiagps.iburn.database.generated.PlayaDatabase.getInstance(context);
            provider = new DataProvider(SqlBrite.create(db));
        }

        return provider;
    }

    public static SqlBrite getSqlBriteInstance(Context context) {
        return getInstance(context).getDb();
    }

    private DataProvider(SqlBrite db) {
        this.db = db;
    }

    public SqlBrite getDb() {
        return db;
    }

    public Observable<SqlBrite.Query> observeTable(String table) {
        return db.createQuery(table, "SELECT * FROM " + table);
    }

    public Observable<SqlBrite.Query> observeFavorites() {

        String sql =
                "SELECT " + generatedProjectionString + ", 1 as " + VirtualType + " FROM " + PlayaDatabase.CAMPS + " WHERE " + PlayaItemTable.favorite + " = 1 " +
                "UNION " +
                "SELECT " + generatedProjectionString + ", 2 as " + VirtualType + " FROM " + PlayaDatabase.ART + " WHERE " + PlayaItemTable.favorite + " = 1 " +
                "UNION " +
                "SELECT " + generatedProjectionString + ", 3 as " + VirtualType + " FROM " + PlayaDatabase.EVENTS + " WHERE " + PlayaItemTable.favorite + " = 1 " +
                "UNION " +
                "SELECT " + generatedProjectionString + ", 4 as " + VirtualType + " FROM " + PlayaDatabase.POIS + " WHERE " + PlayaItemTable.favorite + " = 1 ";

        return db.createQuery(PlayaDatabase.ALL_TABLES, sql, null);
    }

    public Observable<SqlBrite.Query> observeQuery(String query) {
        String sql =
                "SELECT " + generatedProjectionString + ", 1 as type FROM " + PlayaDatabase.CAMPS + " WHERE name LIKE '%" + query + "%'" +
                "UNION " +
                "SELECT " + generatedProjectionString + ", 2 as type FROM " + PlayaDatabase.ART + " WHERE name LIKE '%" + query + "%' " +
                "UNION " +
                "SELECT " + generatedProjectionString + ", 3 as type FROM " + PlayaDatabase.EVENTS + " WHERE name LIKE '%" + query + "%' " +
                "UNION " +
                "SELECT " + generatedProjectionString + ", 4 as type FROM " + PlayaDatabase.POIS + " WHERE name LIKE '%" + query + "%' ";


        return db.createQuery(PlayaDatabase.ALL_TABLES, sql, null);
    }

    public void updateFavorite(String table, int id, boolean isFavorite) {
        ContentValues values = new ContentValues(1);
        values.put(PlayaItemTable.favorite, isFavorite ? 1 : 0);
        db.update(table, values, PlayaItemTable.id + "=?", String.valueOf(id));
    }

    /**
     * @return the int value used in virtual columns to represent a {@link com.gaiagps.iburn.Constants.PlayaItemType}
     */
    public static int getTypeValue(Constants.PlayaItemType type) {
        switch (type) {
            case CAMP:
                return 1;
            case ART:
                return 2;
            case EVENT:
                return 3;
            case POI:
                return 4;
        }
        Timber.w("Unknown PlayaItemType");
        return -1;
    }

    public static Constants.PlayaItemType getTypeValue(int type) {
        switch (type) {
            case 1:
                return Constants.PlayaItemType.CAMP;
            case 2:
                return Constants.PlayaItemType.ART;
            case 3:
                return Constants.PlayaItemType.EVENT;
            case 4:
                return Constants.PlayaItemType.POI;
        }
        throw new IllegalArgumentException("Invalid type value");
    }
}
