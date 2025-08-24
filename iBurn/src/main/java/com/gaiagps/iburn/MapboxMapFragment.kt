package com.gaiagps.iburn


import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.PointF
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.location.Location
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.core.content.ContextCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.PopupMenu
import com.gaiagps.iburn.database.Art
import com.gaiagps.iburn.database.Camp
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.Embargo
import com.gaiagps.iburn.database.Event
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.PlayaItemWithUserData
import com.gaiagps.iburn.database.UserPoi
import com.gaiagps.iburn.js.Geocoder
import com.gaiagps.iburn.location.LocationProvider
import com.google.android.gms.location.LocationRequest
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.exceptions.InvalidLatLngBoundsException
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.VisibleRegion
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.utils.MapFragmentUtils
// org.maplibre.geojson.Point no longer used directly for annotations
import timber.log.Timber
import java.io.File
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.collections.set

// Track MBTiles versions to avoid unnecessary copies from assets
const val MBTILES_VERSION = 4L

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
    private val poiVisibleZoom = 16.0
    private val labelVisibleZoom = 14.5

    private var userPoiButton: ImageView? = null
    private var layersButton: ImageView? = null
    private var addressLabel: TextView? = null
    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var onMapReadyCallback: OnMapReadyCallback? = null

    // Lightweight replacement for Symbol/SymbolManager
    private data class MapMarker(
        val id: Long,
        var latLng: LatLng,
        var title: String?,
        var iconImage: String
    )

    private var showcaseMarker: LatLng? = null
    private var styleRef: Style? = null
    private var nextMarkerId: Long = 1L
    private val markerStore = HashMap<Long, MapMarker>()
    private val annotationsSourceId = "app-annotations-source"
    private val annotationsLayerId = "app-annotations-layer"
    private val cameraUpdate = PublishSubject.create<VisibleRegion>()
    private var cameraUpdateSubscription: Disposable? = null

    private var locationSubscription: Disposable? = null
    private var currentLocation: Location? = null

    /**
     * Showcase a point on the map using a generic pin
     */
    fun showcaseLatLng(context: Context, latLng: LatLng) {
        // We ask for an external context because we want this method to be callable
        // before this fragment is resumed (e.g: shortly after construction)
        // TODO : Refactor to include showcase marker in Bundle on construction
        showcaseMarker(latLng)
    }

    /**
     * Showcase a specific marker on the map. If you just need a generic map pin use [showcaseLatLng]
     */
    fun showcaseMarker(marker: LatLng) {
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
    private fun _showcaseMarker(marker: LatLng) {
        Timber.d("_showcaseMarker")
        mapMarkerAndFitEntireCity(marker)
        map?.locationComponent?.cameraMode = CameraMode.NONE

        if (hasLocationPermission()) {
            map?.locationComponent?.isLocationComponentEnabled = false
        }
        addressLabel?.visibility = View.INVISIBLE
        userPoiButton?.visibility = View.INVISIBLE
    }

    private fun mapMarkerAndFitEntireCity(marker: LatLng) {
        mapView?.getMapAsync { map ->
            Timber.d("Moving camera to %s at zoom %f", marker, markerShowcaseZoom)
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    marker, markerShowcaseZoom
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
                (margin * 11).toInt(),
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
                                val markerName = userPoiButton?.tag as? String?
                                userPoiButton.setTag(null)
                                addCustomPin(
                                    map,
                                    markerLatLng,
                                    markerName ?: "Custom Marker",
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

            // Add Layers toggle button (camp boundaries / big camp names)
            val layersButton =
                inflater.inflate(R.layout.map_image_btn, container, false) as ImageView
            layersButton.visibility = if (state != State.SHOWCASE) View.VISIBLE else View.GONE
            layersButton.setImageResource(R.drawable.ic_map_black_24dp)
            mapView.addView(layersButton)
            // Position below the user POI button, aligned to the same right margin
            setMargins(
                layersButton,
                0,
                margin,
                (margin * 17).toInt(),
                0,
                Gravity.TOP.or(Gravity.END)
            )
            layersButton.setOnClickListener {
                showLayersPopup(layersButton)
            }
            this.layersButton = layersButton

            return this.mapView
        }
        return null
    }

    private fun showLayersPopup(anchor: View) {
        val context = requireContext()
        val prefs = PrefsHelper(context)
        val popup = PopupMenu(context, anchor)
        popup.menuInflater.inflate(R.menu.menu_map_layers, popup.menu)
        // Sync state
        popup.menu.findItem(R.id.menu_show_camp_boundaries)?.isChecked = prefs.showCampBoundaries
        popup.menu.findItem(R.id.menu_show_big_camp_names)?.isChecked = prefs.showBigCampNames

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_show_camp_boundaries -> {
                    val newVal = !item.isChecked
                    item.isChecked = newVal
                    prefs.setShowCampBoundaries(newVal)
                    applyCampLayerPreferences(prefs)
                    true
                }
                R.id.menu_show_big_camp_names -> {
                    val newVal = !item.isChecked
                    item.isChecked = newVal
                    prefs.setShowBigCampNames(newVal)
                    applyCampLayerPreferences(prefs)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    fun showAddMarker(location: LatLng, name: String?) {
        // Move map to location
        map?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                location, markerShowcaseZoom
            )
        )
        userPoiButton?.setTag(name)
        userPoiButton?.performClick()
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

    @Synchronized
    private fun copyAssets(): String {
        val context = requireContext().applicationContext
        val assets = context.assets

        // Prefer external files dir; fall back to internal if unavailable
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val destFile = File(baseDir, "map.mbtiles")
        val tmpFile = File(baseDir, "map.mbtiles.part")

        val prefs = PrefsHelper(context)

        // If we already have a valid file for the current version, use it
        if (destFile.exists() && prefs.copiedMbtilesVersion == MBTILES_VERSION && validateMbtiles(destFile)) {
            return destFile.path
        }

        Timber.d("Copying bundled MBTiles to %s", destFile.path)

        // Ensure directory exists
        baseDir.mkdirs()

        // Copy to a temp file and fsync, then atomically rename into place
        assets.open("map/map.mbtiles").use { input ->
            java.io.FileOutputStream(tmpFile).use { fos ->
                input.copyTo(fos)
                try {
                    fos.fd.sync()
                } catch (ignored: Exception) {
                    // Best-effort; not all filesystems support fsync
                }
            }
        }

        // Verify header before swapping in
        if (!validateMbtiles(tmpFile)) {
            // Clean up bad temp file and throw to surface a clear error
            tmpFile.delete()
            throw IllegalStateException("Bundled MBTiles failed validation after copy")
        }

        // Replace existing atomically
        if (destFile.exists()) destFile.delete()
        if (!tmpFile.renameTo(destFile)) {
            // If rename fails, try manual copy as a fallback
            java.io.FileInputStream(tmpFile).use { inStream ->
                java.io.FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream)
                    try { outStream.fd.sync() } catch (_: Exception) {}
                }
            }
            tmpFile.delete()
        }

        prefs.copiedMbtilesVersion = MBTILES_VERSION
        return destFile.path
    }

    private fun validateMbtiles(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() < 1024) return false
            java.io.FileInputStream(file).use { fis ->
                val header = ByteArray(16)
                val read = fis.read(header)
                read == 16 && String(header, Charsets.UTF_8) == "SQLite format 3\u0000"
            }
        } catch (e: Exception) {
            false
        }
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
            style.withImage("Airport", AppCompatResources.getDrawable(context, R.drawable.airport)!!)
            style.withImage("Rampart", AppCompatResources.getDrawable(context, R.drawable.first_aid)!!)
            style.withImage("Center Camp Plaza", AppCompatResources.getDrawable(context, R.drawable.center_camp)!!)
            style.withImage("center", AppCompatResources.getDrawable(context, R.drawable.center)!!)
            style.withImage("Burner Express Bus Depot", AppCompatResources.getDrawable(context, R.drawable.bus)!!)
            style.withImage("Station 3", AppCompatResources.getDrawable(context, R.drawable.first_aid)!!)
            style.withImage("Station 9", AppCompatResources.getDrawable(context, R.drawable.first_aid)!!)
            style.withImage("Playa Info", AppCompatResources.getDrawable(context, R.drawable.info)!!)
            style.withImage("Ranger Station Berlin", AppCompatResources.getDrawable(context, R.drawable.ranger)!!)
            style.withImage("Ranger Station Tokyo", AppCompatResources.getDrawable(context, R.drawable.ranger)!!)
            style.withImage("Ranger HQ", AppCompatResources.getDrawable(context, R.drawable.ranger)!!)
            style.withImage("Ice Nine Arctica", AppCompatResources.getDrawable(context, R.drawable.ice)!!)
            style.withImage("Arctica Center Camp", AppCompatResources.getDrawable(context, R.drawable.ice)!!)
            style.withImage("Ice Cubed Arctica 3", AppCompatResources.getDrawable(context, R.drawable.ice)!!)
            style.withImage("The Temple", AppCompatResources.getDrawable(context, R.drawable.temple)!!)
            style.withImage("pin", AppCompatResources.getDrawable(context, R.drawable.pin)!!)
            style.withImage("recycle", AppCompatResources.getDrawable(context, R.drawable.recycle)!!)

            map.setStyle(style) { appliedStyle ->
                this.map = map
                this.styleRef = appliedStyle

                // Add our annotations source/layer for SDK 11
                if (appliedStyle.getSource(annotationsSourceId) == null) {
                    appliedStyle.addSource(GeoJsonSource(annotationsSourceId, org.maplibre.geojson.FeatureCollection.fromFeatures(arrayOf())))
                }
                if (appliedStyle.getLayer(annotationsLayerId) == null) {
                    val annotationsLayer = SymbolLayer(annotationsLayerId, annotationsSourceId)

                    // Use app theme colors for text/halo like before
                    val haloColorInt = ContextCompat.getColor(requireContext(), R.color.map_bg)
                    val textColorInt = ContextCompat.getColor(requireContext(), R.color.map_text)
                    val haloHex = String.format("#%06X", 0xFFFFFF and haloColorInt)
                    val textHex = String.format("#%06X", 0xFFFFFF and textColorInt)

                    // Lower text for user POIs to avoid icon overlap
                    val userPoiOffset = Expression.literal(arrayOf(0.0, 2.5))
                    val defaultOffset = Expression.literal(arrayOf(0.0, 0.8))
                    val dynamicOffset = Expression.match(
                        Expression.get("icon"),
                        Expression.literal(UserPoi.ICON_STAR), userPoiOffset,
                        Expression.literal(UserPoi.ICON_HEART), userPoiOffset,
                        Expression.literal(UserPoi.ICON_HOME), userPoiOffset,
                        Expression.literal(UserPoi.ICON_BIKE), userPoiOffset,
                        defaultOffset
                    )

                    annotationsLayer.setProperties(
                        PropertyFactory.iconImage(Expression.coalesce(Expression.get("icon"), Expression.literal("pin"))),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.textField(Expression.coalesce(Expression.get("title"), Expression.literal("") )),
                        PropertyFactory.textSize(12f),
                        PropertyFactory.textHaloColor(haloHex),
                        PropertyFactory.textHaloWidth(2.0f),
                        PropertyFactory.textColor(textHex),
                        PropertyFactory.textOffset(dynamicOffset)
                    )
                    appliedStyle.addLayer(annotationsLayer)
                }

                // Push any existing markers into the new style
                refreshAnnotationSource()

                // Smooth fade-in for labels around labelVisibleZoom
                try {
                    val fadeDelta = 0.5
                    appliedStyle.getLayer(annotationsLayerId)?.setProperties(
                        PropertyFactory.textOpacity(
                            Expression.interpolate(
                                Expression.linear(),
                                Expression.zoom(),
                                Expression.stop(labelVisibleZoom - fadeDelta, 0.0f),
                                Expression.stop(labelVisibleZoom + fadeDelta, 1.0f)
                            )
                        )
                    )
                } catch (t: Throwable) {
                    Timber.w(t, "Unable to apply textOpacity expression to annotations layer")
                }

                // Apply user preferences for camp layer visibility
                try {
                    applyCampLayerPreferences(PrefsHelper(requireContext()))
                } catch (t: Throwable) {
                    Timber.w(t, "Unable to apply camp layer visibility preferences")
                }

                // Tap handling to detect marker clicks
                map.addOnMapClickListener { point ->
                    val screenPoint = map.projection.toScreenLocation(point)

                    // If tap is near the user's current location dot, open Share "My Location"
                    currentLocation?.let { loc ->
                        try {
                            val density = resources.displayMetrics.density
                            val thresholdPx = 32f * density // ~32dp radius
                            val userPoint = map.projection.toScreenLocation(LatLng(loc.latitude, loc.longitude))
                            val dx = screenPoint.x - userPoint.x
                            val dy = screenPoint.y - userPoint.y
                            val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                            if (dist <= thresholdPx) {
                                val pin = com.gaiagps.iburn.database.MapPin(
                                    uid = java.util.UUID.randomUUID().toString(),
                                    title = "My Location",
                                    latitude = loc.latitude.toFloat(),
                                    longitude = loc.longitude.toFloat(),
                                    address = null,
                                    color = "blue"
                                )
                                val intent = com.gaiagps.iburn.activity.ShareActivity.createIntent(requireContext(), pin)
                                startActivity(intent)
                                return@addOnMapClickListener true
                            }
                        } catch (t: Throwable) {
                            Timber.w(t, "Error handling user location tap")
                        }
                    }

                    val features = map.queryRenderedFeatures(screenPoint, annotationsLayerId)
                    val feature = features.firstOrNull()
                    if (feature != null) {
                        val idStr = feature.getStringProperty("mid")
                        val mid = idStr?.toLongOrNull()
                        if (mid != null) {
                            if (markerIdToItem.containsKey(mid)) {
                                val item = markerIdToItem[mid]!!
                                IntentUtil.viewItemDetail(requireActivity(), item)
                            } else if (mappedCustomMarkerIds.containsKey(mid)) {
                                markerStore[mid]?.let { mk -> showEditPinDialog(mk) }
                            }
                            return@addOnMapClickListener true
                        }
                    }
                    false
                }
                val initZoomAmount = 0.2
                val pos = CameraPosition.Builder()
                    .target(LatLng(Geo.MAN_LAT, Geo.MAN_LON))
                    .zoom(defaultZoom - initZoomAmount)
                    .build()

                if (hasLocationPermission()) {
                    val activateOptions =
                        LocationComponentActivationOptions.Builder(requireContext(), appliedStyle)
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

                this.showcaseMarker?.let { _showcaseMarker(it) }
            }
        }
    }

    fun applyCampLayerPreferences(prefs: PrefsHelper) {
        val style = styleRef ?: return
        val showBoundaries = prefs.showCampBoundaries
        val showBigNames = prefs.showBigCampNames

        style.getLayer("camp-boundaries")?.setProperties(
            PropertyFactory.visibility(if (showBoundaries) Property.VISIBLE else Property.NONE)
        )
        style.getLayer("camp-labels-big")?.setProperties(
            PropertyFactory.visibility(if (showBigNames) Property.VISIBLE else Property.NONE)
        )
    }

    private fun setupLocationSub() {
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_NO_POWER)
            .setInterval(5000)

        val context = requireActivity().applicationContext
        locationSubscription?.dispose()
        locationSubscription = LocationProvider.observeCurrentLocation(context, locationRequest)
            .doOnNext { loc -> currentLocation = loc }
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

    private fun setupCameraUpdateSub(map: MapLibreMap) {
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

                val embargoActive = Embargo.isAnyEmbargoActive(prefsHelper)
                val queryAllItems = (state != State.SHOWCASE) && (!embargoActive)

                if (queryAllItems) {
                    val zoom = map.cameraPosition.zoom
                    return@flatMap if (shouldShowPoisAtZoom(zoom)) {
                        Timber.d("Map query for all items + art in-region at zoom %f", zoom)
                        provider.observeAllMapItemsInVisibleRegion(visibleRegion).firstElement().toObservable()
                    } else {
                        Timber.d("Map query for all items (no art) at zoom %f", zoom)
                        provider.observeUserAddedMapItemsOnly().firstElement().toObservable()
                    }
                } else {
                    Timber.d("Map query for user items at zoom %f", map.cameraPosition.zoom)
                    (provider.getUserPoi()).firstElement().toObservable()
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { items: List<PlayaItemWithUserData> ->
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
        styleRef = null
        markerStore.clear()
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
    internal var permanentMarkers = HashSet<Long>()

    // Markers that should be cleared on camera events
    internal var mappedTransientMarkers = ArrayDeque<Long>(MAX_POIS)
    internal var markerIdToItem = HashMap<Long, PlayaItem>()

    private fun refreshAnnotationSource() {
        val style = styleRef ?: return
        val src = style.getSourceAs<GeoJsonSource>(annotationsSourceId) ?: return
        val features = markerStore.values.map { mk ->
            val f = org.maplibre.geojson.Feature.fromGeometry(
                org.maplibre.geojson.Point.fromLngLat(mk.latLng.longitude, mk.latLng.latitude)
            )
            f.addStringProperty("mid", mk.id.toString())
            mk.title?.let { f.addStringProperty("title", it) }
            f.addStringProperty("icon", mk.iconImage)
            f
        }
        src.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeatures(features))
    }

    /**
     * Keep track of the bounds describing a batch of results across Loaders
     */
    private var mResultBounds: LatLngBounds.Builder? = null

    private fun processMapItemResult(items: List<PlayaItemWithUserData>) {

        mResultBounds = LatLngBounds.Builder()

        Timber.d("Got result with %d items", items.size)
        mapView?.getMapAsync { map ->

            val currentZoom = map.cameraPosition.zoom

            val itemsWithLocation = items.filter { it.item.latitude != 0f }
            itemsWithLocation
                .forEach { itemWithUserData ->
                    val item = itemWithUserData.item
                    if (mappedItems.contains(item)) return@forEach // continue to next item

                    if (item is UserPoi) {
                        // UserPois are always-visible and editable when their info window is clicked
                        Timber.d("Adding marker for UserPoi ${item.id}")
                        val marker = addNewMarkerForItem(item)
                        mappedItems.add(item)
                        mappedCustomMarkerIds[marker.id] = item
                    } else if (itemWithUserData.userData.isFavorite) {
                        // Favorites are always-visible, but not editable
                        val marker = addNewMarkerForItem(itemWithUserData.item)
                        markerIdToItem[marker.id] = item
                        mappedItems.add(item)
                        permanentMarkers.add(marker.id)
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


    private fun addNewMarkerForItem(item: PlayaItem): MapMarker {
        val pos = LatLng(item.latitude.toDouble(), item.longitude.toDouble())
        val icon = when (item) {
            is UserPoi -> item.icon
            is Art -> iconArt
            is Camp -> iconCamp
            is Event -> iconEvent
            else -> "pin"
        }
        val marker = MapMarker(nextMarkerId++, pos, item.name, icon)
        markerStore[marker.id] = marker
        refreshAnnotationSource()
        return marker
    }

    /**
     * Map a marker as part of a finite set of markers, limiting the total markers
     * displayed and recycling markers if this limit is exceeded.
     */
    private fun mapRecyclableMarker(
        item: PlayaItem,
        boundsBuilder: LatLngBounds.Builder?
    ): MapMarker? {
        val pos = LatLng(item.latitude.toDouble(), item.longitude.toDouble())

        // Assemble search results region boundary
        if (item !is UserPoi && boundsBuilder != null && state == State.SEARCH) {
            if (cameraBounds.contains(pos)) {
                boundsBuilder.include(pos)
            }
        }

        var marker: MapMarker? = null

        if (mappedTransientMarkers.size == MAX_POIS) {
            // Re-use the eldest Marker
            val oldestId = mappedTransientMarkers.remove()
            val mk = markerStore[oldestId]
            if (mk != null) {
                mk.latLng = pos
                mk.title = item.name
                mk.iconImage = when (item) {
                    is Art -> iconArt
                    is Camp -> iconCamp
                    is Event -> iconEvent
                    is UserPoi -> item.icon
                    else -> mk.iconImage
                }
                mappedTransientMarkers.add(mk.id)
                markerIdToItem[mk.id] = item
                marker = mk
            }
        } else {
            // Create a new Marker
            val mk = addNewMarkerForItem(item)
            markerIdToItem[mk.id] = item
            mappedTransientMarkers.add(mk.id)
            marker = mk
        }
        refreshAnnotationSource()
        return marker
    }

    /**
     * Clear markers marked permanent. These are not removed due to camera change events.
     * Currently used for user-selected favorite items.
     */
    fun clearPermanentMarkers() {
        for (mid in permanentMarkers) {
            markerStore.remove(mid)
            val metaId = markerIdToItem.remove(mid)
            if (metaId != null) mappedItems.remove(metaId)
        }
        permanentMarkers.clear()
        refreshAnnotationSource()
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

    private fun resetMapView(map: MapLibreMap) {
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

        for (mid in mappedTransientMarkers) {
            markerStore.remove(mid)
            val metaId = markerIdToItem.remove(mid)
            if (metaId != null) mappedItems.remove(metaId)
        }
        mappedTransientMarkers.clear()
        refreshAnnotationSource()
    }


    fun areMarkersVisible(): Boolean {
        return mappedTransientMarkers.size > 0
    }

    private fun showEditPinDialog(marker: MapMarker) {
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
            markerTitle?.setText(marker.title)
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

            var didDelete = false

            val builder = AlertDialog.Builder(requireActivity(), R.style.Theme_Iburn_Dialog)
                .setView(dialogBody)
                .setPositiveButton("Move") { dialog, which ->
                    if (state != State.PLACE_USER_POI) {
                        val prePlaceUserPoiState = state

                        state = State.PLACE_USER_POI

                        mapView?.getMapAsync { map ->

                            // No-op: legacy deselection removed with SymbolManager

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
                                    refreshAnnotationSource()
                                }
                            }
                        }
                    }
                }
                .setNeutralButton("Share") { dialog, which ->
                    try {
                        // Build a MapPin from the current UserPoi for sharing
                        val title = (markerTitle?.text?.toString()?.takeIf { it.isNotBlank() }
                            ?: (userPoi.name ?: "Custom Pin"))
                        val pin = com.gaiagps.iburn.database.MapPin(
                            uid = userPoi.playaId ?: java.util.UUID.randomUUID().toString(),
                            title = title,
                            description = userPoi.description,
                            latitude = userPoi.latitude,
                            longitude = userPoi.longitude,
                            address = userPoi.playaAddress,
                            // map color optionally by icon; default to red for now
                            color = "red",
                            icon = userPoi.icon,
                            createdAt = System.currentTimeMillis(),
                            notes = null
                        )
                        val intent = com.gaiagps.iburn.activity.ShareActivity.createIntent(requireContext(), pin)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to share user pin")
                    }
                }
                .setNegativeButton("Delete") { dialog, which ->
                    // Delete Pin
                    didDelete = true
                    removeCustomPin(marker)
                }
            val alertDialog = builder.create()

            // Auto-save on dismiss: update title and icon selections
            alertDialog.setOnDismissListener {
                if (didDelete) return@setOnDismissListener

                // Save the title if changed
                if (markerTitle?.text?.isNotBlank() == true) {
                    marker.title = markerTitle?.text.toString()
                }

                // Update icon based on selection
                when (iconGroup.checkedRadioButtonId) {
                    R.id.btn_star -> marker.iconImage = UserPoi.ICON_STAR
                    R.id.btn_heart -> marker.iconImage = UserPoi.ICON_HEART
                    R.id.btn_home -> marker.iconImage = UserPoi.ICON_HOME
                    R.id.btn_bike -> marker.iconImage = UserPoi.ICON_BIKE
                }

                updateCustomPinWithMarker(marker)
            }

            alertDialog.show()
        }
    }

    /**
     * Adds a custom pin to the current map and database
     */
    private fun addCustomPin(
        map: MapLibreMap,
        latLng: LatLng?,
        title: String,
        @UserPoi.Icon poiIcon: String,
        callback: ((symbol: MapMarker) -> Unit)?
    ) {
        var markerLatLng = latLng
        if (markerLatLng == null) {
            val mapCenter = map.cameraPosition.target
            markerLatLng = LatLng(mapCenter!!.latitude, mapCenter!!.longitude)
        }

        val symbol = MapMarker(nextMarkerId++, markerLatLng, title, UserPoi.ICON_STAR)
        markerStore[symbol.id] = symbol
        refreshAnnotationSource()

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
                .subscribe { userPoiWithUserData ->
                    val userPoi = userPoiWithUserData.item
                    Timber.d("After inserting, userPoi has id ${userPoi.id}")
                    // Make sure UserPoi is added to mappedItems before being inserted as this will
                    // trigger a map items update
                    mappedItems.add(userPoi)
                    mappedCustomMarkerIds[symbol.id] = userPoi
                    callback?.invoke(symbol)
                }
        } catch (e: NumberFormatException) {
            Timber.w("Unable to get id for new custom marker")
        }
    }

    private fun removeCustomPin(marker: MapMarker) {
        markerStore.remove(marker.id)
        refreshAnnotationSource()
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
    private fun updateCustomPinWithMarker(symbol: MapMarker) {
        val userPoi = mappedCustomMarkerIds[symbol.id]
        userPoi?.let { userPoi ->
            userPoi.icon = symbol.iconImage
            userPoi.name = symbol.title
            userPoi.latitude = symbol.latLng.latitude.toFloat()
            userPoi.longitude = symbol.latLng.longitude.toFloat()

            DataProvider.getInstance(requireActivity().applicationContext)
                .observeOn(ioScheduler)
                .map { dataProvider -> dataProvider.update(userPoi) }
                .subscribe { _ -> Timber.d("Updated marker") }
            refreshAnnotationSource()
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
