package com.gaiagps.iburn.api;

import static com.gaiagps.iburn.SECRETSKt.IBURN_API_URL;
import static com.gaiagps.iburn.database.Art.ARTIST;
import static com.gaiagps.iburn.database.Art.ARTIST_LOCATION;
import static com.gaiagps.iburn.database.Art.IMAGE_URL;
import static com.gaiagps.iburn.database.Camp.HOMETOWN;
import static com.gaiagps.iburn.database.Event.ALL_DAY;
import static com.gaiagps.iburn.database.Event.CAMP_PLAYA_ID;
import static com.gaiagps.iburn.database.Event.CHECK_LOC;
import static com.gaiagps.iburn.database.Event.END_TIME;
import static com.gaiagps.iburn.database.Event.END_TIME_PRETTY;
import static com.gaiagps.iburn.database.Event.START_TIME;
import static com.gaiagps.iburn.database.Event.START_TIME_PRETTY;
import static com.gaiagps.iburn.database.Event.TYPE;
import static com.gaiagps.iburn.database.PlayaItem.CONTACT;
import static com.gaiagps.iburn.database.PlayaItem.DESC;
import static com.gaiagps.iburn.database.PlayaItem.FAVORITE;
import static com.gaiagps.iburn.database.PlayaItem.LATITUDE;
import static com.gaiagps.iburn.database.PlayaItem.LATITUDE_UNOFFICIAL;
import static com.gaiagps.iburn.database.PlayaItem.LONGITUDE;
import static com.gaiagps.iburn.database.PlayaItem.LONGITUDE_UNOFFICIAL;
import static com.gaiagps.iburn.database.PlayaItem.NAME;
import static com.gaiagps.iburn.database.PlayaItem.PLAYA_ADDR;
import static com.gaiagps.iburn.database.PlayaItem.PLAYA_ADDR_UNOFFICIAL;
import static com.gaiagps.iburn.database.PlayaItem.PLAYA_ID;
import static com.gaiagps.iburn.database.PlayaItem.URL;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.gaiagps.iburn.DateUtil;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.adapters.AdapterUtils;
import com.gaiagps.iburn.api.response.Art;
import com.gaiagps.iburn.api.response.Camp;
import com.gaiagps.iburn.api.response.DataManifest;
import com.gaiagps.iburn.api.response.Event;
import com.gaiagps.iburn.api.response.EventOccurrence;
import com.gaiagps.iburn.api.response.Location;
import com.gaiagps.iburn.api.response.PlayaItem;
import com.gaiagps.iburn.api.response.ResourceManifest;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.js.Geocoder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.mapboxsdk.geometry.LatLng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

/**
 * A monolithic iBurn data updater. Handles fetching IBurn update data and update the database while
 * preserving user favorites
 * <p>
 * TODO : The API data fetching and Database interaction should be pulled out as deps for better testing
 * Created by davidbrodsky on 6/26/15.
 */
public class IBurnService {

    /**
     * A mechanism for migrating internal app data not defined by the iBurn API.
     */
    public interface UpgradeLifeboat {

        /**
         * Save any database data not represented by the iBurn API
         *
         * @param database the internal app database
         */
        Observable<Boolean> saveData(DataProvider database);

        /**
         * @param row the row of refreshsed data from the iBurn API.
         *            Add any internal data captured in {@link #saveData(DataProvider)}.
         *            Be explicit and do not make assumptions about default values, as
         *            row may be recycled from a previous item.
         */
        void restoreData(ContentValues row);
    }

    /**
     * Persist user-defined favorites based on {@link PlayaItem.PLAYA_ID}.
     * This is suitable for {@link com.gaiagps.iburn.database.Art} and {@link com.gaiagps.iburn.database.Camp} collections.
     * <p>
     * It *cannot* be used with {@link com.gaiagps.iburn.database.Event} because there may be multiple Event entries
     * sharing the same playaId but having differing start and end times
     */
    private static class SimpleLifeboat implements UpgradeLifeboat {
        enum Table {Camp, Art}

