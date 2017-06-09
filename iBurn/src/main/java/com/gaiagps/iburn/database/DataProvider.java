package com.gaiagps.iburn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.PrefsHelper;

import org.intellij.lang.annotations.Flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
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
    public static final String VirtualType = "vtype";

    /**
     * Version of database schema
     */
    public static final long BUNDLED_DATABASE_VERSION = 1;

    /**
     * Version of database data and mbtiles. This is basically the unix time at which bundled data was provided to this build.
     */
    public static final long RESOURCES_VERSION = 0; //1472093065000L; // Unix time of creation

    /**
     * If true, use a bundled pre-populated database (see {@link DBWrapper}. Else start with a fresh database.
     */
    private static final boolean USE_BUNDLED_DB = true;

    private static DataProvider provider;

    private AppDatabase db;
    private QueryInterceptor interceptor;
    private final AtomicBoolean upgradeLock = new AtomicBoolean(false);

//    private ArrayDeque<BriteDatabase.Transaction> transactionStack = new ArrayDeque<>();

    public static Observable<DataProvider> getInstance(@NonNull Context context) {

        // TODO : This ain't thread safe

        if (provider != null) return Observable.just(provider);

        final PrefsHelper prefs = new PrefsHelper(context);

        // TODO : How to use bundled DB?
//        SQLiteOpenHelper openHelper = USE_BUNDLED_DB ? new DBWrapper(context) : com.gaiagps.iburn.database.generated.PlayaDatabase.getInstance(context);


        return Observable.just(PlayaDatabase2Kt.getSharedDb(context))
                .subscribeOn(Schedulers.io())
                .doOnNext(database -> {
                    prefs.setDatabaseVersion(BUNDLED_DATABASE_VERSION);
                    prefs.setBaseResourcesVersion(RESOURCES_VERSION);
                })
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

    private DataProvider(AppDatabase db, @Nullable QueryInterceptor interceptor) {
        this.db = db;
        this.interceptor = interceptor;
    }

    public void beginUpgrade() {
        upgradeLock.set(true);
    }

    public void endUpgrade() {
        upgradeLock.set(false);

        // TODO : Trigger Room observers
        // Trigger all SqlBrite observers via reflection (uses private method)
//        try {
//            Method method = db.getClass().getDeclaredMethod("sendTableTrigger", Set.class);
//            method.setAccessible(true);
//            method.invoke(db, new HashSet<>(PlayaDatabase.ALL_TABLES));
//        } catch (SecurityException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
//            Timber.w(e, "Failed to notify observers on endUpgrade");
//        }
    }

    public int deleteCamps() {
        return clearTable(Camp.TABLE_NAME);
//        return db.getOpenHelper().getWritableDatabase().delete(Camp.TABLE_NAME, "*", null);
//        Cursor result = db.query("DELETE FROM camp; VACUUM", null);
//        if (result != null) result.close();
    }

    private int clearTable(String tablename) {
        return db.getOpenHelper().getWritableDatabase().delete(tablename, null, null);
    }

    public Flowable<List<Camp>> observeCamps() {
        return db.campDao().getAll();
    }

    public Flowable<List<Camp>> observeCampFavorites() {

        // TODO : Honor upgradeLock?
        return db.campDao().getFavorites();
    }

    public Flowable<List<Camp>> observeCampsByName(@NonNull String query) {

        // TODO : Honor upgradeLock
        // TODO : Return structure with metadata on how many art, camps, events etc?
//        return db.campDao().findByName(query);
        return Flowable.empty();
    }

    // TODO : Replace with Table-specific queries
//    public Observable<SqlBrite.Query> createEmbargoExemptQuery(@NonNull final String table, @NonNull String sql, @NonNull String... args) {
//        return db.createQuery(table, sql, args);
//    }
//
//    public Observable<SqlBrite.Query> createQuery(@NonNull final String table, @NonNull String sql, @NonNull String... args) {
//        return db.createQuery(table, interceptQuery(sql, table), args);
//    }
//
//    public Observable<SqlBrite.Query> createQuery(@NonNull final Iterable<String> tables, @NonNull String sql, @NonNull String... args) {
//        return db.createQuery(tables, interceptQuery(sql, tables), args);
//    }
//
//    public int delete(@NonNull String table, @Nullable String whereClause, @Nullable String... whereArgs) {
//        return db.delete(table, whereClause, whereArgs);
//    }
//
//    public int update(@NonNull String table, @NonNull ContentValues values, @Nullable String whereClause, @Nullable String... whereArgs) {
//        return db.update(table, values, whereClause, whereArgs);
//    }

    public void beginTransaction() {
        db.beginTransaction();
//        BriteDatabase.Transaction t = db.newTransaction();
//        transactionStack.push(t);
    }

    public void setTransactionSuccessful() {
        if (!db.inTransaction()) {
            return;
        }

        db.setTransactionSuccessful();
    }

    public void endTransaction() {
        if (!db.inTransaction()) {
            return;
        }

        // TODO: Don't allow this call to proceed without prior call to beginTransaction
        db.endTransaction();
    }

    public void insert(@NonNull String table, @NonNull ContentValues values) {
        db.getOpenHelper().getWritableDatabase().insert(table, 0, values); // TODO : wtf is the int here?
    }

    public int delete(@NonNull String table) {
        switch (table) {
            case Camp.TABLE_NAME:
                return deleteCamps();
            case Art.TABLE_NAME:
                return deleteArt();
            case Event.TABLE_NAME:
                return deleteEvents();
            default:
                Timber.w("Cannot clear unknown table name '%s'", table);
        }
        return 0;
    }
//
//    public long insert(@NonNull String table, @NonNull ContentValues values) {
//        return db.insert(table, values);
//    }

//    public Observable<SqlBrite.Query> observeTable(@NonNull String table,
//                                                   @Nullable String[] projection) {
//
//        return observeTable(table, projection, null);
//    }
//
//    public Observable<SqlBrite.Query> observeTable(@NonNull String table,
//                                                   @Nullable String[] projection,
//                                                   @Nullable String whereClause) {
//
//        String sql = interceptQuery("SELECT " + (projection == null ? "*" : makeProjectionString(projection)) + " FROM " + table, table);
//
//        if (whereClause != null) {
//            sql += " WHERE " + whereClause;
//        }
//
//        if (table.equals(PlayaDatabase.EVENTS)) {
//            sql += " ORDER BY " + EventTable.startTime + " ASC";
//        } else {
//            sql += " ORDER BY " + PlayaItemTable.name + " ASC";
//        }
//
//        return db.createQuery(table, sql)
//                .subscribeOn(Schedulers.computation())
//                .skipWhile(query -> upgradeLock.get());
//
//    }

    public int deleteEvents() {
        return clearTable(Event.TABLE_NAME);

//        return db.getOpenHelper().getWritableDatabase().delete(Event.TABLE_NAME, "*", null);
//        Cursor result = db.query("DELETE FROM event; VACUUM", null);
//        if (result != null) result.close();
    }

    public Flowable<List<Event>> observeEventsOnDayOfTypes(@NonNull String day,
                                                             @Nullable ArrayList<String> types) {

        // TODO : Honor upgradeLock?
        if (types == null) {
            return db.eventDao().findByDay(day);
        } else {
            return db.eventDao().findByDayAndType(day, types);
        }
    }

    public Flowable<List<Event>> observeEventFavorites() {

        // TODO : Honor upgradeLock?
        return db.eventDao().getFavorites();
    }

    public int deleteArt() {
        return clearTable(Art.TABLE_NAME);
//        return db.getOpenHelper().getWritableDatabase().delete(Art.TABLE_NAME, null, null);
//        Cursor result = db.query("DELETE FROM art; VACUUM", null);
//        if (result != null) result.close();
    }

    public Flowable<List<Art>> observeArt() {

        // TODO : Honor upgradeLock?
        return db.artDao().getAll();
    }

    public Flowable<List<Art>> observeArtFavorites() {

        // TODO : Honor upgradeLock?
        return db.artDao().getFavorites();
    }

    public Flowable<List<Art>> observeArtWithAudioTour() {

        // TODO : Honor upgradeLock?
        return db.artDao().getAllWithAudioTour();
    }

    /**
     * Observe all favorites.
     * <p>
     * Note: This query automatically adds in Event.startTime (and 0 values for all non-events),
     * since we always want to show this data for an event.
     */
    public Flowable<List<PlayaItem>> observeFavorites() {

        // TODO : Honor upgradeLock
        // TODO : Return structure with metadata on how many art, camps, events etc?
        return Flowable.zip(
                db.artDao().getFavorites(),
                db.campDao().getFavorites(),
                db.eventDao().getFavorites(),
                (arts, camps, events) -> {
                    ArrayList<PlayaItem> all = new ArrayList<>(arts.size() + camps.size() + events.size());
                    all.addAll(arts);
                    all.addAll(camps);
                    all.addAll(events);
                    return all;
                }
        );
    }

    /**
     * Observe all results for a name query.
     * <p>
     * Note: This query automatically adds in Event.startTime (and 0 values for all non-events),
     * since we always want to show this data for an event.
     */
    public Flowable<List<PlayaItem>> observeNameQuery(@NonNull String query) {

        // TODO : Honor upgradeLock
        // TODO : Return structure with metadata on how many art, camps, events etc?
//        return Flowable.zip(
//                db.artDao().findByName(query),
//                db.campDao().findByName(query),
//                db.eventDao().findByName(query),
//                (arts, camps, events) -> {
//                    ArrayList<PlayaItem> all = new ArrayList<>(arts.size() + camps.size() + events.size());
//                    all.addAll(arts);
//                    all.addAll(camps);
//                    all.addAll(events);
//                    return all;
//                }
//        );
        return Flowable.empty();
    }

    private void update(@NonNull PlayaItem item) {
        if (item instanceof Art) {
            db.artDao().update((Art) item);
        } else if (item instanceof Event) {
            db.eventDao().update((Event) item);
        } else if (item instanceof Camp) {
            db.campDao().update((Camp) item);
        } else {
            Timber.e("Cannot update item of unknown type");
        }
    }

    public void toggleFavorite(@NonNull PlayaItem item) {
        String newFavoriteVal = item.isFavorite ? "0" : "1";
        String tableName = item.getClass().getSimpleName().toLowerCase();
        Cursor cursor = db.query("UPDATE " + tableName + " SET favorite = ? WHERE id = " + item.id, new String[]{newFavoriteVal});
        if (cursor != null) cursor.close();
    }

//    public void toggleFavorite(@NonNull final String table, int id) {
//        db.createQuery(table, "SELECT " + PlayaItemTable.favorite + " FROM " + table + " WHERE " + PlayaItemTable.id + " =?", String.valueOf(id))
//                .first()
//                .map(SqlBrite.Query::run)
//                .map(cursor -> {
//                    if (cursor != null && cursor.moveToFirst()) {
//                        boolean isFavorite = cursor.getInt(cursor.getColumnIndex(PlayaItemTable.favorite)) == 1;
//                        cursor.close();
//                        return isFavorite;
//                    }
//                    throw new IllegalStateException(String.format("No row in %s with id %d exists", table, id));
//                })
//                .subscribe(isFavorite -> updateFavorite(table, id, !isFavorite),
//                        throwable -> Timber.e(throwable, throwable.getMessage()));
//    }

//    /**
//     * @return the int value used in virtual columns to represent a {@link com.gaiagps.iburn.Constants.PlayaItemType}
//     */
//    public static int getTypeValue(Constants.PlayaItemType type) {
//        switch (type) {
//            case CAMP:
//                return 1;
//            case ART:
//                return 2;
//            case EVENT:
//                return 3;
//            case POI:
//                return 4;
//        }
//        Timber.w("Unknown PlayaItemType");
//        return -1;
//    }

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

    /**
     * Add Event-specific columns to a multi-table query, which are required whenever
     * we display an Event.
     */
    private void addEventColsToMultitableQuery(String table, StringBuilder sql) {
        if (!table.equals(PlayaDatabase.EVENTS)) {
            sql.append(", ")
                    .append("'0' as ")
                    .append(EventTable.startTime)
                    .append(", '0' as ")
                    .append(EventTable.startTimePrint)
                    .append(", '0' as ")
                    .append(EventTable.endTime)
                    .append(", '0' as ")
                    .append(EventTable.endTimePrint)
                    .append(", 0 as ")
                    .append(EventTable.allDay)
                    .append(", 'none' as ")
                    .append(EventTable.eventType);
        } else {
            sql.append(", ")
                    .append(EventTable.startTime)
                    .append(", ")
                    .append(EventTable.startTimePrint)
                    .append(", ")
                    .append(EventTable.endTime)
                    .append(", ")
                    .append(EventTable.endTimePrint)
                    .append(", ")
                    .append(EventTable.allDay)
                    .append(", ")
                    .append(EventTable.eventType);
        }
    }
}
