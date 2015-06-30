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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit.RestAdapter;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.GET;
import retrofit.http.Streaming;
import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

/**
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
        return updateTable(service.getArt(), tableName, (item, values, database) -> {
            Art art = (Art) item;
            values.put(ArtTable.artist, art.artist);
            values.put(ArtTable.artistLoc, art.artistLocation);
            database.insert(tableName, values);
        });
    }

    private Observable<Boolean> updateCamps() {
        Timber.d("Updating Camps");

        final String tableName = PlayaDatabase.CAMPS;
        return updateTable(service.getCamps(), tableName, (item, values, database) -> {
            values.put(CampTable.hometown, ((Camp) item).hometown);
            database.insert(tableName, values);
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
        return updateTable(service.getEvents(), tableName, (item, values, database) -> {

            Event event = (Event) item;

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

                // Event uses title, not name
                values.put(EventTable.name, event.title);

                database.insert(tableName, values);
            }
        });
    }

    private Observable<Boolean> updateTable(Observable<? extends Iterable<? extends PlayaItem>> items,
                                            String tableName,
                                            BindObjectToContentValues binder) {

        final AtomicBoolean initializedInsert = new AtomicBoolean(false);
        final SqlBrite sqlBrite = DataProvider.getSqlBriteInstance(context);
        final ContentValues values = new ContentValues();
                .flatMap(Observable::from)
                .doOnCompleted(() -> {
                    Timber.d("Finished %s insert", tableName);
                    sqlBrite.setTransactionSuccessful();
                    sqlBrite.endTransaction();
                })

                    if (!initializedInsert.getAndSet(true)) {
                        int numDeleted = sqlBrite.delete(tableName, PlayaItemTable.id + " > 0", null);
                        Timber.d("Deleted %d existing rows. Beginning %s inserts", numDeleted, tableName);
                        sqlBrite.beginTransaction();
                    }

                    values.clear();
                    bindCommonValues(item, values);
                    binder.bindAndInsertValues(item, values, sqlBrite);
                    return true;
                })
                .doOnError(throwable -> {
                    Timber.e(throwable, "Error inserting " + tableName);
                    sqlBrite.endTransaction();
    }

    interface BindObjectToContentValues<T extends PlayaItem> {

        /**
         * @param item     the data source which extends {@link PlayaItem}
         * @param values   the persisted data sink, which already has all common {@link PlayaItem}
         *                 attributes bound
         * @param database the database on which to perform the insert
         */
        void bindAndInsertValues(T item, ContentValues values, SqlBrite database);
    }

    private void bindCommonValues(PlayaItem item, ContentValues values) {

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
