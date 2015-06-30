package com.gaiagps.iburn.api;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.gaiagps.iburn.SECRETS;
import com.gaiagps.iburn.api.response.Art;
import com.gaiagps.iburn.api.response.Camp;
import com.gaiagps.iburn.api.response.DataManifest;
import com.gaiagps.iburn.api.response.Event;
import com.gaiagps.iburn.api.response.EventOccurrence;
import com.gaiagps.iburn.api.response.PlayaItem;
import com.gaiagps.iburn.api.response.ResourceManifest;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.sqlbrite.SqlBrite;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.GET;
import retrofit.http.Streaming;
import rx.Observable;
import timber.log.Timber;

/**
 * A monolithic iBurn data updater. Handles fetching IBurn update data and update the database while
 * preserving user favorites
 * <p>
 * TODO : The API data fetching and Database interaction should be pulled out as deps for better testing
 * Created by davidbrodsky on 6/26/15.
 */
public class IBurnService {

    public static final String PREFS_NAME = "api";

    /**
     * API Definition
     */
    public interface IBurnAPIService {

        @GET("/update.json.js")
        Observable<DataManifest> getDataManifest();

        @GET("/camps.json.js")
        Observable<List<Camp>> getCamps();

        @GET("/art.json.js")
        Observable<List<Art>> getArt();

        @GET("/events.json.js")
        Observable<List<Event>> getEvents();

        @GET("/iburn.mbtiles.jar")
        @Streaming
        Observable<Response> getTiles();
    }

    /**
     * A mechanism for migrating internal app data not defined by the iBurn API.
     */
    public interface UpgradeLifeboat {

        /**
         * Save any database data not represented by the iBurn API
         *
         * @param database the internal app database
         */
        Observable<Boolean> saveData(SqlBrite database);

        /**
         * @param row the row of refreshsed data from the iBurn API.
         *            Add any internal data captured in {@link #saveData(SqlBrite)}.
         *            Be explicit and do not make assumptions about default values, as
         *            row may be recycled from a previous item.
         */
        void restoreData(ContentValues row);
    }

    /**
     * Persist user-defined favorites based on {@link PlayaItemTable#playaId}.
     * This is suitable for {@link PlayaDatabase#ART} and {@link PlayaDatabase#CAMPS} collections.
     * <p>
     * It *cannot* be used with {@link PlayaDatabase#EVENTS} because there may be multiple Event entries
     * sharing the same playaId but having differing start and end times
     */
    private class SimpleLifeboat implements UpgradeLifeboat {

        private String tableName;
        private List<Integer> favoritePlayaIds;

        public SimpleLifeboat(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public Observable<Boolean> saveData(SqlBrite database) {
            return database.createQuery(tableName, "SELECT " + PlayaItemTable.playaId + " FROM " + tableName + " WHERE " + PlayaItemTable.favorite + " = ?", new String[]{"1"})
                    .map(SqlBrite.Query::run)
                    .map(cursor -> {
                        favoritePlayaIds = new ArrayList<>(cursor.getCount());
                        Timber.d("Found %d %s favorites", cursor.getCount(), tableName);
                        while (cursor.moveToNext()) {
                            favoritePlayaIds.add(cursor.getInt(cursor.getColumnIndex(PlayaItemTable.playaId)));
                        }
                        cursor.close();
                        return true;
                    })
                    .first();
        }

        @Override
        public void restoreData(ContentValues row) {
            row.put(PlayaItemTable.favorite, favoritePlayaIds.contains(row.getAsInteger(PlayaItemTable.playaId)));
        }
    }

    private class EventLifeboat implements UpgradeLifeboat {

        private final String tableName = PlayaDatabase.EVENTS;
        private HashMap<Integer, HashSet<String>> favoriteIds;

        @Override
        public Observable<Boolean> saveData(SqlBrite database) {
            return database.createQuery(tableName, "SELECT " + PlayaItemTable.playaId + " , " + EventTable.startTime + " FROM " + tableName + " WHERE " + PlayaItemTable.favorite + " = ?", new String[]{"1"})
                    .map(SqlBrite.Query::run)
                    .map(cursor -> {
                        favoriteIds = new HashMap<>(cursor.getCount());
                        Timber.d("Found %d %s favorites", cursor.getCount(), tableName);
                        int favoriteId;
                        while (cursor.moveToNext()) {
                            favoriteId = cursor.getInt(0);
                            if (!favoriteIds.containsKey(favoriteId))
                                favoriteIds.put(favoriteId, new HashSet<>());

                            Timber.d("Added fav event with id %d start time %s", cursor.getInt(0), cursor.getString(1));
                            favoriteIds.get(favoriteId).add(cursor.getString(1));
                        }
                        cursor.close();
                        return true;
                    })
                    .first();
        }

