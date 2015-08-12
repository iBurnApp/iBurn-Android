package com.gaiagps.iburn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.PrefsHelper;
import com.squareup.sqlbrite.SqlBrite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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

    public interface QueryInterceptor {
        String onQueryIntercepted(@NonNull String query, @NonNull Iterable<String> tables);
    }

    /**
     * Computed column indicating type for queries that union results across tables
     */
    public static final String VirtualType = "type";

    /**
     * Version of database schema
     */
    public static final long BUNDLED_DATABASE_VERSION = 1;

    /**
     * Version of database data
     */
    public static final long RESOURCES_VERSION = 1438560071000L; // Unix time of creation

    private static final boolean USE_BUNDLED_DB = true;

    private static DataProvider provider;

    private SqlBrite db;
    private QueryInterceptor interceptor;
    private final AtomicBoolean upgradeLock = new AtomicBoolean(false);

    public static Observable<DataProvider> getInstance(@NonNull Context context) {

        if (provider != null) return Observable.just(provider);

        final PrefsHelper prefs = new PrefsHelper(context);

        SQLiteOpenHelper openHelper = USE_BUNDLED_DB ? new DBWrapper(context) : com.gaiagps.iburn.database.generated.PlayaDatabase.getInstance(context);

        return Observable.just(openHelper)
                .subscribeOn(Schedulers.io())
                .doOnNext(database -> {
                    prefs.setDatabaseVersion(BUNDLED_DATABASE_VERSION);
                    prefs.setBaseResourcesVersion(RESOURCES_VERSION);
                })
                .map(SqlBrite::create)
//                .doOnNext(sqlBrite1 -> sqlBrite1.setLoggingEnabled(true))
                .map(sqlBrite -> new DataProvider(sqlBrite, new Embargo(prefs)))
                .doOnNext(dataProvider -> provider = dataProvider);
    }

    public static String makeProjectionString(String[] projection) {
        StringBuilder builder = new StringBuilder();
        for (String column : projection) {
            builder.append(column);
            builder.append(',');
        }
        // Remove the last comma
        return builder.substring(0, builder.length() - 1);
    }

    private DataProvider(SqlBrite db, @Nullable QueryInterceptor interceptor) {
        this.db = db;
        this.interceptor = interceptor;
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

    public Observable<SqlBrite.Query> createQuery(@NonNull final String table, @NonNull String sql, @NonNull String... args) {
        return db.createQuery(table, interceptQuery(sql, table), args);
    }

    public Observable<SqlBrite.Query> createQuery(@NonNull final Iterable<String> tables, @NonNull String sql, @NonNull String... args) {
        return db.createQuery(tables, interceptQuery(sql, tables), args);
    }

    public int delete(@NonNull String table, @Nullable String whereClause, @Nullable String... whereArgs) {
        return db.delete(table, whereClause, whereArgs);
    }

    public int update(@NonNull String table, @NonNull ContentValues values, @Nullable String whereClause, @Nullable String... whereArgs) {
        return db.update(table, values, whereClause, whereArgs);
    }

    public void beginTransaction() {
        db.beginTransaction();
    }

    public void setTransactionSuccessful() {
        db.setTransactionSuccessful();
    }

    public void endTransaction() {
        db.endTransaction();
    }

    public long insert(@NonNull String table, @NonNull ContentValues values) {
        return db.insert(table, values);
    }

    public Observable<SqlBrite.Query> observeTable(@NonNull String table,
                                                   @Nullable String[] projection) {
        return db.createQuery(table,
                interceptQuery("SELECT " + (projection == null ? "*" : makeProjectionString(projection)) + " FROM " + table, table))
                .subscribeOn(Schedulers.computation())
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
                    .append("?");
            args.add('%' + day + '%');
        }

        sql.append(" ORDER BY ");
        sql.append(EventTable.startTime);
        sql.append(" ASC");

        Timber.d("Event filter query " + sql.toString());
        return db.createQuery(PlayaDatabase.EVENTS, interceptQuery(sql.toString(), PlayaDatabase.EVENTS), args.toArray(new String[args.size()]))
                .subscribeOn(Schedulers.computation())
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

        sql.append(" ORDER BY ")
                .append(VirtualType);

        return db.createQuery(PlayaDatabase.ALL_TABLES, interceptQuery(sql.toString(), PlayaDatabase.ALL_TABLES))
                .subscribeOn(Schedulers.computation())
                .skipWhile(query -> upgradeLock.get());
    }

    public Observable<SqlBrite.Query> observeNameQuery(@NonNull String query,
                                                       @Nullable String[] projection) {

        query = '%' + query + '%';
        StringBuilder sql = new StringBuilder();
        int tableIdx = 0;
        String[] params = new String[PlayaDatabase.ALL_TABLES.size()];
        for (String table : PlayaDatabase.ALL_TABLES) {
            params[tableIdx] = query;
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
                    .append(" LIKE ?")
                    .append(" GROUP BY ")
                    .append(PlayaItemTable.name);

            if (tableIdx < PlayaDatabase.ALL_TABLES.size())
                sql.append(" UNION ");
        }

        sql.append(" ORDER BY ")
                .append(VirtualType);

        return db.createQuery(PlayaDatabase.ALL_TABLES, interceptQuery(sql.toString(), PlayaDatabase.ALL_TABLES), params)
                .subscribeOn(Schedulers.computation())
                .skipWhile(queryResp -> upgradeLock.get());
    }

    public Observable<SqlBrite.Query> observeAllTables(@NonNull String whereClause,
                                                       @Nullable String[] projection) {

        whereClause = DatabaseUtils.sqlEscapeString(whereClause);

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
                    .append(whereClause);

            if (tableIdx < PlayaDatabase.ALL_TABLES.size())
                sql.append(" UNION ");
        }

        return db.createQuery(PlayaDatabase.ALL_TABLES, interceptQuery(sql.toString(), PlayaDatabase.ALL_TABLES))
                .subscribeOn(Schedulers.computation())
                .skipWhile(queryResp -> upgradeLock.get());
    }

    public void updateFavorite(@NonNull String table, int id, boolean isFavorite) {
        ContentValues values = new ContentValues(1);
        values.put(PlayaItemTable.favorite, isFavorite ? 1 : 0);
        db.update(table, values, PlayaItemTable.id + "=?", String.valueOf(id));
    }

    public void toggleFavorite(@NonNull String table, int id) {
        db.createQuery(table, "SELECT " + PlayaItemTable.favorite + " FROM " + table + " WHERE " + PlayaItemTable.id + " =?", String.valueOf(id))
                .first()
                .map(SqlBrite.Query::run)
                .map(cursor -> {
                    if (cursor != null && cursor.moveToFirst()) {
                        return cursor.getInt(cursor.getColumnIndex(PlayaItemTable.favorite)) == 1;
                    }
                    throw new IllegalStateException(String.format("No row in %s with id %d exists", table, id));
                })
                .subscribe(isFavorite -> updateFavorite(table, id, !isFavorite),
                        throwable -> Timber.e(throwable, throwable.getMessage()));
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

    private String interceptQuery(String query, String table) {
        return interceptQuery(query, Collections.singleton(table));
    }

    private String interceptQuery(String query, Iterable<String> tables) {
        if (interceptor == null) return query;
        return interceptor.onQueryIntercepted(query, tables);
    }
}
