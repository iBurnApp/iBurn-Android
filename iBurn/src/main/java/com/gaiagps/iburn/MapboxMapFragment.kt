package com.gaiagps.iburn


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gaiagps.iburn.activity.PlayaItemViewActivity
import com.gaiagps.iburn.database.*
import com.gaiagps.iburn.location.LocationProvider
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.VisibleRegion
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.List
import kotlin.collections.dropLastWhile
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.set
import kotlin.collections.toTypedArray


class MapboxMapFragment : Fragment() {

    /**
     * Geographic Bounds of Black Rock City
     * Used to determining whether a location lies
     * within the general vicinity
     */
    val MAX_LAT = 40.807569
    val MAX_LON = -119.181100
    val MIN_LAT = 40.764355
    val MIN_LON = -119.236979

    private val cameraBounds = LatLngBounds.Builder()
            .include(LatLng(MAX_LAT, MIN_LON))
            .include(LatLng(MIN_LAT, MAX_LON))
            .build()

    private enum class State {
        /**
         * Default. Constantly search and show POIs within the viewable map region
         */
        EXPLORE,
        /**
         * Showcase a particular POI and its relation to the user home camp / location
         */
        SHOWCASE,
        /**
         * Show search results
         */
        SEARCH
    }

    private var state = State.EXPLORE

    private val defaultZoom = 12.5
    private val markerShowcaseZoom = 14.5
    private val poiVisibleZoom = 14.0

    private var mapView: MapView? = null
    private var onMapReadyCallback: OnMapReadyCallback? = null

    private var showcaseMarker: MarkerOptions? = null

    private val cameraUpdate = PublishSubject.create<VisibleRegion>()
    private var cameraUpdateSubscription: Disposable? = null

    /**
     * Showcase a point on the map using a generic pin
     */
    fun showcaseLatLng(latLng: LatLng) {
        val marker = MarkerOptions().icon(iconGeneric).position(latLng)
        showcaseMarker(marker)
    }

    /**
     * Showcase a specific marker on the map. If you just need a generic map pin use [showcaseLatLng]
     */
    fun showcaseMarker(marker: MarkerOptions) {
        state = State.SHOWCASE
        showcaseMarker = marker
        if (isResumed) {
            _showcaseMarker(marker)
        }
    }

    private fun _showcaseMarker(marker: MarkerOptions) {
        Timber.d("_showcaseMarker")
        mapMarkerAndFitEntireCity(marker)
//        if (locationSubscription != null) {
//            Timber.d("unsubscribing from location")
//            locationSubscription.unsubscribe()
//            locationSubscription = null
//        }
//        if (addressLabel != null) addressLabel.setVisibility(View.INVISIBLE)
//        val poiBtn = activity.findViewById(R.id.mapPoiBtn) as ImageButton
//        if (poiBtn != null) {
//            poiBtn.visibility = View.GONE
//        }
//        showcaseMarker = null
    }

    private fun mapMarkerAndFitEntireCity(marker: MarkerOptions) {
//        latLngToCenterOn = marker.position
        mapView?.getMapAsync { map ->

            map.addMarker(marker)
            Timber.d("Moving camera to %s at zoom %f", marker.position, markerShowcaseZoom)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.position, markerShowcaseZoom))