        @Override
        public void restoreData(ContentValues row) {
            int playaId = row.getAsInteger(EventTable.playaId);
            row.put(EventTable.favorite, favoriteIds.containsKey(playaId) &&
                    favoriteIds.get(playaId).contains(row.getAsString(EventTable.startTime)));
        }
    }

    Context context;
    IBurnAPIService service;

    DataManifest dataManifest;

    public IBurnService(@NonNull Context context) {
        this.context = context;

        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new PlayaDateTypeAdapter())
                .create();

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(SECRETS.IBURN_API_URL)
                .setConverter(new GsonConverter(gson))
//                .setLogLevel(RestAdapter.LogLevel.HEADERS_AND_ARGS)
                .build();

        service = restAdapter.create(IBurnAPIService.class);
    }

    public void updateData() {
        // Check local update dates for each endpoint, update those that are stale
        final SharedPreferences storage = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        service.getDataManifest()
                .flatMap(dataManifest1 -> {

                    IBurnService.this.dataManifest = dataManifest1;
                    Timber.d("Got Data Manifest. art : %s, camps : %s, events : %s",
                            dataManifest1.art.updated, dataManifest1.camps.updated, dataManifest1.events.updated);

                    ResourceManifest[] resources = new ResourceManifest[]
                            {dataManifest1.art, dataManifest1.camps, dataManifest1.events, dataManifest1.tiles};

                    return Observable.from(resources);
                })
                .filter(resourceManifest -> shouldUpdateResource(storage, resourceManifest))
                .flatMap(resourceManifest -> {
                    //Timber.d("Should update " + resourceManifest.file);
                    return updateResource(resourceManifest, dataManifest);
                })
                .reduce((aBoolean, aBoolean2) -> {
                    //Timber.d("Success %b %b", aBoolean, aBoolean2);
                    return aBoolean && aBoolean2;
                })
                .subscribe(aBoolean -> {
                    Timber.d("Updated All data successfully " + aBoolean);
                });
    }

    private Observable<Boolean> updateArt() {
        Timber.d("Updating art");

        final String tableName = PlayaDatabase.ART;
        return updateTable(service.getArt(), tableName, new SimpleLifeboat(tableName), (item, values, database) -> {
            Art art = (Art) item;
            values.put(ArtTable.artist, art.artist);
            values.put(ArtTable.artistLoc, art.artistLocation);
            database.insert(values);
        });
    }

    private Observable<Boolean> updateCamps() {
        Timber.d("Updating Camps");

        final String tableName = PlayaDatabase.CAMPS;
        return updateTable(service.getCamps(), tableName, new SimpleLifeboat(tableName), (item, values, database) -> {
            values.put(CampTable.hometown, ((Camp) item).hometown);
            database.insert(values);
        });
    }

    private Observable<Boolean> updateEvents() {
        Timber.d("Updating Events");

        // Date format for machine-readable
        final SimpleDateFormat mahineDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US);
        // Date format for human-readable specific-time
        final SimpleDateFormat timeDayFormatter = new SimpleDateFormat("EE M/d h:mm a", Locale.US);
        // Date format for human-readable all-day
        final SimpleDateFormat dayFormatter = new SimpleDateFormat("EE M/d", Locale.US);

