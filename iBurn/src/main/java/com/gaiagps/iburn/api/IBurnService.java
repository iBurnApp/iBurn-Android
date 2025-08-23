package com.gaiagps.iburn.api;

import static com.gaiagps.iburn.SECRETSKt.IBURN_API_URL;
import static com.gaiagps.iburn.database.Art.ARTIST;
import static com.gaiagps.iburn.database.Art.ARTIST_LOCATION;
import static com.gaiagps.iburn.database.Art.IMAGE_URL;
import static com.gaiagps.iburn.database.Camp.HOMETOWN;
import static com.gaiagps.iburn.database.Event.ALL_DAY;
import static com.gaiagps.iburn.database.Event.ART_PLAYA_ID;
import static com.gaiagps.iburn.database.Event.CAMP_PLAYA_ID;
import static com.gaiagps.iburn.database.Event.CHECK_LOC;
import static com.gaiagps.iburn.database.Event.END_TIME;
import static com.gaiagps.iburn.database.Event.END_TIME_PRETTY;
import static com.gaiagps.iburn.database.Event.START_TIME;
import static com.gaiagps.iburn.database.Event.START_TIME_PRETTY;
import static com.gaiagps.iburn.database.Event.TYPE;
import static com.gaiagps.iburn.database.PlayaItem.CONTACT;
import static com.gaiagps.iburn.database.PlayaItem.DESC;
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

import org.maplibre.android.geometry.LatLng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
 * A monolithic iBurn data updater. Handles fetching iBurn update data and updating the database
 * <p>
 * TODO : The API data fetching and Database interaction should be pulled out as deps for better testing
 * Created by davidbrodsky on 6/26/15.
 */
public class IBurnService {



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
        return DataProvider.Companion.getInstance(context)
                .flatMap(provider -> updateData(provider).toObservable())
                .firstOrError();
    }

    public Single<Boolean> updateData(DataProvider provider) {
        Timber.d("Attempting data update...");
        final PrefsHelper storage = new PrefsHelper(context);

        return service.getDataManifest()
                .observeOn(upgradeScheduler)
                .flatMap(dataManifest -> {
                    cachedLocations.clear();
                    cachedUnofficialLocations.clear();
                    Timber.d("Got Data Manifest. art : %s, camps : %s, events : %s",
                            dataManifest.art.updated, dataManifest.camps.updated, dataManifest.events.updated);
                    ResourceManifest[] resources = new ResourceManifest[]{dataManifest.art, dataManifest.camps, dataManifest.events};
                    return Observable.fromArray(resources).map(resource ->
                            new UpdateDataDependencies(provider, dataManifest, resource));
                })
                .filter(dependencies -> shouldUpdateResource(storage, dependencies.resourceManifest))
                .doOnNext(dependencies -> dependencies.dataProvider.beginUpgrade())
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
                .map(dependencies -> true);
    }



    private Single<Long> updateArt(DataProvider provider) {
        Timber.d("Updating art");

        final String tableName = com.gaiagps.iburn.database.Art.TABLE_NAME;
        return updateTable(provider, service.getArt(), tableName, (item, values, database) -> {
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
        return updateTable(provider, service.getCamps(), tableName, (item, values, database) -> {
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
        return updateTable(provider, service.getEvents(), tableName, (item, values, database) -> {

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

            if (event.locatedAtArt != null) {
                values.put(ART_PLAYA_ID, event.locatedAtArt);
            }

            // Insert one row per occurrence, sorted by start time, with zero-based suffix
            List<EventOccurrence> occurrences = new ArrayList<>(event.occurrenceSet);
            // Pre-API24 compatible sort by startTime
            Collections.sort(occurrences, (o1, o2) -> {
                Date t1 = o1.startTime;
                Date t2 = o2.startTime;
                if (t1 == t2) return 0;
                if (t1 == null) return -1;
                if (t2 == null) return 1;
                return t1.compareTo(t2);
            });

            int occurrenceIndex = 0;
            for (EventOccurrence occurrence : occurrences) {
                values.put(PLAYA_ID, event.uid + "-" + occurrenceIndex);
                values.put(START_TIME, apiDateFormat.format(occurrence.startTime));
                values.put(START_TIME_PRETTY, event.allDay ?
                        dayFormatter.format(occurrence.startTime) :
                        timeDayFormatter.format(occurrence.startTime));

                values.put(END_TIME, apiDateFormat.format(occurrence.endTime));
                values.put(END_TIME_PRETTY, event.allDay ?
                        dayFormatter.format(occurrence.endTime) :
                        timeDayFormatter.format(occurrence.endTime));

                database.insert(values);
                occurrenceIndex++;
            }
        });
    }

    private Single<Long> updateTable(DataProvider provider,
                                     Observable<? extends Iterable<? extends PlayaItem>> items,
                                     String tableName,
                                     BindObjectToContentValues binder) {

        final AtomicBoolean initializedInsert = new AtomicBoolean(false);
        final android.content.ContentValues values = new android.content.ContentValues();
        return items.doOnNext(resp -> Timber.d("Got %s API Response", tableName))
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
                    binder.bindAndInsertValues(item, values, finalValues -> provider.insert(tableName, finalValues));
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
     * internal user data such as favorites
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
                // https://github.com/iBurnApp/BlackRockCityPlanner/issues/6
                String locationStr = item.locationString;
                if (!TextUtils.isEmpty(locationStr)) {
                    locationStr = locationStr.replace("None None", "");
                }

                Location location = Location.fromLocation(item.location);

                if (!TextUtils.isEmpty(locationStr) &&
                        !(locationStr.equals("Mobile")) &&
                        location.gps_latitude == 0.0 && location.gps_longitude == 0.0) {
                    LatLng response = Geocoder.INSTANCE.forwardGeocode(context, locationStr).blockingGet();
                    location.gps_latitude = response.getLatitude();
                    location.gps_longitude = response.getLongitude();
                    item.location.gps_latitude = response.getLatitude();
                    item.location.gps_longitude = response.getLongitude();
                }

                cachedLocations.put(item.uid, location);
            }

            if (item.burnermap_location != null) {
                // TODO - this is probably worthless using the new Location type from API
                Location location = new Location();
                location.gps_latitude =  item.burnermap_location.gps_latitude;
                location.gps_longitude = item.burnermap_location.gps_longitude;
                location.frontage = item.burnermap_location.frontage;
                location.intersectionType = item.burnermap_location.intersectionType;
                location.intersection = item.burnermap_location.intersection;
                cachedUnofficialLocations.put(item.uid, location);
            }
        }

        if (item.location != null) {
            values.put(LATITUDE, item.location.gps_latitude);
            values.put(LONGITUDE, item.location.gps_longitude);
            values.put(PLAYA_ADDR, item.locationString);
        } else {
            values.put(LATITUDE, 0);
            values.put(LONGITUDE, 0);
        }

        if (item.burnermap_location != null) {
            values.put(LATITUDE_UNOFFICIAL, item.burnermap_location.gps_latitude);
            values.put(LONGITUDE_UNOFFICIAL, item.burnermap_location.gps_longitude);
            values.put(PLAYA_ADDR_UNOFFICIAL, item.burnermap_location.locationString());
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
