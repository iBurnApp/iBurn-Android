package com.gaiagps.iburn.activity

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Point
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gaiagps.iburn.AudioTourManager
import com.gaiagps.iburn.Callback
import com.gaiagps.iburn.CurrentDateProvider
import com.gaiagps.iburn.DateUtil
import com.gaiagps.iburn.IntentUtil
import com.gaiagps.iburn.ImageSource
import com.gaiagps.iburn.MapboxMapFragment
import com.gaiagps.iburn.PrefsHelper
import com.gaiagps.iburn.R
import com.gaiagps.iburn.adapters.AdapterListener
import com.gaiagps.iburn.adapters.AdapterUtils
import com.gaiagps.iburn.adapters.PlayaItemAdapter
import com.gaiagps.iburn.database.Art
import com.gaiagps.iburn.database.ArtWithUserData
import com.gaiagps.iburn.database.Camp
import com.gaiagps.iburn.database.DataProvider
import com.gaiagps.iburn.database.Embargo
import com.gaiagps.iburn.database.Event
import com.gaiagps.iburn.database.EventWithUserData
import com.gaiagps.iburn.database.PlayaItem
import com.gaiagps.iburn.database.PlayaItemWithUserData
import com.gaiagps.iburn.database.MutantVehicle
import com.gaiagps.iburn.databinding.ActivityPlayaItemViewBinding
import com.gaiagps.iburn.location.LocationProvider
import com.gaiagps.iburn.loadArtImage
import com.gaiagps.iburn.loadMutantVehicleImage
import com.gaiagps.iburn.service.AudioPlayerService
import com.gaiagps.iburn.service.MediaMetadataKeyArtPlayaId
import com.gaiagps.iburn.view.FullscreenImageDialog
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Show the detail view for a Camp, Art installation, or Event
 * Created by davidbrodsky on 8/11/13.
 */
class PlayaItemViewActivity : AppCompatActivity(), AdapterListener {

    companion object {
        // Database primary key for the item to display
        const val EXTRA_PLAYA_ITEM_ID = "playa-item-id"
        const val EXTRA_PLAYA_ITEM_TYPE = "playa-type"
        const val EXTRA_PLAYA_ITEM_CAMP = "playa-camp"
        const val EXTRA_PLAYA_ITEM_ART = "playa-art"
        const val EXTRA_PLAYA_ITEM_EVENT = "playa-event"
        const val EXTRA_PLAYA_ITEM_MUTANT_VEHICLE = "playa-mutant-vehicle"

        // Avoid spamming Crashlytics with duplicate non-fatals per process
        private var reportedBadIntentOnce = false
    }

    private var itemWithUserData: PlayaItemWithUserData? = null
    private var latLng: LatLng? = null

    private var isFavorite = false
    private var showingLocation = false
    private var showingArt = false
    private var showingImage = false

    private var favoriteMenuItem: MenuItem? = null
    private var imageMenuItem: MenuItem? = null

    private var audioTourManager: AudioTourManager? = null
    private var didPopulateViews = false
    private var audioTourToggle: TextView? = null

    private lateinit var binding: ActivityPlayaItemViewBinding
    private var loadedArtImage = false
    private var artImageView: ImageView? = null
    private var mapFragment: MapboxMapFragment? = null
    private var deviceLocation: Location? = null
    private var locationJob: Job? = null
    private val locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()

    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaControllerCallback: MediaControllerCompat.Callback? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayaItemViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        didPopulateViews = false

