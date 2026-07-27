package com.gaiagps.iburn.api

import android.content.ContentValues
import android.content.Context
import androidx.annotation.NonNull
import com.gaiagps.iburn.DateUtil
import com.gaiagps.iburn.PrefsHelper
import com.gaiagps.iburn.BuildConfig
import com.gaiagps.iburn.adapters.AdapterUtils
import com.gaiagps.iburn.api.response.EventOccurrence
import com.gaiagps.iburn.api.response.PlayaItem as ApiPlayaItem
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter
import com.gaiagps.iburn.database.*
import com.gaiagps.iburn.js.Geocoder
import kotlinx.coroutines.runBlocking
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.text.DateFormat
import java.util.*
import java.util.concurrent.Executors

class IBurnService(@NonNull context: Context) {
    private val context = context.applicationContext
    private val service: IBurnApi
    private val cachedLocations = HashMap<String, com.gaiagps.iburn.api.response.Location>()
    private val cachedUnofficialLocations = HashMap<String, com.gaiagps.iburn.api.response.Location>()
    private val apiDateFormat: DateFormat = PlayaDateTypeAdapter.buildIso8601Format()

    init {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Date::class.java, PlayaDateTypeAdapter())
            .create()

        val interceptor = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.IBURN_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        service = retrofit.create(IBurnApi::class.java)
    }

    constructor(@NonNull context: Context, api: IBurnApi) : this(context) {
        // Replace created service with provided one
        thisService = api
    }

    // Workaround for secondary constructor overriding service
    private var thisService: IBurnApi? = null
    private val api: IBurnApi
        get() = thisService ?: service

    suspend fun updateData(): Boolean {
        val provider = DataProvider.getInstance(context)
        return updateData(provider)
    }

    // Java-friendly wrappers
    fun updateDataBlocking(): Boolean = kotlinx.coroutines.runBlocking { updateData() }
    fun updateDataBlocking(provider: DataProvider): Boolean = kotlinx.coroutines.runBlocking { updateData(provider) }

    suspend fun updateData(provider: DataProvider): Boolean {
        Timber.d("Attempting data update...")
        val storage = PrefsHelper(context)
        val manifest = api.getDataManifest()

        cachedLocations.clear()
        cachedUnofficialLocations.clear()

        val toUpdate = listOf(manifest.art, manifest.camps, manifest.events)
            .filter { shouldUpdateResource(storage, it) }

        if (toUpdate.isEmpty()) return true

        provider.beginUpgrade()
        try {
            for (resource in toUpdate) {
                val count = updateResource(provider, manifest, resource)
                Timber.d("Updated %s -> %d rows", resource.file, count)
                if (count > 0) storage.setResourceVersion(resource.file, resource.updated.time)
            }
        } catch (t: Throwable) {
            Timber.e(t, "updateData error")
            return false
        } finally {
            provider.endUpgrade()
            cachedLocations.clear()
            cachedUnofficialLocations.clear()
        }
        return true
    }

    private suspend fun updateResource(
        provider: DataProvider,
        manifest: com.gaiagps.iburn.api.response.DataManifest,
        resource: com.gaiagps.iburn.api.response.ResourceManifest
    ): Long {
        return when (resource.file) {
            manifest.art.file -> updateArt(provider)
            manifest.camps.file -> updateCamps(provider)
            manifest.events.file -> updateEvents(provider)
            else -> 0L
        }
    }

    private fun shouldUpdateResource(storage: PrefsHelper, resource: com.gaiagps.iburn.api.response.ResourceManifest): Boolean {
        val should = storage.getResourceVersion(resource.file) < resource.updated.time
        Timber.d("%s version local:%d remote:%d. Will update: %b", resource.file, storage.getResourceVersion(resource.file), resource.updated.time, should)
        return should
    }

    private suspend fun updateArt(provider: DataProvider): Long {
        Timber.d("Updating art")
        val items = api.getArt().distinctBy { it.uid }
        return updateTable(provider, items, Art.TABLE_NAME) { item, values, database ->
            val art = item as com.gaiagps.iburn.api.response.Art
            values.put(Art.ARTIST, art.artist)
            values.put(Art.ARTIST_LOCATION, art.artistLocation)
            if (art.images != null && art.images.size > 0) {
                values.put(Art.IMAGE_URL, art.images[0].thumbnail_url)
            }
            database.insert(values)
        }
    }

    private suspend fun updateCamps(provider: DataProvider): Long {
        Timber.d("Updating Camps")
        val items = api.getCamps().distinctBy { it.uid }
        return updateTable(provider, items, Camp.TABLE_NAME) { item, values, database ->
            values.put(Camp.HOMETOWN, (item as com.gaiagps.iburn.api.response.Camp).hometown)
            database.insert(values)
        }
    }

    private suspend fun updateEvents(provider: DataProvider): Long {
        Timber.d("Updating Events")
        val items = api.getEvents().distinctBy { it.uid }
        return updateTable(provider, items, Event.TABLE_NAME) { item, values, database ->
            val event = item as com.gaiagps.iburn.api.response.Event
            if (event.occurrenceSet == null) return@updateTable

            values.put(PlayaItem.NAME, event.title)
            values.put(Event.ALL_DAY, event.allDay)
            values.put(Event.CHECK_LOC, event.checkLocation)
            values.put(Event.TYPE, event.eventType?.abbr ?: AdapterUtils.EVENT_TYPE_ABBREVIATION_UNKNOWN)
            if (event.hostedByCamp != null) values.put(Event.CAMP_PLAYA_ID, event.hostedByCamp)
            if (event.locatedAtArt != null) values.put(Event.ART_PLAYA_ID, event.locatedAtArt)

            val occurrences = ArrayList<EventOccurrence>(event.occurrenceSet)
            occurrences.sortWith { o1, o2 ->
                val t1 = o1.startTime
                val t2 = o2.startTime
                when {
                    t1 === t2 -> 0
                    t1 == null -> -1
                    t2 == null -> 1
                    else -> t1.compareTo(t2)
                }
            }

            var index = 0
            for (occurrence in occurrences) {
                values.put(PlayaItem.PLAYA_ID, event.uid + "-" + index)
                values.put(Event.START_TIME, apiDateFormat.format(occurrence.startTime))
                val timeDayFormatter = DateUtil.getPlayaTimeFormat("EE M/d h:mm a")
                val dayFormatter = DateUtil.getPlayaTimeFormat("EE M/d")
                values.put(
                    Event.START_TIME_PRETTY,
                    if (event.allDay) dayFormatter.format(occurrence.startTime) else timeDayFormatter.format(occurrence.startTime)
                )
                values.put(Event.END_TIME, apiDateFormat.format(occurrence.endTime))
                values.put(
                    Event.END_TIME_PRETTY,
                    if (event.allDay) dayFormatter.format(occurrence.endTime) else timeDayFormatter.format(occurrence.endTime)
                )
                database.insert(values)
                index++
            }
        }
    }

    private suspend fun updateTable(
        provider: DataProvider,
        items: List<out ApiPlayaItem>,
        tableName: String,
        binder: (ApiPlayaItem, ContentValues, DataBaseSink) -> Unit
    ): Long {
        val values = ContentValues()
        var count = 0L
        try {
            provider.beginTransaction()
            val numDeleted = provider.delete(tableName)
            Timber.d("Deleted %d existing rows. Beginning %s inserts", numDeleted, tableName)

            for (item in items) {
                values.clear()
                bindBaseValues(item, values)
                binder(item, values) { finalValues -> provider.insert(tableName, finalValues) }
                count++
            }
            Timber.d("Successfully closing %s transaction", tableName)
            provider.setTransactionSuccessful()
        } catch (t: Throwable) {
            Timber.e(t, "Error. Rolling back %s transaction", tableName)
        } finally {
            provider.endTransaction()
        }
        return count
    }

    private fun bindBaseValues(item: ApiPlayaItem, values: ContentValues) {
        values.put(PlayaItem.NAME, item.name ?: "?")
        values.put(PlayaItem.CONTACT, item.contactEmail)
        values.put(PlayaItem.DESC, item.description)
        values.put(PlayaItem.PLAYA_ID, item.uid)

        if (item is com.gaiagps.iburn.api.response.Event) {
            val event = item
            repairEventOccurrenceTimes(event)
            val locationPlayaId = event.hostedByCamp ?: event.locatedAtArt
            if (locationPlayaId != null) {
                cachedLocations[locationPlayaId]?.let { loc ->
                    item.location = loc
                    item.locationString = item.location.locationString()
                }
                cachedUnofficialLocations[locationPlayaId]?.let { loc ->
                    item.burnermap_location = loc
                }
            }
        } else {
            if (item.location != null) {
                var locationStr = item.locationString
                if (!locationStr.isNullOrEmpty()) {
                    locationStr = locationStr.replace("None None", "")
                }
                var location = com.gaiagps.iburn.api.response.Location.fromLocation(item.location)
                if (!locationStr.isNullOrEmpty() && location.gps_latitude == 0.0 && location.gps_longitude == 0.0) {
                    val response = runBlocking { Geocoder.forwardGeocode(context, locationStr) }
                    location.gps_latitude = response.latitude
                    location.gps_longitude = response.longitude
                    item.location.gps_latitude = response.latitude
                    item.location.gps_longitude = response.longitude
                }
                cachedLocations[item.uid] = location
            }
            if (item.burnermap_location != null) {
                val loc = com.gaiagps.iburn.api.response.Location().apply {
                    gps_latitude = item.burnermap_location.gps_latitude
                    gps_longitude = item.burnermap_location.gps_longitude
                    frontage = item.burnermap_location.frontage
                    intersectionType = item.burnermap_location.intersectionType
                    intersection = item.burnermap_location.intersection
                }
                cachedUnofficialLocations[item.uid] = loc
            }
        }

        if (item.location != null) {
            values.put(PlayaItem.LATITUDE, item.location.gps_latitude)
            values.put(PlayaItem.LONGITUDE, item.location.gps_longitude)
            values.put(PlayaItem.PLAYA_ADDR, item.locationString)
        } else {
            values.put(PlayaItem.LATITUDE, 0)
            values.put(PlayaItem.LONGITUDE, 0)
        }
        if (item.burnermap_location != null) {
            values.put(PlayaItem.LATITUDE_UNOFFICIAL, item.burnermap_location.gps_latitude)
            values.put(PlayaItem.LONGITUDE_UNOFFICIAL, item.burnermap_location.gps_longitude)
            values.put(PlayaItem.PLAYA_ADDR_UNOFFICIAL, item.burnermap_location.locationString())
        } else {
            values.put(PlayaItem.LATITUDE_UNOFFICIAL, 0)
            values.put(PlayaItem.LONGITUDE_UNOFFICIAL, 0)
        }
        values.put(PlayaItem.URL, item.url)
    }

    private fun repairEventOccurrenceTimes(event: com.gaiagps.iburn.api.response.Event?) {
        if (event == null || event.occurrenceSet == null) return
        for (occurrence in event.occurrenceSet) {
            val startDate = occurrence.startTime
            val endDate = occurrence.endTime
            if (startDate == null || endDate == null) continue
            if (endDate.before(startDate)) {
                val calStart = Calendar.getInstance().apply { time = startDate }
                val calEndTime = Calendar.getInstance().apply { time = endDate }
                val corrected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, calStart.get(Calendar.YEAR))
                    set(Calendar.MONTH, calStart.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, calStart.get(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, calEndTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, calEndTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, calEndTime.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, 0)
                }
                var correctedEndDate = corrected.time
                if (!correctedEndDate.after(startDate)) {
                    corrected.add(Calendar.DAY_OF_MONTH, 1)
                    correctedEndDate = corrected.time
                }
                Timber.d("Fixed event '%s': Start %s -> End %s (was %s)", event.title, startDate, correctedEndDate, endDate)
                occurrence.endTime = correctedEndDate
            }
        }
    }

    fun interface DataBaseSink { fun insert(values: ContentValues) }
}
