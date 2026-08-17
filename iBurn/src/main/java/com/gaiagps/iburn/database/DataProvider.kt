package com.gaiagps.iburn.database

import android.content.ContentValues
import android.content.Context
import com.gaiagps.iburn.AudioTourManager
import com.gaiagps.iburn.CurrentDateProvider
import com.gaiagps.iburn.DateUtil
import com.gaiagps.iburn.PrefsHelper
import com.gaiagps.iburn.database.Favorite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.maplibre.android.geometry.VisibleRegion
import timber.log.Timber
import java.util.Calendar
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

    fun deleteMutantVehicles(): Int = clearTable(MutantVehicle.TABLE_NAME)

    private fun clearTable(tablename: String): Int {
        return db.openHelper.writableDatabase.delete(tablename, null, null)
    }

    fun observeCamps(): Flow<List<CampWithUserData>> {
        return db.campDao().all
    }

    fun observeCampFavorites(): Flow<List<CampWithUserData>> {

        // TODO : Honor upgradeLock?
        return db.campDao().favorites
    }

    fun observeCampsByName(query: String): Flow<List<CampWithUserData>> {
        // TODO : Honor upgradeLock
        val wildQuery = addWildcardsToQuery(query)
        return db.campDao().findByName(wildQuery)
    }

    fun observeCampByPlayaId(playaId: String): Flow<CampWithUserData> {
        return db.campDao().findByPlayaId(playaId)
    }

    fun observeCampById(id: Int): Flow<CampWithUserData> {
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

    fun insertAndReturnId(table: String, values: ContentValues): Long {
        return db.openHelper.writableDatabase.insert(table, 0, values)
    }

    fun delete(table: String): Int {
        when (table) {
            Camp.TABLE_NAME -> return deleteCamps()
            Art.TABLE_NAME -> return deleteArt()
            MutantVehicle.TABLE_NAME -> return deleteMutantVehicles()
            EventDefinition.TABLE_NAME -> return deleteEvents()
            else -> Timber.w("Cannot clear unknown table name '%s'", table)
        }
        return 0
    }

    fun deleteEvents(): Int {
        clearTable(EventOccurrence.TABLE_NAME)
        return clearTable(EventDefinition.TABLE_NAME)

        //        return db.getOpenHelper().getWritableDatabase().delete(Event.TABLE_NAME, "*", null);
        //        Cursor result = db.query("DELETE FROM event; VACUUM", null);
        //        if (result != null) result.close();
    }

    suspend fun observeEventByPlayaId(id: String): EventWithUserData {
        return db.eventDao().getByPlayaId(id)
    }

    suspend fun observeEventById(id: Int): EventWithUserData {
        return db.eventDao().getById(id)
    }

    fun observeEventsOnDayOfTypes(day: String?,
                                  types: ArrayList<String>?,
                                  includeExpired: Boolean,
                                  eventTiming: String): Flow<List<EventWithUserData>> {

        // TODO : Honor upgradeLock?
        val now = CurrentDateProvider.getCurrentDate().time

        // Handle "all days" case when day is null or empty
        if (day == null || day.isEmpty()) {
            if (types == null || types.isEmpty()) {
                if(eventTiming=="timed"){
                    if(includeExpired == true) {
                        return db.eventDao().findAllDaysTimed()
                    }
                    else{
                        return db.eventDao().findAllDaysNoExpiredTimed(now)
                    }
                }
                else{
                    return db.eventDao().findAllDaysAllDay()
                }
            } else {
                if(eventTiming=="timed"){
                    if(includeExpired == true) {
                        return db.eventDao().findAllDaysAndTypeTimed(types)
                    }
                    else{
                        return db.eventDao().findAllDaysAndTypeNoExpiredTimed(types, now)
                    }
                }
                else{
                    return db.eventDao().findAllDaysAndTypeAllDay(types)
                }
            }
        }

        val (dayStart, dayEnd) = dayBounds(day)

        if (types == null || types.isEmpty()) {
            if(eventTiming=="timed"){
                if(includeExpired == true) {
                    return db.eventDao().findByDayTimed(dayStart, dayEnd)
                }
                else{
                    return db.eventDao().findByDayNoExpiredTimed(dayStart, dayEnd, now)
                }
            }
            else{
                return db.eventDao().findByDayAllDay(dayStart, dayEnd)
            }
        } else {
            if(eventTiming=="timed"){
                if(includeExpired == true) {
                    return db.eventDao().findByDayAndTypeTimed(dayStart, dayEnd, types)
                }
                else{
                    return db.eventDao().findByDayAndTypeNoExpiredTimed(
                        dayStart,
                        dayEnd,
                        types,
                        now
                    )
                }
            }
            else{
                return db.eventDao().findByDayAndTypeAllDay(dayStart, dayEnd, types)
            }
        }
    }

    private fun dayBounds(day: String): Pair<Long, Long> {
        val parts = day.split("/")
        require(parts.size == 2) { "Expected event day in M/d format: $day" }
        val calendar = Calendar.getInstance(DateUtil.PLAYA_TIME_ZONE).apply {
            time = CurrentDateProvider.getCurrentDate()
            set(Calendar.MONTH, parts[0].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, parts[1].toInt())
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return start to calendar.timeInMillis
    }

    fun observeEventsHostedByCamp(camp: Camp): Flow<List<EventWithUserData>> {
        return db.eventDao().findByCampPlayaId(camp.playaId)
    }

    fun observeOtherOccurrencesOfEvent(event: Event): Flow<List<EventWithUserData>> {
        return db.eventDao().findOtherOccurrences(event.eventId, event.id)
    }

    fun observeEventFavorites(): Flow<List<EventWithUserData>> {

        // TODO : Honor upgradeLock?
        return db.eventDao().favorites
    }

    fun observeEventBetweenDates(start: Date, end: Date): Flow<List<EventWithUserData>> {

        // TODO : Honor upgradeLock?
        Timber.d("Start time between %s and %s", start, end)
        return db.eventDao().findInDateRange(start.time, end.time)
    }

    fun deleteArt(): Int {
        return clearTable(Art.TABLE_NAME)
        //        return db.getOpenHelper().getWritableDatabase().delete(Art.TABLE_NAME, null, null);
        //        Cursor result = db.query("DELETE FROM art; VACUUM", null);
        //        if (result != null) result.close();
    }

    fun observeArt(): Flow<List<ArtWithUserData>> {
        // TODO : Honor upgradeLock?
        return db.artDao().all
    }

    fun observeArtFavorites(): Flow<List<ArtWithUserData>> {
        // TODO : Honor upgradeLock?
        return db.artDao().favorites
    }

    fun observeArtWithAudioTour(): Flow<List<ArtWithUserData>> {

        // TODO : Honor upgradeLock?
        return db.artDao().all.map { it.filter {
            val pId = it.item.playaId
            pId != null && AudioTourManager.hasAudioTour(context, pId)
        } }
    }

    fun observeArtByPlayaId(playaId: String): Flow<ArtWithUserData> {
        return db.artDao().findByPlayaId(playaId)
    }

    fun observeArtById(id: Int): Flow<ArtWithUserData> {
        return db.artDao().findById(id)
    }

    fun observeMutantVehicles(): Flow<List<MutantVehicleWithUserData>> =
        db.mutantVehicleDao().all

    fun observeMutantVehicleFavorites(): Flow<List<MutantVehicleWithUserData>> =
        db.mutantVehicleDao().favorites

    fun observeMutantVehicleByPlayaId(playaId: String): Flow<MutantVehicleWithUserData> =
        db.mutantVehicleDao().findByPlayaId(playaId)

    fun observeMutantVehicleById(id: Int): Flow<MutantVehicleWithUserData> =
        db.mutantVehicleDao().findById(id)

    /**
     * Observe all favorites.
     *
     *
     * Note: This query automatically adds in Event.startTime (and 0 values for all non-events),
     * since we always want to show this data for an event.
     */
    fun observeFavorites(): Flow<SectionedPlayaItems> {

        // TODO : Honor upgradeLock
        // TODO : Return structure with metadata on how many art, camps, events etc?
        return combine(
                db.artDao().favorites,
                db.campDao().favorites,
                db.mutantVehicleDao().favorites,
                db.eventDao().favorites)
        { arts, camps, vehicles, events ->

            val sections = ArrayList<IntRange>(4)
            val items = ArrayList<PlayaItemWithUserData>(arts.size + camps.size + vehicles.size + events.size)

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

            if (vehicles.isNotEmpty()) {
                items.addAll(vehicles)
                val vehiclesRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, vehiclesRangeEnd))
                lastRangeEnd = vehiclesRangeEnd
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
    fun observeFtsQuery(query: String): Flow<SectionedPlayaItems> {

        // TODO : Honor upgradeLock
        // TODO : Return structure with metadata on how many art, camps, events etc?
        val wildQuery = addWildcardsToQuery(query)
        val ftsQuery = sanitizeFtsQuery(query)
        return combine(
                db.artDao().searchFts(ftsQuery),
                db.campDao().searchFts(ftsQuery),
                db.eventDao().searchFts(ftsQuery),
                db.mutantVehicleDao().searchFts(ftsQuery),
                db.userPoiDao().findByName(wildQuery))
        { arts, camps, events, vehicles, userpois ->
            val sections = ArrayList<IntRange>(5)
            val items = ArrayList<PlayaItemWithUserData>(arts.size + camps.size + events.size + vehicles.size + userpois.size)

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


            if (vehicles.isNotEmpty()) {
                items.addAll(vehicles)
                val vehiclesRangeEnd = items.size
                sections.add(IntRange(lastRangeEnd, vehiclesRangeEnd))
                lastRangeEnd = vehiclesRangeEnd
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
    fun observeAllMapItemsInVisibleRegion(region: VisibleRegion): Flow<List<PlayaItemWithUserData>> {
        // TODO : Honor upgradeLock

        // Warning: The following is very ethnocentric to Earth C-137 North-Western ... Quadrasphere(?)
        val maxLat = region.farRight!!.latitude.toFloat()
        val minLat = region.nearRight!!.latitude.toFloat()
        val maxLon = region.farRight!!.longitude.toFloat()
        val minLon = region.farLeft!!.longitude.toFloat()

        return combine(
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
    fun observeUserAddedMapItemsOnly(): Flow<List<PlayaItemWithUserData>> {
        // TODO : Honor upgradeLock
        val now = CurrentDateProvider.getCurrentDate().time

        return combine(
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

    fun getUserPoi(): Flow<List<UserPoiWithUserData>> {
        return db.userPoiDao().getAll()
    }

    fun getUserPoiByPlayaId(playaId: String): Flow<UserPoiWithUserData> {
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
            Timber.w("Bundled events are read-only and cannot be updated individually")
        } else if (item is Camp) {
            db.campDao().update(item)
        } else if (item is MutantVehicle) {
            db.mutantVehicleDao().update(item)
        } else if (item is UserPoi) {
            db.userPoiDao().update(item)
        } else {
            Timber.e("Cannot update item of unknown type")
        }
    }

    fun toggleFavorite(item: PlayaItem) {
        val start = if (item is Event) item.startTime else 0
        val pId = if (item is Event) item.eventUid else item.playaId
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

        fun getInstance(context: Context): DataProvider {
            if (provider != null) return provider!!
            val prefs = PrefsHelper(context)
            val db = getSharedDb(context)
            prefs.setBaseResourcesVersion(RESOURCES_VERSION)
            val dp = DataProvider(context, db)
            provider = dp
            return dp
        }

        fun getNewInstance(context: Context, name: String): DataProvider {
            val db = newDatabase(context, name)
            return DataProvider(context, db)
        }

        // Java-friendly blocking helpers for common lookups
    }

    // Blocking helpers for Java callers
    fun getCampByIdBlocking(id: Int): CampWithUserData = runBlocking { observeCampById(id).first() }
    fun getArtByIdBlocking(id: Int): ArtWithUserData = runBlocking { observeArtById(id).first() }
    fun getEventByIdBlocking(id: Int): EventWithUserData = runBlocking { observeEventById(id) }
    fun getMutantVehicleByIdBlocking(id: Int): MutantVehicleWithUserData = runBlocking { observeMutantVehicleById(id).first() }
    fun getCampByPlayaIdBlocking(playaId: String): CampWithUserData = runBlocking { observeCampByPlayaId(playaId).first() }
    fun getArtByPlayaIdBlocking(playaId: String): ArtWithUserData = runBlocking { observeArtByPlayaId(playaId).first() }
    fun getEventByPlayaIdBlocking(playaId: String): EventWithUserData = runBlocking { observeEventByPlayaId(playaId) }
    fun getMutantVehicleByPlayaIdBlocking(playaId: String): MutantVehicleWithUserData = runBlocking { observeMutantVehicleByPlayaId(playaId).first() }

        /**
         * Add wildcards to the beginning and end of a query term

         * @return "%{@param query}%"
         */
        private fun addWildcardsToQuery(query: String): String {
            return "%$query%"
        }

        /**
         * Sanitize a raw user query for use with SQLite FTS MATCH.
         *
         * - Double any embedded double quotes to avoid malformed expressions.
         * - Wrap the entire query in double quotes so it is treated as a phrase,
         *   preventing special characters from breaking parsing.
         */
        private fun sanitizeFtsQuery(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return trimmed
            val escaped = trimmed.replace("\"", "\"\"")
            return "\"$escaped\""
        }
    }

    data class SectionedPlayaItems(val data: List<PlayaItemWithUserData>,
                                   val ranges: List<IntRange>)