        loadPlayaItemFromIntent(intent)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.title = ""
            it.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp)
        }
        setTextContainerMinHeight()
        val fadeAnimation = AlphaAnimation(0f, 1f)
        fadeAnimation.duration = 1000
        fadeAnimation.startOffset = 250
        fadeAnimation.fillAfter = true
        fadeAnimation.isFillEnabled = true
        binding.mapContainer.startAnimation(fadeAnimation)
        // The rest of onCreate will be called after item is loaded
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Reload content for successive intents (e.g., repeated deep links)
        loadPlayaItemFromIntent(intent)
    }

    private fun loadPlayaItemFromIntent(i: Intent) {
        val itemId = i.getIntExtra(EXTRA_PLAYA_ITEM_ID, -1)
        val type = i.getStringExtra(EXTRA_PLAYA_ITEM_TYPE)
        if (itemId == -1 || type == null) {
            Timber.e("Missing itemId or type in Intent: itemId=%d type=%s", itemId, type)
            try {
                FirebaseCrashlytics.getInstance().setCustomKey("PIVA_missing_inputs", true)
                FirebaseCrashlytics.getInstance().setCustomKey("PIVA_item_id", itemId)
                FirebaseCrashlytics.getInstance().setCustomKey("PIVA_item_type", type ?: "null")
                FirebaseCrashlytics.getInstance().log("PIVA: missing inputs in PlayaItemViewActivity.loadPlayaItemFromIntent")
                if (!reportedBadIntentOnce) {
                    FirebaseCrashlytics.getInstance().recordException(
                        IllegalArgumentException("Non-fatal: Missing itemId or type in Intent")
                    )
                    reportedBadIntentOnce = true
                }
            } catch (ignored: Throwable) {
            }

            // Finish quietly to avoid crashing users
            finishWithError(IllegalArgumentException("Missing itemId or type in Intent"))
            return
        }

        // Use lifecycleScope to fetch the item
        lifecycleScope.launch {
            try {
                val provider = DataProvider.getInstance(applicationContext)
                itemWithUserData = withContext(Dispatchers.IO) {
                    when (type) {
                        EXTRA_PLAYA_ITEM_CAMP -> provider.getCampByIdBlocking(itemId)
                        EXTRA_PLAYA_ITEM_ART -> provider.getArtByIdBlocking(itemId)
                        EXTRA_PLAYA_ITEM_EVENT -> provider.getEventByIdBlocking(itemId)
                        EXTRA_PLAYA_ITEM_MUTANT_VEHICLE -> provider.getMutantVehicleByIdBlocking(itemId)
                        else -> throw IllegalArgumentException("Unknown PlayaItem type $type")
                    }
                }
                onPlayaItemLoaded()
            } catch (t: Throwable) {
                finishWithError(t)
            }
        }
    }

    private fun onPlayaItemLoaded() {
        // Continue with the rest of onCreate logic that depends on itemWithUserData
        val item = itemWithUserData
        if (item is ArtWithUserData && item.item.hasAudioTour(this)) {
            onCreateMediaController()
        }
        binding.appbar.addOnOffsetChangedListener { _, verticalOffset ->
            val collapsingTriggerHeight = binding.collapsingToolbar.scrimVisibleHeightTrigger
            val collapsingOffsetTrigger = -(binding.collapsingToolbar.height - collapsingTriggerHeight)
            val scrimFadeDistance = binding.toolbar.height.coerceAtLeast(1)
            binding.toolbarScrim.alpha = (
                (verticalOffset - collapsingOffsetTrigger).toFloat() / scrimFadeDistance
            ).coerceIn(0f, 1f)

            if (verticalOffset <= collapsingOffsetTrigger) {
                // Collapsed
                val imageMenu = imageMenuItem
                if (showingLocation && showingArt && imageMenu != null && imageMenu.isVisible && loadedArtImage) {
                    Timber.d("Setting imageMenu invisible on collapse")
                    imageMenu.isVisible = false
                }
            } else {
                // Expanded
                val imageMenu = imageMenuItem
                if (showingLocation && showingArt && imageMenu != null && !imageMenu.isVisible && loadedArtImage) {
                    Timber.d("Setting imageMenu visible on expand")
                    imageMenu.isVisible = true
                }
            }
        }
        if (!didPopulateViews && itemWithUserData != null) {
            populateViews(itemWithUserData!!)
            didPopulateViews = true
        }
    }

    private fun finishWithError(throwable: Throwable) {
        // Optionally show error to user
        finish()
    }

    private fun onCreateMediaController() {
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, AudioPlayerService::class.java),
            PlayaItemViewMediaConnectionCallback(),
            null
        ) // optional Bundle
        // Connect immediately so ConnectionCallback can fire
        onStartMediaController()
    }

    private fun onStartMediaController() {
        val browser = mediaBrowser
        if (browser != null && !browser.isConnected) {
            browser.connect()
        }
    }

    private fun onStopMediaController() {
        MediaControllerCompat.getMediaController(this)?.let { controller ->
            mediaControllerCallback?.let { callback ->
                controller.unregisterCallback(callback)
            }
        }
        val browser = mediaBrowser
        if (browser != null && browser.isConnected) {
            browser.disconnect()
        }
    }

    override fun onStart() {
        super.onStart()
        // If the browser was created earlier, ensure it's connected
        onStartMediaController()
        startMonitoringLocation()
    }

    override fun onStop() {
        stopMonitoringLocation()
        super.onStop()
        // Tidy up the media connection when leaving the screen
        onStopMediaController()
    }

    private fun startMonitoringLocation() {
        if (locationJob != null) return
        locationJob = LocationProvider.currentLocationFlow(applicationContext, locationRequest)
            .onEach { lastLocation ->
                val prior = deviceLocation
                val deltaMeters = prior?.distanceTo(lastLocation) ?: Float.MAX_VALUE
                deviceLocation = lastLocation
                if (prior == null || deltaMeters > 61f) {
                    itemWithUserData?.item?.let(::updateLocationTimes)
                }
            }
            .catch { error ->
                Timber.e(error, "Failed to get last location")
            }
            .launchIn(lifecycleScope)
    }

    private fun stopMonitoringLocation() {
        locationJob?.cancel()
        locationJob = null
    }

    private inner class PlayaItemViewMediaConnectionCallback : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            Timber.d("Connected to media service")
            // Get the token for the MediaSession
            val token = mediaBrowser!!.sessionToken

            // Create a MediaControllerCompat
            val mediaController = MediaControllerCompat(this@PlayaItemViewActivity, token)

            // Save the controller
            MediaControllerCompat.setMediaController(this@PlayaItemViewActivity, mediaController)

            // Finish building the UI
            setupMediaTransportControls()
        }

        override fun onConnectionFailed() {
            Timber.d("Connection to media service failed")
            // This means no current media session is active
            // Finish building the UI
            // setupMediaTransportControls();
        }
    }

    private inner class PlayaItemMediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            // Selected corresponds to the playing state
            val playbackState = state.state
            Timber.d("onPlaybackStateChanged to %d", playbackState)
            val mediaController = MediaControllerCompat.getMediaController(this@PlayaItemViewActivity)
            setAudioTourToggleStateWithPlaybackState(mediaController, itemWithUserData?.item)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat) {
            Timber.d("onMetadataChanged to %s", metadata)
        }
    }

    private fun setupMediaTransportControls() {
        // Find or create Audio Tour Playback Toggle View
        val audioTourContainer = binding.audioTourContainer
        var audioTourToggle = audioTourContainer.findViewById<TextView>(R.id.audio_tour_toggle)

        if (audioTourToggle == null) {
            audioTourToggle = TextView(this)
            audioTourToggle.id = R.id.audio_tour_toggle
            audioTourToggle.setTextColor(resources.getColor(R.color.regular_text))
            audioTourToggle.textSize = 22f
            audioTourToggle.compoundDrawablePadding = 12 // 8 dp
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 12 // 8 dp
            audioTourToggle.layoutParams = params

            audioTourContainer.addView(audioTourToggle)
        }

        audioTourContainer.visibility = View.VISIBLE
        this.audioTourToggle = audioTourToggle

        // Get initial media state
        val mediaController = MediaControllerCompat.getMediaController(this@PlayaItemViewActivity)
        val metadata = mediaController.metadata
        val pbState = mediaController.playbackState

        val item = itemWithUserData?.item
        audioTourToggle.setOnClickListener {
            val currentMetadata = mediaController.metadata
            val currentPbState = mediaController.playbackState.state
            Timber.d("Audio tour toggle hit in state %d with metadata %s", currentPbState, currentMetadata)

            if (!isCurrentMediaSessionForItem(mediaController, item)) {
                Timber.d("Starting audio tour playback anew for item %s", item?.name)
                // Need to start up the media service
                audioTourManager?.playAudioTourUrl(item as Art)
            } else if (currentPbState == PlaybackStateCompat.STATE_PLAYING) {
                Timber.d("Resuming audio tour playback for item %s", item?.name)
                MediaControllerCompat.getMediaController(this@PlayaItemViewActivity).transportControls.pause()
            } else if (currentPbState == PlaybackStateCompat.STATE_PAUSED) {
                Timber.d("Pausing audio tour playback for item %s", item?.name)
                MediaControllerCompat.getMediaController(this@PlayaItemViewActivity).transportControls.play()
            } else {
                Timber.e("Unable to handle audio tour playback toggle. MediaController in unknown state %s", pbState)
            }
            setAudioTourToggleStateWithPlaybackState(mediaController, item)
        }

        val playbackState = pbState.state
        Timber.d("Initial MediaController state %d with metadata %s", playbackState, metadata)

        setAudioTourToggleStateWithPlaybackState(mediaController, item)

        // Register a Callback to stay in sync
        if (mediaControllerCallback == null) {
            mediaControllerCallback = PlayaItemMediaControllerCallback()
        }
        mediaControllerCallback?.let { callback ->
            mediaController.registerCallback(callback)
        }
    }

    private fun setAudioTourToggleStateWithPlaybackState(
        mediaControllerCompat: MediaControllerCompat?,
        item: PlayaItem?
    ) {
        if (item == null || mediaControllerCompat == null) return

        val playbackState = mediaControllerCompat.playbackState.state
        val audioTourToggleSelected = (playbackState == PlaybackStateCompat.STATE_PLAYING) &&
                isCurrentMediaSessionForItem(mediaControllerCompat, item)
        audioTourToggle?.isSelected = audioTourToggleSelected
        onAudioTourToggleSelectedChanged()
    }

    override fun onResume() {
        super.onResume()
        audioTourManager = AudioTourManager(this)
        if (!didPopulateViews && itemWithUserData != null) {
            populateViews(itemWithUserData!!)
            didPopulateViews = true
        }
    }

    /**
     * Set the text container within NestedScrollView to have height exactly equal to the
     * full height minus status bar and toolbar. This addresses an issue where the
     * collapsing toolbar pattern gets all screwed up.
     */
    private fun setTextContainerMinHeight() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val height = size.y

        val textSizeAttr = intArrayOf(android.R.attr.actionBarSize)
        val indexOfAttrTextSize = 0
        val a = obtainStyledAttributes(textSizeAttr)
        val abHeight = a.getDimensionPixelSize(indexOfAttrTextSize, -1)
        a.recycle()

        val r = resources
        val statusBarPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, r.displayMetrics
        ).toInt()

        binding.textContainer.minimumHeight = height - abHeight - statusBarPx
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_playa_item, menu)

        favoriteMenuItem = menu.findItem(R.id.favorite_menu)
        if (isFavorite) favoriteMenuItem?.setIcon(R.drawable.ic_heart_full_white_24dp)
        favoriteMenuItem?.isVisible = true

        imageMenuItem = menu.findItem(R.id.image_menu)
        if (!loadedArtImage || !showingLocation) {
            imageMenuItem?.isVisible = false
        } else {
            setImageMenuToggle(showingImage)
        }
        Timber.d("onCreateOptionsMenu image visible %b", imageMenuItem?.isVisible)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.favorite_menu -> {
                setFavorite(!isFavorite, true)
                true
            }
            R.id.share_menu -> {
                shareItem()
                true
            }
            R.id.image_menu -> {
                val imageView = artImageView
                if (imageView != null) {
                    val willBeVisible = !showingImage
                    if (willBeVisible) {
                        imageView.bringToFront()
                    } else if (itemWithUserData?.item is Art) {
                        mapFragment?.startShowcase()
                    }
                    imageView.isClickable = willBeVisible
                    imageView.isFocusable = willBeVisible
                    Timber.d("Fading %s art view", if (willBeVisible) "in" else "out")
                    fadeView(imageView, willBeVisible, null)

                    showingImage = willBeVisible
                    setImageMenuToggle(willBeVisible)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun populateViews(itemWithUserData: PlayaItemWithUserData) {
        val item = itemWithUserData.item
        val prefs = PrefsHelper(applicationContext)
        val embargoActive = Embargo.isEmbargoActiveForPlayaItem(prefs, item)
        val addressEmbargoActive = Embargo.isAddressEmbargoActiveForPlayaItem(prefs, item)
        showingLocation = (item.hasLocation() && !embargoActive) || item.hasUnofficialLocation()
        showingArt = (item is Art && item.hasImage()) ||
                (item is MutantVehicle && item.hasImage())
        binding.toolbarScrim.visibility = if (showingLocation || showingArt) View.VISIBLE else View.GONE
        if (showingArt) {
            // Image-backed details should appear immediately, without inheriting the
            // showcase map's entrance animation.
            binding.mapContainer.clearAnimation()
        }

        if (showingLocation) {
            if (item.hasLocation() && !embargoActive) {
                latLng = item.latLng
            } else {
                latLng = item.unofficialLatLng
            }
            latLng?.let { location ->
                Timber.d("adding / centering marker on %f, %f", location.latitude, location.longitude)

                val mapFragment = MapboxMapFragment().also {
                    this.mapFragment = it
                }
                if (item is Art && item.hasImage()) {
                    mapFragment.prepareShowcaseItem(item)
                } else {
                    mapFragment.showcaseItem(item)
                }
                supportFragmentManager.beginTransaction()
                    .add(R.id.map_container, mapFragment)
                    .runOnCommit {
                        // The fragment view may be attached after the image finishes loading.
                        // Restore the requested stacking order once the transaction completes.
                        if (showingArt) {
                            artImageView?.bringToFront()
                        }
                    }
                    .commit()
            }
        } else if (showingArt) {
            // The content image will be added by the type-specific view population.
        } else {
            // Adjust the margin / padding show the heart icon doesn't
            // overlap title + description
            findViewById<View>(R.id.map_container).visibility = View.GONE
            binding.collapsingToolbar.setBackgroundResource(R.color.iburn_color)
            val parms = CollapsingToolbarLayout.LayoutParams(
                CollapsingToolbarLayout.LayoutParams.MATCH_PARENT, 24
            )
            binding.mapContainer.layoutParams = parms
        }

        binding.itemType.setText(
            when (item) {
                is Event -> R.string.detail_type_event
                is Camp -> R.string.detail_type_camp
                is Art -> R.string.detail_type_art
                is MutantVehicle -> R.string.detail_type_mutant_vehicle
                else -> R.string.app_name
            }
        )
        binding.title.text = item.name
        setFavorite(itemWithUserData.userData.isFavorite, false)

        if (!item.description.isNullOrEmpty()) {
            binding.body.text = item.description
            binding.aboutContainer.visibility = View.VISIBLE
        } else {
            binding.aboutContainer.visibility = View.GONE
        }

        if (!addressEmbargoActive) {
            setTextOrHideIfEmpty(item.playaAddress, binding.locationAddress)
        } else if (item.hasUnofficialLocation()) {
            setTextOrHideIfEmpty("BurnerMap: " + item.playaAddressUnofficial, binding.locationAddress)
        } else {
            binding.locationAddress.visibility = View.GONE
        }
        updateLocationRowVisibility()

        updateLocationTimes(item)

        lifecycleScope.launch {
            val provider = DataProvider.getInstance(applicationContext)
            when (item) {
                is Art -> populateArtViews(item, provider)
                is Camp -> populateCampViews(item, provider)
                is Event -> populateEventViews(item, provider)
                is MutantVehicle -> populateMutantVehicleViews(item)
                else -> Timber.e("Unknown PlayaItem type %s", item.javaClass.simpleName)
            }
        }
    }

    private fun updateLocationTimes(item: PlayaItem) {
        val prefs = PrefsHelper(applicationContext)
        val canShowOfficialLocation =
            !Embargo.isEmbargoActiveForPlayaItem(prefs, item) && item.hasLocation()
        val canShowUnofficialLocation = item.hasUnofficialLocation()

        if (!canShowOfficialLocation && !canShowUnofficialLocation) {
            binding.locationTimes.visibility = View.GONE
            updateLocationRowVisibility()
            return
        }

        binding.locationTimes.visibility = View.VISIBLE
        updateLocationRowVisibility()
        val latitude = if (canShowOfficialLocation) item.latitude else item.latitudeUnofficial
        val longitude = if (canShowOfficialLocation) item.longitude else item.longitudeUnofficial
        val event = item as? Event
        val walkTime = binding.locationTimes.findViewById<TextView>(R.id.walk_time)
        val bikeTime = binding.locationTimes.findViewById<TextView>(R.id.bike_time)
        val iconTint = ColorStateList.valueOf(getColor(R.color.sub_sub_text))
        walkTime.compoundDrawableTintList = iconTint
        bikeTime.compoundDrawableTintList = iconTint
        AdapterUtils.setDistanceText(
            deviceLocation,
            CurrentDateProvider.getCurrentDate(),
            event?.startDate,
            event?.endDate,
            walkTime,
            bikeTime,
            latitude,
            longitude
        )
    }

    private fun updateLocationRowVisibility() {
        binding.locationRow.visibility = if (
            binding.locationAddress.visibility == View.VISIBLE ||
            binding.locationTimes.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE
    }

    private fun setMetadata(labelRes: Int, primary: String?, secondary: String? = null) {
        if (primary.isNullOrEmpty() && secondary.isNullOrEmpty()) {
            binding.metadataRow.visibility = View.GONE
            return
        }

        binding.metadataLabel.setText(labelRes)
        setTextOrHideIfEmpty(primary, binding.metadataPrimary)
        setTextOrHideIfEmpty(secondary, binding.metadataSecondary)
        binding.metadataRow.visibility = View.VISIBLE
    }

    private suspend fun populateArtViews(art: Art, provider: DataProvider) {
        setMetadata(R.string.detail_artist, art.artist, art.artistLocation)

        if (art.hasImage()) {
            artImageView = ImageView(this)
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
            artImageView?.layoutParams = params
            artImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
            artImageView?.alpha = 1f
            artImageView?.setBackgroundResource(R.color.map_bg)
            binding.mapContainer.addView(artImageView, 0)
            artImageView?.bringToFront()

            artImageView?.let { imageView ->
                loadArtImage(art, imageView, object : Callback {
                    override fun onSuccess(source: ImageSource) {
                        loadedArtImage = true
                        showingImage = true
                        imageView.alpha = 1f
                        imageView.bringToFront()
                        enableFullscreenImageOnTap(imageView)
                        invalidateOptionsMenu()
                        Timber.d("Loaded art image for %s from %s", art.playaId, source)
                    }

                    override fun onError() {
                        imageView.visibility = View.GONE
                        mapFragment?.startShowcase()
                        Timber.e("Failed to load image %s", art.imageUrl)
                    }
                }, fadeIn = false)
            }
            // TODO : Add Placeholder and error images
        }

        // Note : Audio Tour views are populated separately when connection to the
        // AudioPlaybackService is complete. See setupMediaTransportControls
    }

    private suspend fun populateCampViews(camp: Camp, provider: DataProvider) {
        setMetadata(R.string.detail_hometown, camp.hometown)

        // Display hosted events
        val adapter = PlayaItemAdapter(applicationContext, this)

        // This list will be updated if a favorite changes
        lifecycleScope.launch {
            provider.observeEventsHostedByCamp(camp).collect { events ->
                val pad = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
                ).toInt()

                binding.overflowContainer.removeAllViews()

                if (events.isNotEmpty()) {
                    val wrapper = ContextThemeWrapper(this@PlayaItemViewActivity, R.style.PlayaTextItem)
                    val hostedEventsTitle = TextView(wrapper)
                    hostedEventsTitle.setText(R.string.hosted_events)
                    hostedEventsTitle.setTextColor(getColor(R.color.sub_text))
                    hostedEventsTitle.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    hostedEventsTitle.isAllCaps = true
                    hostedEventsTitle.textSize = 12f
                    hostedEventsTitle.setPadding(pad, pad, pad, pad)
                    binding.overflowContainer.addView(hostedEventsTitle)
                }

                adapter.items = events

                for (idx in events.indices) {
                    val holder = adapter.createViewHolder(binding.overflowContainer, 0) as PlayaItemAdapter.ViewHolder
                    adapter.bindViewHolder(holder, idx)
                    binding.overflowContainer.addView(holder.itemView)
                }
            }
        }
    }

    private fun populateMutantVehicleViews(vehicle: MutantVehicle) {
        setMetadata(R.string.detail_creator, vehicle.artist, vehicle.hometown)
        if (!vehicle.hasImage()) return

        artImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = .99f
        }
        binding.mapContainer.addView(artImageView, 0)
        artImageView?.let { imageView ->
            loadMutantVehicleImage(vehicle, imageView, object : Callback {
                override fun onSuccess(source: ImageSource) {
                    loadedArtImage = true
                    showingImage = true
                    imageView.alpha = 1f
                    imageView.bringToFront()
                    enableFullscreenImageOnTap(imageView)
                    invalidateOptionsMenu()
                }

                override fun onError() {
                    Timber.e("Failed to load image %s", vehicle.imageUrl)
                }
            })
        }
    }

    private fun enableFullscreenImageOnTap(imageView: ImageView) {
        imageView.isClickable = true
        imageView.isFocusable = true
        imageView.contentDescription = getString(R.string.view_image_fullscreen)
        imageView.setOnClickListener {
            if (
                showingImage &&
                imageView.visibility == View.VISIBLE &&
                imageView.alpha > 0f &&
                imageView.drawable != null
            ) {
                FullscreenImageDialog(
                    this,
                    imageView,
                    itemWithUserData?.item?.name
                ).show()
            }
        }
    }

    private suspend fun populateEventViews(event: Event, provider: DataProvider) {
        val nowDate = CurrentDateProvider.getCurrentDate()

        // Describe the event time with some smarts: "[Starts|Ends] [in|at] [20m|4:20p]"
        binding.eventTime.text = DateUtil.getDateString(
            applicationContext,
            nowDate,
            event.startDate,
            event.startTimePretty,
            event.endDate,
            event.endTimePretty
        )
        binding.eventTimeRow.visibility = View.VISIBLE

        // Display Hosted-By-Camp
        val wrapper = ContextThemeWrapper(this, R.style.PlayaTextItem)
        val condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        val pad = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
        ).toInt()

        if (event.hasCampHost()) {
            lifecycleScope.launch {
                try {
                    val camp = provider.observeCampByPlayaId(event.campPlayaId!!).first()
                    showHost(camp)
                } catch (e: Exception) {
                    Timber.w(e, "Could not load camp for event")
                }
            }
        } else if (event.hasArtHost()) {
            lifecycleScope.launch {
                try {
                    val art = provider.observeArtByPlayaId(event.artPlayaId!!).first()
                    showHost(art)
                } catch (e: Exception) {
                    Timber.w(e, "Could not load art host for event")
                }
            }
        }

        // Display other event occurrences
        lifecycleScope.launch {
            try {
                val eventOccurrences = provider.observeOtherOccurrencesOfEvent(event).first()
                Timber.d("Got %d other event occurrences", eventOccurrences.size)
                if (eventOccurrences.isNotEmpty()) {
                    val occurrencesTitle = TextView(wrapper)
                    occurrencesTitle.setText(R.string.also_at)
                    occurrencesTitle.setTextColor(getColor(R.color.sub_text))
                    occurrencesTitle.setTypeface(occurrencesTitle.typeface, Typeface.BOLD)
                    occurrencesTitle.textSize = 15f
                    occurrencesTitle.setPadding(pad, pad, pad, 0)
                    binding.overflowContainer.addView(occurrencesTitle)
                }

                val timeDayFormatter = DateUtil.getPlayaTimeFormat("EEEE, M/d 'at' h:mm a")

                for (occurrence in eventOccurrences) {
                    val eventTv = TextView(wrapper)
                    eventTv.typeface = condensed
                    eventTv.textSize = 20f
                    eventTv.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    eventTv.text = timeDayFormatter.format(occurrence.item.startDate)
                    eventTv.setOnClickListener(RelatedItemOnClickListener(occurrence))
                    eventTv.setPadding(pad, pad, pad, pad)

                    val outValue = TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    eventTv.setBackgroundResource(outValue.resourceId)
                    binding.overflowContainer.addView(eventTv)
                }
            } catch (e: Exception) {
                Timber.w(e, "Could not load event occurrences")
            }
        }
    }

    private fun showHost(host: PlayaItemWithUserData) {
        binding.hostName.text = host.item.name
        binding.hostContainer.tag = host.item.playaId
        binding.hostContainer.setOnClickListener(RelatedItemOnClickListener(host))
        binding.hostContainer.visibility = View.VISIBLE
    }

    private fun setTextOrHideIfEmpty(text: String?, view: TextView) {
        if (!TextUtils.isEmpty(text)) {
            view.text = text
            view.visibility = View.VISIBLE
        } else {
            view.visibility = View.GONE
        }
    }

    private fun isCurrentMediaSessionForItem(
        mediaControllerCompat: MediaControllerCompat?,
        item: PlayaItem?
    ): Boolean {
        if (mediaControllerCompat == null || item == null) return false

        val metadata = mediaControllerCompat.metadata ?: return false
        // Prefer custom key, fall back to standard MEDIA_ID
        var currentlyPlayingArtPlayaId = metadata.getString(MediaMetadataKeyArtPlayaId)
        if (currentlyPlayingArtPlayaId == null) {
            currentlyPlayingArtPlayaId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        }
        return currentlyPlayingArtPlayaId != null && currentlyPlayingArtPlayaId == item.playaId
    }

    private fun onAudioTourToggleSelectedChanged() {
        val toggle = audioTourToggle ?: return
        if (toggle.isSelected) {
            // Playing. Show Pause button
            toggle.setCompoundDrawablesWithIntrinsicBounds(
                resources.getDrawable(R.drawable.ic_pause_circle_outline_light_24dp), null, null, null
            )
            toggle.setText(R.string.pause_audio_tour)
        } else {
            // Paused. Show Play button
            toggle.setCompoundDrawablesWithIntrinsicBounds(
                resources.getDrawable(R.drawable.ic_play_circle_outline_light_24dp), null, null, null
            )
            toggle.setText(R.string.play_audio_tour)
        }
    }

    override fun onItemSelected(item: PlayaItemWithUserData) {
        IntentUtil.viewItemDetail(this, item.item)
    }

    override fun onItemFavoriteButtonSelected(item: PlayaItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                DataProvider.getInstance(applicationContext).toggleFavorite(item)
            } catch (e: Exception) {
                Timber.e(e, "Failed to toggle favorite")
            }
        }
    }

    inner class RelatedItemOnClickListener(private val item: PlayaItemWithUserData) : View.OnClickListener {
        override fun onClick(v: View) {
            IntentUtil.viewItemDetail(this@PlayaItemViewActivity, item.item)
        }
    }

    private fun setFavorite(isFavorite: Boolean, save: Boolean) {
        val item = itemWithUserData
        if (item == null) {
            Timber.w("setFavorite called before model data ready. Ignoring")
            return
        }

        val newMenuDrawableResId = if (isFavorite) {
            R.drawable.ic_heart_full_white_24dp
        } else {
            R.drawable.ic_heart_empty_white_24dp
        }

        favoriteMenuItem?.setIcon(newMenuDrawableResId)
        if (save) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    DataProvider.getInstance(applicationContext).toggleFavorite(item.item)
                } catch (throwable: Throwable) {
                    Timber.e(throwable, "Failed to save")
                }
            }
        }
        this.isFavorite = isFavorite
    }

    private fun shareItem() {
        val item = itemWithUserData?.item
        if (item != null) {
            val shareIntent = ShareActivity.createIntent(this, item)
            startActivity(shareIntent)
        }
    }

    private fun setImageMenuToggle(isShowingImage: Boolean) {
        val imageMenu = imageMenuItem ?: return
        if (isShowingImage) {
            imageMenu.setIcon(R.drawable.ic_map_white_24dp)
        } else {
            imageMenu.setIcon(R.drawable.ic_image_white_24dp)
        }
    }

    private fun fadeView(view: View, fadeIn: Boolean, listener: AnimatorListenerAdapter?) {
        val startAlpha = if (fadeIn) 0f else 1f
        val stopAlpha = 1f - startAlpha

        val fade = ValueAnimator.ofFloat(startAlpha, stopAlpha)
        fade.addUpdateListener { valueAnimator ->
            view.alpha = valueAnimator.animatedValue as Float
        }
        fade.duration = 1000
        listener?.let { fade.addListener(it) }
        fade.start()
    }
}