            Observable.timer(5, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { ignored ->
                        Timber.d("Animating camera")
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(Geo.MAN_LAT, Geo.MAN_LON), defaultZoom), 3 * 1000)
                    }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mapView?.let { mapView ->
            setupMap(mapView)
        }

        val showcaseMarker = this.showcaseMarker
        if (state == State.SHOWCASE && showcaseMarker != null) {
            _showcaseMarker(showcaseMarker)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        inflater?.let { inflater ->
            val context = inflater.context
            val options = MapFragmentUtils.resolveArgs(context, this.arguments)
            val mapView = MapView(context, options)
            this.mapView = mapView
            return this.mapView
        }
        return null
    }

    private fun setupMap(mapView: MapView) {
        mapView.setStyleUrl("mapbox://styles/dchiles/cj3nxjqli000u2soyeb947f7s")
        val pos = CameraPosition.Builder()
                .target(LatLng(Geo.MAN_LAT, Geo.MAN_LON))
                .zoom(defaultZoom)
                .build()

        mapView.getMapAsync { map ->
            if (BuildConfig.MOCK) {
                // TODO : Re-enable mock location after crash resolved
                // https://github.com/mapbox/mapbox-gl-native/pull/9142
                val mockEngine = LocationProvider.MapboxMockLocationSource()
                map.setLocationSource(mockEngine)
            }
            map.myLocationViewSettings.foregroundTintColor = context.resources.getColor(R.color.map_my_location)
            map.myLocationViewSettings.accuracyTintColor = context.resources.getColor(R.color.map_my_location)
            // TODO : Re-enable location after crash resolved
            // https://github.com/mapbox/mapbox-gl-native/pull/9142
            map.isMyLocationEnabled = true
            map.setMinZoomPreference(defaultZoom)
            map.setLatLngBoundsForCameraTarget(cameraBounds)
            map.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
            map.uiSettings.setAllGesturesEnabled(state != State.SHOWCASE)
            map.setOnCameraIdleListener {
                if (!shouldShowPoisAtZoom(map.cameraPosition.zoom) && areMarkersVisible()) {
                    clearMap(false)
                } else {
                    cameraUpdate.onNext(map.projection.visibleRegion)
                }
            }

            map.setOnInfoWindowClickListener { marker ->
                if (markerIdToItem.containsKey(marker.id)) {
                    val item = markerIdToItem[marker.id]!!
                    val i = Intent(activity.applicationContext, PlayaItemViewActivity::class.java)
                    i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM, item)
                    activity.startActivity(i)
                } else if (mappedCustomMarkerIds.containsKey(marker.id)) {
                    showEditPinDialog(marker)
                }
                true
            }

            val prefsHelper = PrefsHelper(activity.applicationContext)
            Timber.d("Subscribing to camera updates")
            cameraUpdateSubscription?.dispose()
            cameraUpdateSubscription = cameraUpdate
                    .debounce(250, TimeUnit.MILLISECONDS)
                    .flatMap { visibleRegion ->
                        DataProvider.getInstance(activity.applicationContext)
                                .map { provider -> Pair(provider, visibleRegion) }
                    }
                    .flatMap { (provider, visibleRegion) ->

                        val embargoActive = Embargo.isEmbargoActive(prefsHelper)
                        val queryAllItems = (state != State.SHOWCASE) && (!embargoActive) && shouldShowPoisAtZoom(map.cameraPosition.zoom)

                        if (queryAllItems) {
                            Timber.d("Map query for all items at zoom %f", map.cameraPosition.zoom)
                            provider.observeAllMapItemsInVisibleRegion(visibleRegion).toObservable()
                        } else {
                            Timber.d("Map query for user items at zoom %f", map.cameraPosition.zoom)
                            (provider.observeUserAddedMapItemsOnly()).toObservable()
                        }
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { items: List<PlayaItem> ->
                        processMapItemResult(items)
                    }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView?.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        mapView?.getMapAsync(this.onMapReadyCallback)
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()

        Timber.d("Unsubscribing from camera updates")
        cameraUpdateSubscription?.dispose()
    }

    fun getMapAsync(onMapReadyCallback: OnMapReadyCallback) {
        this.onMapReadyCallback = onMapReadyCallback
    }

    /**
     * Map of user added pins. Mapbox Marker Id -> PlayaItem
     */
    internal var mappedCustomMarkerIds = HashMap<Long, PlayaItem>()

    /**
     * Set to avoid plotting duplicate items
     */
    internal var mappedItems = HashSet<PlayaItem>()
    /**
     * Map of pins shown in response to explore or search
     */
    private val MAX_POIS = 100

    // Markers that should only be cleared on new query arrival
    internal var permanentMarkers = HashSet<Marker>()
    // Markers that should be cleared on camera events
    internal var mappedTransientMarkers = ArrayDeque<Marker>(MAX_POIS)
    internal var markerIdToItem = HashMap<Long, PlayaItem>()

    /**
     * Keep track of the bounds describing a batch of results across Loaders
     */
    private var mResultBounds: LatLngBounds.Builder? = null

    private fun processMapItemResult(items: List<PlayaItem>) {

        clearPermanentMarkers()
        mResultBounds = LatLngBounds.Builder()

        Timber.d("Got result with %d items", items.size)
        mapView?.getMapAsync { map ->

            val currentZoom = map.cameraPosition.zoom

            val itemsWithLocation = items.filter { it.latitude != 0f }
            itemsWithLocation
                    .forEach { item ->
                        if (mappedItems.contains(item)) return@forEach // continue to next item

                        if (item is UserPoi) {
                            // UserPois are always-visible and editable when their info window is clicked
                            val marker = addNewMarkerForItem(map, item)
                            mappedItems.add(item)
                            mappedCustomMarkerIds[marker.id] = item
                        } else if (item.isFavorite) {
                            // Favorites are always-visible, but not editable
                            val marker = addNewMarkerForItem(map, item)
                            markerIdToItem[marker.id] = item
                            mappedItems.add(item)
                            permanentMarkers.add(marker)
                        } else if (shouldShowPoisAtZoom(currentZoom)) {
                            // Other markers are only displayed at near zoom, and are kept in a pool
                            // of recyclable markers. mapRecyclableMarker handles adding to markerIdToItem
                            val marker = mapRecyclableMarker(map, item, mResultBounds)
                            if (marker != null) {
                                mappedItems.add(item)
                            }
                        }
                    }

            // If displaying search results, try to move the camera to include all results
            if (state == State.SEARCH) {
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(mResultBounds!!.build(), 80))
                } catch (e: InvalidLatLngBoundsException) {
                    Timber.w("Search results bounds are invalid. Likely due to no search results")
                    // No mappable results
                    resetMapView(map)
                }
            }
        }
    }

    private fun shouldShowPoisAtZoom(currentZoom: Double): Boolean {
        return currentZoom > poiVisibleZoom
    }

    private val iconFactory: IconFactory by lazy {
        IconFactory.getInstance(context)
    }

    // TODO : Loading many Icons breaks the entire marker rendering system somehow, so we cache 'em:
    // https://github.com/mapbox/mapbox-gl-native/issues/9026

    private val iconGeneric: Icon by lazy {
        iconFactory.fromResource(R.drawable.pin)
    }

    private val iconArt: Icon by lazy {
        iconFactory.fromResource(R.drawable.art_pin)
    }

    private val iconCamp: Icon by lazy {
        iconFactory.fromResource(R.drawable.camp_pin)
    }

    private val iconEvent: Icon by lazy {
        iconFactory.fromResource(R.drawable.event_pin)
    }

    private val iconUserHome: Icon by lazy {
        iconFactory.fromResource(R.drawable.puck_home)
    }

    private val iconUserStar: Icon by lazy {
        iconFactory.fromResource(R.drawable.puck_star)
    }

    private val iconUserBicycle: Icon by lazy {
        iconFactory.fromResource(R.drawable.puck_bicycle)
    }

    private val iconUserHeart: Icon by lazy {
        iconFactory.fromResource(R.drawable.puck_heart)
    }

    private fun addNewMarkerForItem(map: MapboxMap, item: PlayaItem): Marker {
        val pos = LatLng(item.latitude.toDouble(), item.longitude.toDouble())
        val markerOptions: MarkerOptions
        markerOptions = MarkerOptions().position(pos)
                .title(item.name)

        if (item is UserPoi) {
            styleCustomMarkerOption(markerOptions, item.icon)
        } else if (item is Art) {
            markerOptions.icon(iconArt)
        } else if (item is Camp) {
            markerOptions.icon(iconCamp)
        } else if (item is Event) {
            markerOptions.icon(iconEvent)
        }

        val marker = map.addMarker(markerOptions)
        return marker
    }

    /**
     * Apply style to a custom MarkerOptions before
     * adding to Map
     */
    private fun styleCustomMarkerOption(markerOption: MarkerOptions, @UserPoi.Icon poiIcon: String) {
        when (poiIcon) {
            UserPoi.ICON_HOME -> markerOption.icon(iconUserHome)
            UserPoi.ICON_BIKE -> markerOption.icon(iconUserBicycle)
            UserPoi.ICON_HEART -> markerOption.icon(iconUserHeart)
            else -> markerOption.icon(iconUserStar)
        }
    }

    /**
     * Map a marker as part of a finite set of markers, limiting the total markers
     * displayed and recycling markers if this limit is exceeded.
     * @param areBoundsValid a hack one-dimensional boolean array used to report whether boundsBuilder
     * *                       includes at least one point and will not throw an exception on its build()
     */
    private fun mapRecyclableMarker(map: MapboxMap, item: PlayaItem, boundsBuilder: LatLngBounds.Builder?): Marker? {
        val pos = LatLng(item.latitude.toDouble(), item.longitude.toDouble())

        // Assemble search results region boundary
        if (item !is UserPoi && boundsBuilder != null && state == State.SEARCH) {
            if (cameraBounds.contains(pos)) {
                boundsBuilder.include(pos)
            }
        }

        var marker: Marker? = null

        if (mappedTransientMarkers.size == MAX_POIS) {
            // Re-use the eldest Marker
            marker = mappedTransientMarkers.remove()
            marker.position = pos
            marker.title = item.name

            if (item is Art) {
                marker.icon = iconFactory.fromResource(R.drawable.art_pin)
            } else if (item is Camp) {
                marker.icon = iconFactory.fromResource(R.drawable.camp_pin)
            } else if (item is Event) {
                marker.icon = iconFactory.fromResource(R.drawable.event_pin)
            }

//            marker.setAnchor(0.5f, 0.5f)
            mappedTransientMarkers.add(marker)
            markerIdToItem.put(marker.id, item)
        } else {
            // Create a new Marker
            marker = addNewMarkerForItem(map, item)
            markerIdToItem.put(marker.id, item)
            mappedTransientMarkers.add(marker)
        }

        return marker
    }

    /**
     * Clear markers marked permanent. These are not removed due to camera change events.
     * Currently used for user-selected favorite items.
     */
    fun clearPermanentMarkers() {
        for (marker in permanentMarkers) {
            marker.remove()
            val metaId = markerIdToItem.remove(marker.id)
            if (metaId != null) {
                mappedItems.remove(metaId)
            }
        }
        permanentMarkers.clear()
    }

    /**
     * Return a key used internally to keep track of data items currently mapped,
     * helping us avoid mapping duplicate points.

     */
    private fun generateMarkerIdForItem(item: PlayaItem): String {
        return String.format("%s-%d", item.javaClass.simpleName, item.id)
    }

    /**
     * Return the internal database id for an item given the string id
     * generated by [.generateMarkerIdForItem]
     */
    private fun getDatabaseIdFromGeneratedDataId(dataId: String): Int {
        return Integer.parseInt(dataId.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
    }

    private fun resetMapView(map: MapboxMap) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(Geo.MAN_LAT, Geo.MAN_LON), defaultZoom))
    }

    fun clearMap(clearAll: Boolean) {
        if (clearAll) {
            clearPermanentMarkers()
        }

        for (marker in mappedTransientMarkers) {
            marker.remove()
            val metaId = markerIdToItem.remove(marker.id)
            if (metaId != null) {
                mappedItems.remove(metaId)
            }
        }
        mappedTransientMarkers.clear()
    }


    fun areMarkersVisible(): Boolean {
        return mappedTransientMarkers.size > 0
    }

    private fun showEditPinDialog(marker: Marker) {
//        val dialogBody = activity.layoutInflater.inflate(R.layout.dialog_poi, null)
//        val iconGroup = dialogBody.findViewById(R.id.iconGroup) as RadioGroup
//
//        // Fetch current Marker icon
//        DataProvider.getInstance(activity.applicationContext)
//                .flatMap { dataProvider ->
//                    dataProvider.createQuery(PlayaDatabase.POIS,
//                            "SELECT " + PlayaItemTable.id + ", " + UserPoiTable.drawableResId + " FROM " + PlayaDatabase.POIS + " WHERE " + PlayaItemTable.id + " = ?",
//                            getDatabaseIdFromGeneratedDataId(mappedCustomMarkerIds.get(marker.id)!!).toString())
//                }
//                .first()
//                .map<Cursor>({ it.run() })
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { poi ->
//                    if (poi != null && poi.moveToFirst()) {
//                        val drawableResId = poi.getInt(poi.getColumnIndex(UserPoiTable.drawableResId))
//                        when (drawableResId) {
//                            UserPoiTable.STAR -> (iconGroup.findViewById(R.id.btn_star) as RadioButton).isChecked = true
//                            UserPoiTable.HEART -> (iconGroup.findViewById(R.id.btn_heart) as RadioButton).isChecked = true
//                            UserPoiTable.HOME -> (iconGroup.findViewById(R.id.btn_home) as RadioButton).isChecked = true
//                            UserPoiTable.BIKE -> (iconGroup.findViewById(R.id.btn_bike) as RadioButton).isChecked = true
//                            else -> Timber.e("Unknown custom marker type")
//                        }
//                        poi.close()
//                    }
//                    val markerTitle = dialogBody.findViewById(R.id.markerTitle) as EditText
//                    markerTitle.setText(marker.title)
//                    markerTitle.onFocusChangeListener = object : View.OnFocusChangeListener {
//
//                        internal var lastEntry: String = ""
//
//                        override fun onFocusChange(v: View, hasFocus: Boolean) {
//                            if (hasFocus) {
//                                lastEntry = (v as EditText).text.toString()
//                                v.setText("")
//                            } else if ((v as EditText).text.length == 0) {
//                                v.setText(lastEntry)
//                            }
//                        }
//                    }
//                    AlertDialog.Builder(activity, R.style.Theme_Iburn_Dialog)
//                            .setView(dialogBody)
//                            .setPositiveButton("Done") { dialog, which ->
//                                // Save the title
//                                if (markerTitle.text.length > 0)
//                                    marker.setTitle(markerTitle.text.toString())
//                                marker.hideInfoWindow()
//
//                                var drawableId = 0
//                                when (iconGroup.checkedRadioButtonId) {
//                                    R.id.btn_star -> {
//                                        drawableId = UserPoiTable.STAR
//                                        marker.setIcon(iconUserStar)
//                                    }
//                                    R.id.btn_heart -> {
//                                        drawableId = UserPoiTable.HEART
//                                        marker.setIcon(iconUserHeart)
//                                    }
//                                    R.id.btn_home -> {
//                                        drawableId = UserPoiTable.HOME
//                                        marker.setIcon(iconUserHome)
//                                    }
//                                    R.id.btn_bike -> {
//                                        drawableId = UserPoiTable.BIKE
//                                        marker.setIcon(iconUserBicycle)
//                                    }
//                                }
//                                updateCustomPinWithMarker(marker, drawableId)
//                            }
//                            .setNegativeButton("Delete") { dialog, which ->
//                                // Delete Pin
//                                removeCustomPin(marker)
//                            }.show()
//                }
    }

    private fun removeCustomPin(marker: Marker) {
//        marker.remove()
//        if (mappedCustomMarkerIds.containsKey(marker.id)) {
//            val itemId = getDatabaseIdFromGeneratedDataId(mappedCustomMarkerIds.get(marker.id)!!)
//            DataProvider.getInstance(activity.applicationContext)
//                    .map { provider -> provider.delete(PlayaDatabase.POIS, PlayaItemTable.id + " = ?", itemId.toString()) }
//                    .subscribe { result -> Timber.d("Deleted marker with result " + result!!) }
//        } else
//            Timber.w("Unable to delete marker " + marker.title)
    }

    /**
     * Update a Custom pin placed by a user with state of a map marker.
     *
     *
     * Note: If drawableResId is 0, it is ignored
     */
    private fun updateCustomPinWithMarker(marker: Marker, drawableResId: Int) {
//        if (mappedCustomMarkerIds.containsKey(marker.id)) {
//            val poiValues = ContentValues()
//            poiValues.put(UserPoiTable.name, marker.title)
//            poiValues.put(UserPoiTable.latitude, marker.position.latitude)
//            poiValues.put(UserPoiTable.longitude, marker.position.longitude)
//            if (drawableResId != 0)
//                poiValues.put(UserPoiTable.drawableResId, drawableResId)
//            val itemId = getDatabaseIdFromGeneratedDataId(mappedCustomMarkerIds.get(marker.id)!!)
//            DataProvider.getInstance(activity.applicationContext)
//                    .map { dataProvider -> dataProvider.update(PlayaDatabase.POIS, poiValues, PlayaItemTable.id + " = ?", itemId.toString()) }
//                    .subscribe { numUpdated -> Timber.d("Updated marker with status " + numUpdated!!) }
//        } else
//            Timber.w("Unable to find custom marker in map for updating")
    }

}// Required empty public constructor