        final String tableName = PlayaDatabase.EVENTS;
        return updateTable(service.getEvents(), tableName, new EventLifeboat(), (item, values, database) -> {

            Event event = (Event) item;

            // Event uses title, not name
            values.put(EventTable.name, event.title);

            values.put(EventTable.allDay, event.allDay ? 1 : 0);
            values.put(EventTable.checkLocation, event.checkLocation ? 1 : 0);
            values.put(EventTable.eventType, event.eventType.abbr);

            if (event.hostedByCamp != null) {
                values.put(EventTable.campName, event.hostedByCamp.name);
                values.put(EventTable.campPlayaId, event.hostedByCamp.id);
            }

            for (EventOccurrence occurrence : event.occurrenceSet) {
                values.put(EventTable.startTime, mahineDateFormatter.format(occurrence.startTime));
                values.put(EventTable.startTimePrint, event.allDay ? dayFormatter.format(occurrence.startTime) :
                        timeDayFormatter.format(occurrence.startTime));

                values.put(EventTable.endTime, mahineDateFormatter.format(occurrence.endTime));
                values.put(EventTable.endTimePrint, event.allDay ? dayFormatter.format(occurrence.endTime) :
                        timeDayFormatter.format(occurrence.endTime));

                database.insert(values);
            }
        });
    }

    private Observable<Boolean> updateTable(Observable<? extends Iterable<? extends PlayaItem>> items,
                                            String tableName,
                                            UpgradeLifeboat lifeboat,
                                            BindObjectToContentValues binder) {

        final AtomicBoolean initializedInsert = new AtomicBoolean(false);
        final SqlBrite sqlBrite = DataProvider.getSqlBriteInstance(context);
        final android.content.ContentValues values = new android.content.ContentValues();
        // Fetch remote JSON and all existing internal records that are favorites, simultaneously
        return Observable.zip(
                items.doOnNext(resp -> Timber.d("got items")),
                lifeboat.saveData(sqlBrite).doOnNext(result -> Timber.d("saved data")), (playaItems, lifeboatSuccess) -> {
                    if (!lifeboatSuccess)
                        throw new IllegalStateException("Lifeboat did not complete successfully!");
                    return playaItems;
                })

                .flatMap(Observable::from)

                .doOnCompleted(() -> {
                    Timber.d("Finished %s insert", tableName);
                    sqlBrite.setTransactionSuccessful();
                    sqlBrite.endTransaction();
                })

                .map(item -> {
                    // Delete all old rows before inserting first new row
                    if (!initializedInsert.getAndSet(true)) {
                        int numDeleted = sqlBrite.delete(tableName, PlayaItemTable.id + " > 0", null);
                        Timber.d("Deleted %d existing rows. Beginning %s inserts", numDeleted, tableName);
                        sqlBrite.beginTransaction();
                    }

                    values.clear();
                    bindBaseValues(item, values);
                    binder.bindAndInsertValues(item, values, finalValues -> {
                        lifeboat.restoreData(finalValues);
                        sqlBrite.insert(tableName, finalValues);
                    });
                    return true;
                })

                .doOnError(throwable -> {
                    Timber.e(throwable, "Error inserting " + tableName);
                    sqlBrite.endTransaction();
                })

                .reduce((thisSuccess, accumulatedSuccess) -> thisSuccess && accumulatedSuccess);
    }

    interface BindObjectToContentValues<T extends PlayaItem> {

        /**
         * @param item     the data source which extends {@link PlayaItem}
         * @param values   the persisted data sink, which already has all common {@link PlayaItem}
         *                 attributes bound
         * @param database the database on which to perform the insert via {@link com.gaiagps.iburn.api.IBurnService.DataBaseSink#insert(ContentValues)}
         */
        void bindAndInsertValues(T item, android.content.ContentValues values, DataBaseSink database);
    }

    interface DataBaseSink {
        void insert(ContentValues values);
    }

    /**
     * Bind {@link PlayaItemTable} values described by the iBurn API. This does not include
     * internal data columns like {@link PlayaItemTable#favorite}
     */
    private void bindBaseValues(PlayaItem item, android.content.ContentValues values) {

        // Name is a required column
        values.put(PlayaItemTable.name, item.name != null ? item.name : "?");

        values.put(PlayaItemTable.contact, item.contactEmail);
        values.put(PlayaItemTable.description, item.description);
        values.put(PlayaItemTable.playaId, item.id);
        values.put(PlayaItemTable.latitude, item.latitude);
        values.put(PlayaItemTable.longitude, item.longitude);
        values.put(PlayaItemTable.playaAddress, item.location);
        values.put(PlayaItemTable.url, item.url);
    }

    private Observable<Boolean> updateResource(ResourceManifest resourceManifest, DataManifest dataManifest) {
        String resourceName = resourceManifest.file;

        if (resourceName.equals(dataManifest.art.file))
            return updateArt();

        else if (resourceName.equals(dataManifest.camps.file))
            return updateCamps();

        else if (resourceName.equals(dataManifest.events.file))
            return updateEvents();

        else if (resourceName.equals(dataManifest.tiles.file))
            return Observable.just(true);
//            updateTiles();

        // Unknown or Unimplemented situation
        return Observable.just(false);

    }

    private boolean shouldUpdateResource(SharedPreferences storage, ResourceManifest resource) {
        //Timber.d("%s ver local : %d remote: %d", resource.file, storage.getLong(resource.file, 0), resource.updated.getTime());
        return storage.getLong(resource.file, 0) < resource.updated.getTime();
    }
}
