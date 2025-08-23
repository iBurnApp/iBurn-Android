package com.gaiagps.iburn.database

import android.content.ContentValues
import android.content.Context
import com.gaiagps.iburn.AudioTourManager
import com.gaiagps.iburn.CurrentDateProvider
import com.gaiagps.iburn.DateUtil
import com.gaiagps.iburn.PrefsHelper
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter
import com.gaiagps.iburn.database.Favorite
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Flowables
import io.reactivex.schedulers.Schedulers
import org.maplibre.android.geometry.VisibleRegion
import timber.log.Timber
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Class for interaction with our database via Reactive streams.
 * This is intended as an experiment to replace our use of [android.content.ContentProvider]
 * as it does not meet all of our needs (e.g: Complex UNION queries not possible with Schematic's
 * generated version, and I believe manually writing a ContentProvider is too burdensome and error-prone)
 *
 *
 * Created by davidbrodsky on 6/22/15.
 */
class DataProvider private constructor(private val context: Context, private val db: AppDatabase) {

    private val apiDateFormat = PlayaDateTypeAdapter.buildIso8601Format()

    private val upgradeLock = AtomicBoolean(false)

    fun beginUpgrade() {
        upgradeLock.set(true)
    }

    fun inUpgrade(): Boolean {
        return upgradeLock.get();
    }

