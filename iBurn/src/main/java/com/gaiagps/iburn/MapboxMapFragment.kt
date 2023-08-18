package com.gaiagps.iburn


import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.fragment.app.Fragment
import com.gaiagps.iburn.activity.PlayaItemViewActivity
import com.gaiagps.iburn.database.*
import com.gaiagps.iburn.js.Geocoder
import com.gaiagps.iburn.location.LocationProvider
import com.google.android.gms.location.LocationRequest
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.exceptions.InvalidLatLngBoundsException
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.VisibleRegion
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.sources.VectorSource
import com.mapbox.mapboxsdk.utils.MapFragmentUtils
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.set

// Track MBTiles versions to avoid unnecessary copies from assets
const val MBTILES_VERSION = 1L

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
        SEARCH,

        /**
         * Placing a User POI
         */
        PLACE_USER_POI
    }

    private var state = State.EXPLORE

    private val defaultZoom = 12.5
    private val markerShowcaseZoom = 14.5
    private val poiVisibleZoom = 14.0

    private var userPoiButton: ImageView? = null
    private var addressLabel: TextView? = null
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var onMapReadyCallback: OnMapReadyCallback? = null

    private var showcaseMarker: SymbolOptions? = null
    private var symbolManager: SymbolManager? = null
    private val cameraUpdate = PublishSubject.create<VisibleRegion>()
    private var cameraUpdateSubscription: Disposable? = null

    private var locationSubscription: Disposable? = null

    /**
     * Showcase a point on the map using a generic pin
     */
    fun showcaseLatLng(context: Context, latLng: LatLng) {
        // We ask for an external context because we want this method to be callable
        // before this fragment is resumed (e.g: shortly after construction)
        // TODO : Refactor to include showcase marker in Bundle on construction
        val marker = SymbolOptions()
            .withIconImage("pin")
            .withGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude))
        showcaseMarker(marker)
    }

    /**
     * Showcase a specific marker on the map. If you just need a generic map pin use [showcaseLatLng]
     */
    fun showcaseMarker(marker: SymbolOptions) {
        state = State.SHOWCASE
        showcaseMarker = marker
        if (isResumed) {
            _showcaseMarker(marker)
        }
        locationSubscription?.dispose()
    }

    private fun hasLocationPermission(): Boolean {
        return context
            ?.let { PermissionManager.hasLocationPermissions(it) }
            ?: false
    }

    @SuppressLint("MissingPermission")
    private fun _showcaseMarker(marker: SymbolOptions) {
        Timber.d("_showcaseMarker")
        mapMarkerAndFitEntireCity(marker)
        map?.locationComponent?.cameraMode = CameraMode.NONE

        if (hasLocationPermission()) {
            map?.locationComponent?.isLocationComponentEnabled = false
        }
        addressLabel?.visibility = View.INVISIBLE
        userPoiButton?.visibility = View.INVISIBLE
    }

    private fun mapMarkerAndFitEntireCity(marker: SymbolOptions) {
        mapView?.getMapAsync { map ->
            symbolManager?.create(marker)
            Timber.d("Moving camera to %s at zoom %f", marker.geometry, markerShowcaseZoom)
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    marker.latLng, markerShowcaseZoom
                )
            )

            Observable.timer(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { ignored ->
                    Timber.d("Animating camera")
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                Geo.MAN_LAT,
                                Geo.MAN_LON
                            ), defaultZoom
                        ), 3 * 1000
                    )
                }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        inflater.let { inflater ->
            val context = inflater.context
            val options = MapFragmentUtils.resolveArgs(context, this.arguments)
            val mapView = MapView(context, options)
            this.mapView = mapView

            val dpValue = 10 // margin in dips
            val d = requireActivity().resources!!.displayMetrics!!.density
            val margin = (dpValue * d).toInt() // margin in pixels

            // Add Playa Address label
            val addressLabel =
                inflater.inflate(R.layout.current_playa_address, container, false) as TextView
            addressLabel.visibility = View.INVISIBLE
            mapView.addView(addressLabel)
            setMargins(addressLabel, 0, margin, margin * 5, 0, Gravity.TOP.or(Gravity.END))
            addressLabel.setOnClickListener {
                onUserAddressLabelClicked(longClick = false)
            }
            addressLabel.setOnLongClickListener {
                onUserAddressLabelClicked(longClick = true)
                true
            }
            this.addressLabel = addressLabel

            // Add User POI add button
            val userPoiButton =
                inflater.inflate(R.layout.map_image_btn, container, false) as ImageView
            userPoiButton.visibility = if (state != State.SHOWCASE) View.VISIBLE else View.GONE
            userPoiButton.setImageResource(R.drawable.ic_pin_drop_black_24dp)
            mapView.addView(userPoiButton)
            setMargins(
                userPoiButton,
                0,
                margin,
                (margin * 9.5).toInt(),
                0,
                Gravity.TOP.or(Gravity.END)
            )
            userPoiButton.setOnClickListener {
                var map = this.map
                if (map != null) {
                    if (state != State.PLACE_USER_POI) {
                        val prePlaceUserPoiState = state

                        state = State.PLACE_USER_POI
                        val markerPlaceView =
                            addMarkerPlaceOverlay(inflater) { markerPlaceView, markerLatLng ->
                                Timber.d("Placing marker")
                                state = prePlaceUserPoiState
                                mapView.removeView(markerPlaceView)
                                addCustomPin(
                                    map,
                                    markerLatLng,
                                    "Custom Marker",
                                    UserPoi.ICON_STAR
                                ) { symbol ->
                                    showEditPinDialog(symbol)
                                }
                            }
//
//                                inflater.inflate(R.layout.overlay_place_custom_marker, container, false)
//                        mapView.addView(markerPlaceView)
//                        val viewDimen = convertDpToPixel(200f, context).toInt()
//                        markerPlaceView.layoutParams = FrameLayout.LayoutParams(
//                                viewDimen,
//                                viewDimen * 2)

                        animateDropView(markerPlaceView)

                        // Express train to Jankville! The discrepancy between a view placed at screen center
                        // and where Mapbox reports the camera target is might be due to marker anchors... or something else
                        // TODO : Figure out how to remove this magic margin
//                        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics).toInt()
//                        setMargins(markerPlaceView, 0, 0, 0, px, Gravity.CENTER)

//                        markerPlaceView.findViewById<ImageView>(R.id.star).setOnClickListener {
//                            Timber.d("Placing marker")
//                            state = prePlaceUserPoiState
//                            mapView.removeView(markerPlaceView)
//                            addCustomPin(map, null, "Custom Marker", UserPoi.ICON_STAR, {
//                                marker ->
//                                showEditPinDialog(marker)
//                            })
//                        }
                    }
                }
            }
            this.userPoiButton = userPoiButton

            return this.mapView
        }
        return null
    }

    private fun addMarkerPlaceOverlay(
        inflater: LayoutInflater,
        iconResId: Int = R.drawable.puck_star,
        markerClickListener: (View, LatLng) -> Unit
    ): View {
        val markerPlaceView = inflater.inflate(R.layout.overlay_place_custom_marker, mapView, false)
        mapView?.addView(markerPlaceView)


        val viewDimen = convertDpToPixel(200f, requireContext()).toInt()
        markerPlaceView.layoutParams = FrameLayout.LayoutParams(
            viewDimen,
            viewDimen * 2
        )

        // Express train to Jankville! The discrepancy between a view placed at screen center
        // and where Mapbox reports the camera target is might be due to marker anchors... or something else
        // TODO : Figure out how to remove this magic margin
//        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics).toInt()
        setMargins(markerPlaceView, 0, 0, 0, 0 /*px*/, Gravity.CENTER)

        val markerIcon = markerPlaceView.findViewById<ImageView>(R.id.star)
        markerIcon.setImageResource(iconResId)
        markerIcon.setOnClickListener {
            mapView?.getMapAsync { map ->

                val parent = markerIcon.parent as View

                val markerX = parent.left + (parent.width / 2f)
                val markerY =
                    parent.top + (markerIcon.top + (markerIcon.height / 2f)) // / 2f moves marker up a lil, / 1f moves down a little

                val onScreenPoint = PointF(markerX, markerY)
                val markerLatLng = map.projection.fromScreenLocation(onScreenPoint)
                Timber.d("Marker is at position ${markerIcon.x} / $markerX, ${markerIcon.y} / $markerY translated to LatLng ${markerLatLng.latitude}, ${markerLatLng.longitude}")

                markerClickListener.invoke(markerPlaceView, markerLatLng)
            }
        }

        return markerPlaceView
    }

    private fun copyAssets(): String {
        val tilesInput = requireContext().assets.open("map/map.mbtiles")
        val tilesOutput = File(requireContext().getExternalFilesDir(null), "map.mbtiles")
        val prefs = PrefsHelper(requireContext())
        if (tilesOutput.exists() && prefs.copiedMbtilesVersion == MBTILES_VERSION) {
            return tilesOutput.path
        }
        val tilesOutputStream = tilesOutput.outputStream()
        val buffer = ByteArray(1024)
        var read: Int
        while (tilesInput.read(buffer).also { read = it } != -1) {
            tilesOutputStream.write(buffer, 0, read)
        }
        tilesInput.close()
        tilesOutputStream.flush()
        tilesOutputStream.close()
        prefs.copiedMbtilesVersion = MBTILES_VERSION
        return tilesOutput.path
    }

    @SuppressLint("MissingPermission")
    private fun setupMap(mapView: MapView) {
        mapView?.getMapAsync { map ->
            val tilesPath = copyAssets()
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            var style = Style.Builder()
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                style = style.fromUri("asset://map/iburn-dark.json")
                     .withSource(VectorSource("composite", "mbtiles://$tilesPath"))
             } else {
                 style = style.fromUri("asset://map/iburn-light.json")
                     .withSource(VectorSource("composite", "mbtiles://$tilesPath"))
             }
            val context = requireContext()
            style.withImage(UserPoi.ICON_HEART, AppCompatResources.getDrawable(context, R.drawable.puck_heart)!!)
            style.withImage(UserPoi.ICON_HOME, AppCompatResources.getDrawable(context, R.drawable.puck_home)!!)
            style.withImage(UserPoi.ICON_STAR, AppCompatResources.getDrawable(context, R.drawable.puck_star)!!)
            style.withImage(UserPoi.ICON_BIKE, AppCompatResources.getDrawable(context, R.drawable.puck_bicycle)!!)
            style.withImage(iconEvent, AppCompatResources.getDrawable(context, R.drawable.event_pin)!!)
            style.withImage(iconCamp, AppCompatResources.getDrawable(context, R.drawable.camp_pin)!!)
            style.withImage(iconArt, AppCompatResources.getDrawable(context, R.drawable.art_pin)!!)
            style.withImage("pin", AppCompatResources.getDrawable(context, R.drawable.pin)!!)
            style.withImage("ice", AppCompatResources.getDrawable(context, R.drawable.ice)!!)
            style.withImage("firstAid", AppCompatResources.getDrawable(context, R.drawable.first_aid)!!)
            style.withImage("EmergencyClinic", AppCompatResources.getDrawable(context, R.drawable.first_aid)!!)
            style.withImage("bus", AppCompatResources.getDrawable(context, R.drawable.bus)!!)
            style.withImage("airport", AppCompatResources.getDrawable(context, R.drawable.airport)!!)
            style.withImage("centerCamp", AppCompatResources.getDrawable(context, R.drawable.center_camp)!!)
            style.withImage("center", AppCompatResources.getDrawable(context, R.drawable.center)!!)
            style.withImage("info", AppCompatResources.getDrawable(context, R.drawable.info)!!)
            style.withImage("ranger", AppCompatResources.getDrawable(context, R.drawable.ranger)!!)
            style.withImage("recycle", AppCompatResources.getDrawable(context, R.drawable.recycle)!!)
            style.withImage("temple", AppCompatResources.getDrawable(context, R.drawable.temple)!!)

            map.setStyle(style) {
                this.map = map

                symbolManager = SymbolManager(mapView, map, it)
                symbolManager?.iconAllowOverlap = true
                symbolManager?.textAllowOverlap = true

                symbolManager?.addClickListener { symbol ->
                    if (markerIdToItem.containsKey(symbol.id)) {
                        val item = markerIdToItem[symbol.id]!!
                        val i = Intent(
                            requireActivity().applicationContext,
                            PlayaItemViewActivity::class.java
                        )
                        i.putExtra(PlayaItemViewActivity.EXTRA_PLAYA_ITEM, item)
                        activity?.startActivity(i)
                    } else if (mappedCustomMarkerIds.containsKey(symbol.id)) {
                        showEditPinDialog(symbol)
                    }
                    true
                }
                val initZoomAmount = 0.2
                val pos = CameraPosition.Builder()
                    .target(LatLng(Geo.MAN_LAT, Geo.MAN_LON))
                    .zoom(defaultZoom - initZoomAmount)
                    .build()

                if (hasLocationPermission()) {
                    val activateOptions =
                        LocationComponentActivationOptions.Builder(requireContext(), it)
                            .build()

                    map.locationComponent.activateLocationComponent(activateOptions)
                    map.locationComponent.renderMode = RenderMode.NORMAL
                    if (BuildConfig.MOCK) {
                        val mockProvider = LocationProvider.MapboxMockLocationSource()
                        mockProvider.activate()
                        map.locationComponent.locationEngine = mockProvider
                    }
                    map.locationComponent.isLocationComponentEnabled = true
                }
                map.setMinZoomPreference(defaultZoom)
                map.setLatLngBoundsForCameraTarget(cameraBounds)
                map.moveCamera(CameraUpdateFactory.newCameraPosition(pos))

                if (state != State.SHOWCASE) {
                    Timber.d("Easing camera in")
                    Single.timer(800, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { it ->
                            map.easeCamera(CameraUpdateFactory.zoomBy(initZoomAmount), 500)
                        }
                }

                map.uiSettings.setAllGesturesEnabled(state != State.SHOWCASE)

                map.addOnCameraIdleListener {
                    if (!shouldShowPoisAtZoom(map.cameraPosition.zoom) && areMarkersVisible()) {
                        Timber.d("Clearing transient markers on zoom change")
                        clearMap(false)
                    } else {
                        cameraUpdate.onNext(map.projection.visibleRegion)
                    }
                }

                val showcaseMarker = this.showcaseMarker
                if (state == State.SHOWCASE && showcaseMarker != null) {
                    _showcaseMarker(showcaseMarker)
                }
            }
        }
    }

    private fun setupLocationSub() {
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_NO_POWER)
            .setInterval(5000)

        val context = requireActivity().applicationContext
        locationSubscription?.dispose()
        locationSubscription = LocationProvider.observeCurrentLocation(context, locationRequest)
            .observeOn(ioScheduler)
            .flatMap { location ->
                Geocoder.reverseGeocode(
                    context,
                    location.latitude.toFloat(),
                    location.longitude.toFloat()
                )
                    .toObservable()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ address ->
                addressLabel?.visibility = View.VISIBLE
                addressLabel?.text = address
            }, { error -> Timber.e(error, "Failed to get device location") })
    }

    private fun setupCameraUpdateSub(map: MapboxMap) {
        val prefsHelper = PrefsHelper(requireActivity().applicationContext)
        Timber.d("Subscribing to camera updates")
        cameraUpdateSubscription?.dispose()
        cameraUpdateSubscription = cameraUpdate
            .debounce(250, TimeUnit.MILLISECONDS)
            .flatMap { visibleRegion ->
                DataProvider.getInstance(requireActivity().applicationContext)
                    .map { provider -> Pair(provider, visibleRegion) }
            }
            .flatMap { (provider, visibleRegion) ->

                val embargoActive = Embargo.isEmbargoActive(prefsHelper)
                val queryAllItems = (state != State.SHOWCASE) && (!embargoActive)
                // Note we're only querying user-added (favorites) and user pois, which
                // should be visible at all zooms. If we were to plot all camps, art, etc,
                // we could use the zoom gate, but then we'd only want zoom to affect
                // query of those types, so it'd require a new DataProvider query
                // && shouldShowPoisAtZoom(map.cameraPosition.zoom)

                if (queryAllItems) {
                    Timber.d("Map query for all items at zoom %f", map.cameraPosition.zoom)
                    provider.observeUserAddedMapItemsOnly().firstElement().toObservable()
                } else {
                    Timber.d("Map query for user items at zoom %f", map.cameraPosition.zoom)
                    (provider.getUserPoi()).firstElement().toObservable()
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { items: List<PlayaItem> ->
                processMapItemResult(items)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView?.onCreate(savedInstanceState)
        mapView?.let { mapView ->
            setupMap(mapView)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        mapView?.getMapAsync { map ->
            onMapReadyCallback?.onMapReady(map)
            if (state != State.SHOWCASE) {
                setupCameraUpdateSub(map)
            }
        }
        if (state != State.SHOWCASE) {
            setupLocationSub()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        keepScreenOn(true)
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
        keepScreenOn(false)
    }

    private fun keepScreenOn(enabled: Boolean) {
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (enabled) {
            activity?.window?.addFlags(flag)
        } else {
            activity?.window?.clearFlags(flag)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        locationSubscription?.dispose()
        cameraUpdateSubscription?.dispose()
        Geocoder.close()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
        symbolManager?.onDestroy();

        cameraUpdateSubscription?.dispose()
    }

    fun getMapAsync(onMapReadyCallback: OnMapReadyCallback) {
        this.onMapReadyCallback = onMapReadyCallback
    }

    /**
     * Map of user added pins. Mapbox Marker Id -> UserPoi
     */
    internal var mappedCustomMarkerIds = HashMap<Long, UserPoi>()

    /**
     * Set to avoid plotting duplicate items
     */
    internal var mappedItems = HashSet<PlayaItem>()

    /**
     * Map of pins shown in response to explore or search
     */
    private val MAX_POIS = 100

    // Markers that should only be cleared on new query arrival
    internal var permanentMarkers = HashSet<Symbol>()

    // Markers that should be cleared on camera events
    internal var mappedTransientMarkers = ArrayDeque<Symbol>(MAX_POIS)
    internal var markerIdToItem = HashMap<Long, PlayaItem>()

    /**
     * Keep track of the bounds describing a batch of results across Loaders
     */
    private var mResultBounds: LatLngBounds.Builder? = null

    private fun processMapItemResult(items: List<PlayaItem>) {

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
                        Timber.d("Adding marker for UserPoi ${item.id}")
                        val marker = addNewMarkerForItem(item)
                        mappedItems.add(item)
                        mappedCustomMarkerIds[marker.id] = item
                    } else if (item.isFavorite) {
                        // Favorites are always-visible, but not editable
                        val marker = addNewMarkerForItem(item)
                        markerIdToItem[marker.id] = item
                        mappedItems.add(item)
                        permanentMarkers.add(marker)
                    } else if (shouldShowPoisAtZoom(currentZoom)) {
                        // Other markers are only displayed at near zoom, and are kept in a pool
                        // of recyclable markers. mapRecyclableMarker handles adding to markerIdToItem
                        val marker = mapRecyclableMarker(item, mResultBounds)
                        if (marker != null) {
                            mappedItems.add(item)
                        }
                    }
                }

            // If displaying search results, try to move the camera to include all results
            if (state == State.SEARCH) {
                try {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            mResultBounds!!.build(),
                            80
                        )
                    )
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

    private val iconArt = "art_pin"
    private val iconCamp = "camp_pin"
    private val iconEvent = "event_pin"

    private fun onUserAddressLabelClicked(longClick: Boolean) {

        fun copyAddressToClipboard() {
            val address = addressLabel?.text.toString()
            if (!TextUtils.isEmpty(address)) {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                var clip = ClipData.newPlainText("Current Playa Address", address)
                //clipboard.primaryClip = clip
                Toast.makeText(
                    requireActivity().applicationContext,
                    "Copied address to clipboard",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        fun followCurrentLocaction() {
            map?.locationComponent?.cameraMode = CameraMode.TRACKING
            map?.locationComponent?.renderMode = RenderMode.COMPASS
            map?.locationComponent?.zoomWhileTracking(markerShowcaseZoom)
        }

        if (longClick) copyAddressToClipboard() else followCurrentLocaction()
    }


    private fun addNewMarkerForItem(item: PlayaItem): Symbol {
        val pos = LatLng(item.latitude.toDouble(), item.longitude.toDouble())
        val symbolOptions: SymbolOptions = SymbolOptions()
            .withLatLng(pos)
            .withTextField(item.name)
            .withTextSize(12f)
            .withTextHaloColor("#EBDED1")
            .withTextHaloWidth(2.0F)

        if (item is UserPoi) {
            symbolOptions.withIconImage(item.icon)
                .withTextOffset(floatArrayOf(0f, 2.5f).toTypedArray())
        } else if (item is Art) {
            symbolOptions.withIconImage(iconArt)
                .withTextOffset(floatArrayOf(0f, 0.8f).toTypedArray())
        } else if (item is Camp) {
            symbolOptions.withIconImage(iconCamp)
                .withTextOffset(floatArrayOf(0f, 0.8f).toTypedArray())
        } else if (item is Event) {
            symbolOptions.withIconImage(iconEvent)
                .withTextOffset(floatArrayOf(0f, 0.8f).toTypedArray())
        }

        return symbolManager!!.create(symbolOptions)
    }

    /**
     * Map a marker as part of a finite set of markers, limiting the total markers
     * displayed and recycling markers if this limit is exceeded.
     */
    private fun mapRecyclableMarker(
        item: PlayaItem,
        boundsBuilder: LatLngBounds.Builder?
    ): Symbol? {
        val pos = LatLng(item.latitude.toDouble(), item.longitude.toDouble())

        // Assemble search results region boundary
        if (item !is UserPoi && boundsBuilder != null && state == State.SEARCH) {
            if (cameraBounds.contains(pos)) {
                boundsBuilder.include(pos)
            }
        }

        var marker: Symbol? = null

        if (mappedTransientMarkers.size == MAX_POIS) {
            // Re-use the eldest Marker

            marker = mappedTransientMarkers.remove()
            marker.geometry = Point.fromLngLat(pos.longitude, pos.latitude)
            marker.textField = item.name

            if (item is Art) {
                marker.iconImage = iconArt
            } else if (item is Camp) {
                marker.iconImage = iconCamp
            } else if (item is Event) {
                marker.iconImage = iconEvent
            } else if (item is UserPoi) {
                marker.iconImage = item.icon
            }

//            marker.setAnchor(0.5f, 0.5f)
            mappedTransientMarkers.add(marker)
            markerIdToItem.put(marker.id, item)
        } else {
            // Create a new Marker
            marker = addNewMarkerForItem(item)
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
            symbolManager?.delete(marker)
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
        return Integer.parseInt(dataId.split("-".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1])
    }

    private fun resetMapView(map: MapboxMap) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(Geo.MAN_LAT, Geo.MAN_LON),
                defaultZoom
            )
        )
    }

    fun clearMap(clearAll: Boolean) {
        if (clearAll) {
            clearPermanentMarkers()
        }

        for (marker in mappedTransientMarkers) {
            symbolManager?.delete(marker)
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

    private fun showEditPinDialog(marker: Symbol) {
        if (state == State.SHOWCASE) return

        val dialogBody = requireActivity().layoutInflater!!.inflate(R.layout.dialog_poi, null)
        val iconGroup: RadioGroup = dialogBody.findViewById(R.id.iconGroup)

        // Fetch current Marker icon
        val userPoi = mappedCustomMarkerIds[marker.id]
        userPoi?.let { userPoi ->

            val drawableResId = userPoi.icon
            var checkedRadioButtonLayoutId: Int = 0
            when (drawableResId) {
                UserPoi.ICON_STAR -> checkedRadioButtonLayoutId = R.id.btn_star
                UserPoi.ICON_HEART -> checkedRadioButtonLayoutId = R.id.btn_heart
                UserPoi.ICON_HOME -> checkedRadioButtonLayoutId = R.id.btn_home
                UserPoi.ICON_BIKE -> checkedRadioButtonLayoutId = R.id.btn_bike
                else -> Timber.e("Unknown custom marker type")
            }

            val checkedRadioButton: RadioButton = iconGroup.findViewById(checkedRadioButtonLayoutId)
            checkedRadioButton.isChecked = true


            val markerTitle: EditText? = dialogBody?.findViewById(R.id.markerTitle)
            markerTitle?.setText(marker.textField)
            markerTitle?.onFocusChangeListener = object : View.OnFocusChangeListener {

                internal var lastEntry: String = ""

                override fun onFocusChange(v: View, hasFocus: Boolean) {
                    if (hasFocus) {
                        lastEntry = (v as EditText).text.toString()
                        v.setText("")
                    } else if ((v as EditText).text.isBlank()) {
                        v.setText(lastEntry)
                    }
                }
            }

            AlertDialog.Builder(requireActivity(), R.style.Theme_Iburn_Dialog)
                .setView(dialogBody)
                .setPositiveButton("Save") { dialog, which ->
                    // Save the title
                    if (markerTitle?.text?.isNotBlank() ?: false)
                        marker.textField = markerTitle?.text.toString()

//                        marker.hideInfoWindow()

                    when (iconGroup.checkedRadioButtonId) {
                        R.id.btn_star -> {
                            marker.iconImage = UserPoi.ICON_STAR
                        }

                        R.id.btn_heart -> {
                            marker.iconImage = UserPoi.ICON_HEART
                        }

                        R.id.btn_home -> {
                            marker.iconImage = UserPoi.ICON_HOME
                        }

                        R.id.btn_bike -> {
                            marker.iconImage = UserPoi.ICON_BIKE
                        }
                    }
                    updateCustomPinWithMarker(marker)
                }
                .setNeutralButton("Move") { dialog, which ->
                    if (state != State.PLACE_USER_POI) {
                        val prePlaceUserPoiState = state

                        state = State.PLACE_USER_POI

                        mapView?.getMapAsync { map ->

                            // Deselect markers to close InfoWindows
                            map.deselectMarkers()

                            val layoutInflater =
                                requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                            addMarkerPlaceOverlay(layoutInflater) { markerPlaceView, markerLatLng ->
                                state = prePlaceUserPoiState
                                mapView?.let { mapView ->
                                    mapView.removeView(markerPlaceView)
                                    marker.latLng = markerLatLng
                                    DataProvider.getInstance(requireActivity().applicationContext)
                                        .observeOn(ioScheduler)
                                        .subscribe { provider ->
                                            userPoi.latitude = markerLatLng.latitude.toFloat()
                                            userPoi.longitude = markerLatLng.longitude.toFloat()
                                            provider.update(userPoi)
                                        }
                                    symbolManager?.update(marker)
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("Delete") { dialog, which ->
                    // Delete Pin
                    removeCustomPin(marker)
                }.show()
        }
    }

    /**
     * Adds a custom pin to the current map and database
     */
    private fun addCustomPin(
        map: MapboxMap,
        latLng: LatLng?,
        title: String,
        @UserPoi.Icon poiIcon: String,
        callback: ((symbol: Symbol) -> Unit)?
    ) {
        var markerLatLng = latLng
        if (markerLatLng == null) {
            val mapCenter = map.cameraPosition.target
            markerLatLng = LatLng(mapCenter!!.latitude, mapCenter!!.longitude)
        }

        val symbolOptions: SymbolOptions = SymbolOptions()
            .withLatLng(markerLatLng)
            .withTextField(title)
            .withIconImage(UserPoi.ICON_STAR)
            .withTextSize(12f)
            .withTextHaloColor("#EBDED1")
            .withTextHaloWidth(2.0F)
            .withTextOffset(floatArrayOf(0f, 2.5f).toTypedArray())
        val symbol = symbolManager?.create(symbolOptions)

        val userPoiPlayaId = UUID.randomUUID().toString()
        val userPoi = UserPoi()
        userPoi.name = title
        userPoi.latitude = markerLatLng.latitude.toFloat()
        userPoi.longitude = markerLatLng.longitude.toFloat()
        userPoi.icon = poiIcon
        userPoi.playaId = userPoiPlayaId

        try {
            DataProvider.getInstance(requireActivity().applicationContext)
                .observeOn(ioScheduler)
                .flatMap { dataProvider ->
                    dataProvider.insertUserPoi(userPoi)
                    dataProvider.getUserPoiByPlayaId(userPoiPlayaId).toObservable()
                }
                .firstElement() // Inserting can cause the get query to refresh
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { userPoi ->
                    Timber.d("After inserting, userPoi has id ${userPoi.id}")
                    // Make sure UserPoi is added to mappedItems before being inserted as this will
                    // trigger a map items update
                    mappedItems.add(userPoi)
                    if (symbol != null) {
                        mappedCustomMarkerIds[symbol.id] = userPoi
                    }
                    if (symbol != null) {
                        callback?.invoke(symbol)
                    }
                }
        } catch (e: NumberFormatException) {
            Timber.w("Unable to get id for new custom marker")
        }
    }

    private fun removeCustomPin(marker: Symbol) {
        symbolManager?.delete(marker)
        val userPoi = mappedCustomMarkerIds[marker.id]
        userPoi?.let { userPoi ->
            DataProvider.getInstance(requireActivity().applicationContext)
                .observeOn(ioScheduler)
                .map { provider -> provider.deleteUserPoi(userPoi) }
                .subscribe { _ -> Timber.d("Deleted marker") }
        }
    }

    /**
     * Update a Custom pin placed by a user with state of a map marker.
     *
     *
     * Note: If drawableResId is 0, it is ignored
     */
    private fun updateCustomPinWithMarker(symbol: Symbol) {
        val userPoi = mappedCustomMarkerIds[symbol.id]
        userPoi?.let { userPoi ->
            userPoi.icon = symbol.iconImage
            userPoi.name = symbol.textField
            userPoi.latitude = symbol.latLng.latitude.toFloat()
            userPoi.longitude = symbol.latLng.longitude.toFloat()

            DataProvider.getInstance(requireActivity().applicationContext)
                .observeOn(ioScheduler)
                .map { dataProvider -> dataProvider.update(userPoi) }
                .subscribe { _ -> Timber.d("Updated marker") }
            symbolManager?.update(symbol)
        }
    }

    /**
     * Thanks to SO:
     * http://stackoverflow.com/questions/4472429/change-the-right-margin-of-a-view-programmatically
     */
    private fun setMargins(v: View, l: Int, r: Int, t: Int, b: Int, gravity: Int) {
        if (v.layoutParams is ViewGroup.MarginLayoutParams) {
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(l, t, r, b)
            if (params is FrameLayout.LayoutParams) {
                params.gravity = gravity
            }
            v.requestLayout()
        }
    }

    internal fun convertDpToPixel(dp: Float, context: Context): Float {
        val metrics = context.resources.displayMetrics
        return dp * (metrics.densityDpi / 160).toFloat()
    }

    private fun animateDropView(view: View) {
        val heightPx = view.context.resources.displayMetrics.heightPixels / 2

        view.translationY = -heightPx.toFloat()
        val springAnim = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, 0f)
        springAnim.spring.dampingRatio = .6f
        springAnim.spring.stiffness = 720f
        springAnim.start()
    }
}
