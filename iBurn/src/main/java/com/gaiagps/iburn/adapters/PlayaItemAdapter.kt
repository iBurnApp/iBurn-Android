package com.gaiagps.iburn.adapters

import android.content.Context
import android.location.Location
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import com.gaiagps.iburn.*
import com.gaiagps.iburn.DateUtil.getDateString
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter
import com.gaiagps.iburn.database.*
import com.gaiagps.iburn.location.LocationProvider
import org.jetbrains.annotations.NotNull
import timber.log.Timber
import java.text.ParseException
import java.util.*

/**
 * Facilities the display of a collection of [PlayaItem]s in a [RecyclerView]
 * Created by dbro on 6/7/17.
 */
open class PlayaItemAdapter<T: androidx.recyclerview.widget.RecyclerView.ViewHolder>(
        val context: Context,
        val listener: AdapterListener) :
        androidx.recyclerview.widget.RecyclerView.Adapter<T>(), SectionIndexer {

    protected val apiDateFormat = PlayaDateTypeAdapter.buildIso8601Format()

    var sectionIndexer: PlayaItemSectionIndxer? = null

    open var items: List<PlayaItemWithUserData>? = null
        set(value) {
            field = value
            sectionIndexer?.items = value
            isEmbargoActive = Embargo.isEmbargoActive(prefs)
            notifyDataSetChanged()
        }

    private val normalPaddingBottom: Int
    private val lastItemPaddingBottom: Int
    private var deviceLocation: Location? = null
    private val now = CurrentDateProvider.getCurrentDate()
    private val prefs = PrefsHelper(context)
    private var isEmbargoActive = Embargo.isEmbargoActive(prefs)

    init {
        // TODO : Trigger re-draw when location available / changed?
        LocationProvider.getLastLocation(context.applicationContext).subscribe({
                lastLocation -> deviceLocation = lastLocation
       }, {error -> Timber.e(error, "Failed to get last location")})

        normalPaddingBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, context.resources.displayMetrics).toInt()
        lastItemPaddingBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, context.resources.displayMetrics).toInt()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.listview_playaitem, parent, false)

        val viewHolder = ViewHolder(view)
        setupClickListeners(viewHolder)
        return viewHolder as T
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    override fun onBindViewHolder(viewHolder: T, position: Int) {
        val item = items?.get(position)

        item?.let { itemWithUserData ->
            val item = itemWithUserData.item
            val holder = (viewHolder as ViewHolder)

            var startDate: Date? = null
            var endDate: Date? = null

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

            } else if (item is Event) {
                holder.eventTypeView.visibility = View.VISIBLE
                holder.eventTimeView.visibility = View.VISIBLE

                holder.eventTypeView.text = AdapterUtils.getStringForEventType(item.type)

                try {
                    startDate = apiDateFormat.parse(item.startTime)
                    endDate = apiDateFormat.parse(item.endTime)
                    holder.eventTimeView.text =
                            getDateString(context, now, startDate, item.startTimePretty, endDate, item.endTimePretty)
                } catch (e: ParseException) {
                    Timber.e(e, "Failed to parse event dates")
                    holder.eventTimeView.text = item.startTimePretty
                }

                holder.artistView.visibility = View.GONE
                holder.audioTourView.visibility = View.GONE
                holder.showImage(false)
            } else {
                Timber.e("Unknown Item type! Display behavior will be unexpected")
            }

            holder.titleView.text = item.name
            holder.descView.text = item.description

            val canShowOfficialLocation = !isEmbargoActive && item.hasLocation()
            val canShowLocation = canShowOfficialLocation || item.hasUnofficialLocation()

            if (!canShowLocation) {

                holder.addressView.visibility = View.GONE
                holder.bikeTimeView.visibility = View.GONE
                holder.walkTimeView.visibility = View.GONE

            } else {

                val lat = if (canShowOfficialLocation) item.latitude else item.latitudeUnofficial
                val lon = if (canShowOfficialLocation) item.longitude else item.longitudeUnofficial
                val address = if (canShowOfficialLocation) item.playaAddress else item.playaAddressUnofficial

                // Sets Walk and Bike time, hiding views if item.latitude / longitude is 0
                AdapterUtils.setDistanceText(deviceLocation, now, startDate, endDate, holder.walkTimeView, holder.bikeTimeView,
                        lat, lon)

                if (!TextUtils.isEmpty(address)) {
                    holder.addressView.visibility = View.VISIBLE
                    if (!canShowOfficialLocation) {
                        holder.addressView.text = "BurnerMap: " + address
                    } else {
                        holder.addressView.text = address
                    }
                } else {
                    holder.addressView.visibility = View.GONE
                    holder.bikeTimeView.visibility = View.GONE
                    holder.walkTimeView.visibility = View.GONE
                }
            }

            Timber.d("Binding item: ${item.playaId} favorite ${itemWithUserData.userData.isFavorite}")
            if (itemWithUserData.userData.isFavorite) {
                holder.favoriteView.setImageResource(R.drawable.ic_heart_full)
            } else {
                holder.favoriteView.setImageResource(R.drawable.ic_heart_empty)
            }

            holder.itemView.tag = itemWithUserData

            if (position == items?.lastIndex) {
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
        }
    }

    /**
     * Convenience method to setup item click and favorite button click.
     * Splendidly suitable for calling from [.onCreateViewHolder]
     */
    protected fun setupClickListeners(viewHolder: ViewHolder) {
        viewHolder.itemView.setOnClickListener({ view ->
            if (viewHolder.itemView.tag != null) {
                listener.onItemSelected(view.tag as PlayaItemWithUserData)
            }
        })

        viewHolder.favoriteView.setOnClickListener({ view ->
            if (viewHolder.itemView.tag != null) {
                Timber.d("Favorite btn clicked")
                listener.onItemFavoriteButtonSelected((viewHolder.itemView.tag as PlayaItemWithUserData).item)
            } else {
                Timber.e("Favorite btn clicked, but itemView.tag is null")
            }
        })
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

    open class ViewHolder(val view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

        val imageView: ImageView = view.findViewById(R.id.image)
        val imageMaskView: ImageView = view.findViewById(R.id.imageMask)
        val titleView: TextView = view.findViewById(R.id.title)
        val artistView: TextView = view.findViewById(R.id.artist)
        val audioTourView: TextView = view.findViewById(R.id.audioTourLabel)
        val descView: TextView  = view.findViewById(R.id.description)
        val eventTypeView: TextView = view.findViewById(R.id.type)
        val eventTimeView: TextView = view.findViewById(R.id.time)

        val favoriteView: ImageView = view.findViewById(R.id.heart)
        val addressView: TextView = view.findViewById(R.id.address)

        val walkTimeView: TextView = view.findViewById(R.id.walk_time)
        val bikeTimeView: TextView = view.findViewById(R.id.bike_time)

        fun showImage(doShow: Boolean) {
            val visibility = if (doShow) View.VISIBLE else View.GONE
            imageView.visibility = visibility
            imageMaskView.visibility = visibility
        }
    }

}