    fun endUpgrade() {
        upgradeLock.set(false)

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

    fun deleteCamps(): Int {
        return clearTable(Camp.TABLE_NAME)
    }

    private fun clearTable(tablename: String): Int {
        return db.openHelper.writableDatabase.delete(tablename, null, null)
    }

    fun observeCamps(): Flowable<List<CampWithUserData>> {
        return db.campDao().all
    }

    fun observeCampFavorites(): Flowable<List<CampWithUserData>> {

        // TODO : Honor upgradeLock?
        return db.campDao().favorites
    }

    fun observeCampsByName(query: String): Flowable<List<CampWithUserData>> {

        // TODO : Honor upgradeLock
        val wildQuery = addWildcardsToQuery(query)
        return db.campDao().findByName(wildQuery)
    }

    fun observeCampByPlayaId(playaId: String): Flowable<CampWithUserData> {
        return db.campDao().findByPlayaId(playaId)
    }

    fun observeCampById(id: Int): Flowable<CampWithUserData> {
        return db.campDao().findById(id)
    }

    fun beginTransaction() {
        db.beginTransaction()
        //        BriteDatabase.Transaction t = db.newTransaction();
        //        transactionStack.push(t);
    }

    fun setTransactionSuccessful() {
        if (!db.inTransaction()) {
            return
        }

        db.setTransactionSuccessful()
    }

    fun endTransaction() {
        if (!db.inTransaction()) {
            return
        }

        // TODO: Don't allow this call to proceed without prior call to beginTransaction
        db.endTransaction()
    }

    fun insert(table: String, values: ContentValues) {
        db.openHelper.writableDatabase.insert(table, 0, values) // TODO : wtf is the int here?
    }

    fun delete(table: String): Int {
        when (table) {
            Camp.TABLE_NAME -> return deleteCamps()
            Art.TABLE_NAME -> return deleteArt()
            Event.TABLE_NAME -> return deleteEvents()
            else -> Timber.w("Cannot clear unknown table name '%s'", table)
        }
        return 0
    }

    fun deleteEvents(): Int {
        return clearTable(Event.TABLE_NAME)

        //        return db.getOpenHelper().getWritableDatabase().delete(Event.TABLE_NAME, "*", null);
        //        Cursor result = db.query("DELETE FROM event; VACUUM", null);
        //        if (result != null) result.close();
    }

    fun observeEventByPlayaId(id: String): Single<EventWithUserData> {
        return db.eventDao().getByPlayaId(id)
    }

    fun observeEventById(id: Int): Single<EventWithUserData> {
        return db.eventDao().getById(id)
    }

    fun observeEventsOnDayOfTypes(day: String?,
                                  types: ArrayList<String>?,
                                  includeExpired: Boolean,
                                  eventTiming: String): Flowable<List<EventWithUserData>> {

        // TODO : Honor upgradeLock?
        val isoDateFormat = DateUtil.getIso8601Format()
        val nowDate = CurrentDateProvider.getCurrentDate()
        val now = isoDateFormat.format(nowDate)
        
        // Handle "all days" case when day is null or empty
        if (day == null || day.isEmpty()) {
            // For "all days", we use dummy values for allDayStart/allDayEnd since the query
            // methods don't actually filter by day - they just use these to distinguish 
            // between timed and all-day events
            val dummyDay = "8/25" // Use a dummy day format that DateUtil can parse
            val allDayStart = isoDateFormat.format(
                    DateUtil.getAllDayStartDateTime(dummyDay))
            val allDayEnd = isoDateFormat.format(
                    DateUtil.getAllDayEndDateTime(dummyDay))
            
            if (types == null || types.isEmpty()) {
                if(eventTiming=="timed"){
                    if(includeExpired == true) {
                        return db.eventDao().findAllDaysTimed(allDayStart, allDayEnd)
                    }
                    else{
                        return db.eventDao().findAllDaysNoExpiredTimed(now, allDayStart, allDayEnd)
                    }
                }
                else{
                    return db.eventDao().findAllDaysAllDay(allDayStart, allDayEnd)
                }
            } else {
                if(eventTiming=="timed"){
                    if(includeExpired == true) {
                        return db.eventDao().findAllDaysAndTypeTimed(types, allDayStart, allDayEnd)
                    }
                    else{
                        return db.eventDao().findAllDaysAndTypeNoExpiredTimed(types, now, allDayStart, allDayEnd)
                    }
                }
                else{
                    return db.eventDao().findAllDaysAndTypeAllDay(types, allDayStart, allDayEnd)
                }
            }
        }
        
        // Handle specific day filtering (existing logic)
        val wildDay = addWildcardsToQuery(day)
        val allDayStart = isoDateFormat.format(
                DateUtil.getAllDayStartDateTime(day))
        val allDayEnd = isoDateFormat.format(
                DateUtil.getAllDayEndDateTime(day))

        if (types == null || types.isEmpty()) {
            if(eventTiming=="timed"){
                if(includeExpired == true) {
                    return db.eventDao().findByDayTimed(wildDay,
                            allDayStart,allDayEnd)
                }
                else{
                    return db.eventDao().findByDayNoExpiredTimed(wildDay, now,
                            allDayStart,allDayEnd)
                }
            }
            else{
                return db.eventDao().findByDayAllDay(wildDay,allDayStart,
                        allDayEnd)
            }
        } else {
            if(eventTiming=="timed"){
                if(includeExpired == true) {
                    return db.eventDao().findByDayAndTypeTimed(wildDay,types,
                            allDayStart,allDayEnd)
                }
                else{
                    return db.eventDao().findByDayAndTypeNoExpiredTimed(wildDay,
                            types,now,
                            allDayStart,allDayEnd)
                }
            }
            else{
                return db.eventDao().findByDayAndTypeAllDay(wildDay,types,
                        allDayStart,
                        allDayEnd)
            }
        }
    }

    fun observeEventsHostedByCamp(camp: Camp): Flowable<List<EventWithUserData>> {
        return db.eventDao().findByCampPlayaId(camp.playaId)
    }

    fun observeOtherOccurrencesOfEvent(event: Event): Flowable<List<EventWithUserData>> {
        return db.eventDao().findOtherOccurrences(event.playaId, event.id)
    }

    fun observeEventFavorites(): Flowable<List<EventWithUserData>> {

        // TODO : Honor upgradeLock?
        return db.eventDao().favorites
    }

    fun observeEventBetweenDates(start: Date, end: Date): Flowable<List<EventWithUserData>> {

        val startDateStr = apiDateFormat.format(start)
        val endDateStr = apiDateFormat.format(end)
        // TODO : Honor upgradeLock?
        Timber.d("Start time between %s and %s", startDateStr, endDateStr)
        return db.eventDao().findInDateRange(startDateStr, endDateStr)
    }

    fun deleteArt(): Int {
        return clearTable(Art.TABLE_NAME)
        //        return db.getOpenHelper().getWritableDatabase().delete(Art.TABLE_NAME, null, null);
        //        Cursor result = db.query("DELETE FROM art; VACUUM", null);
        //        if (result != null) result.close();
    }

    fun observeArt(): Flowable<List<ArtWithUserData>> {
        // TODO : Honor upgradeLock?
        return db.artDao().all
    }

    fun observeArtFavorites(): Flowable<List<ArtWithUserData>> {
        // TODO : Honor upgradeLock?
        return db.artDao().favorites
    }

    fun observeArtWithAudioTour(): Flowable<List<ArtWithUserData>> {

        // TODO : Honor upgradeLock?
        return db.artDao().all.map { it.filter {
            val pId = it.item.playaId
            pId != null && AudioTourManager.hasAudioTour(context, pId)
        } }
    }

    fun observeArtByPlayaId(playaId: String): Flowable<ArtWithUserData> {
        return db.artDao().findByPlayaId(playaId)
    }

    fun observeArtById(id: Int): Flowable<ArtWithUserData> {
        return db.artDao().findById(id)
    }

    /**
     * Observe all favorites.
     *
     *
     * Note: This query automatically adds in Event.startTime (and 0 values for all non-events),
     * since we always want to show this data for an event.
     */
    fun observeFavorites(): Flowable<SectionedPlayaItems> {

        // TODO : Honor upgradeLock
        // TODO : Return structure with metadata on how many art, camps, events etc?
        return Flowables.combineLatest(
                db.artDao().favorites,
                db.campDao().favorites,
                db.eventDao().favorites)
        { arts, camps, events ->

            val sections = ArrayList<IntRange>(3)
            val items = ArrayList<PlayaItemWithUserData>(arts.size + camps.size + events.size)

            var lastRangeEnd = 0

            if (camps.size > 0) {
                items.addAll(camps)
                val campRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, campRangeEnd))
                lastRangeEnd = campRangeEnd
            }

            if (arts.size > 0) {
                items.addAll(arts)
                val artRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, artRangeEnd))
                lastRangeEnd = artRangeEnd
            }

            if (events.size > 0) {
                items.addAll(events)
                val eventsRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, eventsRangeEnd))
                lastRangeEnd = eventsRangeEnd
            }

            SectionedPlayaItems(data = items, ranges = sections)
        }
    }

    /**
     * Observe all results for a full text search query.
     *
     * Note: This query automatically adds in Event.startTime (and 0 values for all non-events),
     * since we always want to show this data for an event.
     */
    fun observeFtsQuery(query: String): Flowable<SectionedPlayaItems> {

        // TODO : Honor upgradeLock
        // TODO : Return structure with metadata on how many art, camps, events etc?
        val wildQuery = addWildcardsToQuery(query)
        return Flowables.combineLatest(
                db.artDao().searchFts(query),
                db.campDao().searchFts(query),
                db.eventDao().searchFts(query),
                db.userPoiDao().findByName(wildQuery))
        { arts, camps, events, userpois ->
            val sections = ArrayList<IntRange>(4)
            val items = ArrayList<PlayaItemWithUserData>(arts.size + camps.size + events.size)

            var lastRangeEnd = 0

            if (camps.size > 0) {
                items.addAll(camps)
                val campRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, campRangeEnd))
                lastRangeEnd = campRangeEnd
            }

            if (arts.size > 0) {
                items.addAll(arts)
                val artRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, artRangeEnd))
                lastRangeEnd = artRangeEnd
            }

            if (events.size > 0) {
                items.addAll(events)
                val eventsRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, eventsRangeEnd))
                lastRangeEnd = eventsRangeEnd
            }

            if (userpois.size > 0) {
                items.addAll(userpois)
                val userPoiRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, userPoiRangeEnd))
                lastRangeEnd = userPoiRangeEnd
            }

            SectionedPlayaItems(data = items, ranges = sections)
        }
    }

    /**
     * Returns favorites, user-added markers, ongoing events in [region],
     * and Art within [region]. Intended for high zoom map views to avoid clutter.
     */
    fun observeAllMapItemsInVisibleRegion(region: VisibleRegion): Flowable<List<PlayaItemWithUserData>> {
        // TODO : Honor upgradeLock

        // Warning: The following is very ethnocentric to Earth C-137 North-Western ... Quadrasphere(?)
        val maxLat = region.farRight!!.latitude.toFloat()
        val minLat = region.nearRight!!.latitude.toFloat()
        val maxLon = region.farRight!!.longitude.toFloat()
        val minLon = region.farLeft!!.longitude.toFloat()

        return Flowables.combineLatest(
                // Include Art in-region (plus any favorites)
                db.artDao().findInRegionOrFavorite(maxLat, minLat, maxLon, minLon),
                // Camps remain favorites-only to limit density
                db.campDao().favorites,
                // Include Event favorites
                db.eventDao().favorites,
                // Always include all user POIs
                db.userPoiDao().getAll())
        { arts, camps, events, userpois ->
            val all = ArrayList<PlayaItemWithUserData>(arts.size + camps.size + events.size + userpois.size)
            all.addAll(arts)
            all.addAll(camps)
            all.addAll(events)
            all.addAll(userpois)
            all
        }
    }

    /**
     * Returns favorites and user-added markers only
     */
    fun observeUserAddedMapItemsOnly(): Flowable<List<PlayaItemWithUserData>> {
        // TODO : Honor upgradeLock
        val nowDate = CurrentDateProvider.getCurrentDate()
        val now = DateUtil.getIso8601Format().format(nowDate)

        return Flowables.combineLatest(
                db.artDao().favorites,
                db.campDao().favorites,
                db.eventDao().getNonExpiredFavorites(now),
                db.userPoiDao().getAll())
        { arts, camps, events, userpois ->
            val all = ArrayList<PlayaItemWithUserData>(arts.size + camps.size + events.size + userpois.size)
            all.addAll(arts)
            all.addAll(camps)
            all.addAll(events)
            all.addAll(userpois)
            all
        }
    }

    fun getUserPoi(): Flowable<List<UserPoiWithUserData>> {
        return db.userPoiDao().getAll()
    }

    fun getUserPoiByPlayaId(playaId: String): Flowable<UserPoiWithUserData> {
        return db.userPoiDao().findByPlayaId(playaId)
    }

    fun insertUserPoi(poi: UserPoi) {
        db.userPoiDao().insert(poi)
    }
    fun insertUserPois(poi: List<UserPoi>) {
        db.userPoiDao().insert(*poi.toTypedArray())
    }

    fun deleteUserPoi(poi: UserPoi) {
        db.userPoiDao().delete(poi)
    }

    fun updateFavorites(playaIds: List<String>, isFavorite: Boolean) {
        if (isFavorite) {
            val favs = playaIds.map { Favorite(it) }
            db.favoriteDao().insert(*favs.toTypedArray())
        } else {
            db.favoriteDao().deleteByPlayaIds(playaIds)
        }
    }

    fun update(item: PlayaItem) {
        if (item is Art) {
            db.artDao().update(item)
        } else if (item is Event) {
            db.eventDao().update(item)
        } else if (item is Camp) {
            db.campDao().update(item)
        } else if (item is UserPoi) {
            db.userPoiDao().update(item)
        } else {
            Timber.e("Cannot update item of unknown type")
        }
    }

    fun toggleFavorite(item: PlayaItem) {
        val start = if (item is Event) item.startTime ?: "" else ""
        val pId = item.playaId
        if (pId == null) {
            Timber.e("Cannot toggle favorite for item with null playaId")
            return
        }
        val count = db.favoriteDao().count(pId, start)
        if (count > 0) {
            Timber.d("Removing favorite for %s", pId)
            db.favoriteDao().delete(pId, start)
        } else {
            Timber.d("Adding favorite for %s", pId)
            db.favoriteDao().insert(Favorite(pId, start))
        }
    }

    companion object {

        /**
         * Version of database data and mbtiles. This is basically the unix time at which bundled data was provided to this build.
         */
        val RESOURCES_VERSION: Long = 1692570508202L // Unix time of creation

        private var provider: DataProvider? = null

        //    private ArrayDeque<BriteDatabase.Transaction> transactionStack = new ArrayDeque<>();

        fun getInstance(context: Context): Observable<DataProvider> {

            // TODO : This ain't thread safe

            if (provider != null) return Observable.just(provider!!).subscribeOn(Schedulers.io())

            val prefs = PrefsHelper(context)

            return Observable.just(getSharedDb(context))
                    .subscribeOn(Schedulers.io())
                    .doOnNext { database ->
                        prefs.setBaseResourcesVersion(RESOURCES_VERSION)
                    }
                    .map { sqlBrite -> DataProvider(context, sqlBrite) }
                    .doOnNext { dataProvider -> provider = dataProvider }
        }

        fun getNewInstance(context: Context, name: String): Observable<DataProvider> {
            val prefs = PrefsHelper(context)
            return Observable.fromCallable { newDatabase(context, name) }
                .subscribeOn(Schedulers.io())
                .map { db -> DataProvider(context, db) }
        }

        /**
         * Add wildcards to the beginning and end of a query term

         * @return "%{@param query}%"
         */
        private fun addWildcardsToQuery(query: String): String {
            return "%$query%"
        }
    }

    data class SectionedPlayaItems(val data: List<PlayaItemWithUserData>,
                                   val ranges: List<IntRange>)
}
