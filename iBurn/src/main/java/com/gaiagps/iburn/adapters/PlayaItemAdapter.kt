package com.gaiagps.iburn.adapters

import android.content.Context
import android.graphics.Rect
import android.location.Location
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import com.gaiagps.iburn.*
import com.gaiagps.iburn.DateUtil.getDateString
import com.gaiagps.iburn.database.*
import com.gaiagps.iburn.location.LocationProvider
import com.gaiagps.iburn.view.animateScalePulse
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.util.*

internal object LocationChangedPayload
internal object FavoriteChangedPayload

/**
 * Facilities the display of a collection of [PlayaItem]s in a [RecyclerView]
 * Created by dbro on 6/7/17.
 */
open class PlayaItemAdapter(
        val context: Context,
        val listener: AdapterListener) :
        RecyclerView.Adapter<PlayaItemAdapter.ViewHolder>(), SectionIndexer {

    var sectionIndexer: PlayaItemSectionIndxer? = null

    open var items: List<PlayaItemWithUserData>? = null
        set(value) {
            val oldItems = field
            field = value
            sectionIndexer?.items = value
            notifyItemsChanged(oldItems, value)
        }

    private val normalPaddingBottom: Int
    private val lastItemPaddingBottom: Int
    private var deviceLocation: Location? = null
    private val now = CurrentDateProvider.getCurrentDate()
    private val prefs = PrefsHelper(context)
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var locationJob: kotlinx.coroutines.Job? = null
    // Foreground list-friendly request (balanced accuracy, moderate cadence)
    private val locationRequest: LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()

    /**
     * Room re-runs the entire joined query when a favorite changes. If the result still contains
     * the same items in the same order, only rebind rows whose joined user data changed. A full
     * refresh needlessly rebinds every visible row (including images) and can visibly disturb the
     * list while a favorite is being toggled.
     *
     * Subclasses with headers can translate data positions to adapter positions.
     */
    protected open fun notifyItemsChanged(
        oldItems: List<PlayaItemWithUserData>?,
        newItems: List<PlayaItemWithUserData>?
    ) {
        val changedPositions = changedPositionsIfStructureIsUnchanged(oldItems, newItems)
        if (changedPositions == null) {
            notifyDataSetChanged()
        } else {
            changedPositions.forEach { position ->
                notifyItemChanged(position, FavoriteChangedPayload)
            }
        }
    }

    protected fun changedPositionsIfStructureIsUnchanged(
        oldItems: List<PlayaItemWithUserData>?,
        newItems: List<PlayaItemWithUserData>?
    ): List<Int>? {
        if (oldItems == null || newItems == null || oldItems.size != newItems.size) return null
        if (oldItems.indices.any { oldItems[it].item != newItems[it].item }) return null

        return oldItems.indices.filter { oldItems[it] != newItems[it] }
    }

    init {
        normalPaddingBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics).toInt()
        lastItemPaddingBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, context.resources.displayMetrics).toInt()
    }

    fun startMonitoringLocation() {
        if (locationJob != null) return
        locationJob = LocationProvider.currentLocationFlow(context.applicationContext, locationRequest)
            .onEach { lastLocation ->
                Timber.d("Updating device location $lastLocation")
                val prior = deviceLocation
                val deltaMeters = prior?.distanceTo(lastLocation) ?: Float.MAX_VALUE
                deviceLocation = lastLocation
                if (prior == null || deltaMeters > 61f) { // ~200 feet / 1 minute of walking distance
                    notifyLocationChanged()
                }
            }
            .catch { error ->
                Timber.e(error, "Failed to get last location")
            }
            .launchIn(adapterScope)
    }

    fun stopMonitoringLocation() {
        locationJob?.cancel()
        locationJob = null
    }

    /**
     * Location changes only affect distance labels. Using a payload avoids restarting image loads
     * and changing image visibility while otherwise unchanged rows are rebound.
     */
    protected open fun notifyLocationChanged() {
        notifyItemRangeChanged(0, itemCount, LocationChangedPayload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.listview_playaitem, parent, false)

        val viewHolder = ViewHolder(view)
        // Only setup the main item click listener here (it's position-agnostic)
        viewHolder.itemView.setOnClickListener({ view ->
            if (viewHolder.itemView.tag != null) {
                listener.onItemSelected(view.tag as PlayaItemWithUserData)
            }
        })


        return viewHolder
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    private fun expandFavButtonHitbox(viewHolder: ViewHolder) {
        // For a visually pleasing layout the favorite button needs to be smaller
        // then is comfortable for a touch hitbox, so manually expand that on the parent view
        val delegateArea = Rect()
        val favButton = viewHolder.itemView.findViewById<ImageView>(R.id.heart)
        favButton.getHitRect(delegateArea)
        favButton.bringToFront()

        // Expand touch area by 40 pixels in all directions
        delegateArea.top -= 40
        delegateArea.bottom += 40
        delegateArea.left -= 40
        delegateArea.right += 40

        viewHolder.itemView.touchDelegate = TouchDelegate(delegateArea, favButton)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = items?.get(position)
        val isLastItem = position == items?.lastIndex

        item?.let { itemWithUserData ->
            val item = itemWithUserData.item
            val holder = (viewHolder as ViewHolder)

            if (item is Art) {
                holder.artistView.visibility = View.VISIBLE
                holder.artistView.text = item.artist

                if (item.hasAudioTour(context)) {
                    holder.audioTourView.visibility = View.VISIBLE
                } else {
                    holder.audioTourView.visibility = View.GONE
                }

                holder.eventTypeView.visibility = View.GONE
                holder.eventTimeView.visibility = View.GONE
                holder.showImage(true)
                loadArtImage(item, holder.imageView, object: Callback {
                    override fun onSuccess() {
                        //no-op
                    }

                    override fun onError() {
                        holder.showImage(false)
                    }
                })

            } else if (item is Camp) {
                holder.artistView.visibility = View.GONE
                holder.audioTourView.visibility = View.GONE
                holder.eventTypeView.visibility = View.GONE
                holder.eventTimeView.visibility = View.GONE
                holder.showImage(false)

            } else if (item is MutantVehicle) {
                holder.artistView.visibility = if (item.artist.isNullOrEmpty()) View.GONE else View.VISIBLE
                holder.artistView.text = item.artist
                holder.audioTourView.visibility = View.GONE
                holder.eventTypeView.visibility = View.GONE
                holder.eventTimeView.visibility = View.GONE
                holder.showImage(item.hasImage())
                if (item.hasImage()) {
                    loadMutantVehicleImage(item, holder.imageView, object : Callback {
                        override fun onSuccess() = Unit
                        override fun onError() = holder.showImage(false)
                    })
                }

            } else if (item is Event) {
                holder.eventTypeView.visibility = View.VISIBLE
                holder.eventTimeView.visibility = View.VISIBLE

                holder.eventTypeView.text = AdapterUtils.getStringForEventType(item.type)

                val startDate = item.startDate
                val endDate = item.endDate
                holder.eventTimeView.text =
                    getDateString(
                        context,
                        now,
                        startDate,
                        item.startTimePretty,
                        endDate,
                        item.endTimePretty
                    )

                holder.artistView.visibility = View.GONE
                holder.audioTourView.visibility = View.GONE
                holder.showImage(false)
            } else {
                Timber.e("Unknown Item type! Display behavior will be unexpected")
            }

            holder.titleView.text = item.name
            holder.descView.text = item.description

            bindLocation(viewHolder, item)

            bindFavorite(holder, itemWithUserData)

            holder.itemView.tag = itemWithUserData

            if (isLastItem) {
                // Set footer padding
                holder.itemView.setPadding(normalPaddingBottom,
                        normalPaddingBottom,
                        normalPaddingBottom,
                        lastItemPaddingBottom)
            } else {
                // Set default padding
                holder.itemView.setPadding(normalPaddingBottom,
                        normalPaddingBottom,
                        normalPaddingBottom,
                        normalPaddingBottom)
            }
            holder.itemView.post {
                expandFavButtonHitbox(holder)
            }
        }
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val item = items?.getOrNull(position)
            if (item != null && payloads.all {
                    it === LocationChangedPayload || it === FavoriteChangedPayload
                }) {
                if (payloads.any { it === LocationChangedPayload }) {
                    bindLocation(viewHolder, item.item)
                }
                if (payloads.any { it === FavoriteChangedPayload }) {
                    bindFavorite(viewHolder, item)
                }
                return
            }
        }
        onBindViewHolder(viewHolder, position)
    }

    private fun bindFavorite(holder: ViewHolder, item: PlayaItemWithUserData) {
        var isFavorite = item.userData.isFavorite
        holder.favoriteView.setImageResource(
            if (isFavorite) R.drawable.ic_heart_full_24dp else R.drawable.ic_heart_empty_24dp
        )
        holder.favoriteView.setOnClickListener { view ->
            isFavorite = !isFavorite
            (view as ImageView).setImageResource(
                if (isFavorite) R.drawable.ic_heart_full_24dp else R.drawable.ic_heart_empty_24dp
            )
            if (isFavorite) {
                view.animateScalePulse()
            }
            listener.onItemFavoriteButtonSelected(item.item)
        }
    }

    private fun bindLocation(holder: ViewHolder, item: PlayaItem) {
        val canShowOfficialLocation =
            !Embargo.isEmbargoActiveForPlayaItem(prefs, item) && item.hasLocation()
        val canShowOfficialAddress =
            !Embargo.isAddressEmbargoActiveForPlayaItem(prefs, item) &&
                !TextUtils.isEmpty(item.playaAddress)
        val canShowUnofficialLocation = item.hasUnofficialLocation()

        if (!canShowOfficialLocation && !canShowUnofficialLocation) {
            holder.bikeTimeView.visibility = View.GONE
            holder.walkTimeView.visibility = View.GONE
        } else {
            val lat = if (canShowOfficialLocation) item.latitude else item.latitudeUnofficial
            val lon = if (canShowOfficialLocation) item.longitude else item.longitudeUnofficial
            val event = item as? Event

            AdapterUtils.setDistanceText(
                deviceLocation,
                now,
                event?.startDate,
                event?.endDate,
                holder.walkTimeView,
                holder.bikeTimeView,
                lat,
                lon
            )
        }

        when {
            canShowOfficialAddress -> {
                holder.addressView.visibility = View.VISIBLE
                holder.addressView.text = item.playaAddress
            }
            !TextUtils.isEmpty(item.playaAddressUnofficial) -> {
                holder.addressView.visibility = View.VISIBLE
                holder.addressView.text = "BurnerMap: ${item.playaAddressUnofficial}"
            }
            else -> {
                holder.addressView.visibility = View.GONE
            }
        }
    }


    // <editor-fold desc="SectionIndexer">

    override fun getSections(): Array<Any> {
        val sections = sectionIndexer?.sections

        if (sections?.isEmpty() ?: false) {
            return arrayOf(" ¯\\_(ツ)_/¯") as Array<Any>
        }
        return sections!!
    }

    override fun getSectionForPosition(position: Int): Int {
        return sectionIndexer?.getSectionForPosition(position) ?: 0
    }

    override fun getPositionForSection(position: Int): Int {
        return sectionIndexer?.getPositionForSection(position) ?: 0
    }

    // </editor-fold desc="SectionIndexer">


    open class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        val imageView: ImageView by lazy { view.findViewById(R.id.image) }
        val imageMaskView: ImageView by lazy { view.findViewById(R.id.imageMask) }
        val titleView: TextView by lazy {view.findViewById(R.id.title) }
        val artistView: TextView by lazy {view.findViewById(R.id.artist) }
        val audioTourView: TextView by lazy {view.findViewById(R.id.audioTourLabel) }
        val descView: TextView  by lazy {view.findViewById(R.id.description) }
        val eventTypeView: TextView by lazy {view.findViewById(R.id.type) }
        val eventTimeView: TextView by lazy {view.findViewById(R.id.time) }

        val favoriteView: ImageView by lazy {view.findViewById(R.id.heart) }
        val addressView: TextView by lazy {view.findViewById(R.id.address) }

        val walkTimeView: TextView by lazy {view.findViewById(R.id.walk_time) }
        val bikeTimeView: TextView by lazy {view.findViewById(R.id.bike_time) }

        fun showImage(doShow: Boolean) {
            val visibility = if (doShow) View.VISIBLE else View.GONE
            imageView.visibility = visibility
            imageMaskView.visibility = visibility
        }
    }

}