        private Table table;

        public SimpleLifeboat(@NonNull Table table) {
            this.table = table;
        }

        private Set<String> favoritePlayaIds = new HashSet<>();

        @Override
        public Observable<Boolean> saveData(DataProvider provider) {
            Flowable<? extends List<? extends com.gaiagps.iburn.database.PlayaItem>> items = null;

            if (table == Table.Camp) {
                items = provider.observeCampFavorites();

            } else if (table == Table.Art) {
                items = provider.observeArtFavorites();
            } else {
                throw new IllegalStateException("Unknown table type " + table);
            }

            return items
                    .firstOrError()
                    .map(favorites -> {
                        Timber.d("Found %d favorites", favorites.size());
                        for (com.gaiagps.iburn.database.PlayaItem fav : favorites) {
                            favoritePlayaIds.add(fav.playaId);
                        }
                        return true;
                    })
                    .toObservable();
        }

        @Override
        public void restoreData(ContentValues row) {
            row.put(FAVORITE, favoritePlayaIds.contains(row.getAsString(PLAYA_ID)));
        }
    }

    private static class EventLifeboat implements UpgradeLifeboat {

        private HashMap<String, HashSet<String>> favoriteIds = new HashMap<>();

        @Override
        public Observable<Boolean> saveData(DataProvider provider) {
            return provider.observeEventFavorites()
                    .firstOrError()
                    .map(favorites -> {
                        Timber.d("Found %d event favorites", favorites.size());
                        String favoriteId;
                        for (com.gaiagps.iburn.database.Event favEvent : favorites) {
                            favoriteId = favEvent.playaId;
                            if (!favoriteIds.containsKey(favoriteId))
                                favoriteIds.put(favoriteId, new HashSet<>());
                            favoriteIds.get(favoriteId).add(favEvent.startTime);
                        }
                        return true;
                    })
                    .toObservable();
        }

        @Override
        public void restoreData(ContentValues row) {
            String playaId = row.getAsString(PLAYA_ID);
            row.put(FAVORITE, favoriteIds.containsKey(playaId) &&
                    favoriteIds.get(playaId).contains(row.getAsString(START_TIME)));
        }
    }

    /**
     * Class to represent state needed to update an iBurn collection
     */
    private class UpdateDataDependencies {

        DataProvider dataProvider;
        DataManifest dataManifest;
        ResourceManifest resourceManifest;

        public UpdateDataDependencies(DataProvider dataProvider, DataManifest dataManifest, ResourceManifest resourceManifest) {
            this.dataProvider = dataProvider;
            this.resourceManifest = resourceManifest;
            this.dataManifest = dataManifest;
        }
    }

    Scheduler upgradeScheduler =
            Schedulers.from(
                    Executors.newSingleThreadExecutor()
            );

    Context context;
    IBurnApi service;
    /*
    Store camp and art locations before processing events so we can relate them
     */
    HashMap<String, Location> cachedLocations = new HashMap<>();
    HashMap<String, Location> cachedUnofficialLocations = new HashMap<>();

    private static final DateFormat apiDateFormat = PlayaDateTypeAdapter.buildIso8601Format();

