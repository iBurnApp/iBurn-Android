package com.gaiagps.iburn.fragment

import android.preference.PreferenceManager
import com.gaiagps.iburn.CurrentDateProvider
import com.gaiagps.iburn.PrefsHelper
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter
import com.gaiagps.iburn.database.*
import com.mapbox.mapboxsdk.geometry.VisibleRegion
import com.squareup.sqlbrite.SqlBrite
import rx.Observable
import timber.log.Timber

/**
 * Created by dbro on 6/8/17.
 */
class MapFragmentHelper {
    internal val PROJECTION = arrayOf(PlayaItemTable.name, PlayaItemTable.id, PlayaItemTable.latitude, PlayaItemTable.longitude, PlayaItemTable.favorite)

    internal val PROJECTION_STRING = DataProvider.makeProjectionString(PROJECTION)

    internal var geoWhereClause = String.format("(%s < ? AND %s > ?) AND (%s < ? AND %s > ?)",
            PlayaItemTable.latitude, PlayaItemTable.latitude,
            PlayaItemTable.longitude, PlayaItemTable.longitude)

    internal var ongoingWhereClause = String.format("(%s < ? AND %s > ?) ",
            EventTable.startTime, EventTable.endTime)

    internal var notExpiredWhereClause = String.format("(%s > ?) ",
            EventTable.endTime)

    internal var isFavoriteWhereClause = PlayaItemTable.favorite + " = 1"

    internal var sqlParemeters = arrayOfNulls<String>(11/*15*/)


    fun performQuery(provider: DataProvider, visibleRegion: VisibleRegion, prefs: PrefsHelper, isShowcaseMode: Boolean): Observable<SqlBrite.Query> {
        Timber.d("performQuery")
        // Query all items, not just POIs, if we have a visibleRegion and Embargo is inactive
        // POI table is not affected by Embargo
        val queryVisibleRegion = visibleRegion != null && visibleRegion.farLeft != null && !Embargo.isEmbargoActive(prefs)

        // Don't show non-POI items if we're showcasing a marker to keep the map clear
        val queryNonUserItems = !isShowcaseMode && !Embargo.isEmbargoActive(prefs)

        val sql = StringBuilder()

        // Select User POIs
        sql.append("SELECT ").append(PROJECTION_STRING.replace(PlayaItemTable.favorite, UserPoiTable.drawableResId + " AS " + PlayaItemTable.favorite)).append(", ").append(4).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.POIS)

        if (queryNonUserItems) {
            // Select Events
            sql.append(" UNION ")
                    .append("SELECT ").append(PROJECTION_STRING).append(", ").append(3).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.EVENTS)
                    .append(" WHERE (")
                    .append(isFavoriteWhereClause)
                    .append(" AND ")
                    .append(notExpiredWhereClause)
                    .append(")")

            if (queryVisibleRegion) {
                sql.append(" OR (")
                        .append(ongoingWhereClause)
                        .append(" AND ")
                        .append(geoWhereClause)
                        .append(')')
            }

            // Select Art
            sql.append(" UNION ")
                    .append("SELECT ").append(PROJECTION_STRING).append(", ").append(2).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.ART)
                    .append(" WHERE ")
                    .append(isFavoriteWhereClause)

            if (queryVisibleRegion) {
                sql.append(" OR ")
                        .append(geoWhereClause)
            }

            // Select Camps
            /*
            sql.append(" UNION ")
                    .append("SELECT ").append(PROJECTION_STRING).append(", ").append(1).append(" AS ").append(DataProvider.VirtualType).append(" FROM ").append(PlayaDatabase.CAMPS)
                    .append(" WHERE ")
                    .append(isFavoriteWhereClause);

            if (queryVisibleRegion) {
                sql.append(" OR ")
                        .append(geoWhereClause);
            }
            */

            // Set visible region query parameters
            if (queryVisibleRegion) {
                // Event time
                sqlParemeters[0] = PlayaDateTypeAdapter.iso8601Format.format(CurrentDateProvider.getCurrentDate())
                sqlParemeters[2] = PlayaDateTypeAdapter.iso8601Format.format(CurrentDateProvider.getCurrentDate())
                sqlParemeters[1] = sqlParemeters[2]

                // Event, Art, Camp Geo
                sqlParemeters[7] /*= sqlParemeters[10]*/ = visibleRegion.farLeft.latitude.toString()
                sqlParemeters[3] = sqlParemeters[7]
                sqlParemeters[8] /*= sqlParemeters[11]*/ = visibleRegion.nearRight.latitude.toString()
                sqlParemeters[4] = sqlParemeters[8]
                sqlParemeters[9] /*= sqlParemeters[12]*/ = visibleRegion.nearRight.longitude.toString()
                sqlParemeters[5] = sqlParemeters[9]
                sqlParemeters[10] /*= sqlParemeters[13]*/ = visibleRegion.farLeft.longitude.toString()
                sqlParemeters[6] = sqlParemeters[10]
            }
        }
        if (queryNonUserItems) {
            val arguments : Array<String>? = if (queryVisibleRegion) (sqlParemeters as Array<String>) else null

            if (arguments == null) {
                return provider.createQuery(PlayaDatabase.ALL_TABLES, sql.toString(), null)
            } else {
                return provider.createQuery(PlayaDatabase.ALL_TABLES, sql.toString(), *arguments)
            }
        } else {
            return provider.createQuery(PlayaDatabase.POIS, sql.toString())
        }
    }
}