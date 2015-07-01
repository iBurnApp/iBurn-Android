package com.gaiagps.iburn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gaiagps.iburn.Constants;
import com.squareup.sqlbrite.SqlBrite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Class for interaction with our database via Reactive streams.
 * This is intended as an experiment to replace our use of {@link android.content.ContentProvider}
 * as it does not meet all of our needs (e.g: Complex UNION queries not possible with Schematic's
 * generated version, and I believe manually writing a ContentProvider is too burdensome and error-prone)
 * <p>
 * Created by davidbrodsky on 6/22/15.
 */
public class DataProvider {

    /**
     * Computed column indicating type for queries that union results across tables
     */
    public static String VirtualType = "type";

    private static String makeProjectionString(String[] projection) {
        StringBuilder builder = new StringBuilder();
        for (String column : projection) {
            builder.append(column);
            builder.append(',');
        }
        // Remove the last comma
        return builder.substring(0, builder.length() - 1);
    }

    private static DataProvider provider;

    private SqlBrite db;
    private final AtomicBoolean upgradeLock = new AtomicBoolean(false);

    public static DataProvider getInstance(@NonNull Context context) {
        if (provider == null) {
            com.gaiagps.iburn.database.generated.PlayaDatabase db = com.gaiagps.iburn.database.generated.PlayaDatabase.getInstance(context);
            SqlBrite sqlBrite = SqlBrite.create(db);
            sqlBrite.setLoggingEnabled(true);
            sqlBrite.setLogger(message -> Timber.d(message));
            provider = new DataProvider(SqlBrite.create(db));
        }

        return provider;
    }

    public static SqlBrite getSqlBriteInstance(@NonNull Context context) {
        return getInstance(context).getDb();
    }

    private DataProvider(SqlBrite db) {
        this.db = db;
    }

    public SqlBrite getDb() {
        return db;
    }

    public void beginUpgrade() {
        upgradeLock.set(true);
    }

    public void endUpgrade() {
        upgradeLock.set(false);

        // Trigger all SqlBrite observers via reflection (uses private method)
        try {
            Method method = db.getClass().getDeclaredMethod("sendTableTrigger", Set.class);
            method.setAccessible(true);
            method.invoke(db, new HashSet<>(PlayaDatabase.ALL_TABLES));
        } catch (SecurityException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Timber.w(e, "Failed to notify observers on endUpgrade");
        }
    }

    public Observable<SqlBrite.Query> observeTable(@NonNull String table,
                                                   @Nullable String[] projection) {
        return db.createQuery(table,
                "SELECT " + (projection == null ? "*" : makeProjectionString(projection)) + " FROM " + table)
                .subscribeOn(Schedulers.io())
                .skipWhile(query -> upgradeLock.get());
    }

    public Observable<SqlBrite.Query> observeEventsOnDayOfTypes(@Nullable String day,
                                                                @Nullable ArrayList<String> types,
                                                                @Nullable String[] projection) {

        List<String> args = new ArrayList<>((types == null ? 0 : types.size()) + (day == null ? 0 : 1));
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(projection == null ? "*" : makeProjectionString(projection));
        sql.append(" FROM ");
        sql.append(PlayaDatabase.EVENTS);

        if (day != null || (types != null && types.size() > 0))
            sql.append(" WHERE ");

        if (types != null) {
            for (int x = 0; x < types.size(); x++) {
                sql.append('(')
                        .append(EventTable.eventType)
                        .append("= ?)");

                args.add(types.get(x));

                if (x < types.size() - 1) sql.append(" OR ");
            }
        }

        if (day != null) {
            if (types != null && types.size() > 0) sql.append(" AND ");

            sql.append(EventTable.startTimePrint)
                    .append(" LIKE ")
                    .append("'%?%'");
            args.add(day);
        }

        sql.append(" ORDER BY ");
        sql.append(EventTable.startTime);
        sql.append(" ASC");

        Timber.d("Event filter query " + sql.toString());
        return db.createQuery(PlayaDatabase.EVENTS, sql.toString(), args.toArray(new String[args.size()]))
                .subscribeOn(Schedulers.io())
                .skipWhile(query -> upgradeLock.get());
    }

    public Observable<SqlBrite.Query> observeFavorites(@Nullable String[] projection) {

        StringBuilder sql = new StringBuilder();
        int tableIdx = 0;
        for (String table : PlayaDatabase.ALL_TABLES) {
            tableIdx++;

            sql.append("SELECT ")
                    .append(projection == null ? "*" : makeProjectionString(projection))
                    .append(", ")
                    .append(tableIdx)
                    .append(" as ")
                    .append(VirtualType)
                    .append(" FROM ")
                    .append(table)
                    .append(" WHERE ")
                    .append(PlayaItemTable.favorite)
                    .append(" = 1 ");

            if (tableIdx < PlayaDatabase.ALL_TABLES.size())
                sql.append(" UNION ");
        }

        return db.createQuery(PlayaDatabase.ALL_TABLES, sql.toString())
                .subscribeOn(Schedulers.io())
                .skipWhile(query -> upgradeLock.get());
    }

    public Observable<SqlBrite.Query> observeQuery(@NonNull String query,
                                                   @Nullable String[] projection) {

        query = DatabaseUtils.sqlEscapeString(query);

        StringBuilder sql = new StringBuilder();
        int tableIdx = 0;
        for (String table : PlayaDatabase.ALL_TABLES) {
            tableIdx++;

            sql.append("SELECT ")
                    .append(projection == null ? "*" : makeProjectionString(projection))
                    .append(", ")
                    .append(tableIdx)
                    .append(" as ")
                    .append(VirtualType)
                    .append(" FROM ")
                    .append(table)
                    .append(" WHERE ")
                    .append(PlayaItemTable.name)
                    .append(" LIKE '%?%'");

            if (tableIdx < PlayaDatabase.ALL_TABLES.size())
                sql.append(" UNION ");
        }

        return db.createQuery(PlayaDatabase.ALL_TABLES, sql.toString(), query)
                .subscribeOn(Schedulers.io())
                .skipWhile(queryResp -> upgradeLock.get());
    }

    public void updateFavorite(@NonNull String table, int id, boolean isFavorite) {
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