    public IBurnService(@NonNull Context context) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new PlayaDateTypeAdapter())
                .create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(IBURN_API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        this.service = retrofit.create(IBurnApi.class);
        this.context = context.getApplicationContext();
    }

    public IBurnService(@NonNull Context context, IBurnApi service) {
        this.context = context;
        this.service = service;
    }

    public Single<Boolean> updateData() {
        Timber.d("Attempting data update...");
        // Check local update dates for each endpoint, update those that are stale
        final PrefsHelper storage = new PrefsHelper(context);

        return DataProvider.Companion.getInstance(context)
                .observeOn(upgradeScheduler)
                .flatMap(dataProvider -> service.getDataManifest().map(dataManifest -> new Pair<>(dataProvider, dataManifest)))
                .flatMap(depBundle -> {

                    cachedLocations.clear();
                    cachedUnofficialLocations.clear();

                    Timber.d("Got depBundle");
                    DataProvider dataProvider = depBundle.first;
                    DataManifest dataManifest = depBundle.second;

                    Timber.d("Got Data Manifest. art : %s, camps : %s, events : %s",
                            dataManifest.art.updated, dataManifest.camps.updated, dataManifest.events.updated);

                    ResourceManifest[] resources = new ResourceManifest[]
                            {dataManifest.art, dataManifest.camps, dataManifest.events};

                    return Observable.fromArray(resources).map(resource ->
                            new UpdateDataDependencies((DataProvider) dataProvider, dataManifest, resource));
                })
                .filter(dependencies -> shouldUpdateResource(storage, dependencies.resourceManifest))
                .doOnNext(dependencies -> dependencies.dataProvider.beginUpgrade()) // We really should only do this the first time
                .flatMap(dependencies ->
                        updateResource(dependencies)
                                .map(itemsUpdated -> {
                                    Timber.d("item %s updated %d items", dependencies.resourceManifest.file, itemsUpdated);
                                    if (itemsUpdated > 0)
                                        storage.setResourceVersion(dependencies.resourceManifest.file, dependencies.resourceManifest.updated.getTime());
                                    return dependencies;
                                })
                                .toObservable())
                .doOnError(throwable -> Timber.e(throwable, "updateData error"))
                .toList()
                .doOnSuccess(updateDataDependencies -> {
                    Timber.d("updateData Complete");
                    if (updateDataDependencies.size() > 0) {
                        updateDataDependencies.get(0).dataProvider.endUpgrade();
                        cachedLocations.clear();
                        cachedUnofficialLocations.clear();
                    }
                })
                .map(dependencies -> true); // TODO : More granular success / failure?
//                .subscribe(totalUpdated -> Timber.d("Update complete"), throwable -> Timber.e(throwable, "Update error"));
    }

    private Single<Long> updateArt(DataProvider provider) {
        Timber.d("Updating art");

        final String tableName = com.gaiagps.iburn.database.Art.TABLE_NAME;
        return updateTable(provider, service.getArt(), tableName, new SimpleLifeboat(SimpleLifeboat.Table.Art), (item, values, database) -> {
            Art art = (Art) item;
            values.put(ARTIST, art.artist);
            values.put(ARTIST_LOCATION, art.artistLocation);
//            values.put(AUDIO_TOUR_URL, art.audioTourUrl);
            if (art.images != null && art.images.size() > 0) {
                values.put(IMAGE_URL, art.images.get(0).thumbnail_url);
            }
            database.insert(values);
        });
    }

    private Single<Long> updateCamps(DataProvider provider) {
        Timber.d("Updating Camps");

        final String tableName = com.gaiagps.iburn.database.Camp.TABLE_NAME;
        return updateTable(provider, service.getCamps(), tableName, new SimpleLifeboat(SimpleLifeboat.Table.Camp), (item, values, database) -> {
            values.put(HOMETOWN, ((Camp) item).hometown);
            database.insert(values);
        });
    }

    private Single<Long> updateEvents(DataProvider provider) {
        Timber.d("Updating Events");

        // Date format for machine-readable
        //final SimpleDateFormat mahineDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX", Locale.US);
        // Date format for human-readable specific-time
        final SimpleDateFormat timeDayFormatter = DateUtil.getPlayaTimeFormat("EE M/d h:mm a");
        // Date format for human-readable all-day
        final SimpleDateFormat dayFormatter = DateUtil.getPlayaTimeFormat("EE M/d");

        final String tableName = com.gaiagps.iburn.database.Event.TABLE_NAME;
        return updateTable(provider, service.getEvents(), tableName, new EventLifeboat(), (item, values, database) -> {

            Event event = (Event) item;

            if (event.occurrenceSet == null) {
                // If no occurrence set, ignore for now?
                Timber.d("Event %s without occurrence", event.uid);
                return;
            }

            // Event uses title, not name
            values.put(NAME, event.title);

            values.put(ALL_DAY, event.allDay);
            values.put(CHECK_LOC, event.checkLocation);
            if (event.eventType != null) {
                values.put(TYPE, event.eventType.abbr);
            } else {
                values.put(TYPE, AdapterUtils.EVENT_TYPE_ABBREVIATION_UNKNOWN);
            }

            if (event.hostedByCamp != null) {
                values.put(CAMP_PLAYA_ID, event.hostedByCamp);
            }

            for (EventOccurrence occurrence : event.occurrenceSet) {
                values.put(START_TIME, apiDateFormat.format(occurrence.startTime));
                values.put(START_TIME_PRETTY, (event.allDay == 1) ? dayFormatter.format(occurrence.startTime) :
                        timeDayFormatter.format(occurrence.startTime));

                values.put(END_TIME, apiDateFormat.format(occurrence.endTime));
                values.put(END_TIME_PRETTY, (event.allDay == 1) ? dayFormatter.format(occurrence.endTime) :
                        timeDayFormatter.format(occurrence.endTime));

                database.insert(values);
            }
        });
    }

    private Single<Long> updateTable(DataProvider provider,
                                     Observable<? extends Iterable<? extends PlayaItem>> items,
                                     String tableName,
                                     UpgradeLifeboat lifeboat,
                                     BindObjectToContentValues binder) {

        final AtomicBoolean initializedInsert = new AtomicBoolean(false);
        final android.content.ContentValues values = new android.content.ContentValues();
        // Fetch remote JSON and all existing internal records that are favorites, simultaneously
        return Observable.zip(
                items.doOnNext(resp -> Timber.d("Got %s API Response", tableName)),
                lifeboat.saveData(provider).doOnNext(result -> Timber.d("Backed up %s data", tableName)),
                (playaItems, lifeboatSuccess) -> {
                    if (!lifeboatSuccess)
                        throw new IllegalStateException("Lifeboat did not complete successfully!");
                    return playaItems;
                })

                .flatMap(Observable::fromIterable)

                .map(item -> {
                    // Delete all old rows before inserting first new row
                    if (!initializedInsert.getAndSet(true)) {
                        provider.beginTransaction();
                        int numDeleted = provider.delete(tableName);
                        Timber.d("Deleted %d existing rows. Beginning %s inserts", numDeleted, tableName);
                    }

                    values.clear();
                    bindBaseValues(item, values);
                    binder.bindAndInsertValues(item, values, finalValues -> {
                        lifeboat.restoreData(finalValues);
                        provider.insert(tableName, finalValues);
                    });
                    return true;
                })

                .doOnComplete(() -> {
                    Timber.d("Successfully closing %s transaction", tableName);
                    provider.setTransactionSuccessful();
                    provider.endTransaction();
                })

                .count()

                .doOnSuccess(count -> Timber.d("Inserted %d %s", count, tableName))

                .doOnError(throwable -> {
                    Timber.e(throwable, "Error. Rolling back %s transacton ", tableName);
                    provider.endTransaction();
                });
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
     * Bind {@link com.gaiagps.iburn.database.PlayaItem} values described by the iBurn API. This does not include
     * internal data columns like {@link PlayaItem.FAVORITE}
     */
    private void bindBaseValues(PlayaItem item, android.content.ContentValues values) {

        // Name is a required column
        values.put(NAME, item.name != null ? item.name : "?");

        values.put(CONTACT, item.contactEmail);
        values.put(DESC, item.description);
        values.put(PLAYA_ID, item.uid);

        if (item instanceof Event) {
            // Retrieve locations cached by earlier camps / arts

            Event event = (Event) item;

            String locationPlayaId = (event.hostedByCamp != null) ? event.hostedByCamp : event.locatedAtArt;

            if (locationPlayaId != null) {
                if (cachedLocations.containsKey(locationPlayaId)) {
                    item.location = cachedLocations.get(locationPlayaId);
                }
                if (cachedUnofficialLocations.containsKey(locationPlayaId)) {
                    item.burnermap_location = cachedUnofficialLocations.get(locationPlayaId);
                }
            }

        } else {
            // Set and cache location for later use by events

            if (item.location != null) {

                Location location = new Location();
                location.gps_latitude = item.location.gps_latitude;
                location.gps_longitude = item.location.gps_longitude;
                location.string = item.location.string;
                // https://github.com/iBurnApp/BlackRockCityPlanner/issues/6
                if (!TextUtils.isEmpty(location.string))
                        location.string = location.string.replace("None None", "");

                if (!TextUtils.isEmpty(location.string) &&
                        !(location.string.equals("Mobile")) &&
                        location.gps_latitude == 0.0 && location.gps_longitude == 0.0) {
                    LatLng response = Geocoder.INSTANCE.forwardGeocode(context, location.string).blockingGet();
                    location.gps_latitude = response.getLatitude();
                    location.gps_longitude = response.getLongitude();
                    item.location.gps_latitude = response.getLatitude();
                    item.location.gps_longitude = response.getLongitude();
                }

                cachedLocations.put(item.uid, location);
            }

            if (item.burnermap_location != null) {

                Location location = new Location();
                location.gps_latitude =  item.burnermap_location.gps_latitude;
                location.gps_longitude = item.burnermap_location.gps_longitude;
                location.string = item.burnermap_location.string;
                cachedUnofficialLocations.put(item.uid, location);
            }
        }

        if (item.location != null) {
            values.put(LATITUDE, item.location.gps_latitude);
            values.put(LONGITUDE, item.location.gps_longitude);
            values.put(PLAYA_ADDR, item.location.string);
        } else {
            values.put(LATITUDE, 0);
            values.put(LONGITUDE, 0);
        }

        if (item.burnermap_location != null) {
            values.put(LATITUDE_UNOFFICIAL, item.burnermap_location.gps_latitude);
            values.put(LONGITUDE_UNOFFICIAL, item.burnermap_location.gps_longitude);
            values.put(PLAYA_ADDR_UNOFFICIAL, item.burnermap_location.string);
        } else {
            values.put(LATITUDE_UNOFFICIAL, 0);
            values.put(LONGITUDE_UNOFFICIAL, 0);
        }
        values.put(URL, item.url);
    }

    private Single<Long> updateResource(UpdateDataDependencies dependencies) {
        String resourceName = dependencies.resourceManifest.file;

        if (resourceName.equals(dependencies.dataManifest.art.file))
            return updateArt(dependencies.dataProvider);

        else if (resourceName.equals(dependencies.dataManifest.camps.file))
            return updateCamps(dependencies.dataProvider);

        else if (resourceName.equals(dependencies.dataManifest.events.file))
            return updateEvents(dependencies.dataProvider);

        // Tiles no longer updated via this service
        // TODO: Capture points

        // Unknown or Unimplemented situation
        Timber.w("Unknown resource name %s. Cannot perform update", resourceName);
        return Single.just(0l);
    }

    private boolean shouldUpdateResource(PrefsHelper storage, ResourceManifest resource) {
        boolean shouldUpdate = storage.getResourceVersion(resource.file) < resource.updated.getTime();
        Timber.d("%s version local:%d remote:%d. Will update: %b", resource.file, storage.getResourceVersion(resource.file), resource.updated.getTime(), shouldUpdate);
        return storage.getResourceVersion(resource.file) < resource.updated.getTime();
    }
}
