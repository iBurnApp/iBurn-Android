package com.gaiagps.iburn.api;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.api.response.Art;
import com.gaiagps.iburn.api.response.Camp;
import com.gaiagps.iburn.api.response.DataManifest;
import com.gaiagps.iburn.api.response.Event;
import com.gaiagps.iburn.api.response.EventOccurrence;
import com.gaiagps.iburn.api.response.PlayaItem;
import com.gaiagps.iburn.api.response.ResourceManifest;
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.MapProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static com.gaiagps.iburn.SECRETSKt.IBURN_API_URL;

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
     * Persist user-defined favorites based on {@link PlayaItemTable#playaId}.
     * This is suitable for {@link PlayaDatabase#ART} and {@link PlayaDatabase#CAMPS} collections.
     * <p>
     * It *cannot* be used with {@link PlayaDatabase#EVENTS} because there may be multiple Event entries
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
                            favoritePlayaIds.add(fav.getPlayaId());
                        }
                        return true;
                    })
                    .toObservable();
        }

        @Override
        public void restoreData(ContentValues row) {
            row.put(PlayaItemTable.favorite, favoritePlayaIds.contains(row.getAsString(PlayaItemTable.playaId)));
        }
    }

    private static class EventLifeboat implements UpgradeLifeboat {

        private HashMap<String, HashSet<Long>> favoriteIds = new HashMap<>();

        @Override
        public Observable<Boolean> saveData(DataProvider provider) {
            return provider.observeEventFavorites()
                    .firstOrError()
                    .map(favorites -> {
                        Timber.d("Found %d event favorites", favorites.size());
                        String favoriteId;
                        for (com.gaiagps.iburn.database.Event favEvent : favorites) {
                            favoriteId = favEvent.getPlayaId();
                            if (!favoriteIds.containsKey(favoriteId))
                                favoriteIds.put(favoriteId, new HashSet<>());
                            favoriteIds.get(favoriteId).add(favEvent.getStartTime().getTime());
                        }
                        return true;
                    })
                    .toObservable();
        }

        @Override
        public void restoreData(ContentValues row) {
            String playaId = row.getAsString(ColPlayaId);
            row.put(ColFavorite, favoriteIds.containsKey(playaId) &&
                    favoriteIds.get(playaId).contains(row.getAsString(ColStartTime)));
        }
    }

    /**
     * Class to represent state needed to update an iBurn collection
     */
    private class UpdateDataDependencies {

        DataProvider dataProvider;
        MapProvider mapProvider;
        DataManifest dataManifest;
        ResourceManifest resourceManifest;

        public UpdateDataDependencies(DataProvider dataProvider, MapProvider mapProvider, DataManifest dataManifest, ResourceManifest resourceManifest) {
            this.dataProvider = dataProvider;
            this.mapProvider = mapProvider;
            this.resourceManifest = resourceManifest;
            this.dataManifest = dataManifest;
        }
    }

    Context context;
    IBurnApi service;

    public IBurnService(@NonNull Context context) {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new PlayaDateTypeAdapter())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SECRETS.IBURN_API_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        this.service = retrofit.create(IBurnApi.class);
        this.context = context;
    }

    public IBurnService(@NonNull Context context, IBurnApi service) {
        this.context = context;
        this.service = service;
    }

    public Single<Boolean> updateData() {
        // Check local update dates for each endpoint, update those that are stale
        final PrefsHelper storage = new PrefsHelper(context);

        return Observable.zip(DataProvider.getInstance(context),
                Observable.just(MapProvider.getInstance(context)),
                (dataProvider, mapProvider) -> new Pair<>(dataProvider, mapProvider))
                .flatMap(providerPair -> service.getDataManifest().map(dataManifest -> new Pair<>(providerPair, dataManifest)))
                .flatMap(depBundle -> {
                    Timber.d("Got depBundle");
                    Pair providerPair = depBundle.first;
                    DataManifest dataManifest = depBundle.second;

                    Timber.d("Got Data Manifest. art : %s, camps : %s, events : %s",
                            dataManifest.art.updated, dataManifest.camps.updated, dataManifest.events.updated);

                    ResourceManifest[] resources = new ResourceManifest[]
                            {dataManifest.art, dataManifest.camps, dataManifest.events, dataManifest.tiles};

                    return Observable.fromArray(resources).map(resource ->
                            new UpdateDataDependencies((DataProvider) providerPair.first, (MapProvider) providerPair.second, dataManifest, resource));
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
                    }
                })
                .map(dependencies -> true); // TODO : More granular success / failure?
