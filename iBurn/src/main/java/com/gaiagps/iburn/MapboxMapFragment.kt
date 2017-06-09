package com.gaiagps.iburn


import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import com.gaiagps.iburn.activity.PlayaItemViewActivity
import com.gaiagps.iburn.database.DataProvider
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.VisibleRegion
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import org.reactivestreams.Subscription
import timber.log.Timber
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit


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
    private var cameraUpdateSubscription: Subscription? = null

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
//                map.setLocationSource(LocationProvider.MapboxMockLocationSource())
            }
            map.myLocationViewSettings.foregroundTintColor = context.resources.getColor(R.color.map_my_location)
            map.myLocationViewSettings.accuracyTintColor = context.resources.getColor(R.color.map_my_location)
            // TODO : Re-enable location after crash resolved
            // https://github.com/mapbox/mapbox-gl-native/pull/9142
//            map.isMyLocationEnabled = true
            map.setMinZoomPreference(defaultZoom)
            map.setLatLngBoundsForCameraTarget(cameraBounds)
            map.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
            map.uiSettings.setAllGesturesEnabled(state != State.SHOWCASE)
            map.setOnCameraIdleListener {
                if (map.cameraPosition.zoom < poiVisibleZoom && areMarkersVisible()) {
                    clearMap(false)
                } else {
                    cameraUpdate.onNext(map.projection.visibleRegion)
                }
            }

            map.setOnInfoWindowClickListener { marker ->
                if (markerIdToMeta.containsKey(marker.id)) {
                    val markerMeta = markerIdToMeta[marker.id]!!
                    val model_id = Integer.parseInt(markerMeta.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
                    val model_type = Integer.parseInt(markerMeta.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
                    val modelType = DataProvider.getTypeValue(model_type)
                    val i = Intent(activity.applicationContext, PlayaItemViewActivity::class.java)
                    i.putExtra(PlayaItemViewActivity.EXTRA_MODEL_ID, model_id)
                    i.putExtra(PlayaItemViewActivity.EXTRA_MODEL_TYPE, modelType)
                    activity.startActivity(i)
                } else if (mMappedCustomMarkerIds.containsKey(marker.id)) {
                    showEditPinDialog(marker)
                }
                true
            }
        }

        val prefsHelper = PrefsHelper(activity.applicationContext)
        Timber.d("Subscribing to camera updates")
//        cameraUpdateSubscription?.cancel()
//        cameraUpdateSubscription = cameraUpdate
//                .debounce(250, TimeUnit.MILLISECONDS)
//                .flatMap { visibleRegion ->
//                    DataProvider.getInstance(activity.applicationContext)
//                            .map { provider -> Pair(provider, visibleRegion) }
//                }
//                .flatMap { providerRegionPair ->
//                    val provider = providerRegionPair.first
//                    val visibleRegion = providerRegionPair.second
//
////                    MapFragmentHelper().performQuery(provider = provider, visibleRegion = visibleRegion, prefs = prefsHelper, isShowcaseMode = state == State.SHOWCASE)
//                }
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { items ->
//                    // TODO : refactor to take collection of items
//                    //processMapItemResult(cursor = cursor)
//                }
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
        cameraUpdateSubscription?.cancel()
    }

    fun getMapAsync(onMapReadyCallback: OnMapReadyCallback) {
        this.onMapReadyCallback = onMapReadyCallback
    }

    /**
     * Map of user added pins. Mapbox Marker Id -> Database Id
     */
    internal var mMappedCustomMarkerIds = HashMap<Long, String>()
    /**
     * Map of pins shown in response to explore or search
     */
    private val MAX_POIS = 100

    // Markers that should only be cleared on new query arrival
    internal var permanentMarkers = HashSet<Marker>()
    // Markers that should be cleared on camera events
    internal var mMappedTransientMarkers = ArrayDeque<Marker>(MAX_POIS)
    internal var markerIdToMeta = HashMap<Long, String>()

    /**
     * Keep track of the bounds describing a batch of results across Loaders
     */
    private var mResultBounds: LatLngBounds.Builder? = null

//    private fun processMapItemResult(cursor: Cursor) {
//
//        clearPermanentMarkers()
//        mResultBounds = LatLngBounds.Builder()
//
//        Timber.d("Got cursor result with %d items", cursor.count)
//        mapView?.getMapAsync { map ->
//
//            val currentZoom = map.cameraPosition.zoom
//            var markerMapId: String
//            // Sorry, but Java has no immutable primitives and LatLngBounds has no indicator
//            // of when calling .build() will throw IllegalStateException due to including no points
//            val areBoundsValid = BooleanArray(1)
//            while (cursor.moveToNext()) {
//                if (cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.latitude)) == 0.0) continue
//
//                val typeInt = cursor.getInt(cursor.getColumnIndex(DataProvider.VirtualType))
//                val type = DataProvider.getTypeValue(typeInt)
//
//                markerMapId = generateDataIdForItem(type, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.id)).toLong())
//
//                if (type == Constants.PlayaItemType.POI) {
//                    // POIs are permanent markers that are editable when their info window is clicked
//                    if (!mMappedCustomMarkerIds.containsValue(markerMapId)) {
//                        val marker = addNewMarkerForCursorItem(map, typeInt, cursor)
//                        mMappedCustomMarkerIds.put(marker.getId(), markerMapId)
//                    }
//                } else if (cursor.getInt(cursor.getColumnIndex(PlayaItemTable.favorite)) == 1) {
//                    // Favorites are permanent markers, but are not editable
//                    if (!markerIdToMeta.containsValue(markerMapId)) {
//                        val marker = addNewMarkerForCursorItem(map, typeInt, cursor)
//                        markerIdToMeta.put(marker.getId(), markerMapId)
//                        permanentMarkers.add(marker)
//                    }
//                } else if (currentZoom > poiVisibleZoom) {
//                    // Other markers are recyclable, and may be cleared on camera events
//                    mapRecyclableMarker(map, typeInt, markerMapId, cursor, mResultBounds, areBoundsValid)
//                }
//            }
//            cursor.close()
//            if (areBoundsValid[0] && state == State.SEARCH) {
//                map.animateCamera(CameraUpdateFactory.newLatLngBounds(mResultBounds!!.build(), 80))
//            } else if (!areBoundsValid[0] && state == State.SEARCH) {
//                // No results
//                resetMapView(map)
//            }
//        }
//    }

    private val iconFactory: IconFactory by lazy {
        IconFactory.getInstance(context)
    }

    // TODO : Loading many Icons breaks the entire marker rendering system somehow, so we cache 'em:
    // https://github.com/mapbox/mapbox-gl-native/issues/9026

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

//    private fun addNewMarkerForCursorItem(map: MapboxMap, itemType: Int, cursor: Cursor): Marker {
//        val pos = LatLng(cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.latitude)),
//                cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.longitude)))
//        val markerOptions: MarkerOptions
//        markerOptions = MarkerOptions().position(pos)
//                .title(cursor.getString(cursor.getColumnIndex(PlayaItemTable.name)))
//
//        val iconFactory = IconFactory.getInstance(context)
//
//        val modelType = DataProvider.getTypeValue(itemType)
//        when (modelType) {
//            Constants.PlayaItemType.POI ->
//                // Favorite column is mapped to user poi icon type: A hack to make the union query work
//                styleCustomMarkerOption(markerOptions, cursor.getInt(cursor.getColumnIndex(PlayaItemTable.favorite)))
//            Constants.PlayaItemType.ART -> markerOptions.icon(iconArt)
//            Constants.PlayaItemType.CAMP -> markerOptions.icon(iconCamp)
//            Constants.PlayaItemType.EVENT -> markerOptions.icon(iconEvent)
//        }
//
//        val marker = map.addMarker(markerOptions)
//        return marker
//    }

    /**
     * Apply style to a custom MarkerOptions before
     * adding to Map
     *
     *
     * Note: drawableResId is an int constant from [com.gaiagps.iburn.database.UserPoiTable]
     */
    private fun styleCustomMarkerOption(markerOption: MarkerOptions, drawableResId: Int) {
        when (drawableResId) {
//            UserPoiTable.HOME -> markerOption.icon(iconUserHome)
//            UserPoiTable.STAR -> markerOption.icon(iconUserStar)
//            UserPoiTable.BIKE -> markerOption.icon(iconUserBicycle)
//            UserPoiTable.HEART -> markerOption.icon(iconUserHeart)
        }
    }

    /**
     * Map a marker as part of a finite set of markers, limiting the total markers
     * displayed and recycling markers if this limit is exceeded.

     * @param areBoundsValid a hack one-dimensional boolean array used to report whether boundsBuilder
     * *                       includes at least one point and will not throw an exception on its build()
     */
    private fun mapRecyclableMarker(map: MapboxMap, itemType: Int, markerMapId: String, cursor: Cursor, boundsBuilder: LatLngBounds.Builder?, areBoundsValid: BooleanArray) {
//        if (!markerIdToMeta.containsValue(markerMapId)) {
//            // This POI is not yet mapped
//            val pos = LatLng(cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.latitude)), cursor.getDouble(cursor.getColumnIndex(PlayaItemTable.longitude)))
//            if (itemType != DataProvider.getTypeValue(Constants.PlayaItemType.POI) && boundsBuilder != null && state == State.SEARCH) {
//                if (cameraBounds.contains(pos)) {
//                    boundsBuilder.include(pos)
//                    areBoundsValid[0] = true
//                }
//            }
//            if (mMappedTransientMarkers.size == MAX_POIS) {
//                Timber.d("")
//                // We should re-use the eldest Marker
//                val marker = mMappedTransientMarkers.remove()
//                marker.setPosition(pos)
//                marker.setTitle(cursor.getString(cursor.getColumnIndex(ArtTable.name)))
//
//                val iconFactory = IconFactory.getInstance(context)
//
//                val modelType = DataProvider.getTypeValue(itemType)
//                when (modelType) {
//                    Constants.PlayaItemType.ART -> marker.icon = iconFactory.fromResource(R.drawable.art_pin)
//                    Constants.PlayaItemType.CAMP -> marker.icon = iconFactory.fromResource(R.drawable.camp_pin)
//                    Constants.PlayaItemType.EVENT -> marker.icon = iconFactory.fromResource(R.drawable.event_pin)
//                }
//
////                marker.setAnchor(0.5f, 0.5f)
//                mMappedTransientMarkers.add(marker)
//                markerIdToMeta.put(marker.id, markerMapId)
//            } else {
//                // We shall create a new Marker
//                val marker = addNewMarkerForCursorItem(map, itemType, cursor)
//                markerIdToMeta.put(marker.getId(), markerMapId)
//                mMappedTransientMarkers.add(marker)
//            }
//        }
    }

    /**
     * Clear markers marked permanent. These are not removed due to camera change events.
     * Currently used for user-selected favorite items.
     */
    fun clearPermanentMarkers() {
        for (marker in permanentMarkers) {
            marker.remove()
            markerIdToMeta.remove(marker.id)
        }
        permanentMarkers.clear()
    }

    /**
     * Return a key used internally to keep track of data items currently mapped,
     * helping us avoid mapping duplicate points.

     * @param itemType The type of the item
     * *
     * @param itemId   The database id of the item
     */
//    private fun generateDataIdForItem(itemType: Constants.PlayaItemType, itemId: Long): String {
//        return String.format("%d-%d", DataProvider.getTypeValue(itemType), itemId)
//    }

    /**
     * Return the internal database id for an item given the string id
     * generated by [.generateDataIdForItem]
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

        for (marker in mMappedTransientMarkers) {
            marker.remove()
            markerIdToMeta.remove(marker.id)
        }
        mMappedTransientMarkers.clear()
    }


    fun areMarkersVisible(): Boolean {
        return mMappedTransientMarkers.size > 0
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
//                            getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.id)!!).toString())
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
//        if (mMappedCustomMarkerIds.containsKey(marker.id)) {
//            val itemId = getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.id)!!)
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
//        if (mMappedCustomMarkerIds.containsKey(marker.id)) {
//            val poiValues = ContentValues()
//            poiValues.put(UserPoiTable.name, marker.title)
//            poiValues.put(UserPoiTable.latitude, marker.position.latitude)
//            poiValues.put(UserPoiTable.longitude, marker.position.longitude)
//            if (drawableResId != 0)
//                poiValues.put(UserPoiTable.drawableResId, drawableResId)
//            val itemId = getDatabaseIdFromGeneratedDataId(mMappedCustomMarkerIds.get(marker.id)!!)
//            DataProvider.getInstance(activity.applicationContext)
//                    .map { dataProvider -> dataProvider.update(PlayaDatabase.POIS, poiValues, PlayaItemTable.id + " = ?", itemId.toString()) }
//                    .subscribe { numUpdated -> Timber.d("Updated marker with status " + numUpdated!!) }
//        } else
//            Timber.w("Unable to find custom marker in map for updating")
    }

}// Required empty public constructor
