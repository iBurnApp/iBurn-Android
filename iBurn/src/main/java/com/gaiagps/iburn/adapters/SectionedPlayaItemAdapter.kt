package com.gaiagps.iburn.adapters

import android.content.Context
import android.support.annotation.NonNull
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.gaiagps.iburn.R
import com.gaiagps.iburn.database.*
import com.tonicartos.superslim.LayoutManager
import com.tonicartos.superslim.LinearSLM
import timber.log.Timber

const val ViewTypeHeader = 0
const val ViewTypeContent = 1

/**
 * Created by dbro on 6/13/17.
 */
abstract class SectionedPlayaItemAdapter(context: Context, listener: AdapterListener) :
        PlayaItemAdapter<RecyclerView.ViewHolder>(context, listener) {

    override var items: List<PlayaItem>? = null
        set(value) {
            field = value

            if (value != null) {
                headerPositions = createHeaderPositionsForItems(value)
            } else {
                headerPositions = null
            }
            notifyDataSetChanged()
        }

    /**
     * content position -> header position
     */
    private var positionToHeaderPosition = HashMap<Int, Int>()

    /**
     * content position -> header position
     */
    private var positionToDataPosition = HashMap<Int, Int>()

    private var headerPositions: Set<Int>? = null
        set(value) {
            field = value

            positionToDataPosition.clear()
            positionToHeaderPosition.clear()

            value?.let { headerPositions ->

                var headerCount = 0
                var lastHeaderPos = 0
                // Note position 0 is always the first header
                for (position in 0..(itemCount - 1)) {
                    if (headerPositions.contains(position)) {
                        lastHeaderPos = position
                        headerCount++
                    } else {
                        val dataPosition = position - headerCount
                        if (dataPosition >= items?.size ?: 0) {
                            Timber.e("WARNING: Data position for position $position is greater than items size (${items?.size ?: 0}) : $dataPosition")
                        }
                        positionToDataPosition[position] = position - headerCount
                    }
                    positionToHeaderPosition[position] = lastHeaderPos
                }
                //Timber.d("${items?.size} items with ${headerPositions.size} headers for a total count of $itemCount")
            }
        }

    //<editor-fold desc="Client Provided Implementation">

    abstract fun createHeaderPositionsForItems(items: List<PlayaItem>): Set<Int>

    protected open fun onBindContentViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int, dataPosition: Int) {
        super.onBindViewHolder(viewHolder, dataPosition)
        setLinearSlimParameters(viewHolder, position)
    }

    abstract fun onBindHeaderViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int)

    protected open fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.listview_header_item, parent, false)

        return HeaderViewHolder(itemView)
    }

    protected open fun onCreateContentViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        // PlayaItemAdapter doesn't have itemTypes, so the second parameter value isn't
        // currently necessary
        return super.onCreateViewHolder(parent, ViewTypeHeader)
    }

    //</editor-fold desc="Client Provided Implementation">

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            RecyclerView.ViewHolder {

        if (viewType == ViewTypeHeader) {
            return onCreateHeaderViewHolder(parent)
        } else if (viewType == ViewTypeContent) {
            return onCreateContentViewHolder(parent)
        } else {
            throw IllegalStateException("Invalid view type " + viewType)
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {

        if (isHeaderPosition(position)) {
            onBindHeaderViewHolder(viewHolder, position)
        } else {
            val dataPosition = getDataPositionForPosition(position)
            onBindContentViewHolder(viewHolder, position, dataPosition)
        }
    }

    protected fun setLinearSlimParameters(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val params = viewHolder.itemView.layoutParams as LayoutManager.LayoutParams
        params.setSlm(LinearSLM.ID)
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.firstPosition = getHeaderPositionForPosition(position)
        viewHolder.itemView.layoutParams = params
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHeaderPosition(position)) ViewTypeHeader else ViewTypeContent
    }

    override fun getItemCount(): Int {
        val itemCount = super.getItemCount()
        if (itemCount > 0) {
            return itemCount + (headerPositions?.size ?: 0)
        }
        return itemCount
    }

    override fun getItemId(position: Int): Long {
        if (isHeaderPosition(position)) {
            return getHeaderId(position)
        }
        return super.getItemId(getDataPositionForPosition(position))
    }

    fun getHeaderId(position: Int): Long {
        // return something unlikely to conflict with database ids
        // TODO : Explicitly assign ids for every position
        return Long.MAX_VALUE - position
    }

    fun isHeaderPosition(position: Int): Boolean {
        // If a position's header position is itself, it is a header
        return headerPositions?.contains(position) ?: false
    }

    /**
     * @return the position of the header for the corresponding item position.
     * The value will be less than or equal to [position].
     */
    fun getHeaderPositionForPosition(position: Int): Int {
        return positionToHeaderPosition[position] ?: -1
    }

    /**
     * @return the cursor position for the corresponding item position. Compensate for the presence of headers
     * e.g: Position 1 is cursor position 0, because position 0 is always the first header
     */
    fun getDataPositionForPosition(position: Int): Int {
        val dataPosition =  positionToDataPosition[position] ?: -1
        //Timber.d("Translating position $position to data position $dataPosition")
        return dataPosition
    }

    open class HeaderViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}