//                .subscribe(totalUpdated -> Timber.d("Update complete"), throwable -> Timber.e(throwable, "Update error"));
    }

    private Single<Long> updateTiles(ResourceManifest resourceManifest, MapProvider provider) {
        return service.getTiles()
                .firstOrError()
                .map(response -> {
                    try {
                        provider.offerMapUpgrade(response.raw().body().byteStream(), resourceManifest.updated.getTime());
                        return 1l;
                    } catch (IOException | NullPointerException e) {
                        Timber.e(e, "Error copying mbtiles!");
                        return 0l;
                    }
                });
    }

    private Single<Long> updateArt(DataProvider provider) {
        Timber.d("Updating art");

        final String tableName = PlayaDatabase.ART;
        return updateTable(provider, service.getArt(), tableName, new SimpleLifeboat(SimpleLifeboat.Table.Art), (item, values, database) -> {
            Art art = (Art) item;
            values.put(ColArtist, art.artist);
            values.put(ColArtistLocation, art.artistLocation);
            values.put(ColAudioTourUrl, art.audioTourUrl);
            database.insert(values);
        });
    }

    private Single<Long> updateCamps(DataProvider provider) {
        Timber.d("Updating Camps");

        final String tableName = PlayaDatabase.CAMPS;
        return updateTable(provider, service.getCamps(), tableName, new SimpleLifeboat(SimpleLifeboat.Table.Camp), (item, values, database) -> {
            values.put(com.gaiagps.iburn.database.Camp.ColHometown, ((Camp) item).hometown);
            database.insert(values);
        });
    }

    private Single<Long> updateEvents(DataProvider provider) {
        Timber.d("Updating Events");

        // Date format for machine-readable
        //final SimpleDateFormat mahineDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX", Locale.US);
        // Date format for human-readable specific-time
        final SimpleDateFormat timeDayFormatter = new SimpleDateFormat("EE M/d h:mm a", Locale.US);
        // Date format for human-readable all-day
        final SimpleDateFormat dayFormatter = new SimpleDateFormat("EE M/d", Locale.US);

        final String tableName = PlayaDatabase.EVENTS;
        return updateTable(provider, service.getEvents(), tableName, new EventLifeboat(), (item, values, database) -> {

            Event event = (Event) item;

            if (event.occurrenceSet == null) {
                // If no occurrence set, ignore for now?
                Timber.d("Event %s without occurrence", event.uid);
                return;
            }

            // Event uses title, not name
            values.put(ColName, event.title);

            values.put(ColAllDay, event.allDay);
            values.put(ColCheckLocation, event.checkLocation);
            values.put(ColType, event.eventType.abbr);

            if (event.hostedByCamp != null) {
                values.put(ColCampPlayaId, event.hostedByCamp);
            }

            for (EventOccurrence occurrence : event.occurrenceSet) {
                values.put(ColStartTime, PlayaDateTypeAdapter.iso8601Format.format(occurrence.startTime));
                values.put(ColStartTimePretty, (event.allDay == 1) ? dayFormatter.format(occurrence.startTime) :
                        timeDayFormatter.format(occurrence.startTime));

                values.put(EventTable.endTime, PlayaDateTypeAdapter.iso8601Format.format(occurrence.endTime));
                values.put(EventTable.endTimePrint, (event.allDay == 1) ? dayFormatter.format(occurrence.endTime) :
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

//                .doOnNext(count -> Timber.d("Inserted %d %s", count, tableName))

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
     * Bind {@link PlayaItemTable} values described by the iBurn API. This does not include
     * internal data columns like {@link PlayaItemTable#favorite}
     */
    private void bindBaseValues(PlayaItem item, android.content.ContentValues values) {

        // Name is a required column
        values.put(PlayaItemTable.name, item.name != null ? item.name : "?");

        values.put(PlayaItemTable.contact, item.contactEmail);
        values.put(PlayaItemTable.description, item.description);
        values.put(PlayaItemTable.playaId, item.uid);
        if (item.location != null) {
            values.put(PlayaItemTable.latitude, item.location.gps_latitude);
            values.put(PlayaItemTable.longitude, item.location.gps_longitude);
            values.put(PlayaItemTable.playaAddress, item.location.string);
        }
        values.put(PlayaItemTable.url, item.url);
    }

    private Single<Long> updateResource(UpdateDataDependencies dependencies) {
        String resourceName = dependencies.resourceManifest.file;

        if (resourceName.equals(dependencies.dataManifest.art.file))
            return updateArt(dependencies.dataProvider);

        else if (resourceName.equals(dependencies.dataManifest.camps.file))
            return updateCamps(dependencies.dataProvider);

        else if (resourceName.equals(dependencies.dataManifest.events.file))
            return updateEvents(dependencies.dataProvider);

        else if (resourceName.equals(dependencies.dataManifest.tiles.file))
            return updateTiles(dependencies.resourceManifest, dependencies.mapProvider);

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
